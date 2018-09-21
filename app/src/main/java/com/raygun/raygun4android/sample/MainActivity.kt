package com.raygun.raygun4android.sample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.raygun.raygun4android.RaygunClient
import com.raygun.raygun4android.messages.RaygunUserInfo
import android.widget.Button


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val initialCustomData = HashMap<String,String>()
        initialCustomData.put("firstkey", "firstvalue")

        RaygunClient.init(applicationContext)      // This sets up the client with the API key as provided in your AndroidManifest.xml
        RaygunClient.attachExceptionHandler()      // This attaches a pre-made exception handler to catch all uncaught exceptions, and send them to Raygun

        RaygunClient.setUserCustomData(initialCustomData)

        val button = findViewById(R.id.button) as Button

        button.setOnClickListener{

            var user = RaygunUserInfo()
            user.fullName = "User Name"
            user.firstName = "User"
            user.email = "a@b.com"
            user.uuid = "a uuid"
            user.setAnonymous(false)

            val tw = HashMap<String,String>()

            tw.put("secondkey", "secondvalue")

            // Example 1: Manual exception creation & sending
            RaygunClient.setUser(user)
            RaygunClient.send(Exception("Congratulations, you have sent errors with Raygun4Android"), null, tw)

            // Example 2: A real exception will be thrown here, which will be caught & sent by RaygunClient
            val i = 3 / 0
        }
    }
}
