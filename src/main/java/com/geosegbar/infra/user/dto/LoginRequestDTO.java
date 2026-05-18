package com.geosegbar.infra.user.dto;

import com.geosegbar.common.enums.LoginOriginEnum;

public record LoginRequestDTO(String email, String password, LoginOriginEnum origin) {}
