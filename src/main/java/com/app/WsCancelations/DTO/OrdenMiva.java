package com.app.WsCancelations.DTO;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class OrdenMiva {


    private String idOrden;
    private double valorTotalOrden;
    private List<String> fechasEntrega;
    private String billEmail;
    private String billName;
    private String tipoTransaccion;
    private boolean tieneNextday;
    private boolean tieneNormal;
    private String orderdate;
}
