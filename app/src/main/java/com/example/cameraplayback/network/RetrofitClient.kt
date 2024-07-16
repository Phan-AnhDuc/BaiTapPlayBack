import com.example.cameraplayback.service.ApiService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://stagingapionehome.vnpt-technology.vn/api/"

    private const val TOKEN = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI4NDkxNDA4MTE5MiIsImV4cCI6MzYxMzA0MDMwNywidXNlcl9pZCI6MjA5MzQsInNjb3BlIjpbXSwicGFydG5lcl9pZCI6MH0.mV8g5zq1nelkuv1GscbYY6QCSZ2oPDel06sbi8z6rI3om7N_DS9WFKMuJyKQjvXXCL30uXdU0qEboYxW3jv6rQ"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val original: Request = chain.request()
        val request: Request = original.newBuilder()
            .header("Authorization", "Bearer $TOKEN")
            .build()
        chain.proceed(request)
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(httpClient)
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
