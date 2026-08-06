package com.orman.backend.role.service;

import com.orman.backend.role.dto.response.RolMeResponse;
import java.util.List;

public interface RolMeService {
    RolMeResponse assign(Integer codr, Integer codm);
    void remove(Integer codr, Integer codm);
    List<RolMeResponse> listByRol(Integer codr);
    List<RolMeResponse> listByMenu(Integer codm);
}
