package com.orman.backend.property.integration;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.common.exception.BusinessRuleException;
import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.contract.dto.request.ContratoRequest;
import com.orman.backend.contract.dto.request.RescisionContratoRequest;
import com.orman.backend.contract.dto.response.ContratoResponse;
import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.entity.ContratoEstado;
import com.orman.backend.contract.repository.ContratoRepository;
import com.orman.backend.contract.repository.CuotaRepository;
import com.orman.backend.contract.service.ContratoService;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.property.dto.request.PropiedadRequest;
import com.orman.backend.property.dto.request.UnidadFotoRequest;
import com.orman.backend.property.dto.request.UnidadRequest;
import com.orman.backend.property.dto.response.PropiedadResponse;
import com.orman.backend.property.dto.response.UnidadFotoResponse;
import com.orman.backend.property.dto.response.UnidadResponse;
import com.orman.backend.property.entity.UnidadEntity;
import com.orman.backend.property.repository.UnidadRepository;
import com.orman.backend.property.service.PropiedadService;
import com.orman.backend.property.service.UnidadFotoService;
import com.orman.backend.property.service.UnidadService;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@Rollback
class PropertyModuleIntegrationTest {

    @Autowired private PersonaRepository personaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ContratoRepository contratoRepository;
    @Autowired private CuotaRepository cuotaRepository;
    @Autowired private UnidadRepository unidadRepository;
    @Autowired private PropiedadService propiedadService;
    @Autowired private ContratoService contratoService;
    @Autowired private UnidadService unidadService;
    @Autowired private UnidadFotoService unidadFotoService;

    @Test
    void managesOwnedPropertyUnitsAndPhotosThroughTheBackendServices() {
        Persona propietaria = createPersona("PM-OWNER-001");
        Authentication authentication = authentication(createUsuario("property.owner", propietaria));

        PropiedadResponse propiedad = propiedadService.create(propiedadRequest(propietaria.getCodper()), authentication);
        assertThat(propiedadService.list(null, null, null, org.springframework.data.domain.PageRequest.of(0, 20), authentication)
                .content()).extracting(PropiedadResponse::codprop).containsExactly(propiedad.codprop());

        UnidadResponse unidad = unidadService.create(propiedad.codprop(), unidadRequest("Unidad 101"), authentication);
        UnidadResponse updated = unidadService.update(unidad.coduni(), unidadRequest("Unidad 102"), authentication);
        assertThat(updated.nombre()).isEqualTo("Unidad 102");
        assertThat(unidadService.listByPropiedad(propiedad.codprop(), null,
                org.springframework.data.domain.PageRequest.of(0, 20), authentication)
                .content()).extracting(UnidadResponse::coduni).containsExactly(unidad.coduni());

        UnidadFotoResponse first = unidadFotoService.create(unidad.coduni(), fotoRequest("https://example.test/uno.jpg", 0), authentication);
        UnidadFotoResponse second = unidadFotoService.create(unidad.coduni(), fotoRequest("https://example.test/dos.jpg", 1), authentication);
        assertThat(unidadFotoService.setPortada(unidad.coduni(), second.id(), authentication).portada()).isTrue();
        assertThat(unidadFotoService.listByUnidad(unidad.coduni(), authentication))
                .filteredOn(UnidadFotoResponse::portada).extracting(UnidadFotoResponse::id).containsExactly(second.id());

        unidadFotoService.delete(unidad.coduni(), first.id(), authentication);
        assertThat(unidadFotoService.listByUnidad(unidad.coduni(), authentication))
                .extracting(UnidadFotoResponse::id).containsExactly(second.id());
    }

    @Test
    void rejectsAccessWhenTheAuthenticatedPersonaDoesNotOwnTheProperty() {
        Persona propietaria = createPersona("PM-OWNER-002");
        Authentication ownerAuthentication = authentication(createUsuario("property.owner.two", propietaria));
        PropiedadResponse propiedad = propiedadService.create(propiedadRequest(propietaria.getCodper()), ownerAuthentication);

        Persona otraPersona = createPersona("PM-OTHER-001");
        Authentication otherAuthentication = authentication(createUsuario("property.other", otraPersona));

        assertThatThrownBy(() -> propiedadService.get(propiedad.codprop(), otherAuthentication))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> propiedadService.create(propiedadRequest(otraPersona.getCodper()), ownerAuthentication))
                .isInstanceOf(AccessDeniedException.class);

