package com.orman.backend.user.service;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.user.dto.ChangePasswordRequest;
import com.orman.backend.user.dto.CreateUsuarioRequest;
import com.orman.backend.user.dto.UpdateUsuarioRequest;
import com.orman.backend.user.dto.UsuarioResponse;
import org.springframework.data.domain.Pageable;

public interface UsuarioService {

    UsuarioResponse create(CreateUsuarioRequest request);

    UsuarioResponse get(String login);

    PageResponse<UsuarioResponse> list(Pageable pageable);

    PageResponse<UsuarioResponse> list(String q, Pageable pageable);

    PageResponse<UsuarioResponse> listCommon(Pageable pageable);

    PageResponse<UsuarioResponse> listCommon(String q, Pageable pageable);

    UsuarioResponse update(String login, UpdateUsuarioRequest request);

    UsuarioResponse deactivate(String login);

    UsuarioResponse activate(String login);

    void changePassword(String login, ChangePasswordRequest request);
}
