package org.example;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final long requestInterval;
    private final int requestLimit;
    private int requestCounter;
    private long lastRequestTime;
    private final HttpClient client;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.requestInterval = timeUnit.toMillis(1);
        this.requestLimit = requestLimit;
        this.requestCounter = 0;
        this.lastRequestTime = System.currentTimeMillis();
        client = HttpClient.newHttpClient();
    }

    private synchronized void waitForNextRequest() throws InterruptedException {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastRequestTime;

        if (elapsedTime < requestInterval) {
            long sleepTime = requestInterval - elapsedTime;
            Thread.sleep(sleepTime);
        }

        requestCounter = 0;
        lastRequestTime = System.currentTimeMillis();
    }

    public synchronized String createDocument(Document document, String token)
            throws InterruptedException, URISyntaxException, IOException {
        if (requestCounter >= requestLimit) {
            waitForNextRequest();
        }

        Gson gson = new Gson();
        URI apiUrl = new URI("https://api/v3/lk/documents/commissioning/contract/create" + "pg="
                + document.getPg());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(apiUrl)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(document)))
                .build();

        HttpResponse.BodyHandler<String> handler = HttpResponse.BodyHandlers.ofString();

        HttpResponse<String> response = client.send(request, handler);

        requestCounter++;


        if(response.statusCode() == 200) {
            return response.body();
        }
        throw new RuntimeException("Код ошибки " + response.statusCode());
    }

    private static class Document {
        private final String document_format;
        private final String product_document;
        private final String product_group;
        private final String signature;
        private final String type;

        public Document(String document_format, String product_document, String product_group,
                        String signature, String type) {
            this.document_format = document_format;
            this.product_document = product_document;
            this.product_group = product_group;
            this.signature = signature;
            this.type = type;
        }

        public String getPg() {
            return product_group;
        }
    }
}