package cl.duoc.bancoxyz.bff.mobile.dto;

import java.math.BigDecimal;

public record MobileCuentaResumen(
        Integer cuentaId,
        String nombre,
        String tipo,
        BigDecimal saldoDisponible
) {
}
