package com.geosegbar.infra.user.dto;

import java.util.HashSet;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserClientAssociationDTO {
        private Set<Long> clientIds = new HashSet<>();
}
