package com.orman.backend.authorization.service.impl;

import com.orman.backend.authorization.service.OwnerProtectionService;
import com.orman.backend.common.exception.LastOwnerRequiredException;
import com.orman.backend.role.entity.Rol;
import com.orman.backend.role.repository.RolRepository;
import com.orman.backend.role.repository.RolUsuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OwnerProtectionServiceImpl implements OwnerProtectionService {

    static final String OWNER_ROLE_NAME = "PROPIETARIO";
    private static final short ACTIVE = 1;

    private final RolRepository rolRepository;
    private final RolUsuRepository rolUsuRepository;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void assertCanDeactivateUser(String login) {
        Rol ownerRole = lockOwnerRole();
        if (isActive(ownerRole) && rolUsuRepository.existsActiveOwnerByLogin(login)) {
            requireAnotherActiveOwner();
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void assertCanDeactivatePerson(Integer codper) {
        Rol ownerRole = lockOwnerRole();
        if (isActive(ownerRole) && rolUsuRepository.existsActiveOwnerByPerson(codper)) {
            requireAnotherActiveOwner();
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void assertCanRemoveAssignment(String login, Integer codr) {
        Rol ownerRole = lockOwnerRole();
        if (ownerRole != null && ownerRole.getCodr().equals(codr) && isActive(ownerRole)
                && rolUsuRepository.existsActiveOwnerByLogin(login)) {
            requireAnotherActiveOwner();
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void assertCanModifyProtectedRole(Integer codr) {
        Rol ownerRole = lockOwnerRole();
        if (ownerRole != null && ownerRole.getCodr().equals(codr)) {
            throw new LastOwnerRequiredException();
        }
    }

    private Rol lockOwnerRole() {
        return rolRepository.findByNombreForUpdate(OWNER_ROLE_NAME).orElse(null);
    }

    private boolean isActive(Rol ownerRole) {
        return ownerRole != null && Short.valueOf(ACTIVE).equals(ownerRole.getEstado());
    }

    private void requireAnotherActiveOwner() {
        if (rolUsuRepository.countActiveOwners() <= 1) {
            throw new LastOwnerRequiredException();
        }
    }
}
