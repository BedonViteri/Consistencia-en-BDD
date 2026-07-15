package ec.edu.uteq.banco_austro.controller;

import java.util.List;
import java.util.Map;
import ec.edu.uteq.banco_austro.service.ConsultaDistribuidaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/banco")
public class BancoController {

    private final ConsultaDistribuidaService service;

    public BancoController(ConsultaDistribuidaService service) {
        this.service = service;
    }

    @GetMapping("/saldo/{numero}")
    public Map<String, Object> saldo(@PathVariable String numero) {
        return service.consultarSaldo(numero);
    }

    @GetMapping("/clientes")
    public List<Map<String, Object>> clientes() {
        return service.listarTodosLosClientes();
    }

    @PostMapping("/transferencia")
    public Map<String, Object> transferir(@RequestBody Map<String, Object> payload) {
        String origen = (String) payload.get("origen");
        String destino = (String) payload.get("destino");
        double monto = Double.parseDouble(payload.get("monto").toString());

        try {
            return service.realizarTransferencia(origen, destino, monto);
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }
}