package com.example.crptapi;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.*;
import java.io.*;
import javax.json.*;

public class CrptApi {
    private final int requestLimit;
    private final long timeIntervalMillis;
    private final Semaphore semaphore;
    private final ScheduledExecutorService scheduler;
    private final HttpClient httpClient;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        if (requestLimit <= 0) {
            throw new IllegalArgumentException("Request limit must be positive.");
        }
        this.requestLimit = requestLimit;
        this.timeIntervalMillis = timeUnit.toMillis(1);
        this.semaphore = new Semaphore(requestLimit, true);
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.httpClient = HttpClient.newHttpClient();

        scheduler.scheduleAtFixedRate(semaphore::release, timeIntervalMillis, timeIntervalMillis, TimeUnit.MILLISECONDS);
    }

    public void createDocument(Document document, String signature) throws IOException, InterruptedException {
        semaphore.acquire();
        try {
            String json = toJson(document);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                    .header("Content-Type", "application/json")
                    .header("Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response: " + response.body());
        } finally {
            semaphore.release();
        }
    }

    private String toJson(Document document) {
        JsonObjectBuilder documentBuilder = Json.createObjectBuilder()
                .add("doc_id", document.doc_id)
                .add("doc_status", document.doc_status)
                .add("doc_type", document.doc_type)
                .add("importRequest", document.importRequest)
                .add("owner_inn", document.owner_inn)
                .add("participant_inn", document.participant_inn)
                .add("producer_inn", document.producer_inn)
                .add("production_date", document.production_date)
                .add("production_type", document.production_type)
                .add("reg_date", document.reg_date)
                .add("reg_number", document.reg_number)
                .add("description", Json.createObjectBuilder()
                        .add("participantInn", document.description.participantInn));

        JsonArrayBuilder productsBuilder = Json.createArrayBuilder();
        for (Document.Product product : document.products) {
            productsBuilder.add(Json.createObjectBuilder()
                    .add("certificate_document", product.certificate_document)
                    .add("certificate_document_date", product.certificate_document_date)
                    .add("certificate_document_number", product.certificate_document_number)
                    .add("owner_inn", product.owner_inn)
                    .add("producer_inn", product.producer_inn)
                    .add("production_date", product.production_date)
                    .add("tnved_code", product.tnved_code)
                    .add("uit_code", product.uit_code)
                    .add("uitu_code", product.uitu_code));
        }
        documentBuilder.add("products", productsBuilder);

        JsonObject documentJson = documentBuilder.build();

        StringWriter stringWriter = new StringWriter();
        Json.createWriter(stringWriter).write(documentJson);
        return stringWriter.toString();
    }

    public static class Document {
        public Description description;
        public String doc_id;
        public String doc_status;
        public String doc_type;
        public boolean importRequest;
        public String owner_inn;
        public String participant_inn;
        public String producer_inn;
        public String production_date;
        public String production_type;
        public Product[] products;
        public String reg_date;
        public String reg_number;

        public static class Description {
            public String participantInn;
        }

        public static class Product {
            public String certificate_document;
            public String certificate_document_date;
            public String certificate_document_number;
            public String owner_inn;
            public String producer_inn;
            public String production_date;
            public String tnved_code;
            public String uit_code;
            public String uitu_code;
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 5);

        CrptApi.Document document = new CrptApi.Document();
        document.doc_id = "123";
        document.doc_status = "NEW";
        document.doc_type = "LP_INTRODUCE_GOODS";
        document.importRequest = true;
        document.owner_inn = "1234567890";
        document.participant_inn = "1234567890";
        document.producer_inn = "1234567890";
        document.production_date = "2023-06-25";
        document.production_type = "FULL_PRODUCTION";
        document.reg_date = "2023-06-25";
        document.reg_number = "REG123";
        document.description = new CrptApi.Document.Description();
        document.description.participantInn = "1234567890";
        CrptApi.Document.Product product = new CrptApi.Document.Product();
        product.certificate_document = "CERT123";
        product.certificate_document_date = "2023-06-25";
        product.certificate_document_number = "CERTNUM123";
        product.owner_inn = "1234567890";
        product.producer_inn = "1234567890";
        product.production_date = "2023-06-25";
        product.tnved_code = "TNVED123";
        product.uit_code = "UIT123";
        product.uitu_code = "UITU123";
        document.products = new CrptApi.Document.Product[]{product};

        String signature = "your-signature";
        api.createDocument(document, signature);
    }
}


