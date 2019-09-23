package com.alon.sunposition

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.animation.Animation.RELATIVE_TO_SELF
import android.view.animation.RotateAnimation
import java.util.concurrent.TimeUnit
import io.reactivex.android.schedulers.AndroidSchedulers;

class MainActivity : AppCompatActivity() {

    lateinit var mainActivityViewModel: MainActivityViewModeling

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainActivityViewModel = MainActivityViewModel(this)

        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        mainActivityViewModel.inputs.onResume()
    }

    override fun onPause() {
        super.onPause()
        mainActivityViewModel.inputs.onPause()
    }

    private fun setupObservers() {
        mainActivityViewModel.outputs.compassViewModel
            .outputs.compassPosition
            .throttleLast(100, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({animationData -> animateCompass(animationData)})
    }

    private fun animateCompass(animationData: AnimationData) {
        val rotateAnimation = RotateAnimation(
            animationData.previousDegree,
            animationData.currentDegree,
            RELATIVE_TO_SELF, 0.5f,
            RELATIVE_TO_SELF, 0.5f)
        rotateAnimation.duration = 1000
        rotateAnimation.fillAfter = true
    }

    private fun animateSun(animationData: AnimationData) {

    }
}
