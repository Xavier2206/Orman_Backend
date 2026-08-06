package com.orman.backend.auth.model;

import java.util.UUID;

public record AuthenticatedUser(String login, UUID sid) {
}
