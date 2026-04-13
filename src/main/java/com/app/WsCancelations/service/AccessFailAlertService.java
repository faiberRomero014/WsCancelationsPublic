package com.app.WsCancelations.service;

import com.app.WsCancelations.utils.Constantes;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZoneId;

@Service

    //Envía alertas por correo cuando hay un error de conexión al echivo excel o autenticación con Miva.

    @Autowired
    private MailService mailService;

    private final String EMAIL_NOACCEDIO = Constantes.getEnv("EMAIL_NOACCEDIO");


    public void enviarAlertaFalloAcceso(Exception error) {
        try {
            if (EMAIL_NOACCEDIO == null || EMAIL_NOACCEDIO.trim().isEmpty()) {
                log.warn("No se ha configurado EMAIL_NoAccedio en las variables de entorno. No se enviará correo.");
                return;
            }

            String[] destinatarios = EMAIL_NOACCEDIO.split("[;]+");

            ZoneId zonaBogota = ZoneId.of("America/Bogota");
            LocalDateTime ahora = LocalDateTime.now(zonaBogota);

            String hora = ahora.format(DateTimeFormatter.ofPattern("HH:mm"));
            String fecha = ahora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            String subject = "No se pudo acceder en la ejecución de la hora " + hora +
                    " hora Colombia del día " + fecha;

            String contenido =
                    "fallo al intentar acceder al archivo \n" +
                    (error != null ? error.getMessage() : "No se obtuvo detalle del error.") +
                    "\nPor favor revisar el servicio o la conexión con Miva o OneDrive.\n\n" +
                    "Hora del fallo: " + hora + " (Colombia)\n" +
                    "Fecha: " + fecha ;


            mailService.sendSimpleMail(destinatarios, subject, contenido);
            log.info("Correo de fallo de acceso enviado correctamente a {}", String.join(", ", destinatarios));

        } catch (Exception ex) {
            log.error("Error al intentar enviar correo de fallo de acceso: {}", ex.getMessage(), ex);
        }
    }
}