        unidadService.create(propiedad.codprop(), unidadRequest("Unidad propia"), ownerAuthentication);
        assertThatThrownBy(() -> unidadService.listByPropiedad(propiedad.codprop(), (short) 1,
                org.springframework.data.domain.PageRequest.of(0, 20), otherAuthentication))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void filtersUnitsByOperationalStateBeforeApplyingPagination() {
        Persona owner = createPersona("PM-STATE-OWNER-001");
        Authentication ownerAuthentication = authentication(createUsuario("property.state.owner", owner));
        PropiedadResponse property = propiedadService.create(
                propiedadRequest(owner.getCodper(), "Propiedad estados", "EDIFICIO", (short) 1),
                ownerAuthentication);

        unidadService.create(property.codprop(), unidadRequest("A-01", (short) 1), ownerAuthentication);
        unidadService.create(property.codprop(), unidadRequest("A-02", (short) 0), ownerAuthentication);
        unidadService.create(property.codprop(), unidadRequest("A-03", (short) 1), ownerAuthentication);
        unidadService.create(property.codprop(), unidadRequest("A-04", (short) 0), ownerAuthentication);
        unidadService.create(property.codprop(), unidadRequest("A-05", (short) 1), ownerAuthentication);

        PageResponse<UnidadResponse> all = unidadService.listByPropiedad(property.codprop(), null,
                org.springframework.data.domain.PageRequest.of(0, 20), ownerAuthentication);
        assertThat(all.content()).hasSize(5);
        assertThat(all.totalElements()).isEqualTo(5);

        PageResponse<UnidadResponse> operationalPage = unidadService.listByPropiedad(property.codprop(), (short) 1,
                org.springframework.data.domain.PageRequest.of(0, 2), ownerAuthentication);
        assertThat(operationalPage.content()).extracting(UnidadResponse::estadoOperativo)
                .containsOnly((short) 1);
        assertThat(operationalPage.content()).hasSize(2);
        assertThat(operationalPage.totalElements()).isEqualTo(3);
        assertThat(operationalPage.totalPages()).isEqualTo(2);
        assertThat(operationalPage.first()).isTrue();
        assertThat(operationalPage.last()).isFalse();

        PageResponse<UnidadResponse> secondOperationalPage = unidadService.listByPropiedad(property.codprop(),
                (short) 1, org.springframework.data.domain.PageRequest.of(1, 2), ownerAuthentication);
        assertThat(secondOperationalPage.content()).hasSize(1);
        assertThat(secondOperationalPage.totalElements()).isEqualTo(3);
        assertThat(secondOperationalPage.totalPages()).isEqualTo(2);
        assertThat(secondOperationalPage.first()).isFalse();
        assertThat(secondOperationalPage.last()).isTrue();

        PageResponse<UnidadResponse> nonOperationalPage = unidadService.listByPropiedad(property.codprop(),
                (short) 0, org.springframework.data.domain.PageRequest.of(0, 20), ownerAuthentication);
        assertThat(nonOperationalPage.content()).hasSize(2);
        assertThat(nonOperationalPage.content()).extracting(UnidadResponse::estadoOperativo)
                .containsOnly((short) 0);
        assertThat(nonOperationalPage.totalElements()).isEqualTo(2);

        PropiedadResponse onlyOperationalProperty = propiedadService.create(
                propiedadRequest(owner.getCodper(), "Propiedad solo operativa", "CASA", (short) 1),
                ownerAuthentication);
        unidadService.create(onlyOperationalProperty.codprop(), unidadRequest("B-01", (short) 1), ownerAuthentication);
        PageResponse<UnidadResponse> emptyPage = unidadService.listByPropiedad(onlyOperationalProperty.codprop(),
                (short) 0, org.springframework.data.domain.PageRequest.of(0, 20), ownerAuthentication);
        assertThat(emptyPage.content()).isEmpty();
        assertThat(emptyPage.totalElements()).isZero();
        assertThat(emptyPage.totalPages()).isZero();
        assertThat(emptyPage.first()).isTrue();
        assertThat(emptyPage.last()).isTrue();
    }

