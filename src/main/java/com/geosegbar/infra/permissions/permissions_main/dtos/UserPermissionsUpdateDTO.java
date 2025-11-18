package com.geosegbar.infra.permissions.permissions_main.dtos;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserPermissionsUpdateDTO {

    @NotNull(message = "O ID do usuário é obrigatório")
    private Long userId;

    private DocumentationPermissionUpdateDTO documentationPermission;
    private AttributionsPermissionUpdateDTO attributionsPermission;
    private InstrumentationPermissionUpdateDTO instrumentationPermission;
    private RoutineInspectionPermissionUpdateDTO routineInspectionPermission;
    private List<Long> damIds;

    @Data
    public static class DocumentationPermissionUpdateDTO {

        private Boolean viewPSB;
        private Boolean editPSB;
        private Boolean sharePSB;
    }

    @Data
    public static class AttributionsPermissionUpdateDTO {

        private Boolean editUser;
        private Boolean editDam;
        private Boolean editGeralData;
    }

    @Data
    public static class InstrumentationPermissionUpdateDTO {

        private Boolean viewGraphs;
        private Boolean editGraphsLocal;
        private Boolean editGraphsDefault;
        private Boolean viewRead;
        private Boolean editRead;
        private Boolean viewSections;
        private Boolean editSections;
        private Boolean viewInstruments;
        private Boolean editInstruments;
    }

    @Data
    public static class RoutineInspectionPermissionUpdateDTO {

        private Boolean isFillWeb;
        private Boolean isFillMobile;
    }
}
