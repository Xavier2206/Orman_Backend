package com.orman.backend.contract.dto.request;

import com.orman.backend.contract.entity.CuotaEstado;
import com.orman.backend.contract.exception.InvalidCuotaListFilterException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Locale;

public record CuotaListCriteria(
        Integer codperInquilino,
        Integer codcuo,
        LocalDate periodo,
        CuotaEstado estado,
        Vencimiento vencimiento,
        Integer codprop,
        Integer coduni,
        boolean conPagoPendienteRevision,
        int page,
        int size) {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    public static CuotaListCriteria from(String codperInquilino, String periodo, String estado,
                                         String vencimiento, String codprop, String coduni,
                                         String conPagoPendienteRevision, String page, String size) {
        return from(codperInquilino, null, periodo, estado, vencimiento, codprop, coduni,
                conPagoPendienteRevision, page, size);
    }

    public static CuotaListCriteria from(String codperInquilino, String codcuo, String periodo, String estado,
                                         String vencimiento, String codprop, String coduni,
                                         String conPagoPendienteRevision, String page, String size) {
        return new CuotaListCriteria(
                parsePositiveId("codperInquilino", codperInquilino),
                parsePositiveId("codcuo", codcuo),
                parsePeriodo(periodo),
                parseEstado(estado),
                parseVencimiento(vencimiento),
                parsePositiveId("codprop", codprop),
                parsePositiveId("coduni", coduni),
                parseBoolean(conPagoPendienteRevision),
                parsePage(page),
                parseSize(size));
    }

    private static Integer parsePositiveId(String field, String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            int id = Integer.parseInt(value.trim());
            if (id <= 0) {
                throw invalid(field, "El identificador debe ser positivo.");
            }
            return id;
        } catch (NumberFormatException exception) {
            throw invalid(field, "El identificador debe ser un entero positivo.");
        }
    }

    private static LocalDate parsePeriodo(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            LocalDate parsed = LocalDate.parse(value.trim());
            if (parsed.getDayOfMonth() != 1) {
                throw invalid("periodo", "El período debe corresponder al primer día del mes.");
            }
            return parsed;
        } catch (DateTimeException exception) {
            throw invalid("periodo", "El período debe usar el formato ISO yyyy-MM-dd.");
        }
    }

    private static CuotaEstado parseEstado(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return CuotaEstado.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw invalid("estado", "El estado debe ser PENDIENTE, PARCIAL, PAGADA o ANULADA.");
        }
    }

    private static Vencimiento parseVencimiento(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Vencimiento.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw invalid("vencimiento", "El vencimiento debe ser VENCIDAS, HOY, PROXIMAS o AL_DIA.");
        }
    }

    private static boolean parseBoolean(String value) {
        if (isBlank(value)) {
            return false;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "true" -> true;
            case "false" -> false;
            default -> throw invalid("conPagoPendienteRevision", "El valor debe ser true o false.");
        };
    }

    private static int parsePage(String value) {
        if (isBlank(value)) {
            return DEFAULT_PAGE;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < 0) {
                throw invalid("page", "La página debe ser mayor o igual a cero.");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw invalid("page", "La página debe ser un entero mayor o igual a cero.");
        }
    }

    private static int parseSize(String value) {
        if (isBlank(value)) {
            return DEFAULT_SIZE;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < 1 || parsed > MAX_SIZE) {
                throw invalid("size", "El tamaño debe estar entre 1 y 100.");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw invalid("size", "El tamaño debe ser un entero entre 1 y 100.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static InvalidCuotaListFilterException invalid(String field, String message) {
        return new InvalidCuotaListFilterException(field, message);
    }

    public enum Vencimiento {
        VENCIDAS,
        HOY,
        PROXIMAS,
        AL_DIA
    }
}
