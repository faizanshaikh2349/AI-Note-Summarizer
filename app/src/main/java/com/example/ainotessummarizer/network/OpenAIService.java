package com.example.ainotessummarizer.network;

import com.example.ainotessummarizer.model.QuestionResponseModel;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface OpenAIService {

    @POST("chat/completions")
    Call<QuestionResponseModel> generateQuestions(
            @Body RequestBody body
    );
}