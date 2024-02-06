package ru.testTask;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    public static final String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final HttpClient httpClient = HttpClient.newBuilder().build();
    private final Semaphore requestSemaphore;
    private final Gson gson;
    private final ScheduledExecutorService scheduler;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.requestSemaphore = new Semaphore(requestLimit);
        this.gson = new Gson();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduleRequestLimitReset(timeUnit);
    }

    private void scheduleRequestLimitReset(TimeUnit timeUnit) {
        scheduler.scheduleAtFixedRate(requestSemaphore::release, 0, 10, timeUnit);
    }

    public void createAndSendDocument(Document document, String sign) {
        try {
            requestSemaphore.acquire();
            String json = gson.toJson(SignedDocument.builder()
                    .document(document)
                    .sign(sign)
                    .build());
            HttpResponse response = httpClient.send(buildRequest(json), HttpResponse.BodyHandlers.ofString());
            // обработка ответа
        } catch (InterruptedException | IOException e) {
            Thread.currentThread().interrupt();
        } finally {
            requestSemaphore.release();
        }
    }

    @Builder
    @RequiredArgsConstructor
    private class SignedDocument {
        private final Document document;
        private final String sign;
    }

    private HttpRequest buildRequest(String json) {
        return HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();
    }

    public class Document {
        private Description description;
        @SerializedName("doc_id")
        private String docId;
        @SerializedName("doc_status")
        private String docStatus;
        @SerializedName("doc_type")
        private String docType;
        @SerializedName("importRequest")
        private boolean importRequest;
        @SerializedName("owner_inn")
        private String ownerInn;
        @SerializedName("participant_inn")
        private String participantInn;
        @SerializedName("producer_inn")
        private String producerInn;
        @SerializedName("production_date")
        private Date productionDate;
        @SerializedName("production_type")
        private String productionType;
        @SerializedName("products")
        private List<Product> products;
        @SerializedName("reg_date")
        private Date regDate;
        @SerializedName("reg_number")
        private String regNumber;

        class Description {
            @SerializedName("participantInn")
            private String participantInn;
        }

        class Product {
            @SerializedName("certificate_document")
            private String certificateDocument;
            @SerializedName("certificate_document_date")
            private Date certificateDocumentDate;
            @SerializedName("certificate_document_number")
            private String certificateDocumentNumber;
            @SerializedName("owner_inn")
            private String ownerInn;
            @SerializedName("producer_inn")
            private String producerInn;
            @SerializedName("production_date")
            private Date productionDate;
            @SerializedName("tnved_code")
            private String tnvedCode;
            @SerializedName("uit_code")
            private String uitCode;
            @SerializedName("uitu_code")
            private String uituCode;
        }
    }

}
