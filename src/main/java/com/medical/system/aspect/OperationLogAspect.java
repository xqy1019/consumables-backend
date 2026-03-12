package com.medical.system.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.system.annotation.Log;
import com.medical.system.dto.request.LoginRequest;
import com.medical.system.entity.OperationLog;
import com.medical.system.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(logAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint, Log logAnnotation) throws Throwable {
        long startTime = System.currentTimeMillis();
        OperationLog operationLog = new OperationLog();
        operationLog.setModule(logAnnotation.module());
        operationLog.setAction(logAnnotation.action());
        operationLog.setOperateTime(LocalDateTime.now());

        // 获取当前用户（优先从 SecurityContext，登录场景从方法参数中提取）
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                operationLog.setUsername(auth.getName());
            } else {
                // 登录等未认证场景：从方法参数中查找 LoginRequest 提取用户名
                for (Object arg : joinPoint.getArgs()) {
                    if (arg instanceof LoginRequest lr) {
                        operationLog.setUsername(lr.getUsername());
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("获取用户信息失败", e);
        }

        // 获取请求信息
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                operationLog.setRequestUrl(request.getRequestURI());
                operationLog.setRequestMethod(request.getMethod());
                operationLog.setIpAddr(getClientIp(request));
            }
        } catch (Exception e) {
            log.debug("获取请求信息失败", e);
        }

        // 记录请求参数
        try {
            Object[] args = joinPoint.getArgs();
            String params = objectMapper.writeValueAsString(args);
            if (params.length() > 2000) {
                params = params.substring(0, 2000) + "...";
            }
            operationLog.setRequestParams(params);
        } catch (Exception e) {
            log.debug("序列化请求参数失败", e);
        }

        Object result = null;
        try {
            result = joinPoint.proceed();
            operationLog.setStatus(1);
            operationLog.setResponseCode(200);
        } catch (Throwable e) {
            operationLog.setStatus(0);
            operationLog.setErrorMsg(e.getMessage() != null ?
                e.getMessage().substring(0, Math.min(e.getMessage().length(), 500)) : "未知错误");
            operationLog.setResponseCode(500);
            throw e;
        } finally {
            operationLog.setDurationMs(System.currentTimeMillis() - startTime);
            operationLogService.saveLog(operationLog);
        }

        return result;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
