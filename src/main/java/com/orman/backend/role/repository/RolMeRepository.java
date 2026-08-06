package com.orman.backend.role.repository;

import com.orman.backend.role.entity.RolMe;
import com.orman.backend.role.entity.RolMeId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolMeRepository extends JpaRepository<RolMe, RolMeId> {

    List<RolMe> findByIdCodr(Integer codr);

    List<RolMe> findByIdCodm(Integer codm);
}
