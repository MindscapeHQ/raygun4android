package com.raygun.raygun4androidsample.sample

import android.app.Application
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.raygun.raygun4android.RaygunClient
import com.raygun.raygun4android.messages.shared.RaygunUserInfo
import android.widget.Button
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val initialCustomData = HashMap<String,String>()
        initialCustomData["firstkey"] = "firstvalue"

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

        buttonSend.setOnClickListener {
            val tw = HashMap<String,String>()
            tw["secondkey"] = "secondvalue"

            // Manual exception creation & sending
            RaygunClient.send(Exception("Congratulations, you have sent errors with Raygun4Android"), null, tw)
        }

        buttonCrash.setOnClickListener {
            // A real exception will be thrown here, which will be caught & sent by RaygunClient
            val i = 3 / 0
            Log.d("Raygun4Android-Sample", "This is here purely so that our division by zero calculation in i gets used and not optimised away in a release build: $i")
        }

        buttonHandleException.setOnClickListener {
            // Handle an exception yourself - nothing should be sent to Raygun
            try {
                val i = 3 / 0
                Log.d("Raygun4Android-Sample", "This is here purely so that our division by zero calculation in i gets used and not optimised away in a release build: $i")
            } catch (ex: Exception) {
                val i = 4
                Log.d("Raygun4Android-Sample", "This is here purely so that our alternative value for i gets used and not optimised away in a release build: $i")
            }
        }

        buttonSetUserAnon.setOnClickListener {
            val user = RaygunUserInfo()
            RaygunClient.setUser(user)
        }

        buttonSetUserA.setOnClickListener {
            val user = RaygunUserInfo("superuser3")
            user.fullName = "User Name C"
            user.firstName = "User C"
            user.email = "e@f.com.com"
            RaygunClient.setUser(user)
            RaygunClient.recordBreadcrumb("I'm now user C")
        }

        buttonSetUserB.setOnClickListener{
            val user = RaygunUserInfo("superuser4")
            user.fullName = "User Name D"
            user.firstName = "User D"
            user.email = "g@h.com"
            RaygunClient.setUser(user)
            RaygunClient.recordBreadcrumb("I'm now user D")
        }

        buttonSecondActivity.setOnClickListener {
            startActivity(SecondActivity.getIntent(this@MainActivity))
        }


        textViewAppVersion.text = "App ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE} ${BuildConfig.BUILD_TYPE})"
        textViewProviderVersion.text = "Provider ${com.raygun.raygun4android.BuildConfig.VERSION_NAME} (${com.raygun.raygun4android.BuildConfig.VERSION_CODE} ${BuildConfig.BUILD_TYPE})"

        RaygunClient.recordBreadcrumb("I'm here in Main Activity")

    }
}
