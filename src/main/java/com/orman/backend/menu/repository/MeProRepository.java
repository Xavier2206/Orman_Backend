package com.orman.backend.menu.repository;

import com.orman.backend.menu.entity.MePro;
import com.orman.backend.menu.entity.MeProId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeProRepository extends JpaRepository<MePro, MeProId> {

    List<MePro> findByIdCodm(Integer codm);

    List<MePro> findByIdCodp(Integer codp);
}
