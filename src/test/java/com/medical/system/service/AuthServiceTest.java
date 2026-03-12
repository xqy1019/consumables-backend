package com.medical.system.service;

import com.medical.system.dto.request.LoginRequest;
import com.medical.system.dto.response.LoginResponse;
import com.medical.system.entity.Permission;
import com.medical.system.entity.Role;
import com.medical.system.entity.User;
import com.medical.system.repository.DepartmentRepository;
import com.medical.system.repository.UserRepository;
import com.medical.system.security.JwtTokenProvider;
import com.medical.system.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private UserRepository userRepository;
    @Mock private DepartmentRepository departmentRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void testLogin_success() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("Admin@123456");

        User user = createUser();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateToken("admin")).thenReturn("mock-jwt-token");

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getToken());
        assertEquals("admin", response.getUsername());
        assertEquals(1L, response.getUserId());
        assertTrue(response.getRoles().contains("ADMIN"));
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void testLogin_wrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("wrongpass");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("密码错误"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void testLogin_disabledUser() {
        LoginRequest request = new LoginRequest();
        request.setUsername("disabled_user");
        request.setPassword("password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("用户已禁用"));

        assertThrows(DisabledException.class, () -> authService.login(request));
    }

    private User createUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setRealName("管理员");
        user.setPassword("encoded-password");
        user.setDeptId(1L);
        user.setStatus(1);

        Permission perm = new Permission();
        perm.setId(1L);
        perm.setPermissionCode("menu:dashboard");
        perm.setPermissionName("仪表盘");

        Role role = new Role();
        role.setId(1L);
        role.setRoleName("管理员");
        role.setRoleCode("ADMIN");
        role.setPermissions(Set.of(perm));

        user.setRoles(Set.of(role));
        return user;
    }
}
