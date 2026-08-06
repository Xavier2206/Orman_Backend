package com.orman.backend.authorization.integration;

import com.orman.backend.common.exception.LastOwnerRequiredException;
import com.orman.backend.person.dto.CreatePersonaRequest;
import com.orman.backend.person.dto.PersonaResponse;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.person.service.PersonaService;
import com.orman.backend.role.entity.Rol;
import com.orman.backend.role.entity.RolUsuId;
import com.orman.backend.role.repository.RolRepository;
import com.orman.backend.role.repository.RolUsuRepository;
import com.orman.backend.role.service.RolUsuService;
import com.orman.backend.user.dto.CreateUsuarioRequest;
import com.orman.backend.user.dto.UsuarioResponse;
import com.orman.backend.user.repository.UsuarioRepository;
import com.orman.backend.user.service.UsuarioService;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LastOwnerConcurrencyIntegrationTest {

    private static final String PASSWORD = "clave-ficticia";

    @Autowired private PersonaService personaService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private RolUsuService rolUsuService;
    @Autowired private PersonaRepository personaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private RolUsuRepository rolUsuRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    @Test
    void concurrentDeactivationsNeverLeaveZeroActiveOwners() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        RolState roleState = ensureOwnerRole();
        Fixture first = createFixture("M112C1" + suffix, "m112c1" + suffix);
        Fixture second = createFixture("M112C2" + suffix, "m112c2" + suffix);
        long baseline = rolUsuRepository.countActiveOwners();

        try {
            rolUsuService.assign(first.login(), roleState.role().getCodr());
            rolUsuService.assign(second.login(), roleState.role().getCodr());

            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            CompletableFuture<Boolean> firstResult = deactivateConcurrently(first.login(), ready, start);
            CompletableFuture<Boolean> secondResult = deactivateConcurrently(second.login(), ready, start);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            boolean firstDeactivated = firstResult.get(20, TimeUnit.SECONDS);
            boolean secondDeactivated = secondResult.get(20, TimeUnit.SECONDS);
            long finalCount = rolUsuRepository.countActiveOwners();

            assertThat(finalCount).isGreaterThanOrEqualTo(1);
            if (baseline == 0) {
                assertThat(firstDeactivated ^ secondDeactivated).isTrue();
                assertThat(finalCount).isEqualTo(1);
            }
        } finally {
            cleanup(first, second, roleState);
        }
    }

    private CompletableFuture<Boolean> deactivateConcurrently(String login, CountDownLatch ready,
            CountDownLatch start) {
        return CompletableFuture.supplyAsync(() -> {
            ready.countDown();
            try {
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("No se pudo iniciar la prueba concurrente.");
                }
                usuarioService.deactivate(login);
                return true;
            } catch (LastOwnerRequiredException exception) {
                return false;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Prueba concurrente interrumpida.", exception);
            }
        });
    }

    private RolState ensureOwnerRole() {
        Rol existing = rolRepository.findAll().stream()
                .filter(role -> "PROPIETARIO".equals(role.getNombre()))
                .findFirst().orElse(null);
        if (existing != null) {
            Short originalState = existing.getEstado();
            if (!Short.valueOf((short) 1).equals(originalState)) {
                existing.setEstado((short) 1);
                rolRepository.saveAndFlush(existing);
            }
            return new RolState(existing, false, originalState);
        }
        Rol role = new Rol();
        role.setNombre("PROPIETARIO");
        role.setEstado((short) 1);
        return new RolState(rolRepository.saveAndFlush(role), true, null);
    }

    private Fixture createFixture(String ci, String login) {
        PersonaResponse person = personaService.create(new CreatePersonaRequest(ci, "Propietario concurrente", null,
                null, "F", null, null, "70000000", "A", null));
        UsuarioResponse user = usuarioService.create(new CreateUsuarioRequest(login, PASSWORD, null, person.codper()));
        return new Fixture(user.login(), person.codper());
    }

    private void cleanup(Fixture first, Fixture second, RolState roleState) {
        transactionTemplate.executeWithoutResult(status -> {
            rolUsuRepository.deleteById(new RolUsuId(first.login(), roleState.role().getCodr()));
            rolUsuRepository.deleteById(new RolUsuId(second.login(), roleState.role().getCodr()));
            rolUsuRepository.flush();
            usuarioRepository.deleteById(first.login());
            usuarioRepository.deleteById(second.login());
            usuarioRepository.flush();
            personaRepository.deleteById(first.codper());
            personaRepository.deleteById(second.codper());
            personaRepository.flush();
            if (roleState.created()) {
                rolRepository.deleteById(roleState.role().getCodr());
                rolRepository.flush();
            } else if (!roleState.originalState().equals(roleState.role().getEstado())) {
                roleState.role().setEstado(roleState.originalState());
                rolRepository.saveAndFlush(roleState.role());
            }
        });
    }

    private record Fixture(String login, Integer codper) {
    }

    private record RolState(Rol role, boolean created, Short originalState) {
    }
}
