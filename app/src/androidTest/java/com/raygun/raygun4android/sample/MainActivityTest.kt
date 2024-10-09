package com.raygun.raygun4android.sample

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val intentRule = IntentsTestRule(MainActivity::class.java)

    @Test
    fun testPackageContext() {
        // Test that we can get the app context
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.raygun.raygun4android.debug", appContext.packageName)
    }

    @Test
    fun testButtonSendFunctional() {
        onView(withId(R.id.button_send)).perform(click())
    }

    @Test
    fun verifySecondActivityIsStarted() {
        onView(withId(R.id.button_secondActivity))
            .check(matches(isDisplayed()))
            .check(matches(isEnabled()))
            .perform(click())

        intended(hasComponent(SecondActivity::class.java.name))
    }
}
