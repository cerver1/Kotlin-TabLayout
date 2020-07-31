package com.fair.tablayoutv2

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import dev.bmcreations.scrcast.ScrCast

class FirstFragment : Fragment(R.layout.first_fragment) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val recorder = ScrCast.use(context as Activity)
        recorder.apply {
            // configure options via DSL
            options {
                video {
                    maxLengthSecs = 360
                }
                storage {
                    directoryName = "scrcast-sample"
                }
                notification {
                    title = "Super cool library"
                    description = "shh session in progress"
                    icon = resources.getDrawable(R.drawable.ic_camcorder, null).toBitmap()
                    channel {
                        id = "1337"
                        name = "Recording Service"
                    }
                    showStop = true
                    showPause = true
                    showTimer = true
                }
                moveTaskToBack = false
                startDelayMs = 5_000
            }
        }
    }
}