package com.example.gamemonitoringapp;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;

public class ItexmoSmsSender {

    private static final String API_KEY = "7b7c346a982f04cb7ac1ca04996abd00"; // Replace with your actual API key
    private static final String API_URL = "https://api.semaphore.co/api/v4/messages";
    private final OkHttpClient client = new OkHttpClient();

    public void sendSms(String phoneNumber, String message, Callback callback) {
        RequestBody body = new FormBody.Builder()
                .add("apikey", API_KEY)
                .add("number", phoneNumber)  // Ensure the number is in the correct format
                .add("message", message)
                .build();

        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Handle request failure
                System.err.println("Request failed: " + e.getMessage());
                e.printStackTrace();
                if (callback != null) {
                    callback.onFailure(call, e);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // Read response body once
                String responseBody = response.body() != null ? response.body().string() : "No response body";

                if (!response.isSuccessful()) {
                    // Handle non-successful response
                    System.err.println("Unexpected code " + response.code());
                    System.err.println("Response body: " + responseBody);
                    // Additional debug information
                    System.err.println("URL: " + response.request().url());
                    System.err.println("Headers: " + response.request().headers());
                } else {
                    // Handle successful response
                    System.out.println("Response: " + responseBody);
                }

                if (callback != null) {
                    // Pass the response body to the callback if needed
                    Response newResponse = response.newBuilder()
                            .body(ResponseBody.create(responseBody, response.body().contentType()))
                            .build();
                    callback.onResponse(call, newResponse);
                }
            }
        });
    }

}
