package com.eightbit.samsprung.launcher

import android.os.*
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.eightbit.samsprung.R

class PanelViewFragment : Fragment() {

    private var listener: ViewCreatedListener? = null
    private var layout: LinearLayout? = null

    fun setListener(listener: ViewCreatedListener) {
        this.listener = listener
    }

    fun getLayout() : LinearLayout? {
        return layout
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_panel, container, false
        ) as ViewGroup
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        layout = view as LinearLayout
        listener?.onViewCreated(view)
    }

    interface ViewCreatedListener {
        fun onViewCreated(view: View)
    }
}