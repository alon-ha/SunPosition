package com.alon.sunposition

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.animation.Animation.RELATIVE_TO_SELF
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.TextView
import java.util.concurrent.TimeUnit
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable

class MainActivity : AppCompatActivity() {

    lateinit var mainActivityViewModel: MainActivityViewModeling
    lateinit var compassImgView: ImageView
    lateinit var descriptionTxtView: TextView

    val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainActivityViewModel = MainActivityViewModel(this)
        compassImgView = findViewById(R.id.compassImgView) as ImageView
        descriptionTxtView = findViewById(R.id.descriptionTxtView) as TextView

        setupObservers()
        mainActivityViewModel.inputs.loadData()
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
            .throttleLast(10, TimeUnit.MILLISECONDS)
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

        val sunAzimuthSubscribtion =  mainActivityViewModel.outputs.sunAzimuthScreenRelative
            .observeOn(AndroidSchedulers.mainThread())
//            .throttleLast(10, TimeUnit.MILLISECONDS)
//            .subscribe({animationData -> animateSun(animationData)})

        compositeDisposable.add(compassSubscription)
        compositeDisposable.add(visibilitySubscription)
        compositeDisposable.add(txtSubscription)
    }

    private fun animateCompass(animationData: AnimationData) {
        val rotateAnimation = RotateAnimation(
            animationData.previousDegree,
            animationData.currentDegree,
            RELATIVE_TO_SELF, 0.5f,
            RELATIVE_TO_SELF, 0.5f)
        rotateAnimation.duration = 100
        rotateAnimation.fillAfter = true

        compassImgView.startAnimation(rotateAnimation)
    }

    private fun animateSun(animationData: AnimationData) {

    }
}
