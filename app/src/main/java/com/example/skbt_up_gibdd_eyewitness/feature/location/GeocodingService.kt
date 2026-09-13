package com.example.skbt_up_gibdd_eyewitness.feature.location

import com.google.gson.annotations.SerializedName
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class AddressSearchResult(
    @SerializedName("display_name") val displayName: String,
    val lat: String,
    val lon: String,
) {
    fun locationOrNull(): StaticLocation? {
        val latitude = lat.toDoubleOrNull() ?: return null
        val longitude = lon.toDoubleOrNull() ?: return null
        return StaticLocation(latitude, longitude)
    }
}

class GeocodingService private constructor(private val api: NominatimApi) {
    suspend fun search(query: String): Result<List<AddressSearchResult>> = runCatching {
        api.search(query = "$query, Костромская область, Россия")
    }

    companion object {
        fun create(): GeocodingService {
            val identifyingHeader = Interceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "GIBDD-Eyewitness-Android/1.0 (https://github.com/FaneD1/SKBT-UP-GIBDD-EYEWITNESS)")
                        .build(),
                )
            }
            val retrofit = Retrofit.Builder()
                .baseUrl("https://nominatim.openstreetmap.org/")
                .client(OkHttpClient.Builder().addInterceptor(identifyingHeader).build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return GeocodingService(retrofit.create(NominatimApi::class.java))
        }
    }
}

private interface NominatimApi {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "jsonv2",
        @Query("limit") limit: Int = 5,
        @Query("countrycodes") countryCodes: String = "ru",
        @Query("viewbox") viewbox: String = "38.7,59.7,47.8,56.7",
        @Query("bounded") bounded: Int = 1,
        @Query("accept-language") language: String = "ru",
    ): List<AddressSearchResult>
}
