package com.example.dumb_app.core.network

import com.example.dumb_app.core.model.Plan.PlanDayDto
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {

    private const val BASE_URL = "http://154.9.24.233:8080/api/"

    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    private val gson: Gson by lazy {
        val listPlanDayType = object : TypeToken<List<PlanDayDto>>() {}.type
        val baseGson = Gson()

        val g = GsonBuilder()
            .registerTypeAdapter(
                listPlanDayType,
                SingleOrArrayAdapter<PlanDayDto>(PlanDayDto::class.java, baseGson)
            )
            .setLenient()
            .create()

        android.util.Log.i("NetworkModule", "Custom Gson installed for List<PlanDayDto>")
        g
    }

    val apiService: ApiService by lazy {
        android.util.Log.i("NetworkModule", "Building Retrofit with custom Gson")
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson)) // 必须用上面的 gson
            .build()
            .create(ApiService::class.java)
    }
}
