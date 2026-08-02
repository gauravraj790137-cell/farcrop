package com.example.farcrop.network

import com.example.farcrop.model.V2StandardResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Url

interface ApiService {
    /**
     * Upload a leaf image with crop-cycle metadata.
     * We use a single [MultipartBody] built manually in the repository
     * so we can safely skip null fields and avoid Retrofit's quirky
     * nullable-@Part behaviour which causes 400s on some servers.
     */
    @POST
    suspend fun uploadImage(
        @Url url: String,
        @Body body: RequestBody
    ): V2StandardResponse

    @GET("health")
    suspend fun checkHealth(): V2StandardResponse
}
