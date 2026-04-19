package com.app.WsCancelations.service;

import com.app.WsCancelations.DTO.OrdenMiva;
import com.app.WsCancelations.utils.Constantes;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderService {

    //Crea o actualiza el archivo Excel de órdenes en OneDrive. Detecta nuevas órdenes, envía alertas y notificaciones de despacho.

    private final OneDriveService oneDriveService;
    private final OrderAlertService orderAlertService;
    private final OrderDispatchService orderDispatchService;

    public void exportOrderToOneDrive(List<OrdenMiva> ordenes) throws Exception {
        String filename = Constantes.FILENAMEWSC;
        XSSFWorkbook workbook;
        XSSFSheet sheet;
        int currentRow;

        Set<String> existingOrderIds = new HashSet<>();
        List<String[]> ordenesParaDespachar = new ArrayList<>();
        List<String[]> alertasPendientes = new ArrayList<>();

        if (oneDriveService.fileExists(filename)) {
            try (ByteArrayInputStream existingFile = oneDriveService.downloadFile(filename)) {
                workbook = new XSSFWorkbook(existingFile);
                sheet = workbook.getSheet("Hoja1");
                if (sheet == null) {
                    sheet = workbook.createSheet("Hoja1");
                    currentRow = 0;
                    createHeader(sheet, currentRow++);
                } else {
                    DataFormatter formatter = new DataFormatter();
                    int lastRow = sheet.getLastRowNum();
                    for (int r = 1; r <= lastRow; r++) {
                        Row row = sheet.getRow(r);
                        if (row != null && row.getCell(1) != null) {
                            String idOrden = formatter.formatCellValue(row.getCell(1));
                            existingOrderIds.add(idOrden);
                        }
                    }
                    currentRow = lastRow + 1;

                    // Recorremos filas existentes para detectar si hay "DESPACHAR"
                    for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                        Row row = sheet.getRow(r);
                        if (row == null) continue;

                        Cell queHacerCell = row.getCell(9);   // Columna J
                        Cell devLogCell = row.getCell(13);   // Columna N

                        String queHacer = (queHacerCell != null) ? queHacerCell.getStringCellValue().trim().toUpperCase() : "";
                        String devLog = (devLogCell != null) ? devLogCell.getStringCellValue().trim() : "";

                        if ("DESPACHAR".equals(queHacer) && devLog.isEmpty()) {
                            String ordenId = row.getCell(1).getStringCellValue();      // Columna B
                            String fechaEntrega = row.getCell(3).getStringCellValue(); // Columna D
                            String nextdayValue = row.getCell(4).getStringCellValue(); // Columna E

                            // Leer la columna NOTAS (M) siempre como texto
                            Cell notasCell = row.getCell(12);
                            String notas = "";
                            if (notasCell != null) {
                                DataFormatter notaFormatter = new DataFormatter();
                                notas = notaFormatter.formatCellValue(notasCell);
                                if (notas != null) {
                                    notas = notas.trim();
                                }
                            }

                            ordenesParaDespachar.add(new String[]{ordenId, fechaEntrega, notas, nextdayValue});

                            if (devLogCell == null) devLogCell = row.createCell(13);
                            devLogCell.setCellValue("REENVIADO");
                        }
                    }
                }
            }
        } else {
            workbook = new XSSFWorkbook();
            sheet = workbook.createSheet("Hoja1");
            currentRow = 0;
            createHeader(sheet, currentRow++);
        }

        boolean newOrders = false;

        // Insertamos nuevas órdenes
        for (OrdenMiva orden : ordenes) {
            if (existingOrderIds.contains(orden.getIdOrden())) continue;

            newOrders = true;
            Row row = sheet.createRow(currentRow++);

            // FECHA
            Cell fechaCell = row.createCell(0);
            fechaCell.setCellValue(orden.getOrderdate());
            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("MM/dd/yyyy"));
            fechaCell.setCellStyle(dateStyle);

            // ORDEN
            row.createCell(1).setCellValue(orden.getIdOrden());

            // VALOR TOTAL ORDEN
            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("#,##0.00"));
            Cell valorTotalCell = row.createCell(2);
            valorTotalCell.setCellValue(orden.getValorTotalOrden());
            valorTotalCell.setCellStyle(currencyStyle);

            // FECHA DE ENTREGA
            String fechasConcatenadas = String.join(", ", orden.getFechasEntrega());
            row.createCell(3).setCellValue(fechasConcatenadas);

            // NEXTDAY
            String nextdayValue = "";
            if (orden.isTieneNextday() && !orden.isTieneNormal()) nextdayValue = "si";
            else if (orden.isTieneNextday() && orden.isTieneNormal()) nextdayValue = "si y no";
            row.createCell(4).setCellValue(nextdayValue);

            // CORREO
            row.createCell(5).setCellValue(orden.getBillEmail());

            // NOMBRE
            row.createCell(6).setCellValue(orden.getBillName());

            // CARD
            row.createCell(7).setCellValue(
                    orden.getTipoTransaccion() != null ? orden.getTipoTransaccion() : ""
            );

            // Campos manuales vacíos
            row.createCell(8).setCellValue("");  // ZENDESK
            row.createCell(9).setCellValue("");  // ¿QUÉ HACER?
            row.createCell(10).setCellValue(""); // RESPUESTA MAYRA
            row.createCell(11).setCellValue(""); // ¿PORQUÉ?
            row.createCell(12).setCellValue(""); // NOTAS
            row.createCell(13).setCellValue(""); // DEV LOG

            existingOrderIds.add(orden.getIdOrden());
            alertasPendientes.add(new String[]{orden.getIdOrden(), fechasConcatenadas, nextdayValue});
        }

        for (int i = 0; i <= 13; i++) sheet.autoSizeColumn(i);

        if (sheet.getTables().size() > 0) {
            XSSFTable table = sheet.getTables().get(0);
            AreaReference newArea = new AreaReference(
                    new CellReference(0, 0),
                    new CellReference(sheet.getLastRowNum(), 13),
                    workbook.getSpreadsheetVersion()
            );
            table.setArea(newArea);
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            workbook.close();
            oneDriveService.uploadToOneDrive(new ByteArrayInputStream(out.toByteArray()), filename);
        }

        if (newOrders)
            System.out.println("Archivo " + filename + " actualizado con nuevas órdenes.");
        else
            System.out.println("No se encontraron nuevas órdenes a actualizar.");

        if (!alertasPendientes.isEmpty()) {
            for (String[] alerta : alertasPendientes) {
                String ordenId = alerta[0];
                String fechaEntrega = alerta[1];
                String nextdayValue = alerta[2];
                try {
                    orderAlertService.enviarAlerta(ordenId, fechaEntrega, nextdayValue);
                } catch (Exception e) {
                    System.err.println("Error enviando alerta para la orden " + ordenId + ": " + e.getMessage());
                }
            }
        } else {
            System.out.println("No hay nuevas órdenes para enviar.");
        }

        for (String[] datos : ordenesParaDespachar) {
            String ordenId = datos[0];
            String fechaEntrega = datos[1];
            String notas = datos.length > 2 ? datos[2] : "";
            String nextdayValue = datos.length > 3 ? datos[3] : "";
            orderDispatchService.enviarDespacharOrden(ordenId, fechaEntrega, notas, nextdayValue);
        }
    }

    private void createHeader(XSSFSheet sheet, int rowIndex) {
        Row header = sheet.createRow(rowIndex);
        String[] headers = {
                "FECHA",
                "ORDEN",
                "VALOR TOTAL ORDEN",
                "FECHA DE ENTREGA",
                "NEXTDAY",
                "CORREO",
                "NOMBRE",
                "CARD",
                "ZENDESK",
                "¿QUÉ HACER?",
                "RESPUESTA DE CONFIRMACIÓN (MAYRA)",
                "¿PORQUÉ?",
                "DEV LOG"
        };
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }
    }
}
