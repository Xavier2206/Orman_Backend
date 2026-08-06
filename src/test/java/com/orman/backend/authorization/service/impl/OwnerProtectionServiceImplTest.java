package com.orman.backend.authorization.service.impl;

import com.orman.backend.common.exception.LastOwnerRequiredException;
import com.orman.backend.role.entity.Rol;
import com.orman.backend.role.repository.RolRepository;
import com.orman.backend.role.repository.RolUsuRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerProtectionServiceImplTest {

    @Mock private RolRepository rolRepository;
    @Mock private RolUsuRepository rolUsuRepository;
    @InjectMocks private OwnerProtectionServiceImpl service;

    private Rol ownerRole;

    @BeforeEach
    void setUp() {
        ownerRole = new Rol();
        ownerRole.setNombre("PROPIETARIO");
        ownerRole.setEstado((short) 1);
        setCodr(ownerRole, 7);
    }

    @Test
    void blocksEveryOperationThatWouldRemoveTheLastActiveOwner() {
        when(rolRepository.findByNombreForUpdate("PROPIETARIO")).thenReturn(Optional.of(ownerRole));
        when(rolUsuRepository.existsActiveOwnerByLogin("owner")).thenReturn(true);
        when(rolUsuRepository.existsActiveOwnerByPerson(10)).thenReturn(true);
        when(rolUsuRepository.countActiveOwners()).thenReturn(1L);

        assertThatThrownBy(() -> service.assertCanDeactivateUser("owner"))
                .isInstanceOf(LastOwnerRequiredException.class);
        assertThatThrownBy(() -> service.assertCanDeactivatePerson(10))
                .isInstanceOf(LastOwnerRequiredException.class);
        assertThatThrownBy(() -> service.assertCanRemoveAssignment("owner", 7))
                .isInstanceOf(LastOwnerRequiredException.class);
        assertThatThrownBy(() -> service.assertCanModifyProtectedRole(7))
                .isInstanceOf(LastOwnerRequiredException.class);
    }

    @Test
    void allowsRemovingOneOfTwoActiveOwnersAndIgnoresCommonTargets() {
        when(rolRepository.findByNombreForUpdate("PROPIETARIO")).thenReturn(Optional.of(ownerRole));
        when(rolUsuRepository.existsActiveOwnerByLogin("owner")).thenReturn(true);
        when(rolUsuRepository.countActiveOwners()).thenReturn(2L);

        assertThatCode(() -> service.assertCanDeactivateUser("owner")).doesNotThrowAnyException();
        assertThatCode(() -> service.assertCanRemoveAssignment("common", 9)).doesNotThrowAnyException();
        verify(rolUsuRepository, never()).existsActiveOwnerByLogin("common");
    }

    @Test
    void doesNotInventAnOwnerWhenProtectedRoleDoesNotExist() {
        when(rolRepository.findByNombreForUpdate("PROPIETARIO")).thenReturn(Optional.empty());

        assertThatCode(() -> service.assertCanDeactivateUser("common")).doesNotThrowAnyException();
        assertThatCode(() -> service.assertCanModifyProtectedRole(99)).doesNotThrowAnyException();
        verify(rolUsuRepository, never()).countActiveOwners();
    }

    private void setCodr(Rol rol, Integer codr) {
        try {
            java.lang.reflect.Field field = Rol.class.getDeclaredField("codr");
            field.setAccessible(true);
            field.set(rol, codr);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
