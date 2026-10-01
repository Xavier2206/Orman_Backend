package com.orman.backend.push.service.impl;

import com.orman.backend.config.OrmanTimeConfig;
import com.orman.backend.auth.model.ClientType;
import com.orman.backend.push.repository.DispositivoPushRepository;
import com.orman.backend.push.service.PushDestination;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PushDestinationService {

    private static final Short ACTIVE = 1;

    private final DispositivoPushRepository pushRepository;
    private final Clock clock;

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public List<PushDestination> findEligible(String login) {
        LocalDateTime now = OrmanTimeConfig.technicalNow(clock);
        return pushRepository.findEligibleDestinations(login, ClientType.MOBILE, now, ACTIVE).stream()
                .map(destination -> new PushDestination(destination.getCoddis(), destination.getInstallationId()))
                .toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deactivate(Long coddis) {
        LocalDateTime now = OrmanTimeConfig.technicalNow(clock);
        pushRepository.deactivateByCoddis(coddis, now);
    }
}
