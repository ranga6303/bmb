package com.example.demo.dto;

import com.example.demo.entity.Role;
import jakarta.validation.constraints.NotNull;

public class AssignRoleRequest {

    @NotNull(message = "Role must not be null")
    private Role role;

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
