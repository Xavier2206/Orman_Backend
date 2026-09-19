package com.orman.backend.property.service.impl;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.common.exception.BusinessRuleException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.contract.entity.ContratoEstado;
import com.orman.backend.contract.repository.ContratoRepository;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.property.dto.request.UnidadRequest;
import com.orman.backend.property.dto.response.UnidadResponse;
import com.orman.backend.property.entity.PropiedadEntity;
import com.orman.backend.property.entity.UnidadEntity;
import com.orman.backend.property.mapper.UnidadMapper;
import com.orman.backend.property.repository.PropiedadRepository;
import com.orman.backend.property.repository.UnidadRepository;
import com.orman.backend.property.service.PropertyOwnershipService;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnidadServiceImplTest {

    private static final int CODUNI = 501;

    @Mock private PropiedadRepository propiedadRepository;
    @Mock private UnidadRepository unidadRepository;
    @Mock private ContratoRepository contratoRepository;
    @Mock private UnidadMapper unidadMapper;
    @Mock private PropertyOwnershipService propertyOwnershipService;
    @Mock private Authentication authentication;
    @InjectMocks private UnidadServiceImpl service;

    @Test
    void loadsContractAvailabilityForAUnitPageWithOneAggregatedLookup() {
        PropiedadEntity propiedad = unidad(CODUNI, (short) 1).getPropiedad();
        UnidadEntity blockedUnit = unidad(CODUNI, (short) 1);
        UnidadEntity availableUnit = unidad(502, (short) 1);
        when(propiedadRepository.findById(0)).thenReturn(Optional.of(propiedad));
        when(unidadRepository.findAllByPropiedadOwned(eq(0), eq(77), eq((Short) null), any()))
                .thenReturn(new PageImpl<>(List.of(blockedUnit, availableUnit), PageRequest.of(0, 20), 2));
        when(contratoRepository.findOwnedUnitIdsWithBlockingContracts(77, List.of(CODUNI, 502)))
                .thenReturn(Set.of(CODUNI));
        UnidadResponse blockedResponse = response((short) 1, false);
        UnidadResponse availableResponse = response(502, (short) 1, true);
        when(unidadMapper.toResponse(blockedUnit, false)).thenReturn(blockedResponse);
        when(unidadMapper.toResponse(availableUnit, true)).thenReturn(availableResponse);

        PageResponse<UnidadResponse> result = service.listByPropiedad(0, null, PageRequest.of(0, 20), authentication);

        assertThat(result.content()).containsExactly(blockedResponse, availableResponse);
        verify(contratoRepository).findOwnedUnitIdsWithBlockingContracts(77, List.of(CODUNI, 502));
        verify(contratoRepository, never()).existsByUnidadCoduniAndEstadoIn(any(), any());
    }

    @Test
    void activatesAnInactiveUnitUsingThePessimisticUnitLookup() {
        UnidadEntity unidad = unidad((short) 0);
        UnidadResponse response = response((short) 1);
        givenLockedUnit(unidad);
        when(unidadRepository.saveAndFlush(unidad)).thenReturn(unidad);
        when(contratoRepository.existsByUnidadCoduniAndEstadoIn(CODUNI, blockingStates())).thenReturn(false);
        when(unidadMapper.toResponse(unidad, true)).thenReturn(response);

        UnidadResponse result = service.activate(CODUNI, authentication);

        assertThat(result).isSameAs(response);
        assertThat(unidad.getEstadoOperativo()).isEqualTo((short) 1);
        verify(unidadRepository).findByCoduniForUpdate(CODUNI);
        verify(propertyOwnershipService).assertCurrentPropietaria(authentication,
                unidad.getPropiedad().getPropietaria());
        verify(unidadRepository).saveAndFlush(unidad);
        verify(contratoRepository).existsByUnidadCoduniAndEstadoIn(CODUNI, blockingStates());
    }

    @Test
    void activationIsIdempotentWhenTheUnitIsAlreadyActive() {
        UnidadEntity unidad = unidad((short) 1);
        when(unidadRepository.findByCoduniForUpdate(CODUNI)).thenReturn(Optional.of(unidad));
        when(contratoRepository.existsByUnidadCoduniAndEstadoIn(CODUNI, blockingStates())).thenReturn(false);
        when(unidadMapper.toResponse(unidad, true)).thenReturn(response((short) 1));

        service.activate(CODUNI, authentication);

        assertThat(unidad.getEstadoOperativo()).isEqualTo((short) 1);
        verify(unidadRepository, never()).saveAndFlush(any());
        verify(contratoRepository).existsByUnidadCoduniAndEstadoIn(CODUNI, blockingStates());
    }

    @Test
    void deactivatesAnActiveUnitWhenThereIsNoCurrentContract() {
        UnidadEntity unidad = unidad((short) 1);
        givenLockedUnit(unidad);
        when(contratoRepository.existsByUnidadCoduniAndEstadoIn(CODUNI, blockingStates()))
                .thenReturn(false);
        when(unidadRepository.saveAndFlush(unidad)).thenReturn(unidad);
        when(unidadMapper.toResponse(unidad, true)).thenReturn(response((short) 0));

        UnidadResponse result = service.deactivate(CODUNI, authentication);

        assertThat(result.estadoOperativo()).isEqualTo((short) 0);
        assertThat(unidad.getEstadoOperativo()).isEqualTo((short) 0);
        InOrder order = inOrder(unidadRepository, propertyOwnershipService, contratoRepository);
        order.verify(unidadRepository).findByCoduniForUpdate(CODUNI);
        order.verify(propertyOwnershipService).assertCurrentPropietaria(authentication,
                unidad.getPropiedad().getPropietaria());
        order.verify(contratoRepository).existsByUnidadCoduniAndEstadoIn(CODUNI, blockingStates());
        verify(unidadRepository).saveAndFlush(unidad);
    }

    @Test
    void blocksDeactivationWithACurrentContractWithoutChangingTheUnit() {
        UnidadEntity unidad = unidad((short) 1);
        givenLockedUnit(unidad);
        when(contratoRepository.existsByUnidadCoduniAndEstadoIn(CODUNI, blockingStates()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.deactivate(CODUNI, authentication))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("No se puede desactivar la unidad porque tiene un Contrato PROGRAMADO o VIGENTE.");

        assertThat(unidad.getEstadoOperativo()).isEqualTo((short) 1);
        verify(contratoRepository).existsByUnidadCoduniAndEstadoIn(CODUNI, blockingStates());
        verify(unidadRepository, never()).saveAndFlush(any());
    }

    @Test
    void deactivationIsIdempotentWhenTheUnitIsAlreadyInactive() {
        UnidadEntity unidad = unidad((short) 0);
        when(unidadRepository.findByCoduniForUpdate(CODUNI)).thenReturn(Optional.of(unidad));
        when(contratoRepository.existsByUnidadCoduniAndEstadoIn(CODUNI, blockingStates())).thenReturn(false);
        when(unidadMapper.toResponse(unidad, true)).thenReturn(response((short) 0));

        service.deactivate(CODUNI, authentication);

        assertThat(unidad.getEstadoOperativo()).isEqualTo((short) 0);
        verify(unidadRepository, never()).saveAndFlush(any());
        verify(contratoRepository).existsByUnidadCoduniAndEstadoIn(CODUNI, blockingStates());
    }

    @Test
    void rejectsUpdatingTheOperationalStateThroughPut() {
        UnidadEntity unidad = unidad((short) 1);
        when(unidadRepository.findByCoduni(CODUNI)).thenReturn(Optional.of(unidad));

        assertThatThrownBy(() -> service.update(CODUNI, request("Unidad editada", (short) 0), authentication))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El estado operativo de la unidad debe modificarse mediante las acciones de activar o desactivar.");

        verify(unidadMapper, never()).update(any(), any());
        verify(unidadRepository, never()).saveAndFlush(any());
        verify(unidadRepository, never()).existsByPropiedadCodpropAndNombreAndCoduniNot(any(), any(), any());
    }

    @Test
    void allowsPutWhenTheOperationalStateDoesNotChange() {
        UnidadEntity unidad = unidad((short) 1);
        when(unidadRepository.findByCoduni(CODUNI)).thenReturn(Optional.of(unidad));
        when(unidadRepository.existsByPropiedadCodpropAndNombreAndCoduniNot(0, "Unidad editada", CODUNI))
                .thenReturn(false);
        when(unidadRepository.saveAndFlush(unidad)).thenReturn(unidad);
        when(contratoRepository.existsByUnidadCoduniAndEstadoIn(CODUNI, blockingStates())).thenReturn(false);
        when(unidadMapper.toResponse(unidad, true)).thenReturn(response((short) 1));

        UnidadResponse result = service.update(CODUNI, request("Unidad editada", (short) 1), authentication);

        assertThat(result.estadoOperativo()).isEqualTo((short) 1);
        verify(unidadMapper).update(unidad, request("Unidad editada", (short) 1));
        verify(unidadRepository).saveAndFlush(unidad);
    }

    @Test
    void rejectsPutWhenAnInactiveUnitWouldBeActivated() {
        UnidadEntity unidad = unidad((short) 0);
        when(unidadRepository.findByCoduni(CODUNI)).thenReturn(Optional.of(unidad));

        assertThatThrownBy(() -> service.update(CODUNI, request("Unidad editada", (short) 1), authentication))
                .isInstanceOf(BusinessRuleException.class);

        verify(unidadMapper, never()).update(any(), any());
        verify(unidadRepository, never()).saveAndFlush(any());
    }

    @Test
    void preservesOwnershipAndDoesNotChangeAUnitOwnedByAnotherPersona() {
        UnidadEntity unidad = unidad((short) 0);
        when(unidadRepository.findByCoduniForUpdate(CODUNI)).thenReturn(Optional.of(unidad));
        doThrow(new AccessDeniedException("Acceso denegado."))
                .when(propertyOwnershipService)
                .assertCurrentPropietaria(authentication, unidad.getPropiedad().getPropietaria());

        assertThatThrownBy(() -> service.activate(CODUNI, authentication))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(unidad.getEstadoOperativo()).isEqualTo((short) 0);
        verify(unidadRepository, never()).saveAndFlush(any());
    }

    @Test
    void returnsNotFoundWhenTheLockedUnitDoesNotExist() {
        when(unidadRepository.findByCoduniForUpdate(CODUNI)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(CODUNI, authentication))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Unidad no encontrada.");

        verifyNoInteractions(propertyOwnershipService, contratoRepository);
    }

    @Test
    void usesPessimisticWriteLockForTheStatusTransitionLookup() throws NoSuchMethodException {
        Lock lock = UnidadRepository.class.getMethod("findByCoduniForUpdate", Integer.class)
                .getAnnotation(Lock.class);

        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    private void givenLockedUnit(UnidadEntity unidad) {
        when(unidadRepository.findByCoduniForUpdate(CODUNI)).thenReturn(Optional.of(unidad));
    }

    private UnidadEntity unidad(short estadoOperativo) {
        return unidad(CODUNI, estadoOperativo);
    }

    private UnidadEntity unidad(int coduni, short estadoOperativo) {
        UnidadEntity unidad = new UnidadEntity();
        ReflectionTestUtils.setField(unidad, "coduni", coduni);
        PropiedadEntity propiedad = new PropiedadEntity();
        ReflectionTestUtils.setField(propiedad, "codprop", 0);
        Persona propietaria = new Persona();
        ReflectionTestUtils.setField(propietaria, "codper", 77);
        propiedad.setPropietaria(propietaria);
        unidad.setPropiedad(propiedad);
        unidad.setNombre("Unidad 101");
        unidad.setEstadoOperativo(estadoOperativo);
        return unidad;
    }

    private UnidadRequest request(String nombre, short estadoOperativo) {
        return new UnidadRequest(nombre, "DEPARTAMENTO", null, new BigDecimal("40.00"), (short) 1, (short) 1,
                1, "Bloque A", new BigDecimal("2500.00"), estadoOperativo);
    }

    private UnidadResponse response(short estadoOperativo) {
        return response(CODUNI, estadoOperativo, true);
    }

    private UnidadResponse response(short estadoOperativo, boolean disponibleParaContrato) {
        return response(CODUNI, estadoOperativo, disponibleParaContrato);
    }

    private UnidadResponse response(int coduni, short estadoOperativo, boolean disponibleParaContrato) {
        return new UnidadResponse(coduni, 0, "Unidad 101", "DEPARTAMENTO", null,
                new BigDecimal("40.00"), (short) 1, (short) 1, 1, "Bloque A",
                new BigDecimal("2500.00"), estadoOperativo, disponibleParaContrato);
    }

    private List<ContratoEstado> blockingStates() {
        return List.of(ContratoEstado.PROGRAMADO, ContratoEstado.VIGENTE);
    }
}
