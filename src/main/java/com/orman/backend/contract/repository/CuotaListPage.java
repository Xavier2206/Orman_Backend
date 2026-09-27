package com.orman.backend.contract.repository;

import java.util.List;

public record CuotaListPage(List<CuotaListProjection> content, long totalElements) {
}
