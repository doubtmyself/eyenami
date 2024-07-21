//package team.eyenami.ui.home
//
//import android.Manifest
//import android.content.Context
//import android.content.pm.PackageManager
//import android.hardware.Sensor
//import android.hardware.SensorEvent
//import android.hardware.SensorEventListener
//import android.hardware.SensorManager
//import android.os.Bundle
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ImageView
//import androidx.camera.core.CameraSelector
//import androidx.camera.core.ImageCapture
//import androidx.camera.core.Preview
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import androidx.fragment.app.Fragment
//import team.eyenami.databinding.FragmentHomeBinding
//import java.io.File
//
//class HomeFragment2 : Fragment(), SensorEventListener {
//
//    private var _binding: FragmentHomeBinding? = null
//    // This property is only valid between onCreateView and
//    // onDestroyView.
//    private val binding get() = _binding!!
//
//    private lateinit var sensorManager: SensorManager
//    private var accelerometer: Sensor? = null
//    private var lastUpdate: Long = 0
//    private var lastX: Float = 0.0f
//    private var lastY: Float = 0.0f
//    private var lastZ: Float = 0.0f
//    private var runningState: Boolean = false
//
//    private lateinit var imageCapture: ImageCapture
//    private lateinit var capturedImageView: ImageView
//
//    private val speedSamples = mutableListOf<Double>()
//
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentHomeBinding.inflate(inflater, container, false)
//        val root: View = binding.root
//
//
//        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
//        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
//
//        if (allPermissionsGranted()) {
//            startCamera()
//        } else {
//            ActivityCompat.requestPermissions(
//                requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
//            )
//        }
//
//        capturedImageView = binding.capturedImageView
//
//        return root
//    }
//
//
//    override fun onResume() {
//        super.onResume()
//        accelerometer?.also { accel ->
//            sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
//        }
//    }
//
//
//    override fun onPause() {
//        super.onPause()
//        sensorManager.unregisterListener(this)
//    }
//
//
//    override fun onSensorChanged(event: SensorEvent?) {
//        event?.let {
//            val curTime = System.currentTimeMillis()
//            // Only allow one update every 100ms.
//            if ((curTime - lastUpdate) > 50) {
//                val diffTime = (curTime - lastUpdate)
//                lastUpdate = curTime
//
//                val x = event.values[0]
//                val y = event.values[1]
//                val z = event.values[2]
//
//                val deltaX = x - lastX
//                val deltaY = y - lastY
//                val deltaZ = z - lastZ
//                val speed = Math.sqrt((deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ).toDouble()) / diffTime * 10000
//
//                speedSamples.add(speed)
//                if (speedSamples.size > SAMPLE_SIZE) {
//                    speedSamples.removeAt(0)
//                }
//                val averageSpeed = speedSamples.average()
//
//                if (averageSpeed > 100) {
//                    // This is a simple threshold to determine running or moving.
//                    runningState = true
//                    binding.textHome.text = "User is running or moving"
//                    takePhoto()
//                } else {
//                    runningState = false
//                    binding.textHome.text = "User is stationary"
//                }
//
//                lastX = x
//                lastY = y
//                lastZ = z
//            }
//        }
//    }
//
//    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
//        // Do something if sensor accuracy changes
//    }
//
//    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
//        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
//    }
//
//    private fun startCamera() {
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
//
//        cameraProviderFuture.addListener(Runnable {
//            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
//            val preview = Preview.Builder()
//                .build()
//                .also {
//                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
//                }
//
//            imageCapture = ImageCapture.Builder().build()
//
//            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//
//            try {
//                cameraProvider.unbindAll()
//                cameraProvider.bindToLifecycle(
//                    this, cameraSelector, preview, imageCapture)
//
//            } catch(exc: Exception) {
//                Log.e(TAG, "Use case binding failed", exc)
//            }
//
//        }, ContextCompat.getMainExecutor(requireContext()))
//    }
//
//    private fun takePhoto() {
//        val photoFile = File(
//            requireContext().externalMediaDirs.firstOrNull(),
//            "${System.currentTimeMillis()}.jpg"
//        )
//
//        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
//
////        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(requireContext()), object : ImageCapture.OnImageSavedCallback {
////            override fun onError(exc: ImageCaptureException) {
////                Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
////            }
////
////            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
////                val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
////                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
////                activity?.runOnUiThread {
////                    capturedImageView.setImageBitmap(bitmap)
////                    capturedImageView.visibility = View.VISIBLE
////                    binding.previewView.visibility = View.GONE
////                }
////            }
////        })
//    }
//
//    companion object {
//        private const val TAG = "CameraXApp"
//        private const val REQUEST_CODE_PERMISSIONS = 10
//        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}