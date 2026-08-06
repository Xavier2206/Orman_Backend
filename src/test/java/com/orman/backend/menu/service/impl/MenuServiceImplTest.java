package com.orman.backend.menu.service.impl;

import com.orman.backend.common.exception.BusinessRuleException;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.menu.dto.request.CreateMenuRequest;
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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
}
