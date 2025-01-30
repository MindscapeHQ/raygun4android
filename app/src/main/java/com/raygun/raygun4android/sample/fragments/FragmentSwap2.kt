package com.raygun.raygun4android.sample.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.raygun.raygun4android.sample.R

class FragmentSwap2 : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_swap2, container, false)
        view.findViewById<Button>(R.id.button).setOnClickListener {
            parentFragmentManager.beginTransaction().apply {
                replace(R.id.fragment_container_view, FragmentSwap1())
                commit()
            }
        }
        return view
    }
}
