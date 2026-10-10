package cl.duoc.bancoxyz.bff.mobile.service;

import cl.duoc.bancoxyz.bff.common.client.AccountServiceClient;
import cl.duoc.bancoxyz.bff.mobile.dto.MobileCuentaResumen;
import org.springframework.stereotype.Service;

@Service
public class MobileCuentaService {

    private final AccountServiceClient client;
    public MobileCuentaService(AccountServiceClient client) { this.client = client; }

    public MobileCuentaResumen obtenerCuenta(Integer cuentaId) {
        var cuenta = client.findAccount(cuentaId);
        return new MobileCuentaResumen(cuenta.accountId(), cuenta.customerName(), cuenta.accountType(), cuenta.availableBalance());
    }
}
