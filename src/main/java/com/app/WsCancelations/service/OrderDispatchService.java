package com.app.WsCancelations.service;

import com.app.WsCancelations.utils.Constantes;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class OrderDispatchService {

    //Envía correos cuando una orden debe despacharse.

    @Autowired
    private MailService mailService;


    private final String[] DESTINATARIOS_DESPACHAR = new String[] {"EXAMPLE@EXAMPLE.COM"};

    public void enviarDespacharOrden(String orden, String fechaEntrega, String notas, String nextdayValue) {
        String subject = "DESPACHAR ORDEN " + orden;

        StringBuilder content = new StringBuilder();
        content.append("DESPACHAR ORDEN ").append(orden).append("\n")
                .append("Signifyd aprueba segunda revisión de ").append(orden).append(". Por favor despachar.\n")
                .append("Delivery date: ").append(fechaEntrega != null ? fechaEntrega : "").append("\n");

        if (notas != null && !notas.isEmpty()) {
            content.append("\nNotas:\n").append(notas);
        }

        String tipo = nextdayValue != null ? nextdayValue.trim().toLowerCase() : "";
        String destinatariosEnv = null;

        switch (tipo) {
            case "si":
                destinatariosEnv = Constantes.getEnv("EMAIL_NEXTDAY");
                break;
            case "":
                destinatariosEnv = Constantes.getEnv("EMAIL_NODESPACHAR");
                break;
            case "si y no":
                destinatariosEnv = Constantes.getEnv("EMAIL_SIYNO");
                break;
            default:
                log.warn("Tipo de NEXTDAY desconocido para orden {}: '{}'. No se enviará correo.", orden, nextdayValue);
                return;
        }

        if (destinatariosEnv == null || destinatariosEnv.isEmpty()) {
            log.error("Variable de entorno no configurada para '{}'. No se enviará correo de orden {}", tipo, orden);
            return;
        }

        // Convertimos lista separada por punto y coma en array
        String[] destinatarios = destinatariosEnv.split(";");

        try {
            mailService.sendSimpleMail(destinatarios, subject, content.toString());
            log.info("Correo DESPACHAR enviado para la orden {} con fecha {} y notas ", orden, fechaEntrega);
        } catch (Exception e) {
            log.error("Error enviando correo DESPACHAR para {}: {}", orden, e.getMessage(), e);
        }
    }
}
