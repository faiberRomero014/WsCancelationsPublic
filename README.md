# WsCancelations

Servicio en **Spring Boot** que consulta periódicamente la API de **Miva** en busca de órdenes con pagos rechazados o pendientes (posible fraude, según **Signifyd**, **Paypal** o **Amazon Pay**), las registra en un archivo Excel almacenado en **OneDrive** y envía **alertas por correo** según el tipo de orden detectada.

## ¿Qué hace?

1. **Consulta Miva**: cada hora (y al iniciar la aplicación) pregunta a la API de Miva por las órdenes creadas en las últimas N horas (configurable).
2. **Filtra órdenes sospechosas**: revisa los métodos de pago de cada orden (`authnet`/`braintree`, `paypalcp`, `amazonpay`) y se queda solo con las que tienen estado `DECLINED` o `PENDING`.
3. **Extrae información relevante**: ID de orden, valor total, fechas de entrega, correo y nombre del cliente, tipo de transacción, y si tiene ítems "Next Day" y/o normales.
4. **Actualiza un Excel en OneDrive**: descarga el archivo existente (o crea uno nuevo), agrega solo las órdenes que aún no estén registradas, y lo vuelve a subir.
5. **Envía alertas por correo**:
   - Cuando aparece una orden nueva, según si es *Next Day*, *No Despachar* o *Sí y No* (mezcla de ambas).
   - Cuando una fila del Excel fue marcada manualmente como `DESPACHAR`, se notifica a los destinatarios correspondientes.
   - Cuando falla el acceso al archivo o a Miva, se notifica el error.
6. **Reintentos controlados**: si el archivo está bloqueado (error 423), reintenta hasta 3 veces con 5 minutos de espera entre intentos, evitando ejecuciones duplicadas.

## Arquitectura / Paquetes

```
com.app.WsCancelations
├── WsCancelationsApplication      # Clase principal (Spring Boot + @EnableScheduling)
├── controller
│   └── WsMivaQuerys                # Endpoint REST + tareas programadas
├── interfaces
│   ├── ConsultaMivaInt
│   └── RecorrerJsonMivaInt
├── service
│   ├── impl
│   │   ├── ConsultaMivaImpl        # Llama a la API de Miva
│   │   └── RecorrerJsonMivaImp     # Procesa el JSON de Miva y filtra órdenes
│   ├── OrderService                # Genera/actualiza el Excel y coordina alertas
│   ├── OneDriveService             # Sube/descarga archivos vía Microsoft Graph API
│   ├── OrderAlertService           # Alertas de nuevas órdenes (Next Day / No Despachar / Sí y No)
│   ├── OrderDispatchService        # Alertas de "despachar orden"
│   ├── AccessFailAlertService      # Alertas de fallo de acceso (Excel/Miva)
│   ├── MailService                 # Envío genérico de correos
│   └── RetryScheduler              # Control de concurrencia y reintentos
├── DTO
│   └── OrdenMiva                   # Modelo de una orden procesada
└── utils
    ├── Constantes                  # Variables de entorno del sistema
    └── JsonOrders                  # Genera el JSON de consulta para Miva
```

## Flujo principal

