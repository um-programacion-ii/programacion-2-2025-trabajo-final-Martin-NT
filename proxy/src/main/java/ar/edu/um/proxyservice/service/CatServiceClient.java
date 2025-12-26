package ar.edu.um.proxyservice.service;
import ar.edu.um.proxyservice.client.CatServiceFeignClient;
import java.util.Map;

import ar.edu.um.proxyservice.service.dto.BloquearAsientosRequestDTO;
import ar.edu.um.proxyservice.service.dto.BloquearAsientosResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
/**
 * Fachada sobre CatServiceFeignClient.
 *
 * - Centraliza logs y errores básicos.
 * - No transforma DTOs (eso lo hace el controller/servicio que arma el Map/DTO).
 * - JWT se aplica en CatServiceFeignConfig.
 */
@Service
public class CatServiceClient {
    private static final Logger log = LoggerFactory.getLogger(CatServiceClient.class);

    private final CatServiceFeignClient feignClient;

    public CatServiceClient(CatServiceFeignClient feignClient) {
        this.feignClient = feignClient;
    }

    public String listarEventosResumidos() {
        String op = "listarEventosResumidos";
        try {
            log.info("🎓 [Cátedra] Llamando a {} vía Feign", op);
            String body = feignClient.listarEventosResumidos();
            log.info("🎓 [Cátedra] Respuesta {} -> bodyLength={}", op, body != null ? body.length() : null);
            return body;
        } catch (Exception e) {
            log.error("🎓 [Cátedra] Error llamando a {} vía Feign", op, e);
            return null;
        }
    }

    public String listarEventosCompletos() {
        String op = "listarEventosCompletos";
        try {
            log.info("🎓 [Cátedra] Llamando a {} vía Feign", op);
            String body = feignClient.listarEventosCompletos();
            log.info("🎓 [Cátedra] Respuesta {} -> bodyLength={}", op, body != null ? body.length() : null);
            return body;
        } catch (Exception e) {
            log.error("🎓 [Cátedra] Error llamando a {} vía Feign", op, e);
            return null;
        }
    }

    public String obtenerEventoPorId(Long id) {
        String op = "obtenerEventoPorId";
        try {
            log.info("🎓 [Cátedra] Llamando a {}({}) vía Feign", op, id);
            String body = feignClient.obtenerEventoPorId(id);
            log.info("🎓 [Cátedra] Respuesta {}({}) -> bodyLength={}", op, id, body != null ? body.length() : null);
            return body;
        } catch (Exception e) {
            log.error("🎓 [Cátedra] Error llamando a {}({}) vía Feign", op, id, e);
            return null;
        }
    }

    public String forzarActualizacion() {
        String op = "forzarActualizacion";
        try {
            log.info("🎓 [Cátedra] Llamando a {} vía Feign", op);
            String body = feignClient.forzarActualizacion();
            log.info("🎓 [Cátedra] Respuesta {} -> bodyLength={}", op, body != null ? body.length() : null);
            return body;
        } catch (Exception e) {
            log.error("🎓 [Cátedra] Error llamando a {} vía Feign", op, e);
            return null;
        }
    }

    /**
     * POST /api/endpoints/v1/realizar-venta
     */
    public String realizarVenta(Map<String, Object> ventaJson) {
        String op = "realizarVenta";
        try {
            log.info("🎓 [Cátedra] Llamando a {} vía Feign", op);
            String resp = feignClient.realizarVenta(ventaJson);
            log.info("🎓 [Cátedra] Respuesta {} -> bodyLength={}", op, resp != null ? resp.length() : null);
            return resp;
        } catch (Exception e) {
            log.error("🎓 [Cátedra] Error llamando a {} vía Feign", op, e);
            throw e;
        }
    }

    /**
     * POST /api/endpoints/v1/bloquear-asientos
     */
    public BloquearAsientosResponseDTO bloquearAsientos(
            BloquearAsientosRequestDTO request
    ) {
        String op = "bloquearAsientos";
        try {
            log.info("🎓 [Cátedra] Llamando a {} vía Feign con payload={}", op, request);
            return feignClient.bloquearAsientos(request);
        } catch (Exception e) {
            log.error("🎓 [Cátedra] Error llamando a {} vía Feign", op, e);
            throw e;
        }
    }

    /**
     * GET /api/endpoints/v1/listar-ventas
     */
    public String listarVentas() {
        String op = "listarVentas";
        try {
            log.info("🎓 [Cátedra] Llamando a {} vía Feign", op);
            String body = feignClient.listarVentas();
            log.info("🎓 [Cátedra] Respuesta {} -> bodyLength={}", op, body != null ? body.length() : null);
            return body;
        } catch (Exception e) {
            log.error("🎓 [Cátedra] Error llamando a {} vía Feign", op, e);
            return null;
        }
    }

    /**
     * GET /api/endpoints/v1/listar-venta/{id}
     */
    public String listarVentaPorId(Long id) {
        String op = "listarVentaPorId";
        try {
            log.info("🎓 [Cátedra] Llamando a {}({}) vía Feign", op, id);
            String body = feignClient.listarVentaPorId(id);
            log.info("🎓 [Cátedra] Respuesta {}({}) -> bodyLength={}", op, id, body != null ? body.length() : null);
            return body;
        } catch (Exception e) {
            log.error("🎓 [Cátedra] Error llamando a {}({}) vía Feign", op, id, e);
            return null;
        }
    }


}
