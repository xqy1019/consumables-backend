package com.medical.system.controller;

import com.medical.system.common.Result;
import com.medical.system.service.impl.TraceServiceImpl;
import com.medical.system.service.impl.TraceServiceImpl.PatientTraceVO;
import com.medical.system.service.impl.TraceServiceImpl.MaterialTraceVO;
import com.medical.system.service.impl.TraceServiceImpl.UdiTraceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trace")
@RequiredArgsConstructor
public class TraceController {

    private final TraceServiceImpl traceService;

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAuthority('menu:tracing')")
    public Result<PatientTraceVO> traceByPatient(@PathVariable String patientId) {
        return Result.success(traceService.traceByPatient(patientId));
    }

    @GetMapping("/material/{materialId}")
    @PreAuthorize("hasAuthority('menu:tracing')")
    public Result<MaterialTraceVO> traceByMaterial(@PathVariable Long materialId) {
        return Result.success(traceService.traceByMaterial(materialId));
    }

    @GetMapping("/udi/{udiCode}")
    @PreAuthorize("hasAuthority('menu:tracing')")
    public Result<UdiTraceVO> traceByUdi(@PathVariable String udiCode) {
        return Result.success(traceService.traceByUdi(udiCode));
    }
}
