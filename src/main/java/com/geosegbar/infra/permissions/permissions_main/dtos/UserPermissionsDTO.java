package com.geosegbar.infra.permissions.permissions_main.dtos;

import java.util.List;

import com.geosegbar.entities.AttributionsPermissionEntity;
import com.geosegbar.entities.DamPermissionEntity;
import com.geosegbar.entities.DocumentationPermissionEntity;
import com.geosegbar.entities.InstrumentationPermissionEntity;
import com.geosegbar.entities.RoutineInspectionPermissionEntity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserPermissionsDTO {
    private DocumentationPermissionEntity documentationPermission;
    private AttributionsPermissionEntity attributionsPermission;
    private InstrumentationPermissionEntity instrumentationPermission;
    private RoutineInspectionPermissionEntity routineInspectionPermission;
    private List<DamPermissionEntity> damPermissions;
}
