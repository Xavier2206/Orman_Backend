package com.orman.backend.contract.repository;

import com.orman.backend.contract.dto.request.CuotaListCriteria;
import java.time.LocalDate;

public interface CuotaListRepository {

    CuotaListPage searchOwned(Integer codperPropietaria, CuotaListCriteria criteria, LocalDate hoy,
                              LocalDate fechaLimite);
}
