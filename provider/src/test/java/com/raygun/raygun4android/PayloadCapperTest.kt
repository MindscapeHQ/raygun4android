package com.raygun.raygun4android

import com.raygun.raygun4android.messages.crashreporting.RaygunBreadcrumbMessage
import com.raygun.raygun4android.messages.crashreporting.RaygunErrorMessage
import com.raygun.raygun4android.messages.crashreporting.RaygunErrorStackTraceLineMessage
import com.raygun.raygun4android.messages.crashreporting.RaygunMessage
import com.raygun.raygun4android.messages.crashreporting.RaygunMessageDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PayloadCapperTest {
    private fun makeMessage(
        breadcrumbCount: Int = 0,
        stackFrameCount: Int = 0,
    ): RaygunMessage {
        val breadcrumbs =
            if (breadcrumbCount > 0) {
                (1..breadcrumbCount).map { i ->
                    RaygunBreadcrumbMessage.Builder("breadcrumb $i").build()
                }
            } else {
                null
            }

        val error =
            if (stackFrameCount > 0) {
                RaygunErrorMessage(
                    message = "TestException",
                    className = "com.test.TestException",
                    stackTrace =
                        (1..stackFrameCount).map { i ->
                            RaygunErrorStackTraceLineMessage(
                                lineNumber = i,
                                className = "com.test.SomeClass",
                                fileName = "SomeClass.kt",
                                methodName = "method$i",
                            )
                        },
                )
            } else {
                null
            }

        return RaygunMessage(
            details = RaygunMessageDetails(error = error, breadcrumbs = breadcrumbs),
        )
    }

    private fun identity(msg: RaygunMessage): String = "x".repeat(100)

    @Test
    fun smallPayload_noChanges() {
        val msg = makeMessage(breadcrumbCount = 5, stackFrameCount = 10)
        PayloadCapper.capIfNeeded(msg) { "small" }
        assertEquals(5, msg.details.breadcrumbs?.size)
        assertEquals(
            10,
            msg.details.error
                ?.stackTrace
                ?.size,
        )
    }

    @Test
    fun largeBreadcrumbs_cappedTo31() {
        val msg = makeMessage(breadcrumbCount = 100)
        PayloadCapper.capIfNeeded(msg) { json ->
            // First call is over limit, after capping breadcrumbs it's under
            if (msg.details.breadcrumbs?.size == 100) {
                "x".repeat(130 * 1024)
            } else {
                "small"
            }
        }
        val breadcrumbs = msg.details.breadcrumbs!!
        assertEquals(31, breadcrumbs.size)
        assertTrue(breadcrumbs[0].message!!.contains("breadcrumbs removed"))
        assertEquals("breadcrumb 71", breadcrumbs[1].message)
        assertEquals("breadcrumb 100", breadcrumbs[30].message)
    }

    @Test
    fun largeStackTrace_cappedWithNote() {
        val msg = makeMessage(stackFrameCount = 200)
        PayloadCapper.capIfNeeded(msg) { "x".repeat(130 * 1024) }
        val frames = msg.details.error!!.stackTrace
        // top 20 + 1 note + bottom 20 = 41
        assertEquals(41, frames.size)
        // First frame is original top
        assertEquals("method1", frames[0]!!.methodName)
        // Frame at index 20 is the note
        assertTrue(frames[20]!!.methodName.contains("frames removed"))
        assertTrue(frames[20]!!.className.contains("Raygun4Android"))
        // Last frame is original bottom
        assertEquals("method200", frames[40]!!.methodName)
    }

    @Test
    fun innerError_alsoCappped() {
        val innerError =
            RaygunErrorMessage(
                message = "InnerException",
                className = "com.test.InnerException",
                stackTrace =
                    (1..150).map { i ->
                        RaygunErrorStackTraceLineMessage(
                            lineNumber = i,
                            className = "com.test.Inner",
                            fileName = "Inner.kt",
                            methodName = "innerMethod$i",
                        )
                    },
            )

        val msg =
            RaygunMessage(
                details =
                    RaygunMessageDetails(
                        error =
                            RaygunErrorMessage(
                                message = "OuterException",
                                className = "com.test.OuterException",
                                stackTrace =
                                    (1..150).map { i ->
                                        RaygunErrorStackTraceLineMessage(
                                            lineNumber = i,
                                            className = "com.test.Outer",
                                            fileName = "Outer.kt",
                                            methodName = "outerMethod$i",
                                        )
                                    },
                                innerError = innerError,
                            ),
                    ),
            )

        PayloadCapper.capIfNeeded(msg) { "x".repeat(130 * 1024) }
        assertEquals(
            41,
            msg.details.error!!
                .stackTrace.size,
        )
        assertEquals(
            41,
            msg.details.error!!
                .innerError!!
                .stackTrace.size,
        )
    }

    @Test
    fun noBreadcrumbs_noError() {
        val msg = makeMessage()
        PayloadCapper.capIfNeeded(msg) { "x".repeat(130 * 1024) }
        assertEquals(null, msg.details.breadcrumbs)
        assertEquals(null, msg.details.error)
    }

    @Test
    fun breadcrumbsUnder30_notTouched() {
        val msg = makeMessage(breadcrumbCount = 20)
        PayloadCapper.capIfNeeded(msg) { "x".repeat(130 * 1024) }
        assertEquals(20, msg.details.breadcrumbs?.size)
    }

    @Test
    fun exactly30Breadcrumbs_notCapped() {
        val msg = makeMessage(breadcrumbCount = 30)
        PayloadCapper.capIfNeeded(msg) { "x".repeat(130 * 1024) }
        assertEquals(30, msg.details.breadcrumbs?.size)
        assertEquals("breadcrumb 1", msg.details.breadcrumbs!![0].message)
    }

    @Test
    fun exactly31Breadcrumbs_cappedTo31WithNote() {
        val msg = makeMessage(breadcrumbCount = 31)
        PayloadCapper.capIfNeeded(msg) { json ->
            if (
                msg.details.breadcrumbs?.size == 31 &&
                msg.details.breadcrumbs!![0].message == "breadcrumb 1"
            ) {
                "x".repeat(130 * 1024)
            } else {
                "small"
            }
        }
        val breadcrumbs = msg.details.breadcrumbs!!
        assertEquals(31, breadcrumbs.size)
        assertTrue(breadcrumbs[0].message!!.contains("1 earlier breadcrumbs removed"))
        assertEquals("breadcrumb 2", breadcrumbs[1].message)
        assertEquals("breadcrumb 31", breadcrumbs[30].message)
    }

    @Test
    fun exactly100StackFrames_notCapped() {
        val msg = makeMessage(stackFrameCount = 100)
        PayloadCapper.capIfNeeded(msg) { "x".repeat(130 * 1024) }
        assertEquals(
            100,
            msg.details.error!!
                .stackTrace.size,
        )
        assertEquals(
            "method1",
            msg.details.error!!
                .stackTrace[0]!!
                .methodName,
        )
    }

    @Test
    fun exactly101StackFrames_capped() {
        val msg = makeMessage(stackFrameCount = 101)
        PayloadCapper.capIfNeeded(msg) { "x".repeat(130 * 1024) }
        val frames = msg.details.error!!.stackTrace
        assertEquals(41, frames.size)
        assertEquals("method1", frames[0]!!.methodName)
        assertTrue(frames[20]!!.methodName.contains("61 frames removed"))
        assertEquals("method101", frames[40]!!.methodName)
    }

    @Test
    fun breadcrumbCappingSufficient_stackFramesUntouched() {
        val msg = makeMessage(breadcrumbCount = 100, stackFrameCount = 200)
        PayloadCapper.capIfNeeded(msg) { json ->
            if (msg.details.breadcrumbs?.size == 100) {
                "x".repeat(130 * 1024)
            } else {
                "small"
            }
        }
        assertEquals(31, msg.details.breadcrumbs!!.size)
        assertEquals(
            200,
            msg.details.error!!
                .stackTrace.size,
        )
    }

    @Test
    fun breadcrumbNoteContainsCorrectCount() {
        val msg = makeMessage(breadcrumbCount = 50)
        PayloadCapper.capIfNeeded(msg) { json ->
            if (msg.details.breadcrumbs?.size == 50) {
                "x".repeat(130 * 1024)
            } else {
                "small"
            }
        }
        val note = msg.details.breadcrumbs!![0]
        assertTrue(note.message!!.contains("20 earlier breadcrumbs removed"))
    }

    @Test
    fun stackFrameNoteContainsCorrectCount() {
        val msg = makeMessage(stackFrameCount = 300)
        PayloadCapper.capIfNeeded(msg) { "x".repeat(130 * 1024) }
        val note = msg.details.error!!.stackTrace[20]!!
        assertEquals("260 frames removed from middle of stack trace", note.methodName)
    }

    @Test
    fun capIfNeeded_returnsSerializedJson() {
        val msg = makeMessage(breadcrumbCount = 5)
        val result = PayloadCapper.capIfNeeded(msg) { "the-json-output" }
        assertEquals("the-json-output", result)
    }

    @Test
    fun stillOverLimit_afterAllCapping_returnsJsonAnyway() {
        val msg = makeMessage(breadcrumbCount = 100, stackFrameCount = 200)
        val bigJson = "x".repeat(130 * 1024)
        val result = PayloadCapper.capIfNeeded(msg) { bigJson }
        assertEquals(bigJson, result)
        // Verify capping was still applied
        assertEquals(31, msg.details.breadcrumbs!!.size)
        assertEquals(
            41,
            msg.details.error!!
                .stackTrace.size,
        )
    }

    @Test
    fun emptyBreadcrumbList_notTouched() {
        val msg = RaygunMessage(details = RaygunMessageDetails(breadcrumbs = emptyList()))
        PayloadCapper.capIfNeeded(msg) { "x".repeat(130 * 1024) }
        assertEquals(0, msg.details.breadcrumbs!!.size)
    }

    @Test
    fun tripleNestedInnerError_allCapped() {
        fun makeError(depth: Int): RaygunErrorMessage =
            RaygunErrorMessage(
                message = "Exception at depth $depth",
                className = "com.test.Exception$depth",
                stackTrace =
                    (1..150).map { i ->
                        RaygunErrorStackTraceLineMessage(
                            lineNumber = i,
                            className = "com.test.Class$depth",
                            fileName = "Class$depth.kt",
                            methodName = "method$i",
                        )
                    },
                innerError = if (depth > 0) makeError(depth - 1) else null,
            )

        val msg = RaygunMessage(details = RaygunMessageDetails(error = makeError(3)))

        PayloadCapper.capIfNeeded(msg) { "x".repeat(130 * 1024) }

        var error: RaygunErrorMessage? = msg.details.error
        var level = 0
        while (error != null) {
            assertEquals("Stack at depth $level should be capped", 41, error.stackTrace.size)
            error = error.innerError
            level++
        }
        assertEquals(4, level)
    }

    @Test
    fun breadcrumbNote_hasWarningLevel() {
        val msg = makeMessage(breadcrumbCount = 50)
        PayloadCapper.capIfNeeded(msg) { json ->
            if (msg.details.breadcrumbs?.size == 50) {
                "x".repeat(130 * 1024)
            } else {
                "small"
            }
        }
        val note = msg.details.breadcrumbs!![0]
        assertEquals("Raygun4Android", note.category)
    }
}
