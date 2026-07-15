package ec.edu.uteq.banco_austro.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultaDistribuidaService {

    private final JdbcTemplate jdbcCuenca;
    private final JdbcTemplate jdbcQuito;
    private final JdbcTemplate jdbcGuayaquil;

    public ConsultaDistribuidaService(
            @Qualifier("dsCuenca") DataSource dsCuenca,
            @Qualifier("dsQuito") DataSource dsQuito,
            @Qualifier("dsGuayaquil") DataSource dsGuayaquil) {
        this.jdbcCuenca = new JdbcTemplate(dsCuenca);
        this.jdbcQuito = new JdbcTemplate(dsQuito);
        this.jdbcGuayaquil = new JdbcTemplate(dsGuayaquil);
    }

    public Map<String, Object> consultarSaldo(String numero) {
        JdbcTemplate destino = enrutar(numero);
        String sql = "SELECT numero, saldo, oficina FROM cuentas WHERE numero = ?";
        List<Map<String, Object>> filas = destino.queryForList(sql, numero);
        
        if (filas.isEmpty()) {
            return Map.of("error", "Cuenta no encontrada", "numero", numero);
        }
        return filas.get(0);
    }

    public List<Map<String, Object>> listarTodosLosClientes() {
        String sql = "SELECT cedula, nombre, ciudad FROM clientes";
        List<Map<String, Object>> union = new ArrayList<>();

        try {
            union.addAll(jdbcCuenca.queryForList(sql));
        } catch (Exception e) {
            System.err.println("Advertencia: Nodo CUENCA fuera de línea.");
        }

        try {
            union.addAll(jdbcQuito.queryForList(sql));
        } catch (Exception e) {
            System.err.println("Advertencia: Nodo QUITO fuera de línea.");
        }

        try {
            union.addAll(jdbcGuayaquil.queryForList(sql));
        } catch (Exception e) {
            System.err.println("Advertencia: Nodo GUAYAQUIL fuera de línea.");
        }

        return union;
    }

    @Transactional
    public Map<String, Object> realizarTransferencia(String origen, String destino, double monto) {
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero.");
        }

        JdbcTemplate dbOrigen = enrutar(origen);
        JdbcTemplate dbDestino = enrutar(destino);

        String sqlSaldo = "SELECT saldo FROM cuentas WHERE numero = ?";
        Double saldoOrigen = dbOrigen.queryForObject(sqlSaldo, Double.class, origen);
        if (saldoOrigen == null || saldoOrigen < monto) {
            throw new IllegalStateException("Saldo insuficiente en la cuenta de origen: " + origen);
        }

        String sqlDebito = "UPDATE cuentas SET saldo = saldo - ? WHERE numero = ?";
        dbOrigen.update(sqlDebito, monto, origen);

        String sqlCredito = "UPDATE cuentas SET saldo = saldo + ? WHERE numero = ?";
        int filasAfectadas = dbDestino.update(sqlCredito, monto, destino);

        if (filasAfectadas == 0) {
            throw new IllegalArgumentException("La cuenta destino no existe: " + destino);
        }

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Transferencia realizada con éxito.");
        respuesta.put("origen", origen);
        respuesta.put("destino", destino);
        respuesta.put("monto", monto);
        respuesta.put("tipo", dbOrigen == dbDestino ? "INTRA_SEDE (Local)" : "INTER_SEDE (Distribuida)");

        return respuesta;
    }

    private JdbcTemplate enrutar(String numero) {
        if (numero == null || numero.length() < 2) {
            throw new IllegalArgumentException("Numero de cuenta invalido: " + numero);
        }
        String prefijo = numero.substring(0, 2);
        return switch (prefijo) {
            case "22" -> jdbcCuenca;
            case "17" -> jdbcQuito;
            case "09" -> jdbcGuayaquil; 
            default -> throw new IllegalArgumentException("Prefijo de oficina no reconocido: " + prefijo);
        };
    }
}