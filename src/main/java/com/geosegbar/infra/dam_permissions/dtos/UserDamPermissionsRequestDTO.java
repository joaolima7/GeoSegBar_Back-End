package com.geosegbar.infra.dam_permissions.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDamPermissionsRequestDTO {
    private List<DamPermissionDTO> permissions;
}
