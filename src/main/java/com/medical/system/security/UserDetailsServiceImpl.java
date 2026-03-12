package com.medical.system.security;

import com.medical.system.entity.User;
import com.medical.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameAndStatus(username, 1)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在或已禁用: " + username));

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        // 加载角色
        user.getRoles().forEach(role ->
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getRoleCode())));
        // 加载权限
        user.getRoles().forEach(role ->
                role.getPermissions().forEach(perm ->
                        authorities.add(new SimpleGrantedAuthority(perm.getPermissionCode()))));

        return new CustomUserDetails(
                user.getId(),
                user.getDeptId(),
                user.getUsername(),
                user.getPassword(),
                authorities);
    }
}
