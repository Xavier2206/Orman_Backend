package com.orman.backend.contract.integration;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.common.exception.BusinessRuleException;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.contract.dto.request.ContratoRequest;
import com.orman.backend.contract.dto.request.RescisionContratoRequest;
import com.orman.backend.contract.dto.response.ContratoResponse;
import com.orman.backend.contract.service.ContratoArchivoService;
import com.orman.backend.contract.entity.CuotaEntity;
import com.orman.backend.contract.entity.CuotaEstado;
import com.orman.backend.contract.entity.ContratoEstado;
import com.orman.backend.contract.repository.CuotaRepository;
import com.orman.backend.contract.service.ContratoService;
import com.orman.backend.contract.service.impl.ContratoActivacionScheduler;
import com.orman.backend.payment.entity.MetodoPago;
import com.orman.backend.payment.entity.OrigenRegistroPago;
import com.orman.backend.payment.entity.PagoEntity;
import com.orman.backend.payment.entity.PagoEstado;
import com.orman.backend.payment.repository.PagoRepository;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.property.entity.PropiedadEntity;
import com.orman.backend.property.entity.UnidadEntity;
import com.orman.backend.property.repository.PropiedadRepository;
import com.orman.backend.property.repository.UnidadRepository;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@Rollback
class ContractModuleIntegrationTest {

    private static final LocalDate MES_ACTUAL = LocalDate.now(ZoneId.of("America/La_Paz")).withDayOfMonth(1);

    @Autowired private PersonaRepository personaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PropiedadRepository propiedadRepository;
    @Autowired private UnidadRepository unidadRepository;
    @Autowired private CuotaRepository cuotaRepository;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private ContratoService contratoService;
    @Autowired private ContratoArchivoService contratoArchivoService;
    @Autowired private ContratoActivacionScheduler contratoActivacionScheduler;

    @Test
    void createsCurrentContractAsActiveAndFutureContractAsScheduledWithQuotas() {
        Context current = context("CURRENT", (short) 1, (short) 1, (short) 1);
        ContratoResponse vigente = create(current, MES_ACTUAL, MES_ACTUAL.plusMonths(2));
        assertThat(vigente.estado()).isEqualTo("VIGENTE");
        assertThat(vigente.moneda()).isEqualTo("BOB");
        assertThat(cuotaRepository.findAllByContratoCodconOrderByPeriodoAsc(vigente.codcon())).hasSize(2);

        Context future = context("FUTURE", (short) 1, (short) 1, (short) 1);
        ContratoResponse programado = create(future, MES_ACTUAL.plusMonths(1), MES_ACTUAL.plusMonths(3));
        assertThat(programado.estado()).isEqualTo("PROGRAMADO");
        assertThat(cuotaRepository.findAllByContratoCodconOrderByPeriodoAsc(programado.codcon())).hasSize(2);
    }

    @Test
    void isolatesContractFileUploadListingDownloadAndDeletionByPropertyOwner() throws Exception {
        Context ownerA = context("FILEOWNER-A", (short) 1, (short) 1, (short) 1);
        ContratoResponse contractA = create(ownerA, MES_ACTUAL, MES_ACTUAL.plusMonths(1));
        Context ownerB = context("FILEOWNER-B", (short) 1, (short) 1, (short) 1);
        ContratoResponse contractB = create(ownerB, MES_ACTUAL, MES_ACTUAL.plusMonths(1));
        byte[] pdf;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            pdf = output.toByteArray();
        }
        var file = contratoArchivoService.create(contractB.codcon(),
                new MockMultipartFile("archivo", "contrato-b.pdf", "application/pdf", pdf), 0,
                ownerB.authentication());

        assertThatThrownBy(() -> contratoArchivoService.create(contractB.codcon(),
                new MockMultipartFile("archivo", "otro.pdf", "application/pdf", pdf), 1,
                ownerA.authentication())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> contratoArchivoService.listByContrato(contractB.codcon(), ownerA.authentication()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> contratoArchivoService.download(contractB.codcon(), file.codarc(),
                ownerA.authentication())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> contratoArchivoService.delete(contractB.codcon(), file.codarc(),
                ownerA.authentication())).isInstanceOf(AccessDeniedException.class);
        assertThat(contratoArchivoService.listByContrato(contractB.codcon(), ownerB.authentication()))
                .extracting(row -> row.codarc()).containsExactly(file.codarc());
        assertThat(contractA.codcon()).isNotEqualTo(contractB.codcon());
    }

