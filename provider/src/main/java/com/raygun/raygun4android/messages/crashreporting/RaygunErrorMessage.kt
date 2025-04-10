package com.raygun.raygun4android.messages.crashreporting

data class RaygunErrorMessage(
    var innerError: RaygunErrorMessage? = null,
    var message: String? = null,
    var className: String? = null,
    var stackTrace: List<RaygunErrorStackTraceLineMessage?> = emptyList(),
) {
    companion object {
        operator fun invoke(throwable: Throwable?): RaygunErrorMessage {
            if (throwable == null) {
                return RaygunErrorMessage()
            }
            val message = throwable.javaClass.simpleName + ": " + throwable.message

            var innerError: RaygunErrorMessage? = null
            if (throwable.cause != null) {
                innerError = RaygunErrorMessage(throwable.cause!!)
            }

            val ste = throwable.stackTrace
            val stackTrace: MutableList<RaygunErrorStackTraceLineMessage?> =
                MutableList(size = ste.size) { null }

            for (i in ste.indices) {
                stackTrace[i] = RaygunErrorStackTraceLineMessage(ste[i])
            }

            return RaygunErrorMessage(
                innerError = innerError,
                message = message,
                className = throwable.javaClass.canonicalName,
                stackTrace = stackTrace,
            )
        }
    }
}
