package com.raygun.raygun4android.sample.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.raygun.raygun4android.sample.R

/**
 * Activity that contains a Fragment.
 *
 * [Fragment1] is defined in the activity_navigation.xml in a FragmentContainerView.
 *
 * RUM registers the FragmentManager from this Activity automatically,
 * reporting the Fragment lifecycle events as RUM navigation events.
 */
class NavigationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_navigation)
        setTitle(R.string.fragment_text)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    companion object {
        fun getIntent(context: Context): Intent = Intent(context, NavigationActivity::class.java)
    }
}
