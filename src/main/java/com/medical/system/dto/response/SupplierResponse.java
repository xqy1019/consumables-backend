package com.medical.system.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class SupplierResponse {
    private Long id;
    private String supplierName;
    private String supplierCode;
    private String contactPerson;
    private String phone;
    private String email;
    private String address;
    private String licenseNo;
    private LocalDate licenseExpiry;
    private Integer status;
    private LocalDateTime createTime;
}
