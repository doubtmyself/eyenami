package team.eyenami.utills

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import team.eyenami.BuildConfig
import java.io.IOException
import java.util.Locale

class Util {
    companion object {
        public fun getSystemLanguage(): Locale {
            return Locale.getDefault()
        }



        // 모델 목록을 표현할 데이터 클래스
//        data class Model(
//            @SerializedName("id") val id: String,
//            @SerializedName("name") val name: String
//        )
//
//        data class ModelListResponse(
//            @SerializedName("models") val models: List<Model>
//        )
//
//        fun fetchAvailableModels() {
//            val client = OkHttpClient()
//
//            val request = Request.Builder()
//                .url("https://api-endpoint/v1beta/models")
//                .addHeader("Authorization", "Bearer ${BuildConfig.apiKey}")
//                .build()
//
//            client.newCall(request).execute().use { response ->
//                if (!response.isSuccessful) throw IOException("Unexpected code $response")
//
//                val responseBody = response.body?.string()
//                val gson = Gson()
//                val modelListResponse = gson.fromJson(responseBody, ModelListResponse::class.java)
//
//                modelListResponse.models.forEach { model ->
//                    println("Model ID: ${model.id}, Model Name: ${model.name}")
//                }
//            }
//        }

    }
}