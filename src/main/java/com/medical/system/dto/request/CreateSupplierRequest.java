package com.medical.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateSupplierRequest {
    @NotBlank(message = "供应商名称不能为空")
    private String supplierName;
    @NotBlank(message = "供应商编码不能为空")
    private String supplierCode;
    private String contactPerson;
    private String phone;
    private String email;
    private String address;
    private String licenseNo;
    private LocalDate licenseExpiry;
}
