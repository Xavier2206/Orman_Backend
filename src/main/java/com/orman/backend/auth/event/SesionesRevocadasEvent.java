package com.orman.backend.auth.event;

import java.util.List;
import java.util.UUID;

public record SesionesRevocadasEvent(List<UUID> sids) {

    public SesionesRevocadasEvent {
        sids = List.copyOf(sids);
    }
}
