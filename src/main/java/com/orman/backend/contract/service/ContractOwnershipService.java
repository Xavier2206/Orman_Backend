package com.orman.backend.contract.service;

import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.property.entity.UnidadEntity;
import org.springframework.security.core.Authentication;

public interface ContractOwnershipService {

    UnidadEntity findOwnedUnidad(Integer coduni, Authentication authentication);

    UnidadEntity findOwnedUnidadForUpdate(Integer coduni, Authentication authentication);

    ContratoEntity findOwnedContrato(Integer codcon, Authentication authentication);

    ContratoEntity findOwnedContratoForUpdate(Integer codcon, Authentication authentication);
}
