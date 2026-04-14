package com.app.WsCancelations.service;

import com.app.WsCancelations.utils.Constantes;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class OrderAlertService {
    /*
    Usa MailService para mandar los mensajes y verifica las órdenes y envía alertas específicas por correo dependiendo del caso:
    -Órdenes “Next Day”
    -Órdenes “No Despachar”
    -Órdenes “Sí y No”
    */

    @Autowired
    private MailService mailService;

    private final String EMAIL_NEXTDAY = Constantes.getEnv("EMAIL_NEXTDAY");
    private final String EMAIL_NODESPACHAR = Constantes.getEnv("EMAIL_NODESPACHAR");
    private final String EMAIL_SIYNO = Constantes.getEnv("EMAIL_SIYNO");

    public void enviarAlerta(String orden, String fechaEntrega, String nextDayValue) {
        if (nextDayValue == null || nextDayValue.trim().isEmpty()) {
            enviarCorreoNoDespachar(orden, fechaEntrega);
            return;
        }

        String valor = nextDayValue.trim().toUpperCase();

        if ("SI Y NO".equals(valor)) {
            enviarCorreoSiYNo(orden, fechaEntrega);
        } else if (valor.contains("SI")) {
            enviarCorreoNextDay(orden, fechaEntrega);
        } else {
            enviarCorreoNoDespachar(orden, fechaEntrega);
        }
    }

    public void enviarAlerta(String orden, String fechaEntrega, boolean tieneNextday, boolean tieneNormal) {
        if (tieneNextday && tieneNormal) {
            enviarCorreoSiYNo(orden, fechaEntrega);
        } else if (tieneNextday) {
            enviarCorreoNextDay(orden, fechaEntrega);
        } else {
            enviarCorreoNoDespachar(orden, fechaEntrega);
        }
    }

    private void enviarCorreoNextDay(String orden, String fechaEntrega) {
        String subject = "PRIORITARIO NEXT DAY enncargado CANCELAR " + orden + " ALERTA DE FRAUDE SIGNIFYD";
        String content = "Buen día,\n\n" +
                "Se solicita inicialmente cancelar orden " + orden + " NEXT DAY, Alerta de Fraude en signifyd.\n" +
                "Se deja en el drive para revisión con cliente.\n\n" +
                "Delivery date: " + (fechaEntrega != null ? fechaEntrega : "");

        try {
            String[] destinatarios = parseDestinatarios(EMAIL_NEXTDAY);
            mailService.sendSimpleMail(destinatarios, subject, content);
            log.info("Alerta NEXT DAY enviada para orden {} a {}", orden, String.join(", ", destinatarios));
        } catch (Exception e) {
            log.error("Error enviando alerta NEXT DAY para {}: {}", orden, e.getMessage(), e);
        }
    }

    private void enviarCorreoNoDespachar(String orden, String fechaEntrega) {
        String subject = "PRIORITARIO ALERTA DE FRAUDE SIGNIFYD " + orden + " NO DESPACHAR";
        String content = "Buen día,\n\n" +
                "La orden de referencia " + orden + " no despachar. Alerta de Fraude en Authorize(signifyd).\n" +
                "Se cambia fecha en QBS para NO despacho aún, se adjunta al drive para contactar al cliente.\n" +
                "Así validar si se tiene criterio de nueva revisión y reactivación en QBS con la fecha correcta.\n\n" +
                "Delivery date: " + (fechaEntrega != null ? fechaEntrega : "");

        try {
            String[] destinatarios = parseDestinatarios(EMAIL_NODESPACHAR);
            mailService.sendSimpleMail(destinatarios, subject, content);
            log.info("Alerta NO DESPACHAR enviada para orden {} a {}", orden, String.join(", ", destinatarios));
        } catch (Exception e) {
            log.error("Error enviando alerta NO DESPACHAR para {}: {}", orden, e.getMessage(), e);
        }
    }

    private void enviarCorreoSiYNo(String orden, String fechaEntrega) {
        String subject = "PRIORITARIO ALERTA DE FRAUDE SIGNIFYD " + orden + " NO DESPACHAR ojo item de Bogotá e item Next day";
        String content = "Buen día,\n\n" +
                "La orden de referencia " + orden + " no despachar. Alerta de Fraude en Authorize(signifyd).\n" +
                "Se adjunta al drive para contactar al cliente y validar si se tiene criterio de nueva revisión.\n\n" +
                "Delivery date: " + (fechaEntrega != null ? fechaEntrega : "");

        try {
            String[] destinatarios = parseDestinatarios(EMAIL_SIYNO);
            mailService.sendSimpleMail(destinatarios, subject, content);
            log.info("Alerta 'SI Y NO' enviada para orden {} a {}", orden, String.join(", ", destinatarios));
        } catch (Exception e) {
            log.error("Error enviando alerta 'SI Y NO' para {}: {}", orden, e.getMessage(), e);
        }
    }

    //Divide los correos configurados en variables de entorno por (;)
    private String[] parseDestinatarios(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return new String[]{};
        }
        return raw.split("[;]+");
    }
}
