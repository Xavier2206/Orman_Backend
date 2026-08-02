package com.orman.backend.role.service;

import com.orman.backend.role.dto.response.RolUsuResponse;

import java.util.List;

public interface RolUsuService {

    RolUsuResponse assign(String login, Integer codr);

    void remove(String login, Integer codr);

    List<RolUsuResponse> listByUsuario(String login);

    List<RolUsuResponse> listByRol(Integer codr);
}
