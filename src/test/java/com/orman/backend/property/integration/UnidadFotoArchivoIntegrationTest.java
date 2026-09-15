package com.orman.backend.property.integration;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.property.dto.request.PropiedadRequest;
import com.orman.backend.property.dto.request.UnidadFotoMetadataRequest;
import com.orman.backend.property.dto.request.UnidadFotoRequest;
import com.orman.backend.property.dto.request.UnidadRequest;
import com.orman.backend.property.dto.response.PropiedadResponse;
import com.orman.backend.property.dto.response.UnidadFotoResponse;
import com.orman.backend.property.dto.response.UnidadResponse;
import com.orman.backend.property.entity.UnidadFotoEntity;
import com.orman.backend.property.repository.UnidadFotoRepository;
import com.orman.backend.property.service.PropiedadService;
import com.orman.backend.property.service.UnidadFotoService;
import com.orman.backend.property.service.UnidadService;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@Rollback
class UnidadFotoArchivoIntegrationTest {

    @Autowired private PersonaRepository personaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PropiedadService propiedadService;
    @Autowired private UnidadService unidadService;
    @Autowired private UnidadFotoService unidadFotoService;
    @Autowired private UnidadFotoRepository unidadFotoRepository;

    @Test
    void storesServesUpdatesReplacesAndDeletesAnInternalPhotoWithoutBreakingLegacyUrls() throws Exception {
        Persona owner = persona("UF-INT-OWNER");
        Authentication authentication = authentication(usuario("uf.int.owner", owner));
        PropiedadResponse property = propiedadService.create(propertyRequest(owner.getCodper()), authentication);
        UnidadResponse unit = unidadService.create(property.codprop(), unitRequest(), authentication);

        UnidadFotoResponse legacy = unidadFotoService.create(unit.coduni(),
                new UnidadFotoRequest("https://example.test/legacy.jpg", "Sala", "Sala", 0), authentication);
        UnidadFotoResponse internal = unidadFotoService.createInternal(unit.coduni(), png(2200, 1100, true),
                new UnidadFotoMetadataRequest("Baño principal", "Baño", 1), authentication);

        assertThat(legacy.url()).isNotNull();
        assertThat(legacy.tieneArchivo()).isFalse();
        assertThat(internal.url()).isNull();
        assertThat(internal.tieneArchivo()).isTrue();
        UnidadFotoEntity persisted = unidadFotoRepository.findById(internal.id()).orElseThrow();
        assertThat(persisted.getUrl()).isNull();
        assertThat(persisted.getFotoRef()).matches("unidades/" + unit.coduni() + "/[0-9a-f-]+\\.jpg");

        UnidadFotoService.UnidadFotoResource resource = unidadFotoService.getArchivo(unit.coduni(), internal.id(), authentication);
        BufferedImage stored = ImageIO.read(resource.resource().getInputStream());
        assertThat(resource.mediaType().toString()).isEqualTo("image/jpeg");
        assertThat(stored.getWidth()).isEqualTo(1600);
        assertThat(stored.getHeight()).isEqualTo(800);

        UnidadFotoResponse metadata = unidadFotoService.updateMetadata(unit.coduni(), internal.id(),
                new UnidadFotoMetadataRequest("Baño renovado", "Baño", 2), authentication);
        assertThat(metadata.tieneArchivo()).isTrue();
        assertThat(metadata.titulo()).isEqualTo("Baño renovado");
        UnidadFotoResponse replacement = unidadFotoService.replaceArchivo(unit.coduni(), internal.id(), png(40, 20, false),
                authentication);
        assertThat(replacement.id()).isEqualTo(internal.id());
        assertThat(replacement.titulo()).isEqualTo("Baño renovado");
        assertThat(unidadFotoService.getArchivo(unit.coduni(), internal.id(), authentication).resource().contentLength())
                .isPositive();

        assertThat(unidadFotoService.setPortada(unit.coduni(), internal.id(), authentication).portada()).isTrue();
        unidadFotoService.delete(unit.coduni(), internal.id(), authentication);
        assertThat(unidadFotoRepository.findById(internal.id())).isEmpty();
        assertThat(unidadFotoRepository.findById(legacy.id())).isPresent();
    }

    @Test
    void protectsInternalPhotoOperationsByActualOwnership() {
        Persona owner = persona("UF-INT-OWNER-2");
        Authentication ownerAuthentication = authentication(usuario("uf.int.owner.two", owner));
        PropiedadResponse property = propiedadService.create(propertyRequest(owner.getCodper()), ownerAuthentication);
        UnidadResponse unit = unidadService.create(property.codprop(), unitRequest(), ownerAuthentication);

        Persona other = persona("UF-INT-OTHER");
        Authentication otherAuthentication = authentication(usuario("uf.int.other", other));
        assertThatThrownBy(() -> unidadFotoService.createInternal(unit.coduni(), png(30, 30, false),
                new UnidadFotoMetadataRequest(null, "Sala", 0), otherAuthentication))
                .isInstanceOf(AccessDeniedException.class);
    }

    private MockMultipartFile png(int width, int height, boolean transparent) throws Exception {
        BufferedImage image = new BufferedImage(width, height,
                transparent ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        try {
            graphics.setColor(transparent ? new Color(255, 0, 0, 80) : Color.BLUE);
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        return new MockMultipartFile("foto", "foto.png", "image/png", bytes.toByteArray());
    }

    private Persona persona(String ci) {
        Persona persona = new Persona();
        persona.setCi(ci);
        persona.setNombre("Persona Unidad Foto");
        persona.setGenero('F');
        persona.setCorreo(ci.toLowerCase() + "@example.test");
        persona.setTelefono("70000000");
        persona.setTipoPersona('A');
        return personaRepository.saveAndFlush(persona);
    }

    private Usuario usuario(String login, Persona persona) {
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

    private PropiedadRequest propertyRequest(Integer ownerId) {
        return new PropiedadRequest("Propiedad unidad foto", "CASA", "Calle 1", "La Paz", null,
                null, null, null, ownerId, new BigDecimal("1000.00"), (short) 1);
    }

    private UnidadRequest unitRequest() {
        return new UnidadRequest("Unidad foto", "DEPARTAMENTO", null, new BigDecimal("40.00"), (short) 1,
                (short) 1, 1, "Bloque A", new BigDecimal("2500.00"), (short) 1);
    }
}
