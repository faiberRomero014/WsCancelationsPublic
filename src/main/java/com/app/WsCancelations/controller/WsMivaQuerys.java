package com.app.WsCancelations.controller;

import com.app.WsCancelations.DTO.OrdenMiva;
import com.app.WsCancelations.interfaces.RecorrerJsonMivaInt;
import com.app.WsCancelations.service.*;
import com.app.WsCancelations.utils.Constantes;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
public class WsMivaQuerys {

    //Contiene las consultas  que se usan para obtener órdenes desde la API de Miva.

    @Autowired
    private RecorrerJsonMivaInt jsonMiva;

    @Autowired
    private OrderService orderService;

    @Autowired
    private RetryScheduler retryScheduler;

    @Autowired
    private AccessFailAlertService accessFailAlertService; 

    // EJECUCIÓN AUTOMÁTICA INMEDIATA AL INICIAR
    @EventListener(ApplicationReadyEvent.class)
    public void ejecutarAlIniciar() {
        log.info("Ejecutando proceso inicial al iniciar la aplicación...");
        if (retryScheduler.marcarInicio()) {
            ejecutarProceso(1, true);
        } else {
            log.warn("Ya había un proceso en curso al iniciar.");
        }
    }

    // EJECUCIÓN AUTOMÁTICA PROGRAMADA (cada hora)
    @Scheduled(cron = "0 0/60 * * * ?")
    public void tareaProgramada() {
        log.info("Ejecución automática programada iniciada...");
        if (!retryScheduler.marcarInicio()) {
            log.warn("Ya hay una ejecución automática en curso. Se omite este ciclo.");
            return;
        }
        ejecutarProceso(1, true);
    }

    // ENDPOINT MANUAL
    @GetMapping("/consultarOrdenesPendientesPago")
    public ResponseEntity<String> ejecutarManual() {
        log.info("Ejecución manual solicitada vía endpoint.");
        ejecutarProceso(1, false);
        return ResponseEntity.ok("Ejecución manual iniciada correctamente.");
    }

    private void ejecutarProceso(int intento, boolean esAutomatico) {
        int totalIntentos = 4; // 1 inicial + 3 reintentos
        try {
            ZoneId zonaLocal = ZoneId.of("America/Bogota");
            LocalDateTime ahora = LocalDateTime.now();

            long fin = ahora.atZone(zonaLocal).toEpochSecond();
            long ini = ahora.minusHours(Integer.parseInt(Constantes.getEnv(Constantes.horasRevisar)))
                    .atZone(zonaLocal).toEpochSecond();

            log.info("Consulta desde {} hasta {}", ini, fin);

            List<OrdenMiva> respuesta = jsonMiva.consultaOrdenesNoPagas(String.valueOf(ini), String.valueOf(fin));

            if (respuesta == null || respuesta.isEmpty()) {
                log.warn("No se encontraron nuevas órdenes a actualizar en el intento {}/{}", intento, totalIntentos);
            } else {
                orderService.exportOrderToOneDrive(respuesta);
                log.info("Proceso ejecutado correctamente en el intento {}/{}", intento, totalIntentos);
            }

        } catch (Exception e) {
            log.error("Error en intento {}/{}: {}", intento, totalIntentos, e.getMessage());

            if (e.getMessage() != null && e.getMessage().contains("423") && intento < totalIntentos) {
                log.warn("Archivo bloqueado. Reintentando en 5 minutos. Intento {}/{}", intento + 1, totalIntentos);
                retryScheduler.scheduleRetry(() -> ejecutarProceso(intento + 1, true), 5);
            } else if (intento >= totalIntentos) {
                log.error("Se alcanzó el máximo de intentos ({}). Enviando correo de error.", totalIntentos);
                accessFailAlertService.enviarAlertaFalloAcceso(e);
            }
        } finally {
            retryScheduler.marcarFin();
        }
    }
}
