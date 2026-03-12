package com.medical.system.service.impl;

import com.medical.system.common.PageResult;
import com.medical.system.entity.*;
import com.medical.system.exception.BusinessException;
import com.medical.system.repository.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SurgeryServiceImpl {

    private final SurgeryRecordRepository surgeryRepo;
    private final SurgeryMaterialBindingRepository bindingRepo;
    private final MaterialUdiRepository udiRepo;
    private final MaterialRepository materialRepo;
    private final DepartmentRepository deptRepo;
    private final UserRepository userRepo;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails ud) {
            return userRepo.findByUsername(ud.getUsername()).map(User::getId).orElse(null);
        }
        return null;
    }

    public PageResult<SurgeryVO> getSurgeryList(String keyword, Long deptId, Pageable pageable) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword;
        Page<SurgeryRecord> page = surgeryRepo.findByConditions(kw, deptId, pageable);
        return PageResult.of(page.getContent().stream().map(this::toVO).collect(Collectors.toList()),
                page.getTotalElements(), pageable.getPageNumber() + 1, pageable.getPageSize());
    }

    @Transactional
    public SurgeryVO createSurgery(SurgeryRecord surgery) {
        surgery.setSurgeryNo("SUR" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        surgery.setCreatedBy(getCurrentUserId());
        return toVO(surgeryRepo.save(surgery));
    }

    public SurgeryVO getSurgeryDetail(Long id) {
        SurgeryRecord surgery = surgeryRepo.findById(id)
                .orElseThrow(() -> new BusinessException("手术记录不存在"));
        SurgeryVO vo = toVO(surgery);
        List<SurgeryMaterialBinding> bindings = bindingRepo.findBySurgeryId(id);
        vo.setBindings(bindings.stream().map(b -> {
            BindingVO bvo = new BindingVO();
            bvo.setId(b.getId());
            bvo.setSurgeryId(b.getSurgeryId());
            bvo.setUdiId(b.getUdiId());
            bvo.setMaterialId(b.getMaterialId());
            bvo.setQuantity(b.getQuantity());
            bvo.setUseDate(b.getUseDate());
            bvo.setRemark(b.getRemark());
            materialRepo.findById(b.getMaterialId()).ifPresent(m -> bvo.setMaterialName(m.getMaterialName()));
            if (b.getUdiId() != null) {
                udiRepo.findById(b.getUdiId()).ifPresent(u -> bvo.setUdiCode(u.getUdiCode()));
            }
            return bvo;
        }).collect(Collectors.toList()));
        return vo;
    }

    @Transactional
    public void bindMaterials(Long surgeryId, List<Map<String, Object>> bindings) {
        if (!surgeryRepo.existsById(surgeryId)) throw new BusinessException("手术记录不存在");
        for (Map<String, Object> b : bindings) {
            SurgeryMaterialBinding binding = new SurgeryMaterialBinding();
            binding.setSurgeryId(surgeryId);
            if (b.containsKey("udiId") && b.get("udiId") != null) {
                binding.setUdiId(Long.valueOf(b.get("udiId").toString()));
                udiRepo.findById(binding.getUdiId()).ifPresent(u -> {
                    u.setStatus("USED");
                    udiRepo.save(u);
                });
            }
            binding.setMaterialId(Long.valueOf(b.get("materialId").toString()));
            binding.setQuantity(b.containsKey("quantity") ? Integer.valueOf(b.get("quantity").toString()) : 1);
            binding.setUseDate(LocalDateTime.now());
            if (b.containsKey("remark") && b.get("remark") != null) binding.setRemark(b.get("remark").toString());
            bindingRepo.save(binding);
        }
    }

    private SurgeryVO toVO(SurgeryRecord s) {
        SurgeryVO vo = new SurgeryVO();
        vo.setId(s.getId());
        vo.setSurgeryNo(s.getSurgeryNo());
        vo.setPatientId(s.getPatientId());
        vo.setPatientName(s.getPatientName());
        vo.setPatientAge(s.getPatientAge());
        vo.setPatientGender(s.getPatientGender());
        vo.setDeptId(s.getDeptId());
        vo.setSurgeryDate(s.getSurgeryDate());
        vo.setSurgeryType(s.getSurgeryType());
        vo.setDoctorName(s.getDoctorName());
        vo.setStatus(s.getStatus());
        vo.setRemark(s.getRemark());
        vo.setCreateTime(s.getCreateTime());
        if (s.getDeptId() != null) {
            deptRepo.findById(s.getDeptId()).ifPresent(d -> vo.setDeptName(d.getDeptName()));
        }
        return vo;
    }

    @Data
    public static class SurgeryVO {
        private Long id;
        private String surgeryNo;
        private String patientId;
        private String patientName;
        private Integer patientAge;
        private String patientGender;
        private Long deptId;
        private String deptName;
        private LocalDateTime surgeryDate;
        private String surgeryType;
        private String doctorName;
        private String status;
        private String remark;
        private LocalDateTime createTime;
        private List<BindingVO> bindings;
    }

    @Data
    public static class BindingVO {
        private Long id;
        private Long surgeryId;
        private Long udiId;
        private String udiCode;
        private Long materialId;
        private String materialName;
        private Integer quantity;
        private LocalDateTime useDate;
        private String remark;
    }
}
