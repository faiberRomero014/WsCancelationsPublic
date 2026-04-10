package com.app.WsCancelations.service.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

import com.app.WsCancelations.DTO.OrdenMiva;
import com.app.WsCancelations.interfaces.ConsultaMivaInt;
import com.app.WsCancelations.interfaces.RecorrerJsonMivaInt;
import com.app.WsCancelations.service.AccessFailAlertService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class RecorrerJsonMivaImp implements RecorrerJsonMivaInt {

    //Procesa y recorre el JSON devuelto por Miva para extraer los datos importantes.

    @Autowired
    private ConsultaMivaInt consultaMiva;

    @Autowired
    private AccessFailAlertService accessFailAlertService;

    @Override
    public List<OrdenMiva> consultaOrdenesNoPagas(String fechaInicialConsulta, String fechaFinalConsulta) {
        List<OrdenMiva> ordenesNoPagas = new ArrayList<>();
        try {
            JSONObject orders = consultaMiva.getOrdersJson(fechaInicialConsulta, fechaFinalConsulta);
            JSONObject getDataGeneral = orders.getJSONObject("data");
            JSONArray getData = getDataGeneral.getJSONArray("data");

            for (int d = 0; d < getData.length(); d++) {
                JSONObject orden = (JSONObject) getData.get(d);

                String tipoTransaccion = "";
                boolean extraerDatos = false;

                JSONArray getPayments = orden.getJSONArray("payments");
                for (int p = 0; p < getPayments.length(); p++) {
                    JSONObject paymentOrden = (JSONObject) getPayments.get(p);
                    JSONObject modulePayments = (JSONObject) paymentOrden.getJSONObject("module");
                    String codePayment = modulePayments.getString("code");

                    if (codePayment.equals("authnet") || codePayment.equals("braintree")) {
                        String statusSignifyd = orden.getJSONObject("CustomField_Values")
                                .getJSONObject("signifyd")
                                .getString("g_disp");
                        if (statusSignifyd.equals("DECLINED")) {
                            tipoTransaccion = "Signifyd";
                            extraerDatos = true;
                        }
                    } else if (codePayment.equals("paypalcp")) {
                        String statusPaypal = paymentOrden.getJSONObject("data").getString("transaction_status");
                        if (statusPaypal.equals("DECLINED") || statusPaypal.equals("PENDING")) {
                            tipoTransaccion = "Paypal";
                            extraerDatos = true;
                        }
                    } else if (codePayment.equals("amazonpay")) {
                        String statusAmazonPay = paymentOrden.getJSONObject("data")
                                .getString("AuthorizationDetails:AuthorizationStatus:State");
                        if (statusAmazonPay.equals("Closed") || statusAmazonPay.equals("Declined")) {
                            tipoTransaccion = "Amazon";
                            extraerDatos = true;
                        }
                    }
                }

                if (extraerDatos) {
                    List<String> fechasEntrega = new ArrayList<>();
                    boolean tieneNextday = false, tieneNormal = false;

                    JSONArray getItems = orden.getJSONArray("items");
                    for (int i = 0; i < getItems.length(); i++) {
                        JSONObject item = getItems.getJSONObject(i);
                        String productCode = item.getString("code");
                        if (productCode.contains("next")) {
                            tieneNextday = true;
                        } else {
                            tieneNormal = true;
                        }

                        JSONArray optionsItem = item.getJSONArray("options");
                        for (int o = 0; o < optionsItem.length(); o++) {
                            JSONObject option = optionsItem.getJSONObject(o);
                            String attrCodeOption = option.getString("attr_code");
                            String valueOption = option.getString("value");

                            if (attrCodeOption.equals("Delivery")) {
                                try {
                                    Date fecha = new SimpleDateFormat("yyyy-MM-dd").parse(valueOption);
                                    String fechaFormateada = new SimpleDateFormat("dd-MMM", new java.util.Locale("es", "ES"))
                                            .format(fecha);
                                    if (!fechasEntrega.contains(fechaFormateada)) {
                                        fechasEntrega.add(fechaFormateada);
                                    }
                                } catch (Exception e) {
                                    try {
                                        Date fechaAlt = new SimpleDateFormat("MMM dd, yyyy", java.util.Locale.ENGLISH)
                                                .parse(valueOption);
                                        String fechaFormateada = new SimpleDateFormat("dd-MMM",
                                                new java.util.Locale("es", "ES")).format(fechaAlt);
                                        if (!fechasEntrega.contains(fechaFormateada)) {
                                            fechasEntrega.add(fechaFormateada);
                                        }
                                    } catch (Exception ex) {
                                        if (!fechasEntrega.contains(valueOption)) {
                                            fechasEntrega.add(valueOption);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    String idOrden = String.valueOf(orden.getInt("id"));
                    double valorTotalOrden = orden.getDouble("total");
                    String billEmail = orden.getString("bill_email");
                    String billName = orden.getString("bill_fname") + " " + orden.getString("bill_lname");

                    String orderdate = "";
                    try {
                        long orderEpoch = orden.getLong("orderdate");
                        orderdate = new SimpleDateFormat("yyyy/MM/dd").format(new Date(orderEpoch * 1000));
                    } catch (Exception e) {
                        orderdate = orden.optString("orderdate", "");
                    }

                    OrdenMiva ordenRevisada = OrdenMiva
                            .builder()
                            .idOrden(idOrden)
                            .valorTotalOrden(valorTotalOrden)
                            .orderdate(orderdate)
                            .fechasEntrega(fechasEntrega)
                            .billEmail(billEmail)
                            .billName(billName)
                            .tipoTransaccion(tipoTransaccion)
                            .tieneNextday(tieneNextday)
                            .tieneNormal(tieneNormal)
                            .build();

                    ordenesNoPagas.add(ordenRevisada);
                }
            }

        } catch (Exception e) {

            log.error("consultaOrdenesNoPagas " + e.getMessage());
            accessFailAlertService.enviarAlertaFalloAcceso(e);
        }

        return ordenesNoPagas;
    }
}
