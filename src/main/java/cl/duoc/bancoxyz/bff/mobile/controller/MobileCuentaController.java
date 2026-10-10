package cl.duoc.bancoxyz.bff.mobile.controller;

import cl.duoc.bancoxyz.bff.mobile.dto.MobileCuentaResumen;
import cl.duoc.bancoxyz.bff.mobile.service.MobileCuentaService;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/mobile")
public class MobileCuentaController {

    private final MobileCuentaService service;

    public MobileCuentaController(MobileCuentaService service) {
        this.service = service;
    }

    @GetMapping("/cuentas/{cuentaId}")
    public MobileCuentaResumen obtenerCuenta(@PathVariable @Min(1) Integer cuentaId) {
        return service.obtenerCuenta(cuentaId);
    }
}
