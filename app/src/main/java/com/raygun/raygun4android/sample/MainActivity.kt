package com.raygun.raygun4android.sample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.raygun.raygun4android.RaygunClient
import com.raygun.raygun4android.messages.shared.RaygunUserInfo
import android.widget.Button
import android.widget.TextView


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val initialCustomData = HashMap<String,String>()
        initialCustomData.put("firstkey", "firstvalue")

        RaygunClient.init(application)             // This sets up the client with the API key as provided in your AndroidManifest.xml
        RaygunClient.enableCrashReporting()
        RaygunClient.enableRUM(this)

        RaygunClient.setUserCustomData(initialCustomData)

        val button_send = findViewById(R.id.button_send) as Button
        val button_crash = findViewById(R.id.button_crash) as Button
        val button_set_user_anon = findViewById(R.id.button_set_user_anon) as Button
        val button_set_user_A = findViewById(R.id.button_set_user_A) as Button
        val button_set_user_B = findViewById(R.id.button_set_user_B) as Button
        val textView_appVersion = findViewById(R.id.textView_appVersion) as TextView
        val textView_providerVersion = findViewById(R.id.textView_providerVersion) as TextView

        button_send.setOnClickListener {
            val tw = HashMap<String,String>()
            tw.put("secondkey", "secondvalue")

            // Manual exception creation & sending
            RaygunClient.send(Exception("Congratulations, you have sent errors with Raygun4Android"), null, tw)
        }

        button_crash.setOnClickListener {
            // A real exception will be thrown here, which will be caught & sent by RaygunClient
            val i = 3 / 0
        }

        button_set_user_anon.setOnClickListener {
            var user = RaygunUserInfo()
            RaygunClient.setUser(user)
        }

        button_set_user_A.setOnClickListener {
            var user = RaygunUserInfo("superuser3")
            user.fullName = "User Name C"
            user.firstName = "User C"
            user.email = "e@f.com.com"
            RaygunClient.setUser(user)
        }

        button_set_user_B.setOnClickListener{
            var user = RaygunUserInfo("superuser4")
            user.fullName = "User Name D"
            user.firstName = "User D"
            user.email = "g@h.com"
            RaygunClient.setUser(user)
        }

        textView_appVersion.text = "App ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE} ${BuildConfig.BUILD_TYPE})";
        textView_providerVersion.text = "Provider ${com.raygun.raygun4android.BuildConfig.VERSION_NAME} (${com.raygun.raygun4android.BuildConfig.VERSION_CODE} ${BuildConfig.BUILD_TYPE})";
    }
}
