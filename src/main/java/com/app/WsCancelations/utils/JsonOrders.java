package com.app.WsCancelations.utils;


public class JsonOrders {

    //Genera el JSON usado para consultar órdenes en la API de Miva según las fechas dadas.

    private String jsonInputString;

    public String getInputString(String fechaInicialConsulta, String fechaFinalConsulta) {
        jsonInputString = "{\r\n"
                + "    \"Store_Code\": \"GR\",\r\n"
                + "    \"Function\": \"OrderList_Load_Query\",\r\n"
                + "    \"Count\": 0,\r\n"
                + "    \"Offset\": 0,\r\n"
                + "    \"Filter\": [\r\n"
                + "        {\r\n"
                + "            \"name\": \"search\",\r\n"
                + "            \"value\": [\r\n"
                + "                {\r\n"
                + "                    \"field\": \"orderdate\",\r\n"
                + "                    \"operator\": \"GT\",\r\n"
                + "                    \"value\":" + fechaInicialConsulta + "\r\n"
                + "                }\r\n"
                + "            ]\r\n"
                + "        },\r\n"
                + "        {\r\n"
                + "            \"name\": \"search\",\r\n"
                + "            \"value\": [\r\n"
                + "                {\r\n"
                + "                    \"field\": \"orderdate\",\r\n"
                + "                    \"operator\": \"LT\",\r\n"
                + "                    \"value\":" +fechaFinalConsulta +"\r\n"
                + "                }\r\n"
                + "            ]\r\n"
                + "        },\r\n"
                + "        {\r\n"
                + "            \"name\": \"ondemandcolumns\",\r\n"
                + "            \"value\": [\r\n"
                + "                \"items\",\r\n"
                + "                \"payments\",\r\n"
                + "                \"payment_module\",\r\n"
                + "                \"payment_data\",\r\n"
                + "                \"CustomField_Values:*\"\r\n"
                + "\r\n"
                + "            ]\r\n"
                + "        }\r\n"
                + "    ]\r\n"
                + "}";
        return jsonInputString;
    }
}
