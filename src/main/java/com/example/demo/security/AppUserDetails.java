package com.example.demo.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;

public class AppUserDetails implements UserDetails {
    private final String username;
    private final String password;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final Collection<? extends GrantedAuthority> authorities;
    private final LocalDateTime lastPasswordChange;

    public AppUserDetails(
        String username,
        String password,
        boolean enabled,
        boolean accountNonLocked,
        Collection<? extends GrantedAuthority> authorities,
        LocalDateTime lastPasswordChange
    ) {
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.accountNonLocked = accountNonLocked;
        this.authorities = authorities;
        this.lastPasswordChange = lastPasswordChange;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public LocalDateTime getLastPasswordChange() {
        return lastPasswordChange;
    }
}
