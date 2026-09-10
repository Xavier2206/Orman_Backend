package com.orman.backend.menu.service.impl;

import com.orman.backend.common.exception.BusinessRuleException;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.menu.dto.request.CreateMenuRequest;
import com.orman.backend.menu.dto.response.MenuResumenResponse;
import com.orman.backend.menu.entity.Menu;
import com.orman.backend.menu.mapper.MenuMapper;
import com.orman.backend.menu.repository.MeProRepository;
import com.orman.backend.menu.repository.MenuRepository;
import com.orman.backend.menu.service.MeProService;
import com.orman.backend.process.entity.Proceso;
import com.orman.backend.process.repository.ProcesoRepository;
import com.orman.backend.role.entity.Rol;
import com.orman.backend.role.repository.RolMeRepository;
import com.orman.backend.role.repository.RolRepository;
import com.orman.backend.role.service.RolMeService;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuServiceImplTest {
    @Mock private MenuRepository menuRepository;
    @Mock private EntityManager entityManager;
    @Spy private MenuMapper menuMapper = new MenuMapper();
    @InjectMocks private MenuServiceImpl menuService;
    @Mock private RolRepository rolRepository;
    @Mock private RolMeRepository rolMeRepository;
    @Mock private ProcesoRepository procesoRepository;
    @Mock private MeProRepository meProRepository;

    @Test
    void createsNormalizedMenuAndRejectsDuplicateName() {
        Menu saved = new Menu("PERSONAS", "users", (short) 1);
        org.springframework.test.util.ReflectionTestUtils.setField(saved, "codm", 10);
        when(menuRepository.existsByNombre("PERSONAS")).thenReturn(false);
        when(menuRepository.saveAndFlush(any(Menu.class))).thenReturn(saved);
        assertThat(menuService.create(new CreateMenuRequest(" personas ", " users ", (short) 1)).nombre()).isEqualTo("PERSONAS");
        when(menuRepository.existsByNombre("PERSONAS")).thenReturn(true);
        assertThatThrownBy(() -> menuService.create(new CreateMenuRequest("personas", null, null))).isInstanceOf(ConflictException.class);
    }

    @Test
    void rejectsInactiveEndsWhenAssigningRelations() {
        Rol inactiveRol = new Rol(); org.springframework.test.util.ReflectionTestUtils.setField(inactiveRol, "codr", 1); inactiveRol.setNombre("OPERADOR"); inactiveRol.setEstado((short) 0);
        Menu activeMenu = new Menu("PERSONAS", null, (short) 1);
        org.springframework.test.util.ReflectionTestUtils.setField(activeMenu, "codm", 2);
        when(rolRepository.findById(1)).thenReturn(Optional.of(inactiveRol));
        when(menuRepository.findById(2)).thenReturn(Optional.of(activeMenu));
        RolMeService rolMeService = new com.orman.backend.role.service.impl.RolMeServiceImpl(rolRepository, menuRepository, rolMeRepository);
        assertThatThrownBy(() -> rolMeService.assign(1, 2)).isInstanceOf(BusinessRuleException.class);

        Proceso inactiveProceso = new Proceso("LISTAR", "personas", (short) 0);
        org.springframework.test.util.ReflectionTestUtils.setField(inactiveProceso, "codp", 3);
        when(procesoRepository.findById(3)).thenReturn(Optional.of(inactiveProceso));
        MeProService meProService = new MeProServiceImpl(menuRepository, procesoRepository, meProRepository);
        assertThatThrownBy(() -> meProService.assign(2, 3)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void searchesMenusWithNormalizedQueryAndFilteredPagination() {
        Menu menu = new Menu("CONTROL DE ACCESO", "shield", (short) 1);
        org.springframework.test.util.ReflectionTestUtils.setField(menu, "codm", 11);
        Pageable pageable = PageRequest.of(1, 2, Sort.by("nombre").ascending());
        when(menuRepository.search(eq("control"), eq((short) 1), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(menu), pageable, 3));

        var response = menuService.list(" control ", (short) 1, pageable);

        assertThat(response.content()).extracting(value -> value.nombre()).containsExactly("CONTROL DE ACCESO");
        assertThat(response.totalElements()).isEqualTo(3);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.first()).isFalse();
        assertThat(response.last()).isTrue();
        verify(menuRepository).search("control", (short) 1, pageable);
    }

    @Test
    void treatsBlankQueryAsNoFilterAndBuildsGlobalResumen() {
        Pageable pageable = PageRequest.of(0, 20);
        when(menuRepository.search(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));
        when(menuRepository.count()).thenReturn(5L);
        when(menuRepository.countByEstado((short) 1)).thenReturn(3L);
        when(menuRepository.countByEstado((short) 0)).thenReturn(2L);

        assertThat(menuService.list("   ", null, pageable).totalElements()).isZero();
        assertThat(menuService.resumen()).isEqualTo(new MenuResumenResponse(5, 3, 2));
        verify(menuRepository).search(isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void supportsResumenWhenAllMenusHaveTheSameEstado() {
        when(menuRepository.count()).thenReturn(3L, 2L, 0L);
        when(menuRepository.countByEstado((short) 1)).thenReturn(3L, 0L, 0L);
        when(menuRepository.countByEstado((short) 0)).thenReturn(0L, 2L, 0L);

        assertThat(menuService.resumen()).isEqualTo(new MenuResumenResponse(3, 3, 0));
        assertThat(menuService.resumen()).isEqualTo(new MenuResumenResponse(2, 0, 2));
        assertThat(menuService.resumen()).isEqualTo(new MenuResumenResponse(0, 0, 0));
    }
}