    @Test
    void changesOnlyOperationalStateThroughExplicitActions() {
        Persona owner = createPersona("PM-ACTION-OWNER-001");
        Authentication ownerAuthentication = authentication(createUsuario("property.action.owner", owner));
        PropiedadResponse property = propiedadService.create(propiedadRequest(owner.getCodper()), ownerAuthentication);

        UnidadResponse created = unidadService.create(property.codprop(), unidadRequest("ACTION-01", (short) 0),
                ownerAuthentication);
        UnidadResponse activated = unidadService.activate(created.coduni(), ownerAuthentication);

        assertThat(activated.estadoOperativo()).isEqualTo((short) 1);
        assertThat(activated.nombre()).isEqualTo(created.nombre());
        assertThat(activated.tipoUnidad()).isEqualTo(created.tipoUnidad());
        assertThat(activated.precioBase()).isEqualByComparingTo(created.precioBase());

        UnidadResponse deactivated = unidadService.deactivate(created.coduni(), ownerAuthentication);

        assertThat(deactivated.estadoOperativo()).isEqualTo((short) 0);
        assertThat(deactivated.nombre()).isEqualTo(created.nombre());
        assertThat(deactivated.tipoUnidad()).isEqualTo(created.tipoUnidad());
        assertThat(unidadRepository.findById(created.coduni()).orElseThrow().getEstadoOperativo())
                .isEqualTo((short) 0);
    }

    @Test
    void blocksDeactivationWithAConfirmedContractAndPreservesContractAndQuotaData() {
        Persona owner = createPersona("PM-ACTION-OWNER-002");
        Authentication ownerAuthentication = authentication(createUsuario("property.action.owner.two", owner));
        PropiedadResponse property = propiedadService.create(propiedadRequest(owner.getCodper()), ownerAuthentication);
        UnidadResponse unit = unidadService.create(property.codprop(), unidadRequest("ACTION-02", (short) 1),
                ownerAuthentication);
        Persona tenant = createTenant("PM-ACTION-TENANT-001");
        ContratoResponse contract = createConfirmedContract(unit.coduni(), tenant, ownerAuthentication);
        var quotasBefore = cuotaRepository.findAllByContratoCodconOrderByPeriodoAsc(contract.codcon()).stream()
                .map(quota -> quota.getPeriodo() + "|" + quota.getMonto() + "|" + quota.getEstado())
                .toList();

        assertThatThrownBy(() -> unidadService.deactivate(unit.coduni(), ownerAuthentication))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("No se puede desactivar la unidad porque tiene un Contrato PROGRAMADO o VIGENTE.");

        assertThat(unidadRepository.findById(unit.coduni()).orElseThrow().getEstadoOperativo())
                .isEqualTo((short) 1);
        assertThat(contratoRepository.findById(contract.codcon()).orElseThrow().getEstado())
                .isEqualTo(ContratoEstado.VIGENTE);
        assertThat(cuotaRepository.findAllByContratoCodconOrderByPeriodoAsc(contract.codcon()).stream()
                .map(quota -> quota.getPeriodo() + "|" + quota.getMonto() + "|" + quota.getEstado())
                .toList()).containsExactlyElementsOf(quotasBefore);
    }

    @Test
    void allowsDeactivationAfterFinishingTheCurrentContractWithoutChangingUnitAutomatically() {
        Persona owner = createPersona("PM-ACTION-OWNER-003");
        Authentication ownerAuthentication = authentication(createUsuario("property.action.owner.three", owner));
        PropiedadResponse property = propiedadService.create(propiedadRequest(owner.getCodper()), ownerAuthentication);
        UnidadResponse unit = unidadService.create(property.codprop(), unidadRequest("ACTION-03", (short) 1),
                ownerAuthentication);
        Persona tenant = createTenant("PM-ACTION-TENANT-002");
        ContratoResponse contract = createConfirmedContract(unit.coduni(), tenant, ownerAuthentication);

        ContratoEntity storedContract = contratoRepository.findById(contract.codcon()).orElseThrow();
        storedContract.setEstado(ContratoEstado.FINALIZADO);
        contratoRepository.saveAndFlush(storedContract);

        assertThat(unidadRepository.findById(unit.coduni()).orElseThrow().getEstadoOperativo())
                .isEqualTo((short) 1);
        assertThat(unidadService.deactivate(unit.coduni(), ownerAuthentication).estadoOperativo())
                .isEqualTo((short) 0);
    }

