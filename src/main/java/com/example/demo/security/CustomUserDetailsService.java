package com.example.demo.security;

import com.example.demo.entity.Permission;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        for (Permission permission : user.getRole().getPermissions()) {
            authorities.add(new SimpleGrantedAuthority(permission.name()));
        }

        boolean notLocked = user.getAccountLockedUntil() == null || user.getAccountLockedUntil().isBefore(LocalDateTime.now());

        return new AppUserDetails(
            user.getUsername(),
            user.getPassword(),
            user.isEnabled(),
            notLocked,
            authorities,
            user.getLastPasswordChange()
        );
    }
}
