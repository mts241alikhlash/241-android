package id.sch.mts241alikhlash.exam.data.remote

import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class EnvelopeConverterFactory : Converter.Factory() {
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit,
    ): Converter<ResponseBody, *>? {
        if (getRawType(type) == ApiEnvelope::class.java) return null
        if (type == Unit::class.java || type == Void::class.java) return null
        val envelopeType = envelopeOf(type)
        val delegate = retrofit.nextResponseBodyConverter<ApiEnvelope<Any>>(this, envelopeType, annotations)
        val isList = List::class.java.isAssignableFrom(getRawType(type))
        return Converter<ResponseBody, Any> { body ->
            val envelope = delegate.convert(body)!!
            if (isList && envelope.data == null) emptyList<Any>() else envelope.unwrap()
        }
    }

    private fun envelopeOf(inner: Type): Type =
        object : ParameterizedType {
            override fun getRawType(): Type = ApiEnvelope::class.java

            override fun getActualTypeArguments(): Array<Type> = arrayOf(inner)

            override fun getOwnerType(): Type? = null
        }
}
