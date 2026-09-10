package com.orman.backend.property.integration;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.property.dto.request.PropiedadRequest;
import com.orman.backend.property.dto.request.UnidadFotoRequest;
import com.orman.backend.property.dto.request.UnidadRequest;
import com.orman.backend.property.dto.response.PropiedadResponse;
import com.orman.backend.property.dto.response.UnidadFotoResponse;
import com.orman.backend.property.dto.response.UnidadResponse;
import com.orman.backend.property.service.PropiedadService;
import com.orman.backend.property.service.UnidadFotoService;
import com.orman.backend.property.service.UnidadService;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import java.math.BigDecimal;
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
    @Autowired private PropiedadService propiedadService;
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
        assertThat(unidadService.listByPropiedad(propiedad.codprop(), org.springframework.data.domain.PageRequest.of(0, 20), authentication)
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
    }

    private Persona createPersona(String ci) {
        Persona persona = new Persona();
        persona.setCi(ci);
        persona.setNombre("Persona de prueba");
        persona.setGenero('F');
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

    private Authentication authentication(Usuario usuario) {
        return new TestingAuthenticationToken(new AuthenticatedUser(usuario.getLogin(), UUID.randomUUID()), null,
                List.of(new SimpleGrantedAuthority("ROLE_PROPIETARIO")));
    }

    private PropiedadRequest propiedadRequest(Integer codperPropietaria) {
        return new PropiedadRequest("Propiedad de prueba", "CASA", "Calle 1", "La Paz", null,
                null, null, null, codperPropietaria, new BigDecimal("1000.00"), (short) 1);
    }

    private UnidadRequest unidadRequest(String nombre) {
        return new UnidadRequest(nombre, "DEPARTAMENTO", null, new BigDecimal("40.00"), (short) 1, (short) 1,
                1, "Bloque A", new BigDecimal("2500.00"), (short) 1);
    }

    private UnidadFotoRequest fotoRequest(String url, Integer orden) {
        return new UnidadFotoRequest(url, null, "Sala", orden);
    }
}
