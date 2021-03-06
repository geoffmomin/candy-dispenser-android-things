package za.co.riggaroo.iwantcandy.ui.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.support.annotation.StringRes
import android.util.Log
import android.view.View
import android.widget.*
import com.google.android.gms.vision.face.FaceDetector
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper
import za.co.riggaroo.iwantcandy.R
import za.co.riggaroo.iwantcandy.repo.TwitterRepository
import za.co.riggaroo.iwantcandy.ui.components.CandyCamera
import za.co.riggaroo.iwantcandy.ui.components.CandyMachineActuator
import za.co.riggaroo.iwantcandy.utils.BoardDefaults


class CandyActivity : Activity(), CandyContract.CandyView {

    private val TAG: String = "CandyActivity"
    private lateinit var photoImageView: ImageView
    private lateinit var photoOverlay: Bitmap
    private lateinit var progressBarLoading: ProgressBar
    private lateinit var errorTextView: TextView
    private lateinit var candyPresenter: CandyPresenter

    private var candyMachine: CandyMachineActuator? = null
    private lateinit var camera: CandyCamera

    private lateinit var buttonCandy: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupScreenElements()
        setupCandyDispenser()
        setupCamera()
        setupPresenter()
    }

    private fun setupPresenter() {
        val faceDetector = FaceDetector.Builder(this)
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build()
        val overlayBitmapOptions = BitmapFactory.Options()
        overlayBitmapOptions.inMutable = true
        photoOverlay = BitmapFactory.decodeResource(resources, R.drawable.picture_frame, overlayBitmapOptions)

        candyPresenter = CandyPresenter(
                TwitterRepository(TwitterRepository.DependencyProvider(), this, resources.getStringArray(R.array.tweet_text), resources.getStringArray(R.array.tweet_random_emoji)),
                faceDetector, this, photoOverlay)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase))
    }

    private fun setupCandyDispenser() {
        candyMachine = CandyMachineActuator(BoardDefaults.candyDispensingPin)
    }

    private fun setupCamera() {
        camera = CandyCamera.getInstance()
        camera.initializeCamera(this, Handler(), mOnImageAvailableListener)
    }


    private fun setupScreenElements() {
        errorTextView = findViewById(R.id.text_view_error)
        photoImageView = findViewById(R.id.image_view_photo)
        progressBarLoading = findViewById(R.id.progress_bar_loading)
        buttonCandy = findViewById(R.id.button_activate_candy)
        buttonCandy.setOnClickListener { _ ->
            pressSmile()
        }
        val buttonSettings = findViewById<ImageButton>(R.id.button_settings)
        buttonSettings.setOnClickListener { _ ->
            openSettings()
        }
    }

    private fun openSettings() {

        startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
    }

    private fun pressSmile() {
        candyPresenter.onTakePhotoPressed()
    }

    private val mOnImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        val image = reader.acquireLatestImage()
        val imageBuffer = image.planes[0].buffer
        val imageBytes = ByteArray(imageBuffer.remaining())
        imageBuffer.get(imageBytes)
        image.close()

        candyPresenter.onPictureTaken(imageBytes)
    }


    private fun showErrorMessage(@StringRes message: Int) {
        Log.d(TAG, getString(message))
        errorTextView.text = getString(message)
        hideLoading()
    }

    override fun clearMessages() {
        errorTextView.text = ""
    }

    override fun showLoading() {
        buttonCandy.visibility = View.INVISIBLE
        progressBarLoading.visibility = View.VISIBLE
    }

    override fun hideLoading() {
        buttonCandy.visibility = View.VISIBLE
        progressBarLoading.visibility = View.INVISIBLE
    }

    override fun showNoSmileDetected() {
        showErrorMessage(R.string.error_not_smiling)
    }

    override fun showDispensingCandy() {
        showErrorMessage(R.string.success_dispensing_candy)
    }

    override fun showNoFacesDetected() {
        showErrorMessage(R.string.error_no_faces_detected)
    }

    override fun showFaceDetectorNotOperational() {
        showErrorMessage(R.string.error_face_detector_not_operational)
    }

    override fun showImage(overlayedBitmap: Bitmap) {
        photoImageView.setImageBitmap(overlayedBitmap)
    }

    override fun dispenseCandy() {
        candyMachine?.dispenseCandy()

    }

    override fun triggerCamera() {
        camera.takePicture()
    }

    override fun onDestroy() {
        super.onDestroy()
        candyMachine?.close()
        camera.close()
    }

    override fun showTweetPosted() {
        Log.d(TAG, getString(R.string.tweet_posted_success)) //TODO Change to toast or snackbar when they work on AT
    }

    override fun showTweetError(exception: Exception) {
        Log.e(TAG, getString(R.string.tweet_failed) + exception.message)
    }
}