package com.raygun.raygun4android.messages.crashreporting

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RaygunMessageTest {
    @Test
    fun `test occurredOn is set to time on initialization`() {
        val message = RaygunMessage()
        assertNotNull("occurredOn should not be null", message.occurredOn)
    }

    @Test
    fun `test occurredOn is not overwritten when provided`() {
        val customOccurredOn = "2023-01-01T12:00:00"
        val message = RaygunMessage(occurredOn = customOccurredOn)

        assertNotNull("occurredOn should not be null", message.occurredOn)
        assertTrue(
            "occurredOn should match the provided value",
            message.occurredOn == customOccurredOn,
        )
    }
}
