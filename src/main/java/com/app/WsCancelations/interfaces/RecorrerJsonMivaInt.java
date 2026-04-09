package com.app.WsCancelations.interfaces;

import com.app.WsCancelations.DTO.OrdenMiva;

import java.util.List;

public interface RecorrerJsonMivaInt {
    public List<OrdenMiva> consultaOrdenesNoPagas (String fechaInicialConsulta, String fechaFinalConsulta);

}
