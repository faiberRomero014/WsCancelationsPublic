package com.app.WsCancelations.utils;
/*
 * @autor jeimy 12/06/2025
 * @version 1.0 12/06/2025
 */
public class Constantes {

    //Guarda las variables de entorno del sistema (tokens, correos, rutas, etc.).

    public static final String filePath = "FILEPATH";
    //estas variables se usan aquí (ConsultaMivaImpl)
    public static final String accessToken = "ACCESS_TOKEN";
    public static final String endPointUrl = "END_POINT_URL";

    //esta variable se usan aquí (WsMivaQuerys)
    public static final String horasRevisar = "HORASREVISAR";

    //estas variables se usan aquí (OneDriveService)
    public static final String CLIENT_ID = getEnv("CLIENT_ID");
    public static final String CLIENT_SECRET = getEnv("CLIENT_SECRET");
    public static final String TENANT_ID = getEnv("TENANT_ID");
    public static final String FOLDER = getEnv("FOLDER");
    public static final String USER_EMAIL = getEnv("USER_EMAIL");

    //esta variable se usan aquí (OrderService)
    public static final String FILENAMEWSC = getEnv("FILENAMEWSC");

    //estos correos se utilizan de forma correspondiente en (OrderAlertService, OrderDispatchService, AccessFailAlertService)
    public static final String emailNextDay = getEnv("EMAIL_NEXTDAY");
    public static final String emailNoDespachar = getEnv("EMAIL_NODESPACHAR");
    public static final String emailSiYNo = getEnv("EMAIL_SIYNO");
    public static final String emailNoAccedio = getEnv("EMAIL_NOACCEDIO");


    public static final String getEnv (String nombreVariable) {
        return System.getenv(nombreVariable);
    }


}