    @Test
    void filtersContractsByPropertyUnitAndStateWithoutCrossingOwnerBoundary() {
        Context propertyA = context("FILTERA", (short) 1, (short) 1, (short) 1);
        ContratoResponse propertyACurrent = create(propertyA, MES_ACTUAL, MES_ACTUAL.plusMonths(1));
        ContratoResponse propertyAFuture = create(propertyA, MES_ACTUAL.plusMonths(1), MES_ACTUAL.plusMonths(2));

        Context propertyB = context("FILTERB", (short) 1, (short) 1, (short) 1);
        create(propertyB, MES_ACTUAL, MES_ACTUAL.plusMonths(1));

        Integer codpropA = propertyA.unidad().getPropiedad().getCodprop();
        Integer codpropB = propertyB.unidad().getPropiedad().getCodprop();

        assertThat(contratoService.list(null, codpropA, null, null, page(), propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon)
                .containsExactlyInAnyOrder(propertyACurrent.codcon(), propertyAFuture.codcon());
        assertThat(contratoService.list(null, codpropA, propertyA.unidad().getCoduni(), null, page(),
                propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon)
                .containsExactlyInAnyOrder(propertyACurrent.codcon(), propertyAFuture.codcon());
        assertThat(contratoService.list(null, codpropA, propertyA.unidad().getCoduni(), ContratoEstado.VIGENTE, page(),
                propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon)
                .containsExactly(propertyACurrent.codcon());
        assertThat(contratoService.list(null, codpropA, propertyB.unidad().getCoduni(), null, page(),
                propertyA.authentication()).content()).isEmpty();
        assertThat(contratoService.list(null, codpropA, null, ContratoEstado.VIGENTE, page(),
                propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon)
                .containsExactly(propertyACurrent.codcon());
        assertThat(contratoService.list(null, codpropB, null, null, page(), propertyA.authentication()).content())
                .isEmpty();
        assertThat(contratoService.list(null, null, null, null, page(), propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon)
                .containsExactlyInAnyOrder(propertyACurrent.codcon(), propertyAFuture.codcon());
        assertThat(contratoService.list(null, codpropA, null, null, page(), propertyB.authentication()).content())
                .isEmpty();
    }

    @Test
    void summarizesContractStatesWithoutCrossingOwnerBoundary() {
        Context owner = context("SUMMARY", (short) 1, (short) 1, (short) 1);
        ContratoResponse vigente = create(owner, MES_ACTUAL, MES_ACTUAL.plusMonths(1));

        ContratoResponse programado = createWith(owner, addUnidad(owner, "PROGRAMADO"), owner.inquilino(),
                MES_ACTUAL.plusMonths(1), MES_ACTUAL.plusMonths(2));

        ContratoResponse finalizado = createWith(owner, addUnidad(owner, "FINALIZADO"), owner.inquilino(),
                MES_ACTUAL.minusMonths(3), MES_ACTUAL.minusMonths(1));
        markAll(finalizado, CuotaEstado.PAGADA);
        assertThat(contratoService.finish(finalizado.codcon(), owner.authentication()).estado())
                .isEqualTo("FINALIZADO");

        ContratoResponse rescindido = createWith(owner, addUnidad(owner, "RESCINDIDO"), owner.inquilino(),
                MES_ACTUAL.minusMonths(2), MES_ACTUAL.plusMonths(2));
        cuotas(rescindido).stream().filter(q -> !q.getPeriodo().isAfter(MES_ACTUAL))
                .forEach(q -> q.setEstado(CuotaEstado.PAGADA));
        cuotaRepository.flush();
        assertThat(rescind(rescindido, owner).estado()).isEqualTo("RESCINDIDO");

        Context otherOwner = context("SUMMARYOTHER", (short) 1, (short) 1, (short) 1);
        create(otherOwner, MES_ACTUAL, MES_ACTUAL.plusMonths(1));

        var ownerSummary = contratoService.resumen(owner.authentication());
        assertThat(ownerSummary.vigentes()).isEqualTo(1);
        assertThat(ownerSummary.programados()).isEqualTo(1);
        assertThat(ownerSummary.finalizados()).isEqualTo(1);
        assertThat(ownerSummary.rescindidos()).isEqualTo(1);

        var otherOwnerSummary = contratoService.resumen(otherOwner.authentication());
        assertThat(otherOwnerSummary.vigentes()).isEqualTo(1);
        assertThat(otherOwnerSummary.programados()).isZero();
        assertThat(otherOwnerSummary.finalizados()).isZero();
        assertThat(otherOwnerSummary.rescindidos()).isZero();
        assertThat(vigente.codcon()).isNotEqualTo(0);
        assertThat(programado.codcon()).isNotEqualTo(0);
    }

    @Test
    void searchesContractsByTenantNameAndKeepsOwnerIsolation() {
        Context propertyA = context("SEARCHA", (short) 1, (short) 1, (short) 1);
        setTenantName(propertyA, "Álvaro", "Pérez", "Gómez");
        ContratoResponse contractA = create(propertyA, MES_ACTUAL, MES_ACTUAL.plusMonths(1));

        Context propertyB = context("SEARCHB", (short) 1, (short) 1, (short) 1);
        setTenantName(propertyB, "Álvaro", "Pérez", "Gómez");
        create(propertyB, MES_ACTUAL, MES_ACTUAL.plusMonths(1));

        Integer codpropA = propertyA.unidad().getPropiedad().getCodprop();

        // Búsqueda por nombre (con y sin acento, mayúsculas y minúsculas)
        assertThat(contratoService.list("álvaro", null, null, null, page(), propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon).containsExactly(contractA.codcon());
        assertThat(contratoService.list("alvaro", null, null, null, page(), propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon).containsExactly(contractA.codcon());
        assertThat(contratoService.list("ÁLVARO", null, null, null, page(), propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon).containsExactly(contractA.codcon());
        assertThat(contratoService.list("ALVARO", null, null, null, page(), propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon).containsExactly(contractA.codcon());

        // Búsqueda por apellido paterno (con y sin acento)
        assertThat(contratoService.list("Pérez", null, null, null, page(), propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon).containsExactly(contractA.codcon());
        assertThat(contratoService.list("perez", null, null, null, page(), propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon).containsExactly(contractA.codcon());
        assertThat(contratoService.list("PEREZ", null, null, null, page(), propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon).containsExactly(contractA.codcon());

        // Búsqueda por apellido materno (con y sin acento)
        assertThat(contratoService.list("Gómez", null, null, null, page(), propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon).containsExactly(contractA.codcon());
        assertThat(contratoService.list("gomez", null, null, null, page(), propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon).containsExactly(contractA.codcon());
        assertThat(contratoService.list("GOMEZ", null, null, null, page(), propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon).containsExactly(contractA.codcon());

        // Búsqueda parcial
        assertThat(contratoService.list("álv", null, null, null, page(), propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon).containsExactly(contractA.codcon());
        assertThat(contratoService.list("alv", null, null, null, page(), propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon).containsExactly(contractA.codcon());
        assertThat(contratoService.list("Pe", null, null, null, page(), propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon).containsExactly(contractA.codcon());
        assertThat(contratoService.list("Góm", null, null, null, page(), propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon).containsExactly(contractA.codcon());
        assertThat(contratoService.list("mez", null, null, null, page(), propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon).containsExactly(contractA.codcon());

        // Búsqueda por combinaciones (nombre + apellido, ambos apellidos, etc.)
        assertThat(contratoService.list("Álvaro Pérez", null, null, null, page(), propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon).containsExactly(contractA.codcon());
        assertThat(contratoService.list("alvaro perez", null, null, null, page(), propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon).containsExactly(contractA.codcon());
        assertThat(contratoService.list("Álvaro Gómez", null, null, null, page(), propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon).containsExactly(contractA.codcon());
        assertThat(contratoService.list("Pérez Gómez", null, null, null, page(), propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon).containsExactly(contractA.codcon());
        assertThat(contratoService.list("Pérez Álvaro", null, null, null, page(), propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon).containsExactly(contractA.codcon());
        assertThat(contratoService.list("Álvaro Pérez Gómez", null, null, null, page(), propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon).containsExactly(contractA.codcon());
        assertThat(contratoService.list("  alvaro    perez  ", null, null, null, page(), propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon).containsExactly(contractA.codcon());

        // Combinación con estado
        assertThat(contratoService.list("alvaro", null, null, ContratoEstado.VIGENTE, page(),
                propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon).containsExactly(contractA.codcon());

        // Combinación con propiedad
        assertThat(contratoService.list("alvaro", codpropA, null, null, page(), propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon).containsExactly(contractA.codcon());

        // Aislamiento por propietaria
        assertThat(contratoService.list("alvaro", codpropA, null, null, page(), propertyB.authentication()).content())
                .isEmpty();

        // Sin coincidencia
        assertThat(contratoService.list("NoExiste", null, null, null, page(), propertyA.authentication()).content())
                .isEmpty();
    }

    @Test
    void searchesTenantWithMaternalSurnameOnlyAndAvoidsDoubleSpaces() {
        Context context = context("MATONLY", (short) 1, (short) 1, (short) 1);
        setTenantName(context, "Carlos", null, "Mamani");
        ContratoResponse contract = create(context, MES_ACTUAL, MES_ACTUAL.plusMonths(1));

        assertThat(contratoService.list("Carlos", null, null, null, page(), context.authentication()).content())
                .extracting(ContratoResponse::codcon).containsExactly(contract.codcon());
        assertThat(contratoService.list("Mamani", null, null, null, page(), context.authentication()).content())
                .extracting(ContratoResponse::codcon).containsExactly(contract.codcon());
        assertThat(contratoService.list("Carlos Mamani", null, null, null, page(), context.authentication()).content())
                .extracting(ContratoResponse::codcon).containsExactly(contract.codcon());
        assertThat(contratoService.list("carlos mamani", null, null, null, page(), context.authentication()).content())
                .extracting(ContratoResponse::codcon).containsExactly(contract.codcon());
    }

    @Test
    void filtersContractsByCombinedFiltersWithAndSemantics() {
        Context context = context("COMB", (short) 1, (short) 1, (short) 1);
        setTenantName(context, "Juan", "Pérez", "Gómez");
        ContratoResponse contractVigente = create(context, MES_ACTUAL, MES_ACTUAL.plusMonths(2));

        UnidadEntity unit2 = addUnidad(context, "U2");
        Persona tenant2 = addInquilino("T2", "María", "López", "Rojas");
        ContratoResponse contractProgramado = createWith(context, unit2, tenant2,
                MES_ACTUAL.plusMonths(1), MES_ACTUAL.plusMonths(3));

        Integer codprop = context.unidad().getPropiedad().getCodprop();
        Integer coduni1 = context.unidad().getCoduni();
        Integer coduni2 = unit2.getCoduni();

        // q + codprop + coduni + estado todos coincidentes
        assertThat(contratoService.list("Juan", codprop, coduni1, ContratoEstado.VIGENTE, page(),
                context.authentication()).content())
                .extracting(ContratoResponse::codcon).containsExactly(contractVigente.codcon());

        // Si cambia estado -> vacío (AND)
        assertThat(contratoService.list("Juan", codprop, coduni1, ContratoEstado.PROGRAMADO, page(),
                context.authentication()).content()).isEmpty();

        // Si cambia unidad -> vacío (AND)
        assertThat(contratoService.list("Juan", codprop, coduni2, ContratoEstado.VIGENTE, page(),
                context.authentication()).content()).isEmpty();

        // Si cambia q -> vacío (AND)
        assertThat(contratoService.list("María", codprop, coduni1, ContratoEstado.VIGENTE, page(),
                context.authentication()).content()).isEmpty();

        // Coincidencia para el segundo contrato
        assertThat(contratoService.list("María", codprop, coduni2, ContratoEstado.PROGRAMADO, page(),
                context.authentication()).content())
                .extracting(ContratoResponse::codcon).containsExactly(contractProgramado.codcon());

        // Si se combina con otra propiedad no perteneciente -> vacío
        assertThat(contratoService.list("Juan", codprop + 9999, coduni1, ContratoEstado.VIGENTE, page(),
                context.authentication()).content()).isEmpty();
    }

    @Test
    void paginatesContractsCorrectly() {
        Context context = context("PAGE", (short) 1, (short) 1, (short) 1);
        setTenantName(context, "Ana", "Silva", "Castro");
        create(context, MES_ACTUAL, MES_ACTUAL.plusMonths(1));

        UnidadEntity unit2 = addUnidad(context, "U2");
        Persona t2 = addInquilino("T2", "Ana", "Torres", "Paz");
        createWith(context, unit2, t2, MES_ACTUAL.plusMonths(1), MES_ACTUAL.plusMonths(2));

        UnidadEntity unit3 = addUnidad(context, "U3");
        Persona t3 = addInquilino("T3", "Beatriz", "Vega", "Molina");
        createWith(context, unit3, t3, MES_ACTUAL.plusMonths(2), MES_ACTUAL.plusMonths(3));

        // Paginación general con size 2
        var page0 = contratoService.list(null, null, null, null, PageRequest.of(0, 2), context.authentication());
        assertThat(page0.content()).hasSize(2);
        assertThat(page0.totalElements()).isEqualTo(3);
        assertThat(page0.totalPages()).isEqualTo(2);
        assertThat(page0.first()).isTrue();
        assertThat(page0.last()).isFalse();

        var page1 = contratoService.list(null, null, null, null, PageRequest.of(1, 2), context.authentication());
        assertThat(page1.content()).hasSize(1);
        assertThat(page1.totalElements()).isEqualTo(3);
        assertThat(page1.totalPages()).isEqualTo(2);
        assertThat(page1.first()).isFalse();
        assertThat(page1.last()).isTrue();

        // Paginación con filtro q="Ana" (2 resultados) y size 1
        var filteredPage0 = contratoService.list("Ana", null, null, null, PageRequest.of(0, 1), context.authentication());
        assertThat(filteredPage0.content()).hasSize(1);
        assertThat(filteredPage0.totalElements()).isEqualTo(2);
        assertThat(filteredPage0.totalPages()).isEqualTo(2);
        assertThat(filteredPage0.first()).isTrue();
        assertThat(filteredPage0.last()).isFalse();

        var filteredPage1 = contratoService.list("Ana", null, null, null, PageRequest.of(1, 1), context.authentication());
        assertThat(filteredPage1.content()).hasSize(1);
        assertThat(filteredPage1.totalElements()).isEqualTo(2);
        assertThat(filteredPage1.totalPages()).isEqualTo(2);
        assertThat(filteredPage1.first()).isFalse();
        assertThat(filteredPage1.last()).isTrue();
    }

    @Test
    void scheduledContractBecomesActiveOnItsStartDateIdempotently() {
        Context context = context("SCHEDULE", (short) 1, (short) 1, (short) 1);
        LocalDate start = MES_ACTUAL.plusMonths(1);
        ContratoResponse contract = create(context, start, start.plusMonths(2));

        assertThat(contratoActivacionScheduler.activateFor(start)).isEqualTo(1);
        assertThat(contratoActivacionScheduler.activateFor(start)).isZero();
        assertThat(contratoService.get(contract.codcon(), context.authentication()).estado()).isEqualTo("VIGENTE");
    }

    @Test
    void acceptsContiguousContractsAndRejectsOverlappingIntervals() {
        Context context = context("OVERLAP", (short) 1, (short) 1, (short) 1);
        create(context, MES_ACTUAL, MES_ACTUAL.plusMonths(2));
        create(context, MES_ACTUAL.plusMonths(2), MES_ACTUAL.plusMonths(4));

        assertThatThrownBy(() -> create(context, MES_ACTUAL.plusMonths(1), MES_ACTUAL.plusMonths(3)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("solapa");
    }

    @Test
    void revalidatesPropertyUnitTenantAndMoneyWhenRegistering() {
        Context inactiveProperty = context("PROPERTY", (short) 0, (short) 1, (short) 1);
        assertThatThrownBy(() -> create(inactiveProperty, MES_ACTUAL, MES_ACTUAL.plusMonths(1)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("Propiedad");

        Context inactiveUnit = context("UNIT", (short) 1, (short) 0, (short) 1);
        assertThatThrownBy(() -> create(inactiveUnit, MES_ACTUAL, MES_ACTUAL.plusMonths(1)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("Unidad");

        Context inactiveTenant = context("TENANT", (short) 1, (short) 1, (short) 0);
        assertThatThrownBy(() -> create(inactiveTenant, MES_ACTUAL, MES_ACTUAL.plusMonths(1)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("inquilina");

        Context activeWrongType = context("TENANTTYPE", (short) 1, (short) 1, (short) 1);
        Persona activeAdmin = persona("ACTIVEADMIN-" + System.nanoTime(), (short) 1);
        assertThatThrownBy(() -> contratoService.create(activeWrongType.unidad().getCoduni(),
                request(activeAdmin.getCodper(), MES_ACTUAL, MES_ACTUAL.plusMonths(1), new BigDecimal("1500.00")),
                activeWrongType.authentication()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("tipo INQUILINO");

        Context money = context("MONEY", (short) 1, (short) 1, (short) 1);
        assertThatThrownBy(() -> contratoService.create(money.unidad().getCoduni(),
                request(money.inquilino().getCodper(), MES_ACTUAL, MES_ACTUAL.plusMonths(1), BigDecimal.ZERO),
                money.authentication())).isInstanceOf(BusinessRuleException.class).hasMessageContaining("mayor a cero");
    }

    @Test
    void rejectsFinishingBeforeContractEnd() {
        Context context = context("EARLY", (short) 1, (short) 1, (short) 1);
        ContratoResponse contract = create(context, MES_ACTUAL, MES_ACTUAL.plusMonths(2));
        assertThatThrownBy(() -> contratoService.finish(contract.codcon(), context.authentication()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("antes");
    }

    @Test
    void rejectsFinishingWithPendingOrPartialQuota() {
        Context pendingContext = context("FINPENDING", (short) 1, (short) 1, (short) 1);
        ContratoResponse pending = create(pendingContext, MES_ACTUAL.minusMonths(2), MES_ACTUAL.minusMonths(1));
        assertThatThrownBy(() -> contratoService.finish(pending.codcon(), pendingContext.authentication()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("pagadas");

        Context partialContext = context("FINPARTIAL", (short) 1, (short) 1, (short) 1);
        ContratoResponse partial = create(partialContext, MES_ACTUAL.minusMonths(2), MES_ACTUAL.minusMonths(1));
        CuotaEntity cuota = cuotas(partial).getFirst();
        cuota.setEstado(CuotaEstado.PARCIAL);
        cuotaRepository.saveAndFlush(cuota);
        assertThatThrownBy(() -> contratoService.finish(partial.codcon(), partialContext.authentication()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("pagadas");
    }

    @Test
    void rejectsFinishingWhenPaymentIsPendingReview() {
        Context context = context("FINPAYMENT", (short) 1, (short) 1, (short) 1);
        ContratoResponse contract = create(context, MES_ACTUAL.minusMonths(2), MES_ACTUAL.minusMonths(1));
        markAll(contract, CuotaEstado.PAGADA);
        pendingPayment(cuotas(contract).getFirst(), context.usuario());
        assertThatThrownBy(() -> contratoService.finish(contract.codcon(), context.authentication()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("pendientes de revisión");
    }

    @Test
    void finishesOnlyAfterDateWithAllQuotasPaid() {
        Context context = context("FINISH", (short) 1, (short) 1, (short) 1);
        ContratoResponse contract = create(context, MES_ACTUAL.minusMonths(2), MES_ACTUAL.minusMonths(1));
        markAll(contract, CuotaEstado.PAGADA);
        assertThat(contratoService.finish(contract.codcon(), context.authentication()).estado())
                .isEqualTo("FINALIZADO");
    }

    @Test
    void rejectsRescissionWhenEffectivePeriodIsPendingOrPartial() {
        Context context = context("RESPENDING", (short) 1, (short) 1, (short) 1);
        ContratoResponse contract = create(context, MES_ACTUAL.minusMonths(2), MES_ACTUAL.plusMonths(3));
        cuotas(contract).stream().filter(q -> q.getPeriodo().isBefore(MES_ACTUAL))
                .forEach(q -> q.setEstado(CuotaEstado.PAGADA));
        cuotaRepository.flush();
        assertThatThrownBy(() -> rescind(contract, context))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("inclusive");

        CuotaEntity current = cuotas(contract).stream().filter(q -> q.getPeriodo().equals(MES_ACTUAL))
                .findFirst().orElseThrow();
        current.setEstado(CuotaEstado.PARCIAL);
        cuotaRepository.saveAndFlush(current);
        assertThatThrownBy(() -> rescind(contract, context))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("inclusive");
    }

    @Test
    void rescindsWhenPaidThroughEffectivePeriodAndAnnulsLaterQuotas() {
        Context context = context("RESCIND", (short) 1, (short) 1, (short) 1);
        ContratoResponse contract = create(context, MES_ACTUAL.minusMonths(2), MES_ACTUAL.plusMonths(3));
        cuotas(contract).stream().filter(q -> !q.getPeriodo().isAfter(MES_ACTUAL))
                .forEach(q -> q.setEstado(CuotaEstado.PAGADA));
        cuotas(contract).stream().filter(q -> q.getPeriodo().isAfter(MES_ACTUAL))
                .findFirst().orElseThrow().setEstado(CuotaEstado.PAGADA);
        cuotaRepository.flush();

        assertThat(rescind(contract, context).estado()).isEqualTo("RESCINDIDO");
        assertThat(cuotas(contract)).allSatisfy(q -> {
            if (q.getPeriodo().isAfter(MES_ACTUAL)) {
                assertThat(q.getEstado()).isEqualTo(CuotaEstado.ANULADA);
            }
        });
    }

    @Test
    void rejectsRescissionWhileAnyPaymentAwaitsReview() {
        Context context = context("RESPAYMENT", (short) 1, (short) 1, (short) 1);
        ContratoResponse contract = create(context, MES_ACTUAL.minusMonths(1), MES_ACTUAL.plusMonths(2));
        cuotas(contract).stream().filter(q -> !q.getPeriodo().isAfter(MES_ACTUAL))
                .forEach(q -> q.setEstado(CuotaEstado.PAGADA));
        cuotaRepository.flush();
        pendingPayment(cuotas(contract).getLast(), context.usuario());
        assertThatThrownBy(() -> rescind(contract, context))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("pendientes de revisión");
    }

    private ContratoResponse create(Context context, LocalDate start, LocalDate end) {
        return contratoService.create(context.unidad().getCoduni(),
                request(context.inquilino().getCodper(), start, end, new BigDecimal("1500.00")),
                context.authentication());
    }

    private void setTenantName(Context context, String name, String ap, String am) {
        context.inquilino().setNombre(name);
        context.inquilino().setAp(ap);
        context.inquilino().setAm(am);
        context.inquilino().setFechaRegistro(LocalDateTime.now());
        personaRepository.flush();
    }

    private UnidadEntity addUnidad(Context context, String suffix) {
        UnidadEntity unit = new UnidadEntity();
        unit.setPropiedad(context.unidad().getPropiedad());
        unit.setNombre(context.unidad().getNombre() + "-" + suffix);
        unit.setTipoUnidad("DEPARTAMENTO");
        unit.setArea(new BigDecimal("40.00"));
        unit.setDormitorios((short) 1);
        unit.setBanos((short) 1);
        unit.setPiso(1);
        unit.setPrecioBase(new BigDecimal("1500.00"));
        unit.setEstadoOperativo((short) 1);
        return unidadRepository.saveAndFlush(unit);
    }

    private Persona addInquilino(String prefix, String name, String ap, String am) {
        String token = Long.toUnsignedString(System.nanoTime(), 36);
        Persona p = persona(prefix + token, (short) 1, 'I');
        p.setNombre(name);
        p.setAp(ap);
        p.setAm(am);
        p.setFechaRegistro(LocalDateTime.now());
        return personaRepository.saveAndFlush(p);
    }

    private ContratoResponse createWith(Context context, UnidadEntity unit, Persona tenant, LocalDate start, LocalDate end) {
        return contratoService.create(unit.getCoduni(),
                request(tenant.getCodper(), start, end, new BigDecimal("1500.00")),
                context.authentication());
    }

    private ContratoResponse rescind(ContratoResponse contract, Context context) {
        return contratoService.rescind(contract.codcon(),
                new RescisionContratoRequest(MES_ACTUAL, "Terminación anticipada acordada."),
                context.authentication());
    }

    private ContratoRequest request(Integer tenant, LocalDate start, LocalDate end, BigDecimal amount) {
        return new ContratoRequest(tenant, start, end, amount, new BigDecimal("500.00"));
    }

    private PageRequest page() {
        return PageRequest.of(0, 20);
    }

    private List<CuotaEntity> cuotas(ContratoResponse contract) {
        return cuotaRepository.findAllByContratoCodconOrderByPeriodoAsc(contract.codcon());
    }

    private void markAll(ContratoResponse contract, CuotaEstado estado) {
        cuotas(contract).forEach(q -> q.setEstado(estado));
        cuotaRepository.flush();
    }

    private void pendingPayment(CuotaEntity cuota, Usuario actor) {
        PagoEntity pago = new PagoEntity();
        pago.setCuota(cuota);
        pago.setMonto(new BigDecimal("10.00"));
        pago.setMetodo(MetodoPago.EFECTIVO);
        pago.setFechaPago(LocalDateTime.now());
        pago.setFechaRegistro(LocalDateTime.now());
        pago.setEstado(PagoEstado.PENDIENTE_REVISION);
        pago.setOrigenRegistro(OrigenRegistroPago.PROPIETARIA);
        pago.setRegistradoPor(actor);
        pago.setIdempotencyKey(UUID.randomUUID());
        pagoRepository.saveAndFlush(pago);
    }

    private Context context(String prefix, short propertyState, short unitState, short tenantState) {
        String token = Long.toUnsignedString(System.nanoTime(), 36);
        Persona owner = persona(prefix + "O" + token, (short) 1);
        Usuario user = usuario("ct." + prefix.toLowerCase() + "." + token, owner);
        PropiedadEntity property = new PropiedadEntity();
        property.setNombre("Propiedad " + prefix + token);
        property.setTipo("CASA");
        property.setDireccion("Calle de prueba");
        property.setCiudad("La Paz");
        property.setPropietaria(owner);
        property.setInversionInicial(BigDecimal.ZERO);
        property.setEstado(propertyState);
        property = propiedadRepository.saveAndFlush(property);
        UnidadEntity unit = new UnidadEntity();
        unit.setPropiedad(property);
        unit.setNombre("Unidad " + prefix + token);
        unit.setTipoUnidad("DEPARTAMENTO");
        unit.setArea(new BigDecimal("40.00"));
        unit.setDormitorios((short) 1);
        unit.setBanos((short) 1);
        unit.setPiso(1);
        unit.setPrecioBase(new BigDecimal("1500.00"));
        unit.setEstadoOperativo(unitState);
        unit = unidadRepository.saveAndFlush(unit);
        Persona tenant = persona(prefix + "T" + token, tenantState, 'I');
        return new Context(user, authentication(user), unit, tenant);
    }

    private Persona persona(String ci, short estado) {
        return persona(ci, estado, 'A');
    }

    private Persona persona(String ci, short estado, char tipoPersona) {
        String normalized = ci.substring(0, Math.min(20, ci.length()));
        Persona persona = new Persona();
        persona.setCi(normalized);
        persona.setNombre("Persona contractual");
        persona.setGenero('F');
        persona.setEstado(estado);
        persona.setCorreo(normalized.toLowerCase() + "@example.test");
        persona.setTelefono("70000000");
        persona.setTipoPersona(tipoPersona);
        persona.setFechaRegistro(LocalDateTime.now());
        return personaRepository.saveAndFlush(persona);
    }

    private Usuario usuario(String login, Persona persona) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login.substring(0, Math.min(30, login.length())));
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

    private record Context(Usuario usuario, Authentication authentication, UnidadEntity unidad, Persona inquilino) {
    }
}
