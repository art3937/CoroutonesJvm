
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dto.Comment
import dto.Post
import dto.PostWithComments
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import ru.netology.coroutines.dto.Author
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


//fun main() {
//    val custom = Executors.newFixedThreadPool(64).asCoroutineDispatcher()
//    with(CoroutineScope(EmptyCoroutineContext)) {
//        launch(Dispatchers.Default) {
//            println(Thread.currentThread().name)
//        }
//
//        launch(Dispatchers.IO) {
//            println(Thread.currentThread().name)
//        }
//
//        launch(custom) {
//            println(Thread.currentThread().name)
//        }
//    }
//    Thread.sleep(1000L)
//    custom.close()
//}

    private val gson = Gson()
    private val BASE_URL = "http://127.0.0.1:9999"
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor(::println).apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    fun main() {
        with(CoroutineScope(EmptyCoroutineContext)) {
            launch {
                try {
                    val posts = getPosts(client)
                        .map { post ->
                            async {
                                PostWithComments( post, getComments(client, post.id),getAuthor(client, post.authorId))
                            }
                        }.awaitAll()
                    posts.forEach { println(it) }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        Thread.sleep(30_000L)
    }

    suspend fun OkHttpClient.apiCall(url: String): Response {
        return suspendCoroutine { continuation ->
            Request.Builder()
                .url(url)
                .build()
                .let(::newCall)
                .enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        continuation.resume(response)
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        continuation.resumeWithException(e)
                    }
                })
        }
    }

    suspend fun <T> makeRequest(url: String, client: OkHttpClient, typeToken: TypeToken<T>): T =
        withContext(Dispatchers.IO) {
            client.apiCall(url)
                .let { response ->
                    if (!response.isSuccessful) {
                        response.close()
                        throw RuntimeException(response.message)
                    }
                    val body = response.body ?: throw RuntimeException("response body is null")
                    gson.fromJson(body.string(), typeToken.type)
                }
        }

    suspend fun getPosts(client: OkHttpClient): List<Post> =
        makeRequest("$BASE_URL/api/slow/posts", client, object : TypeToken<List<Post>>() {})

    suspend fun getComments(client: OkHttpClient, id: Long): List<Comment> =
        makeRequest("$BASE_URL/api/slow/posts/$id/comments", client, object : TypeToken<List<Comment>>() {})

suspend fun getAuthor(client: OkHttpClient, id: Long): Author =
   makeRequest("$BASE_URL/api/slow/authors/$id", client, object : TypeToken<Author>() {})

