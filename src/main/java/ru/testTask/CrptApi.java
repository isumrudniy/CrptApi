package ru.testTask;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    public static final String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");
    private final OkHttpClient client;
    private final Semaphore requestSemaphore;
    private final Gson gson;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.client = new OkHttpClient();
        this.requestSemaphore = new Semaphore(requestLimit);
        this.gson = new Gson();
        scheduleRequestLimitReset(timeUnit);
    }

    private void scheduleRequestLimitReset(TimeUnit timeUnit) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(requestSemaphore::drainPermits, 0, 1, timeUnit);
    }

    public void createDocument(Document document, String sign) {
        try {
            requestSemaphore.acquire();
            String json = gson.toJson(SignedDocument.builder()
                    .document(document)
                    .sign(sign)
                    .build());
            Request request = buildRequest(json);
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    requestSemaphore.release();
                    //обработка ошибки
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    requestSemaphore.release();
                    //обработка ответа
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Builder
    @RequiredArgsConstructor
    private static class SignedDocument {
        private final Document document;
        private final String sign;
    }

    private Request buildRequest(String json) {
        return new Request.Builder()
                .url(URL)
                .post(RequestBody.create(json, JSON_MEDIA_TYPE))
                .build();
    }

    public static class Document {
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
