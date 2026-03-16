package com.medical.system.exception;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {
    private final String resourceName;
    private final Object resourceId;

    public ResourceNotFoundException(String resourceName, Object resourceId) {
        super(resourceName + "不存在，ID: " + resourceId);
        this.resourceName = resourceName;
        this.resourceId = resourceId;
    }
}
