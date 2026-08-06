package com.orman.backend.menu.service;

import com.orman.backend.menu.dto.response.MeProResponse;
import java.util.List;

public interface MeProService {
    MeProResponse assign(Integer codm, Integer codp);
    void remove(Integer codm, Integer codp);
    List<MeProResponse> listByMenu(Integer codm);
    List<MeProResponse> listByProceso(Integer codp);
}