```
WsMivaQuerys (endpoint / tarea programada / al iniciar)
        │
        ▼
RecorrerJsonMivaImp.consultaOrdenesNoPagas()
        │  usa
        ▼
ConsultaMivaImpl.getOrdersJson()  ──► API de Miva (OrderList_Load_Query)
        │
        ▼
Filtra órdenes con pago rechazado/pendiente
        │
        ▼
OrderService.exportOrderToOneDrive()
        │
        ├─► OneDriveService (descarga/sube el Excel vía Microsoft Graph)
        ├─► OrderAlertService (alerta por orden nueva)
        └─► OrderDispatchService (alerta por orden marcada DESPACHAR)

Si algo falla en el proceso ──► AccessFailAlertService (correo de error)
Si el archivo está bloqueado ──► RetryScheduler (reintenta hasta 3 veces)
```

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/consultarOrdenesPendientesPago` | Dispara manualmente el proceso de consulta de órdenes. |

Además, el proceso se ejecuta automáticamente:
- **Al iniciar** la aplicación (`ApplicationReadyEvent`).
- **Cada hora** (`@Scheduled(cron = "0 0/60 * * * ?")`).

## Configuración (variables de entorno)

El proyecto lee la configuración sensible desde variables de entorno (clase `Constantes`):

| Variable | Uso |
|---|---|
| `ACCESS_TOKEN` | Token de acceso a la API de Miva. |
| `END_POINT_URL` | URL del endpoint de la API de Miva. |
| `HORASREVISAR` | Cantidad de horas hacia atrás a consultar en cada ejecución. |
| `CLIENT_ID` | Client ID de la app registrada en Azure (OneDrive/Graph). |
| `CLIENT_SECRET` | Client secret de la app en Azure. |
| `TENANT_ID` | Tenant de Azure AD. |
| `FOLDER` | Carpeta de OneDrive donde se guarda el Excel. |
| `USER_EMAIL` | Cuenta de OneDrive dueña de la carpeta. |
| `FILENAMEWSC` | Nombre del archivo Excel a mantener. |
| `EMAIL_NEXTDAY` | Destinatarios para alertas de órdenes "Next Day". |
| `EMAIL_NODESPACHAR` | Destinatarios para alertas de "No Despachar". |
| `EMAIL_SIYNO` | Destinatarios para alertas mixtas ("Sí y No"). |
| `EMAIL_NOACCEDIO` | Destinatarios para alertas de fallo de acceso. |

Como estas variables se leen con `System.getenv(...)` (clase `Constantes`), **no dependen de `application.properties`**: se pueden definir de cualquier forma que ponga esos valores en el entorno del sistema donde corre la aplicación. Algunas opciones comunes:

- **Variables de entorno del sistema operativo** (export en Linux/Mac, `setx`/`set` en Windows, o configuradas en el servidor/contenedor donde se despliegue).
- **Configuración de ejecución del IDE** (en IntelliJ IDEA o Eclipse, en la pestaña *Environment variables* de la configuración de "Run/Debug").
- **Archivo `.env`** cargado por la herramienta de arranque o el orquestador (por ejemplo, definido en `docker-compose.yml` con `environment:` o `env_file:`).
- **Variables de entorno del contenedor/plataforma** (Docker `-e VAR=valor`, Kubernetes `ConfigMap`/`Secret`, o las variables de entorno del panel de un PaaS como Azure App Service, Railway, Heroku, etc.).
- **Perfiles de CI/CD** (GitHub Actions, GitLab CI, Jenkins), donde se configuran como *secrets* o *environment variables* del pipeline.

En cambio, en `application.properties`/`application.yml` **sí** se debe configurar aparte:
- Datos del servidor SMTP para `JavaMailSender` (host, puerto, usuario, contraseña).
- `app.mail.from`: correo remitente usado por `MailService`.

> Nota: si se prefiere, estas propiedades de `application.properties` también pueden referenciar variables de entorno usando la sintaxis de Spring `${NOMBRE_VARIABLE}` (por ejemplo `spring.mail.username=${MAIL_USERNAME}`), en lugar de escribir los valores directamente en el archivo.

## Estructura del Excel generado

El archivo mantiene una tabla con las columnas:

`FECHA | ORDEN | VALOR TOTAL ORDEN | FECHA DE ENTREGA | NEXTDAY | CORREO | NOMBRE | CARD | ZENDESK | ¿QUÉ HACER? | RESPUESTA DE CONFIRMACIÓN (MAYRA) | ¿PORQUÉ? | NOTAS | DEV LOG`

Las últimas columnas (`ZENDESK`, `¿QUÉ HACER?`, `RESPUESTA...`, `¿PORQUÉ?`, `NOTAS`, `DEV LOG`) se dejan vacías para diligenciamiento manual del equipo. Cuando alguien marca la columna `¿QUÉ HACER?` como `DESPACHAR` y `DEV LOG` está vacío, el sistema envía el correo de despacho y marca `DEV LOG` como `REENVIADO` para no volver a notificar la misma fila.

## Tecnologías

- **Java + Spring Boot** (Web, Scheduling, Mail)
- **Apache POI** (lectura/escritura de Excel `.xlsx`)
- **org.json** (parseo de JSON de la API de Miva)
- **Microsoft Graph API** (OAuth2 client credentials) para OneDrive
- **Lombok** (`@Data`, `@Builder`, `@RequiredArgsConstructor`, logging con Log4j2/Slf4j)

## Manejo de errores y concurrencia

- `RetryScheduler` usa un `AtomicBoolean` para evitar que se solapen ejecuciones (manual, programada o de reintento).
- Si el error contiene el código `423` (archivo bloqueado en OneDrive), se reintenta hasta 3 veces adicionales con 5 minutos de espera.
- Si se agotan los intentos, o el error es de otro tipo, se envía un correo de alerta con `AccessFailAlertService`.

Autor

Faiber Romero

Proyecto desarrollado para automatizar el monitoreo de órdenes con alertas de fraude, integrando la API de Miva, Microsoft Graph y notificaciones por correo electrónico.
