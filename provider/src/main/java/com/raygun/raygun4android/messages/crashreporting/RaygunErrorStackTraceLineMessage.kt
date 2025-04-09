package com.raygun.raygun4android.messages.crashreporting

data class RaygunErrorStackTraceLineMessage(
    var lineNumber: Int,
    var className: String,
    var fileName: String,
    var methodName: String,
) {
    companion object {
        operator fun invoke(
            element: StackTraceElement,
        ): RaygunErrorStackTraceLineMessage {
            return RaygunErrorStackTraceLineMessage(
                lineNumber = element.lineNumber,
                className = element.className,
                fileName = element.fileName ?: "",
                methodName = element.methodName,
            )
        }
    }
}
