package com.app.WsCancelations.service.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.app.WsCancelations.interfaces.ConsultaMivaInt;
import com.app.WsCancelations.utils.Constantes;
import com.app.WsCancelations.utils.JsonOrders;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class ConsultaMivaImpl implements ConsultaMivaInt {

    /*
    Se encarga de consultar la API de Miva, obtener las órdenes y devolverlas en formato JSON o como objetos OrdenMiva.
    es quien realiza la conexión real con Miva.
    */
    @Override
    public JSONObject getOrdersJson(String fechaInicialConsulta, String fechaFinalConsulta) {
        String res = "";
        JSONObject json = null;

        try {
            URL url = new URL(Constantes.getEnv(Constantes.endPointUrl));
            String accessToken = Constantes.getEnv(Constantes.accessToken);
            JsonOrders jsonModel = new JsonOrders();
            String jsonInputString = jsonModel.getInputString(fechaInicialConsulta, fechaFinalConsulta);

            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("X-Miva-API-Authorization", "MIVA "+ accessToken);
            con.setDoOutput(true);

            try (OutputStream os = con.getOutputStream()){
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            } catch (Exception e) {
                log.error("Error en getOrdersJson:OutputStream "+ e.getMessage());
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))){
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                res = response.toString();
            } catch (Exception e) {
                log.error("Error en getOrdersJson:BufferedReader "+ e.getMessage());
            }

            json = new JSONObject(res);

        } catch (Exception e) {
            log.error("Error en getOrdersJson "+ e.getMessage());
        }
        return json;
    }
}