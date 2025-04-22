@file:OptIn(DelicateCoroutinesApi::class)

package com.raygun.raygun4android.sample

import android.app.Application
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.raygun.raygun4android.RaygunClient
import com.raygun.raygun4android.messages.shared.RaygunUserInfo
import com.raygun.raygun4android.sample.fragments.NavigationActivity
import com.raygun.raygun4android.sample.fragments.NavigationFragmentSwapActivity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Arrays

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val initialCustomData = HashMap<String, String>()
        initialCustomData["firstkey"] = "firstvalue"

        // Provide a custom OkHttpClient
        //        RaygunClient.setOkHttpClientBuilder {
        //            Log.d("MainActivity", "Using custom http client");
        //            OkHttpClient().newBuilder().build();
        //        }

        // This sets up the client with the API key as provided in your AndroidManifest.xml
        RaygunClient.init(applicationContext as Application)
        RaygunClient.enableCrashReporting()
        RaygunClient.enableRUM(this)
        RaygunClient.shouldProcessBreadcrumbLocation(true)

        RaygunClient.setCustomData(initialCustomData)

        RaygunClient.setOnBeforeSend(SampleOnBeforeSend())

        val buttonSend = findViewById<Button>(R.id.button_send)
        val buttonCrash = findViewById<Button>(R.id.button_crash)
        val buttonHandleException = findViewById<Button>(R.id.button_handleException)
        val buttonSetUserAnon = findViewById<Button>(R.id.button_set_user_anon)
        val buttonSetUserA = findViewById<Button>(R.id.button_set_user_A)
        val buttonSetUserB = findViewById<Button>(R.id.button_set_user_B)
        val textViewAppVersion = findViewById<TextView>(R.id.textView_appVersion)
        val textViewProviderVersion = findViewById<TextView>(R.id.textView_providerVersion)
        val buttonSecondActivity = findViewById<Button>(R.id.button_secondActivity)
        val buttonNavigationActivity = findViewById<Button>(R.id.button_navigationActivity)
        val buttonFragmentSwapActivity =
            findViewById<Button>(R.id.button_navigationFragmentSwapActivity)
        val buttonSendLargePayload = findViewById<Button>(R.id.button_send_large)

        buttonSend.setOnClickListener {
            val tw = HashMap<String, String>()
            tw["secondkey"] = "secondvalue"

            // Manual exception creation & sending
            RaygunClient.send(
                Exception("Congratulations, you have sent errors with Raygun4Android"),
                null,
                tw,
            )

            // Manual exception creation & sending via 2 strings
            RaygunClient.send("My custom message", "The reason for the error", null, tw)

            sendNetworkRequest()

            Snackbar
                .make(
                    it,
                    getString(R.string.you_have_just_sent_an_error_with_raygun4android),
                    Snackbar.LENGTH_SHORT,
                ).show()
        }

        // Test 10KB limit on Workmanager.
        // Raygun API accepts up to 128 KB.
        buttonSendLargePayload.setOnClickListener {
            // Generate a large String to attach as message
            val size = 100_000 // 100 KB
            val chars = CharArray(size)
            Arrays.fill(chars, 'a')
            val message = String(chars)

            // Send payload
            RaygunClient.send("large crash", message, null, null)

            Snackbar
                .make(
                    it,
                    getString(R.string.you_have_just_sent_an_error_with_raygun4android),
                    Snackbar.LENGTH_SHORT,
                ).show()
        }

        buttonCrash.setOnClickListener {
            // A real exception will be thrown here, which will be caught & sent by RaygunClient
            val i = 3 / 0
            Log.d(
                "Raygun4Android-Sample",
                "This is here purely so that our division by zero calculation in i gets used and not optimised away " +
                    "in a release build: $i",
            )
        }

        buttonHandleException.setOnClickListener {
            // Handle an exception yourself - nothing should be sent to Raygun
            try {
                val i = 3 / 0
                Log.d(
                    "Raygun4Android-Sample",
                    "This is here purely so that our division by zero calculation in i gets used and not optimised away " +
                        "in a release build: $i",
                )
            } catch (ex: Exception) {
                val i = 4
                Log.d(
                    "Raygun4Android-Sample",
                    "This is here purely so that our alternative value for i gets used and not optimised away " +
                        "in a release build: $i",
                )
                Snackbar
                    .make(
                        it,
                        getString(R.string.you_just_created_and_caught_an_exception),
                        Snackbar.LENGTH_SHORT,
                    ).show()
            }
        }

        buttonSetUserAnon.setOnClickListener {
            GlobalScope.launch {
                val user = RaygunUserInfo.anonymous()
                RaygunClient.setUser(user)
                Snackbar
                    .make(
                        it,
                        getString(R.string.user_is_now_set_to_anonymous_for_future_raygun_reports),
                        Snackbar.LENGTH_SHORT,
                    ).show()
            }
        }

        buttonSetUserA.setOnClickListener {
            val user = RaygunUserInfo.create("superuser3")
            user.fullName = "User Name A"
            user.firstName = "User A"
            user.email = "e@f.com.com"
            RaygunClient.setUser(user)
            RaygunClient.recordBreadcrumb("I'm now user A")
            Snackbar
                .make(
                    it,
                    getString(R.string.user_is_now_set_to_user_a_for_future_raygun_reports),
                    Snackbar.LENGTH_SHORT,
                ).show()
        }

        buttonSetUserB.setOnClickListener {
            val user = RaygunUserInfo.create("superuser4")
            user.fullName = "User Name B"
            user.firstName = "User B"
            user.email = "g@h.com"
            RaygunClient.setUser(user)
            RaygunClient.recordBreadcrumb("I'm now user B")
            Snackbar
                .make(
                    it,
                    getString(R.string.user_is_now_set_to_user_b_for_future_raygun_reports),
                    Snackbar.LENGTH_SHORT,
                ).show()
        }

        buttonSecondActivity.setOnClickListener {
            startSecondActivity.launch(SecondActivity.getIntent(this@MainActivity))
        }

        buttonNavigationActivity.setOnClickListener {
            // Launches an Activity containing a Fragment
            this.startActivity(NavigationActivity.getIntent(this))
        }

        buttonFragmentSwapActivity.setOnClickListener {
            // Launches an Activity containing a Fragment
            this.startActivity(NavigationFragmentSwapActivity.getIntent(this))
        }

        textViewAppVersion.text =
            getString(
                R.string.app_version_text,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
                BuildConfig.BUILD_TYPE,
            )
        textViewProviderVersion.text =
            getString(
                R.string.provider_version_text,
                com.raygun.raygun4android.BuildConfig.VERSION_NAME,
                com.raygun.raygun4android.BuildConfig.VERSION_CODE,
                com.raygun.raygun4android.BuildConfig.BUILD_TYPE,
            )

        RaygunClient.recordBreadcrumb("I'm here in Main Activity")
    }

    private val startSecondActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val rootView: View = findViewById(android.R.id.content)
                Snackbar
                    .make(
                        rootView,
                        getString(R.string.we_returned_to_first_activity),
                        Snackbar.LENGTH_SHORT,
                    ).show()
            }
        }

    @OptIn(DelicateCoroutinesApi::class)
    fun sendNetworkRequest() {
        GlobalScope.launch(Dispatchers.IO) {
            Log.d("Raygun4Android-Sample", "Sending network request")

            try {
                val url = URL("https://example.com")
                val urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.requestMethod = "GET"

                val `in` = BufferedReader(InputStreamReader(urlConnection.inputStream))
                var content: String? = ""
                var current: String?
                while ((`in`.readLine().also { current = it }) != null) {
                    content += current
                }
                urlConnection.disconnect()
                Log.d("Raygun4Android-Sample", "Network Response: $content")
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e("Raygun4Android-Sample", "Network error: $e")
            }
        }
    }
}
