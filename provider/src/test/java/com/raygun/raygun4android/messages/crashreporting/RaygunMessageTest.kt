package com.raygun.raygun4android.messages.crashreporting

import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
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

    @Test
    fun `parse payload`() {
        // Example payload from https://raygun.com/documentation/product-guides/crash-reporting/api/
        val payload = """
            {
            	"occurredOn": "2015-09-08T01:55:28Z",
            	"details": {
            		"machineName": "ServerMachine1",
            		"groupingKey": "ErrorGroup",
            		"version": "1.0.0.1",
            		"client": {
            			"name": "Example Raygun Client",
            			"version": "0.0.0.1",
            			"clientUrl": "/documentation/integrations/api"
            		},
            		"error": {
            			"innerError": {},
            			"data": {
            				"example": 5
            			},
            			"className": "ErrorClass",
            			"message": "An error occurred",
            			"stackTrace": [
            			{
            				"lineNumber": 55,
            				"className": "BrokenService",
            				"columnNumber": 23,
            				"fileName": "BrokenService.cs",
            				"methodName": "BreakEverything()"
            			}]
            		},
            		"environment": {
            			"processorCount": 4,
            			"osVersion": "Windows 10",
            			"windowBoundsWidth": 2560,
            			"windowBoundsHeight": 1440,
            			"browser-Width": 2560,
            			"browser-Height": 1440,
            			"screen-Width": 2560,
            			"screen-Height": 1440,
            			"resolutionScale": 1.0,
            			"color-Depth": 24,
            			"currentOrientation": "Landscape",
            			"cpu": "Intel(R) Core(TM) i5-2500 CPU @ 3.30GHz",
            			"packageVersion": "package version",
            			"architecture": "ARMv7-A",
            			"deviceManufacturer": "Nokia",
            			"model": "Lumia 920",
            			"totalPhysicalMemory": 1024,
            			"availablePhysicalMemory": 16,
            			"totalVirtualMemory": 16,
            			"availableVirtualMemory": 16,
            			"diskSpaceFree": 50000,
            			"deviceName": "Nexus 7",
            			"locale": "en-nz",
            			"utcOffset": -12,
            			"browser": "Mozilla",
            			"browserName": "Netscape",
            			"browser-Version": "5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.65 Safari/537.36",
            			"platform": "Win32"
            		},
            		"tags": ["tag1", "tag 2", "tag-3"],
            		"userCustomData": {
            			"domain": "WORKPLACE",
            			"area": "51"
            		},
            		"request": {
            			"hostName": "https://raygun.io",
            			"url": "/documentation/integrations/api",
            			"httpMethod": "POST",
            			"iPAddress": ["127.0.0.1"],
            			"queryString": {
            				"q": "searchParams"
            			},
            			"form": {
            				"firstName": "Example",
            				"lastName": "Person",
            				"newsletter": true
            			},
            			"headers": {
            				"Referer": "www.google.com",
            				"Host": "raygun.io"
            			},
            			"rawData": "{\"Test\": 5}"
            		},
            		"response": {
            			"statusCode": 500
            		},
            		"user": {
            			"identifier": "123456789",
            			"isAnonymous": false,
            			"email": "test@example.com",
            			"fullName": "Test User",
            			"firstName": "Test",
            			"uuid": "783491e1-d4a9-46bc-9fde-9b1dd9ef6c6e"
            		},
            		"breadcrumbs": [{
            			"timeStamp": 1504799959639,
            			"level": 1,
            			"type": "navigation",
            			"category": "checkout",
            			"message": "User navigated to the shopping cart",
            			"className": "ShoppingCart",
            			"methodName": "ViewBasket",
            			"lineNumber": 156,
            			"customData": { "from": "/category/product/123", "to": "/cart/view" }
            		}]
            	}
            }
        """.trimIndent()
        val actual = Gson().fromJson(payload, RaygunMessage::class.java)

        val expected = RaygunMessage(
            occurredOn = "2015-09-08T01:55:28Z",
            details = RaygunMessageDetails(
                groupingKey = "ErrorGroup"
            )
        )

        assertEquals("original and parsed payload match", expected, actual)
    }
}
