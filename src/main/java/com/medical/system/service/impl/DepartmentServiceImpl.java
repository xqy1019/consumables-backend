package com.medical.system.service.impl;

import com.medical.system.dto.request.CreateDepartmentRequest;
import com.medical.system.dto.response.DepartmentResponse;
import com.medical.system.entity.Department;
import com.medical.system.exception.BusinessException;
import com.medical.system.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl {

    private final DepartmentRepository departmentRepository;

    public List<DepartmentResponse> getDepartmentTree() {
        List<Department> all = departmentRepository.findByStatusOrderByLevelAscDeptNameAsc(1);
        Map<Long, DepartmentResponse> map = all.stream()
                .collect(Collectors.toMap(Department::getId, this::convertToResponse));

        List<DepartmentResponse> roots = new ArrayList<>();
        for (Department dept : all) {
            DepartmentResponse resp = map.get(dept.getId());
            if (dept.getParentId() == null) {
                roots.add(resp);
            } else {
                DepartmentResponse parent = map.get(dept.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) parent.setChildren(new ArrayList<>());
                    parent.getChildren().add(resp);
                }
            }
        }
        return roots;
    }

    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findByStatus(1).stream()
                .map(this::convertToResponse).collect(Collectors.toList());
    }

    public DepartmentResponse getDepartmentById(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new com.medical.system.exception.BusinessException("科室不存在"));
        return convertToResponse(dept);
    }

    @Transactional
    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
        if (departmentRepository.existsByDeptCode(request.getDeptCode())) {
            throw new BusinessException("科室编码已存在");
        }
        Department dept = new Department();
        dept.setDeptName(request.getDeptName());
        dept.setDeptCode(request.getDeptCode());
        dept.setParentId(request.getParentId());
        dept.setLevel(request.getLevel());
        dept.setDescription(request.getDescription());
        return convertToResponse(departmentRepository.save(dept));
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long id, CreateDepartmentRequest request) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("科室不存在"));
        dept.setDeptName(request.getDeptName());
        dept.setDescription(request.getDescription());
        dept.setParentId(request.getParentId());
        return convertToResponse(departmentRepository.save(dept));
    }

    @Transactional
    public void deleteDepartment(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("科室不存在"));
        dept.setStatus(0);
        departmentRepository.save(dept);
    }

    public DepartmentResponse convertToResponse(Department dept) {
        DepartmentResponse response = new DepartmentResponse();
        response.setId(dept.getId());
        response.setDeptName(dept.getDeptName());
        response.setDeptCode(dept.getDeptCode());
        response.setParentId(dept.getParentId());
        response.setLevel(dept.getLevel());
        response.setDescription(dept.getDescription());
        response.setStatus(dept.getStatus());
        response.setCreateTime(dept.getCreateTime());
        if (dept.getParentId() != null) {
            departmentRepository.findById(dept.getParentId())
                    .ifPresent(p -> response.setParentName(p.getDeptName()));
        }
        return response;
    }
}
