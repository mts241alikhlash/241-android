package id.sch.mts241alikhlash.cbt.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.io.IOException

const val GENERIC_ERROR_MESSAGE = "Terjadi kesalahan. Coba lagi."
const val NETWORK_ERROR_MESSAGE = "Tidak dapat terhubung ke server."

object ApiErrorCode {
    const val SHORTFALL = "SHORTFALL"
    const val REBIND = "REBIND"
    const val MIN_SUBMIT = "MIN_SUBMIT"
    const val SESSION_LOST = "SESSION_LOST"
    const val WINDOW_CLOSED = "WINDOW_CLOSED"
}

@Serializable
data class ApiEnvelope<T>(
    val statusCode: Int,
    val message: String = "",
    val data: T? = null,
    val code: String? = null
)

fun Throwable.userMessage(): String = (this as? ApiException)?.message ?: GENERIC_ERROR_MESSAGE

class ApiException(
    val statusCode: Int,
    override val message: String,
    val code: String? = null
) : Exception(message)

fun <T> ApiEnvelope<T>.unwrap(): T =
    data ?: throw ApiException(statusCode, message.ifBlank { GENERIC_ERROR_MESSAGE }, code)

suspend fun <T> apiCall(block: suspend () -> ApiEnvelope<T>): T = guarded { block().unwrap() }

suspend fun <T> guarded(block: suspend () -> T): T =
    try {
        block()
    } catch (e: ApiException) {
        throw e
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        val body = e.serverError()
        throw ApiException(
            e.code(),
            body?.first ?: GENERIC_ERROR_MESSAGE,
            body?.second
        )
    } catch (e: IOException) {
        throw ApiException(0, NETWORK_ERROR_MESSAGE)
    } catch (e: SerializationException) {
        throw ApiException(0, GENERIC_ERROR_MESSAGE)
    }

private val errorJson = Json { ignoreUnknownKeys = true }

private fun HttpException.serverError(): Pair<String, String?>? =
    runCatching {
        val body = response()?.errorBody()?.string().orEmpty()
        val json = errorJson.parseToJsonElement(body).jsonObject
        val message = json["message"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        val code = json["code"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        message?.let { it to code }
    }.getOrNull()