    @Test
    void allowsDeactivationAfterRescindingTheCurrentContractWithoutChangingUnitAutomatically() {
        Persona owner = createPersona("PM-ACTION-OWNER-004");
        Authentication ownerAuthentication = authentication(createUsuario("property.action.owner.four", owner));
        PropiedadResponse property = propiedadService.create(propiedadRequest(owner.getCodper()), ownerAuthentication);
        UnidadResponse unit = unidadService.create(property.codprop(), unidadRequest("ACTION-04", (short) 1),
                ownerAuthentication);
        Persona tenant = createTenant("PM-ACTION-TENANT-003");
        ContratoResponse contract = createConfirmedContract(unit.coduni(), tenant, ownerAuthentication);

        ContratoEntity storedContract = contratoRepository.findById(contract.codcon()).orElseThrow();
        storedContract.setEstado(ContratoEstado.RESCINDIDO);
        storedContract.setFechaRescision(LocalDate.of(2026, 6, 1));
        storedContract.setMotivoRescision("Rescisión de prueba.");
        contratoRepository.saveAndFlush(storedContract);

        assertThat(unidadRepository.findById(unit.coduni()).orElseThrow().getEstadoOperativo())
                .isEqualTo((short) 1);
        assertThat(unidadService.deactivate(unit.coduni(), ownerAuthentication).estadoOperativo())
                .isEqualTo((short) 0);
    }

    @Test
    void rejectsOperationalStateChangesThroughPutButAllowsEditingWithTheCurrentState() {
        Persona owner = createPersona("PM-ACTION-OWNER-005");
        Authentication ownerAuthentication = authentication(createUsuario("property.action.owner.five", owner));
        PropiedadResponse property = propiedadService.create(propiedadRequest(owner.getCodper()), ownerAuthentication);
        UnidadResponse unit = unidadService.create(property.codprop(), unidadRequest("ACTION-05", (short) 1),
                ownerAuthentication);

        assertThatThrownBy(() -> unidadService.update(unit.coduni(), unidadRequest("ACTION-05-REJECTED", (short) 0),
                ownerAuthentication)).isInstanceOf(BusinessRuleException.class);
        assertThat(unidadService.update(unit.coduni(), unidadRequest("ACTION-05-EDITED", (short) 1),
                ownerAuthentication).estadoOperativo()).isEqualTo((short) 1);
        assertThat(unidadRepository.findById(unit.coduni()).orElseThrow().getEstadoOperativo())
                .isEqualTo((short) 1);
    }

