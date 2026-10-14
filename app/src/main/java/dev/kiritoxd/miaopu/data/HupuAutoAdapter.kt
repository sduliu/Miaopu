package dev.kiritoxd.miaopu.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Data source for Hupu forum topic threads (e.g. the car section).
 * Uses the public m.hupu.com JSON endpoints, no login required for reads.
 */
class HupuAutoAdapter {
    suspend fun getThreads(
        topic: AutoTopic,
        page: Int,
        cursor: String,
    ): AdapterResult<AutoThreadPage> {
        val endpoint = "${HupuUrls.MOBILE_BBS_BASE}/api/v2/bbs/topicThreads" +
            "?topicId=${topic.topicId}&page=$page&cursor=${encode(cursor)}"
        val request = Request.Builder().url(endpoint)
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .header("Referer", "https://m.hupu.com/")
            .build()
        val response = try {
            HupuHttpTransport.execute(request)
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            return AdapterResult.failure(
                source = SOURCE,
                status = AdapterStatus.TRANSIENT_FAILURE,
                message = error.message ?: "网络连接失败",
                retryable = true,
            )
        }
        return withContext(Dispatchers.Default) { response.toAdapterResult(SOURCE) }
    }

    private fun HupuHttpResponse.toAdapterResult(source: String): AdapterResult<AutoThreadPage> = try {
        when {
            status == HttpURLConnection.HTTP_UNAUTHORIZED || status == HttpURLConnection.HTTP_FORBIDDEN ->
                AdapterResult.failure(source, AdapterStatus.AUTH_REQUIRED, "虎扑登录状态已失效", false, status)
            status !in 200..299 ->
                AdapterResult.failure(source, AdapterStatus.TRANSIENT_FAILURE, "虎扑服务暂时不可用", true, status)
            else -> {
                val json = JSONObject(body)
                if (json.optInt("code") != SUCCESS_CODE) {
                    val message = json.optString("msg").takeIf { it.isNotBlank() } ?: "虎扑服务暂时不可用"
                    AdapterResult.failure(source, AdapterStatus.INVALID_RESPONSE, message, true, status)
                } else {
                    AdapterResult.success(source, HupuJsonParser.autoThreads(body))
                }
            }
        }
    } catch (error: CancellationException) {
        throw error
    } catch (error: IOException) {
        AdapterResult.failure(source, AdapterStatus.TRANSIENT_FAILURE, error.message ?: "网络连接失败", true, status)
    } catch (error: Exception) {
        AdapterResult.failure(source, AdapterStatus.INVALID_RESPONSE, error.message ?: "虎扑响应格式已变化", false, status)
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

    private companion object {
        const val SOURCE = "hupu.auto.threads"
        const val SUCCESS_CODE = 200
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Miaopu/1.0) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36"
    }
}
