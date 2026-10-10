package cl.duoc.bancoxyz.bff.common.client;
import java.math.BigDecimal;
public record AccountServiceResponse(Integer accountId, String customerName, String accountType,
        BigDecimal availableBalance) {}
