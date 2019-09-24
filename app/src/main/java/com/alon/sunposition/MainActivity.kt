package com.alon.sunposition

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation.RELATIVE_TO_SELF
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.TimeUnit
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import androidx.constraintlayout.widget.ConstraintLayout


class MainActivity : AppCompatActivity() {

    private lateinit var mainActivityViewModel: MainActivityViewModeling
    private lateinit var compassImgView: ImageView
    private lateinit var sunImgView: ImageView
    private lateinit var descriptionTxtView: TextView

    private val throttleIntervalDuration: Long = 10
    private val animationDuration: Long = 100
    private val compositeDisposable = CompositeDisposable()
    private val permissionsRequestLocationServiceCode = 1122

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainActivityViewModel = MainActivityViewModel(this)
        compassImgView = findViewById(R.id.compassImgView)
        descriptionTxtView = findViewById(R.id.descriptionTxtView)
        sunImgView = findViewById(R.id.sunImgView)

        setupObservers()
        if (isLocationPermissionGranted()) {
            mainActivityViewModel.inputs.loadData()
        } else {
            askForLocationPermission()
        }
    }

    override fun onDestroy() {
        compositeDisposable.dispose()
        super.onDestroy()
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
        val compassSubscription = mainActivityViewModel.outputs.compassViewModel
            .outputs.compassPosition
            .observeOn(AndroidSchedulers.mainThread())
            .throttleLast(throttleIntervalDuration, TimeUnit.MILLISECONDS)
            .subscribe {animationData -> animateCompass(animationData)}

        val visibilitySubscription = mainActivityViewModel.outputs.descriptionVisibility
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { isVisible ->
                val visibility = if (isVisible) View.VISIBLE else View.GONE
                descriptionTxtView.setVisibility(visibility)
            }

        val txtSubscription = mainActivityViewModel.outputs.descriptionTxt
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { txt ->
                descriptionTxtView.text = txt
            }

        val sunAzimuthSubscription =  mainActivityViewModel.outputs.sunAzimuthScreenRelative
            .observeOn(AndroidSchedulers.mainThread())
            .throttleLast(throttleIntervalDuration, TimeUnit.MILLISECONDS)
            .subscribe {animationData -> animateSun(animationData)}

        compositeDisposable.add(compassSubscription)
        compositeDisposable.add(visibilitySubscription)
        compositeDisposable.add(txtSubscription)
        compositeDisposable.add(sunAzimuthSubscription)
    }

    private fun animateCompass(animationData: AnimationData) {
        val rotateAnimation = RotateAnimation(
            animationData.previousDegree,
            animationData.currentDegree,
            RELATIVE_TO_SELF, 0.5f,
            RELATIVE_TO_SELF, 0.5f)
        rotateAnimation.duration = animationDuration
        rotateAnimation.fillAfter = true

        compassImgView.startAnimation(rotateAnimation)
    }

    private fun animateSun(animationData: AnimationData) {
        Log.d("Alon", "Sun current degree: " + animationData.currentDegree + "°")

        val angleAnimation = ValueAnimator.ofFloat(animationData.previousDegree, animationData.currentDegree)
        angleAnimation.duration = animationDuration
        angleAnimation.interpolator = LinearInterpolator()

        angleAnimation.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Float
            val layoutParams = sunImgView.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.circleAngle = value
            sunImgView.layoutParams =layoutParams
        }

        angleAnimation.start()
    }

    private fun isLocationPermissionGranted(): Boolean {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED)
    }

    private fun askForLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            permissionsRequestLocationServiceCode
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            permissionsRequestLocationServiceCode -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted
                    mainActivityViewModel.inputs.loadData()
                } else {
                    // permission denied
                    Log.d("Alon", "Location permission denied")

                }
            }
        }
    }
}
