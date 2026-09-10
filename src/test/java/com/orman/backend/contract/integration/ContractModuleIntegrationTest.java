package com.orman.backend.contract.integration;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.contract.dto.request.ContratoArchivoRequest;
import com.orman.backend.contract.dto.request.ContratoRenovacionRequest;
import com.orman.backend.contract.dto.request.ContratoRequest;
import com.orman.backend.contract.dto.request.RescisionContratoRequest;
import com.orman.backend.contract.dto.response.ContratoResponse;
import com.orman.backend.contract.dto.response.CuotaResponse;
import com.orman.backend.contract.service.ContratoArchivoService;
import com.orman.backend.contract.service.ContratoService;
import com.orman.backend.contract.service.CuotaService;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.property.entity.PropiedadEntity;
import com.orman.backend.property.entity.UnidadEntity;
import com.orman.backend.property.repository.PropiedadRepository;
import com.orman.backend.property.repository.UnidadRepository;
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
class ContractModuleIntegrationTest {

    @Autowired private PersonaRepository personaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PropiedadRepository propiedadRepository;
    @Autowired private UnidadRepository unidadRepository;
    @Autowired private ContratoService contratoService;
    @Autowired private ContratoArchivoService contratoArchivoService;
    @Autowired private CuotaService cuotaService;

    @Test
    void managesDraftConfirmationQuotasRenewalAndRescissionForTheOwner() {
        Persona propietaria = createPersona("CM-OWNER-001");
        Authentication authentication = authentication(createUsuario("contract.owner", propietaria));
        UnidadEntity unidad = createUnidad(propietaria, "Unidad 101");
        Persona inquilino = createPersona("CM-TENANT-001");

        ContratoResponse borrador = contratoService.createDraft(unidad.getCoduni(), request(inquilino.getCodper(),
                LocalDate.of(2026, 9, 1), LocalDate.of(2027, 9, 1)), authentication);
        assertThat(borrador.estado()).isEqualTo("BORRADOR");

        ContratoResponse vigente = contratoService.confirm(borrador.codcon(), authentication);
        assertThat(vigente.estado()).isEqualTo("VIGENTE");
        assertThat(vigente.fechaConfirmacion()).isNotNull();

        List<CuotaResponse> cuotas = cuotaService.listByContrato(vigente.codcon(), authentication);
        assertThat(cuotas).hasSize(12);
        assertThat(cuotas).allSatisfy(cuota -> assertThat(cuota.estado()).isEqualTo("PENDIENTE"));
        assertThat(cuotas.getFirst().periodo()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(cuotas.getLast().periodo()).isEqualTo(LocalDate.of(2027, 8, 1));

        assertThat(contratoArchivoService.create(vigente.codcon(), new ContratoArchivoRequest(
                "https://example.test/contrato.pdf", "contrato.pdf", "application/pdf", 0), authentication).id())
                .isNotNull();

        ContratoResponse renovacion = contratoService.renew(vigente.codcon(), new ContratoRenovacionRequest(
                LocalDate.of(2027, 9, 1), LocalDate.of(2028, 9, 1), new BigDecimal("2700.00"),
                new BigDecimal("2500.00")), authentication);
        assertThat(renovacion.estado()).isEqualTo("BORRADOR");
        assertThat(renovacion.codconOrigen()).isEqualTo(vigente.codcon());

        ContratoResponse rescindido = contratoService.rescind(vigente.codcon(), new RescisionContratoRequest(
                LocalDate.of(2027, 1, 1), "Finalización anticipada acordada."), authentication);
        assertThat(rescindido.estado()).isEqualTo("RESCINDIDO");
        assertThat(rescindido.fechaRescision()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(cuotaService.listByContrato(vigente.codcon(), authentication))
                .allSatisfy(cuota -> assertThat(cuota.estado()).isEqualTo("PENDIENTE"));
    }

    @Test
    void rejectsAnotherAuthenticatedPersonaAndInvalidStateTransitions() {
        Persona propietaria = createPersona("CM-OWNER-002");
        Authentication ownerAuthentication = authentication(createUsuario("contract.owner.two", propietaria));
        UnidadEntity unidad = createUnidad(propietaria, "Unidad 102");
        Persona inquilino = createPersona("CM-TENANT-002");
        ContratoResponse borrador = contratoService.createDraft(unidad.getCoduni(), request(inquilino.getCodper(),
                LocalDate.of(2026, 9, 1), LocalDate.of(2027, 9, 1)), ownerAuthentication);

        Persona otraPersona = createPersona("CM-OTHER-001");
        Authentication otherAuthentication = authentication(createUsuario("contract.other", otraPersona));
        assertThatThrownBy(() -> contratoService.get(borrador.codcon(), otherAuthentication))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> contratoService.finish(borrador.codcon(), ownerAuthentication))
                .hasMessageContaining("vigente");
    }

    private Persona createPersona(String ci) {
        Persona persona = new Persona();
        persona.setCi(ci);
        persona.setNombre("Persona contractual");
        persona.setGenero('F');
        persona.setEstado((short) 1);
        persona.setCorreo(ci.toLowerCase() + "@example.test");
        persona.setTelefono("70000000");
        persona.setTipoPersona('A');
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

    private UnidadEntity createUnidad(Persona propietaria, String nombre) {
        PropiedadEntity propiedad = new PropiedadEntity();
        propiedad.setNombre("Propiedad contractual " + nombre);
        propiedad.setTipo("CASA");
        propiedad.setDireccion("Calle de prueba");
        propiedad.setCiudad("La Paz");
        propiedad.setPropietaria(propietaria);
        propiedad.setInversionInicial(BigDecimal.ZERO);
        propiedad.setEstado((short) 1);
        propiedad = propiedadRepository.saveAndFlush(propiedad);
        UnidadEntity unidad = new UnidadEntity();
        unidad.setPropiedad(propiedad);
        unidad.setNombre(nombre);
        unidad.setTipoUnidad("DEPARTAMENTO");
        unidad.setArea(new BigDecimal("50.00"));
        unidad.setDormitorios((short) 1);
        unidad.setBanos((short) 1);
        unidad.setPiso(1);
        unidad.setPrecioBase(new BigDecimal("2500.00"));
        unidad.setEstadoOperativo((short) 1);
        return unidadRepository.saveAndFlush(unidad);
    }

    private Authentication authentication(Usuario usuario) {
        return new TestingAuthenticationToken(new AuthenticatedUser(usuario.getLogin(), UUID.randomUUID()), null,
                List.of(new SimpleGrantedAuthority("ROLE_PROPIETARIO")));
    }

    private ContratoRequest request(Integer codperInquilino, LocalDate inicio, LocalDate fin) {
        return new ContratoRequest(codperInquilino, inicio, fin, new BigDecimal("2500.00"),
                new BigDecimal("2500.00"));
    }
}
