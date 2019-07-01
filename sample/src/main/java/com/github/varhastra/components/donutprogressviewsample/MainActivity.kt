package com.github.varhastra.components.donutprogressviewsample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private val rand = Random(27)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        donutProgressView.progress = 75
//        donutProgressView.startAngle = -90f
//        donutProgressView.progressColor = 0xFF2196f3.toInt()
//        donutProgressView.trackColor = 0x1F2196f3.toInt()
//        donutProgressView.animationDurationMillis = 300L

        buttonPlus.setOnClickListener {
            val newProgress = (donutProgressView.progress + rand.nextInt(10, 26))
            if (newProgress > donutProgressView.maxProgress) {
                donutProgressView.setProgress(donutProgressView.maxProgress, true)
            } else {
                donutProgressView.setProgress(newProgress, true)
            }
        }

        buttonMinus.setOnClickListener {
            val newProgress = (donutProgressView.progress - rand.nextInt(10, 26))
            if (newProgress < donutProgressView.minProgress) {
                donutProgressView.setProgress(donutProgressView.minProgress, true)
            } else {
                donutProgressView.setProgress(newProgress, true)
            }
        }

        buttonReset.setOnClickListener { donutProgressView.setProgress(donutProgressView.minProgress, true) }
    }
}