    @Test
    void listsTotalUnitCountsPerOwnedPropertyAndPreservesFiltersAndPagination() {
        Persona owner = createPersona("PM-COUNT-OWNER-001");
        Authentication ownerAuthentication = authentication(createUsuario("property.count.owner", owner));

        PropiedadResponse withoutUnits = propiedadService.create(
                propiedadRequest(owner.getCodper(), "A Sin Unidades", "CASA", (short) 0), ownerAuthentication);
        PropiedadResponse oneUnit = propiedadService.create(
                propiedadRequest(owner.getCodper(), "B Una Unidad", "EDIFICIO", (short) 1), ownerAuthentication);
        PropiedadResponse severalUnits = propiedadService.create(
                propiedadRequest(owner.getCodper(), "C Varias Unidades", "EDIFICIO", (short) 1), ownerAuthentication);

        unidadService.create(oneUnit.codprop(), unidadRequest("B-01", (short) 1), ownerAuthentication);
        unidadService.create(severalUnits.codprop(), unidadRequest("C-01", (short) 1), ownerAuthentication);
        unidadService.create(severalUnits.codprop(), unidadRequest("C-02", (short) 0), ownerAuthentication);
        unidadService.create(severalUnits.codprop(), unidadRequest("C-03", (short) 1), ownerAuthentication);

        Persona otherOwner = createPersona("PM-COUNT-OWNER-002");
        Authentication otherAuthentication = authentication(createUsuario("property.count.other", otherOwner));
        PropiedadResponse otherProperty = propiedadService.create(
                propiedadRequest(otherOwner.getCodper(), "Z Propiedad Ajena", "CASA", (short) 1), otherAuthentication);
        unidadService.create(otherProperty.codprop(), unidadRequest("Z-01", (short) 1), otherAuthentication);

        PageResponse<PropiedadResponse> all = propiedadService.list(null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 20), ownerAuthentication);
        assertThat(all.content()).extracting(PropiedadResponse::codprop)
                .containsExactly(withoutUnits.codprop(), oneUnit.codprop(), severalUnits.codprop());
        assertThat(all.content()).extracting(PropiedadResponse::cantidadUnidades)
                .containsExactly(0L, 1L, 3L);
        assertThat(all.content()).extracting(PropiedadResponse::unidadesHabilitadas)
                .containsExactly(0L, 1L, 2L);
        assertThat(all.content()).extracting(PropiedadResponse::unidadesOcupadas)
                .containsExactly(0L, 0L, 0L);
        assertThat(all.content()).extracting(PropiedadResponse::ocupacion)
                .containsExactly(new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00"));
        assertThat(all.totalElements()).isEqualTo(3);

        PageResponse<PropiedadResponse> filteredByQuery = propiedadService.list("varias", null, null,
                org.springframework.data.domain.PageRequest.of(0, 20), ownerAuthentication);
        assertThat(filteredByQuery.content()).extracting(PropiedadResponse::codprop)
                .containsExactly(severalUnits.codprop());
        assertThat(filteredByQuery.content().get(0).cantidadUnidades()).isEqualTo(3);
        assertThat(filteredByQuery.content().get(0).unidadesHabilitadas()).isEqualTo(2);

        PageResponse<PropiedadResponse> filteredByType = propiedadService.list(null, "EDIFICIO", null,
                org.springframework.data.domain.PageRequest.of(0, 20), ownerAuthentication);
        assertThat(filteredByType.content()).extracting(PropiedadResponse::codprop)
                .containsExactly(oneUnit.codprop(), severalUnits.codprop());

        PageResponse<PropiedadResponse> filteredByState = propiedadService.list(null, null, (short) 0,
                org.springframework.data.domain.PageRequest.of(0, 20), ownerAuthentication);
        assertThat(filteredByState.content()).extracting(PropiedadResponse::codprop)
                .containsExactly(withoutUnits.codprop());
        assertThat(filteredByState.content().get(0).cantidadUnidades()).isZero();
        assertThat(filteredByState.content().get(0).unidadesHabilitadas()).isZero();
        assertThat(filteredByState.content().get(0).unidadesOcupadas()).isZero();
        assertThat(filteredByState.content().get(0).ocupacion()).isEqualByComparingTo("0.00");

        PageResponse<PropiedadResponse> firstPage = propiedadService.list(null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 2), ownerAuthentication);
        assertThat(firstPage.content()).hasSize(2);
        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(firstPage.first()).isTrue();
        assertThat(firstPage.last()).isFalse();

