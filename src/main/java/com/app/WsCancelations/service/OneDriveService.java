package com.app.WsCancelations.service;

import com.app.WsCancelations.utils.Constantes;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import java.io.ByteArrayInputStream;
import java.util.Map;


    private final RestTemplate restTemplate = new RestTemplate();

    // Obtiene un token de acceso válido desde Microsoft Graph usando Client Credentials
    private String getAccessToken() {
        String tokenUrl = "https://login.microsoftonline.com/" + Constantes.TENANT_ID + "/oauth2/v2.0/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", Constantes.CLIENT_ID);
        form.add("client_secret", Constantes.CLIENT_SECRET);
        form.add("scope", "https://graph.microsoft.com/.default");
        form.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Error obteniendo token de Microsoft Graph");
        }

        return (String) response.getBody().get("access_token");
    }

    public boolean fileExists(String filename) {
        try {
            HttpHeaders headers = getAuthHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);

            String url = String.format(
                    "https://graph.microsoft.com/v1.0/users/%s/drive/root:/%s/%s",
                    Constantes.USER_EMAIL, Constantes.FOLDER, filename
            );

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    public ByteArrayInputStream downloadFile(String filename) throws IOException {
        HttpHeaders headers = getAuthHeaders();
        HttpEntity<Void> request = new HttpEntity<>(headers);

        String url = String.format(
                "https://graph.microsoft.com/v1.0/users/%s/drive/root:/%s/%s:/content",
                Constantes.USER_EMAIL, Constantes.FOLDER, filename
        );

        ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, request, byte[].class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return new ByteArrayInputStream(response.getBody());
        }
        throw new IOException("No se pudo descargar el archivo " + filename);
    }

    public void uploadToOneDrive(InputStream fileStream, String filename) throws IOException {
        HttpHeaders headers = getAuthHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        byte[] bytes = fileStream.readAllBytes();

        String url = String.format(
                "https://graph.microsoft.com/v1.0/users/%s/drive/root:/%s/%s:/content",
                Constantes.USER_EMAIL, Constantes.FOLDER, filename
        );

        HttpEntity<byte[]> request = new HttpEntity<>(bytes, headers);

        restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
    }

    private HttpHeaders getAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessToken());
        return headers;
    }
}