        PageResponse<PropiedadResponse> secondPage = propiedadService.list(null, null, null,
                org.springframework.data.domain.PageRequest.of(1, 2), ownerAuthentication);
        assertThat(secondPage.content()).hasSize(1);
        assertThat(secondPage.first()).isFalse();
        assertThat(secondPage.last()).isTrue();
    }

    @Test
    void calculatesPropertyOccupancyFromEnabledUnitsAndCurrentContracts() {
        Persona propietaria = createPersona("POCC-OWNER-01");
        Authentication ownerAuthentication = authentication(createUsuario("property.occupancy.owner", propietaria));
        PropiedadResponse property = propiedadService.create(
                propiedadRequest(propietaria.getCodper(), "Propiedad Ocupacion", "EDIFICIO", (short) 1),
                ownerAuthentication);
        Persona tenant = createPersona("POCC-TENANT-01");

        List<UnidadResponse> enabledUnits = java.util.stream.IntStream.rangeClosed(1, 8)
                .mapToObj(index -> unidadService.create(property.codprop(), unidadRequest("O-0" + index, (short) 1),
                        ownerAuthentication))
                .toList();
        enabledUnits.subList(0, 7).forEach(unidad -> createContract(unidad.coduni(), property.codprop(), propietaria,
                tenant, ContratoEstado.VIGENTE));

        UnidadResponse disabledUnit = unidadService.create(property.codprop(), unidadRequest("O-09", (short) 0),
                ownerAuthentication);
        createContract(disabledUnit.coduni(), property.codprop(), propietaria, tenant, ContratoEstado.VIGENTE);

        PropiedadResponse result = propiedadService.list(null, null, null,
                        org.springframework.data.domain.PageRequest.of(0, 20), ownerAuthentication)
                .content().getFirst();

        assertThat(result.cantidadUnidades()).isEqualTo(9);
        assertThat(result.unidadesHabilitadas()).isEqualTo(8);
        assertThat(result.unidadesOcupadas()).isEqualTo(7);
        assertThat(result.ocupacion()).isEqualByComparingTo("87.50");
    }

    @Test
    void excludesNonCurrentContractsAndDisabledUnitsFromPropertyOccupancy() {
        Persona propietaria = createPersona("POCC-OWNER-02");
        Authentication ownerAuthentication = authentication(createUsuario("property.occupancy.owner.two", propietaria));
        PropiedadResponse property = propiedadService.create(
                propiedadRequest(propietaria.getCodper(), "Propiedad Estados", "CASA", (short) 1),
                ownerAuthentication);
        Persona tenant = createPersona("POCC-TENANT-02");

        UnidadResponse draft = unidadService.create(property.codprop(), unidadRequest("S-01", (short) 1),
                ownerAuthentication);
        UnidadResponse finished = unidadService.create(property.codprop(), unidadRequest("S-02", (short) 1),
                ownerAuthentication);
        UnidadResponse rescinded = unidadService.create(property.codprop(), unidadRequest("S-03", (short) 1),
                ownerAuthentication);
        UnidadResponse disabled = unidadService.create(property.codprop(), unidadRequest("S-04", (short) 0),
                ownerAuthentication);
        createContract(draft.coduni(), property.codprop(), propietaria, tenant, ContratoEstado.PROGRAMADO);
        createContract(finished.coduni(), property.codprop(), propietaria, tenant, ContratoEstado.FINALIZADO);
        createContract(rescinded.coduni(), property.codprop(), propietaria, tenant, ContratoEstado.RESCINDIDO);
        createContract(disabled.coduni(), property.codprop(), propietaria, tenant, ContratoEstado.VIGENTE);

        PropiedadResponse result = propiedadService.list(null, null, null,
                        org.springframework.data.domain.PageRequest.of(0, 20), ownerAuthentication)
                .content().getFirst();

        assertThat(result.cantidadUnidades()).isEqualTo(4);
        assertThat(result.unidadesHabilitadas()).isEqualTo(3);
        assertThat(result.unidadesOcupadas()).isZero();
        assertThat(result.ocupacion()).isEqualByComparingTo("0.00");
    }

    @Test
    void doesNotIncludeAnotherOwnersUnitsInPropertyMetrics() {
        Persona owner = createPersona("POCC-OWNER-03");
        Authentication ownerAuthentication = authentication(createUsuario("property.occupancy.owner.three", owner));
        PropiedadResponse ownProperty = propiedadService.create(
                propiedadRequest(owner.getCodper(), "Propiedad Propia", "CASA", (short) 1), ownerAuthentication);

        Persona otherOwner = createPersona("POCC-OWNER-04");
        Authentication otherAuthentication = authentication(createUsuario("property.occupancy.other", otherOwner));
        PropiedadResponse otherProperty = propiedadService.create(
                propiedadRequest(otherOwner.getCodper(), "Propiedad Ajena", "EDIFICIO", (short) 1), otherAuthentication);
        UnidadResponse ownUnit = unidadService.create(ownProperty.codprop(), unidadRequest("P-01", (short) 1),
                ownerAuthentication);
        UnidadResponse otherUnit = unidadService.create(otherProperty.codprop(), unidadRequest("A-01", (short) 1),
                otherAuthentication);
        Persona tenant = createPersona("POCC-TENANT-03");
        createContract(ownUnit.coduni(), ownProperty.codprop(), owner, tenant, ContratoEstado.VIGENTE);
        createContract(otherUnit.coduni(), otherProperty.codprop(), otherOwner, tenant, ContratoEstado.VIGENTE);

        PageResponse<PropiedadResponse> result = propiedadService.list(null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 20), ownerAuthentication);

        assertThat(result.content()).extracting(PropiedadResponse::codprop).containsExactly(ownProperty.codprop());
        assertThat(result.content().getFirst().unidadesOcupadas()).isEqualTo(1);
        assertThat(result.content().getFirst().ocupacion()).isEqualByComparingTo("100.00");
    }

    private ContratoEntity createContract(Integer coduni, Integer codprop, Persona propietaria, Persona tenant,
                                          ContratoEstado estado) {
        UnidadEntity unidad = unidadRepository.findById(coduni).orElseThrow();
        assertThat(unidad.getPropiedad().getCodprop()).isEqualTo(codprop);
        assertThat(unidad.getPropiedad().getPropietaria().getCodper()).isEqualTo(propietaria.getCodper());
        ContratoEntity contrato = new ContratoEntity();
        contrato.setUnidad(unidad);
        contrato.setInquilino(tenant);
        contrato.setFechaInicio(LocalDate.of(2026, 1, 1));
        contrato.setFechaFin(LocalDate.of(2027, 1, 1));
        contrato.setMontoMensual(new BigDecimal("1000.00"));
        contrato.setMoneda("BOB");
        contrato.setGarantia(new BigDecimal("1000.00"));
        contrato.setEstado(estado);
        contrato.setFechaRegistro(LocalDateTime.now());
        if (estado == ContratoEstado.RESCINDIDO) {
            contrato.setFechaRescision(LocalDate.of(2026, 6, 1));
            contrato.setMotivoRescision("Rescisión de prueba");
        }
        return contratoRepository.saveAndFlush(contrato);
    }

    private ContratoResponse createConfirmedContract(Integer coduni, Persona tenant, Authentication authentication) {
        return contratoService.create(coduni,
                new ContratoRequest(tenant.getCodper(), LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1),
                        new BigDecimal("1000.00"), new BigDecimal("1000.00")), authentication);
    }

    private Persona createPersona(String ci) {
        return createPersona(ci, 'A');
    }

    private Persona createTenant(String ci) {
        return createPersona(ci, 'I');
    }

    private Persona createPersona(String ci, char tipoPersona) {
        Persona persona = new Persona();
        persona.setCi(ci);
        persona.setNombre("Persona de prueba");
        persona.setGenero('F');
        persona.setEstado((short) 1);
        persona.setCorreo(ci.toLowerCase() + "@example.test");
        persona.setTelefono("70000000");
        persona.setTipoPersona(tipoPersona);
        return personaRepository.saveAndFlush(persona);
    }

    private Usuario createUsuario(String login, Persona persona) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setPasswd("hash-no-expuesto");
        usuario.setEstado((short) 1);
        usuario.setPersona(persona);
        usuario.setFechaCreacion(LocalDateTime.now());
        return usuarioRepository.saveAndFlush(usuario);
    }

    private Authentication authentication(Usuario usuario) {
        return new TestingAuthenticationToken(new AuthenticatedUser(usuario.getLogin(), UUID.randomUUID()), null,
                List.of(new SimpleGrantedAuthority("ROLE_PROPIETARIO")));
    }

    private PropiedadRequest propiedadRequest(Integer codperPropietaria) {
        return propiedadRequest(codperPropietaria, "Propiedad de prueba", "CASA", (short) 1);
    }

    private PropiedadRequest propiedadRequest(Integer codperPropietaria, String nombre, String tipo, short estado) {
        return new PropiedadRequest(nombre, tipo, "Calle 1", "La Paz", null,
                null, null, null, codperPropietaria, new BigDecimal("1000.00"), estado);
    }

    private UnidadRequest unidadRequest(String nombre) {
        return unidadRequest(nombre, (short) 1);
    }

    private UnidadRequest unidadRequest(String nombre, short estadoOperativo) {
        return new UnidadRequest(nombre, "DEPARTAMENTO", null, new BigDecimal("40.00"), (short) 1, (short) 1,
                1, "Bloque A", new BigDecimal("2500.00"), estadoOperativo);
    }

    private UnidadFotoRequest fotoRequest(String url, Integer orden) {
        return new UnidadFotoRequest(url, null, "Sala", orden);
    }
}
