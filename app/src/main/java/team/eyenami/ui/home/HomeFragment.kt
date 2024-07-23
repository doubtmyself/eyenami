package team.eyenami.ui.home

import android.Manifest
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.animation.doOnEnd
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.airbnb.lottie.LottieDrawable
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import team.eyenami.BuildConfig
import team.eyenami.R
import team.eyenami.databinding.FragmentHomeBinding
import team.eyenami.obj.SettingManager
import team.eyenami.ui.query.ChatAdapter
import team.eyenami.ui.query.ChatMessage
import team.eyenami.utills.AIResponse
import team.eyenami.utills.Util
import timber.log.Timber
import java.io.File
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val binding by viewBinding(FragmentHomeBinding::bind)


    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private lateinit var chatAdapter: ChatAdapter
    private val gson = Gson()
    private val model = GenerativeModel(
        "gemini-1.5-pro",
        // Retrieve API key as an environmental variable defined in a Build Configuration
        // see https://github.com/google/secrets-gradle-plugin for further instructions
        BuildConfig.apiKey,
        generationConfig = generationConfig {
            temperature = 0.4f
            topK = 40
            topP = 0.9f
            maxOutputTokens = 300
            responseMimeType = "application/json"
        },
        safetySettings = listOf(
            SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.MEDIUM_AND_ABOVE)
        )
    )
//    private val chatHistory = listOf<Content>()
//    private val chat = model.startChat(chatHistory)

    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService

    private val initialPrompt = """
    AI for visually impaired. Follow these rules:
    1. Analyze images for crucial info for blind users.
    2. Categorize as:
       - DANGER: Immediate risks or threats (e.g., obstacles, stairs, moving vehicles)
       - INFO: General environmental information or situation descriptions
       - GUIDE: Directional guidance or action instructions
    3. Max 50 char descriptions.
    4. JSON format:
       {"response":{"category":"CATEGORY","description":"DESCRIPTION"}}
    5. Use ${Util.getSystemLanguage()} language.
    6. Focus on safety and independence.
    
    Here are examples of correct responses:
    1. Image: A corridor with a puddle of water on the floor
       Response: {"response": {"category": "DANGER", "description": "Water puddle on floor, slip hazard"}}
    2. Image: An office with a computer and books on a desk
       Response: {"response": {"category": "INFO", "description": "Office environment, desk with computer and books"}}
    3. Image: A corridor with an exit sign visible on the left
       Response: {"response": {"category": "GUIDE", "description": "Exit 10 meters to the left"}}
    Always respond using this format. If additional information or explanation is needed, provide it concisely within the description.
    """.trimIndent()
    private var photoJob: Job? = null

    private lateinit var tts: TextToSpeech

//    private var setUpFlag = false


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUp()
    }

    @Synchronized
    private fun setUp() {
        if (allPermissionsGranted()) {
            initialize()
            addListener()
            setupCamera()
        } else {
            requestPermissions()
        }
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
        )
    }

    private fun initialize() {
        chatAdapter = ChatAdapter()
        cameraExecutor = Executors.newSingleThreadExecutor()

        tts = TextToSpeech(activity, TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Util.getSystemLanguage())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Timber.e("TTS: Language is not supported")
                }
            } else {
                Timber.e("TTS: Initialization failed")
            }
        })
    }

    private fun addListener() {
        binding.root.setOnClickListener {
            if (!binding.circularProgressBar.indeterminateMode) {
                binding.circularProgressBar.indeterminateMode = true
                binding.aniWave.repeatCount = LottieDrawable.INFINITE
                startPhotoJob()
            } else {
                binding.circularProgressBar.indeterminateMode = false
                photoJob?.cancel() // Coroutine 작업 중지
            }
        }

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                // TTS 발화 시작
                lifecycleScope.launch(Dispatchers.Main) {
                    startVoice()
                }
            }

            override fun onDone(utteranceId: String) {
                // TTS 발화 종료
                lifecycleScope.launch(Dispatchers.Main) {
                    stopVoice()
                }
            }

            override fun onError(utteranceId: String) {
                // TTS 발화 중 오류 발생
            }
        })
    }


    @Synchronized
    private fun startPhotoJob() {
        photoJob?.cancel()
        photoJob = lifecycleScope.launch {
            while (isActive) {
                takePhoto()
                delay(SettingManager.getDetectionCountMS()) // 작업을 반복할 간격을 설정 (예: 1초)
            }
        }
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture)
                binding.circularProgressBar.isEnabled = true
            } catch (exc: Exception) {
                Timber.e("Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
        Timber.d("Camera setup completed")
    }

    private fun takePhoto() {
        Timber.d("Taking photo...")
        val photoFile = File(
            requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "${System.currentTimeMillis()}.jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Timber.e("Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                        processCapturedImage(bitmap)
                    }
                }
            })
    }

    override fun onPause() {
        super.onPause()
        photoJob?.cancel() // onPause 시 Coroutine 작업 중지
        startAnimator?.cancel()
        stopAnimator?.cancel()
    }

    override fun onResume() {
        super.onResume()
        setUp()
    }

    private fun preprocessImage(bitmap: Bitmap): Bitmap {
        val targetSize = 512
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
        return scaledBitmap
    }

    private val mutex = Mutex()
    private suspend fun processCapturedImage(bitmap: Bitmap) {
        val preprocessedImage = preprocessImage(bitmap)
        mutex.withLock {
            try {
                Timber.d("Processing captured image...")
                val response = model.generateContent(
                    content {
                        image(preprocessedImage)
                        text(initialPrompt)
                    }
                )

                response.text?.let { responseText ->
                    Timber.d("AI response: $responseText")
                    try {
                        val aiResponse = gson.fromJson(responseText, AIResponse::class.java)
                        val category = aiResponse.response.category
                        val description = aiResponse.response.description
                        withContext(Dispatchers.Main) {
                            chatAdapter.addMessage(ChatMessage("$category: $description", true))
                            speakTTS(aiResponse)
                        }
                    } catch (e: Exception) {
                        Timber.e("Error parsing JSON response: ${e.message}")
                        withContext(Dispatchers.Main) {
                            chatAdapter.addMessage(ChatMessage("INFO: $responseText", true))
                        }
                    }
                } ?: run {
                    Timber.e("Empty response from AI")
                    withContext(Dispatchers.Main) {
                        chatAdapter.addMessage(ChatMessage("AI로부터 빈 응답을 받았습니다.", true))
                    }
                }
            } catch (e: Exception) {
                Timber.e("Error generating content from image: ${e.message}")
                withContext(Dispatchers.Main) {
                    chatAdapter.addMessage(ChatMessage("이미지 처리 중 오류: ${e.message}", true))
                }
            }
        }
    }

    private fun speakTTS(category: AIResponse) {
        val utteranceId = UUID.randomUUID().toString()
        when (category.response.category) {
            "DANGER" -> {
                tts.setSpeechRate(2.0f)
                tts.speak(
                    category.response.description,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    utteranceId
                )
            }

            "INFO" -> {
                tts.setSpeechRate(2.0f)
                tts.speak(
                    category.response.description,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    utteranceId
                )
            }

            "GUIDE" -> {
                tts.setSpeechRate(1.0f)
                tts.speak(
                    category.response.description,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    utteranceId
                )
            }

            else -> {
                tts.setSpeechRate(1.0f)
                tts.speak(
                    category.response.description,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    utteranceId
                )
            }
        }

    }

    private var startAnimator: ValueAnimator? = null;
    private fun startVoice() {
        val startScale = 0.1f
        // 종료 스케일 (1f는 원래 크기)
        val endScale = 1f
        // 애니메이션 지속 시간 (밀리초)
        val duration = 1000L
        startAnimator = ValueAnimator.ofFloat(startScale, endScale).apply {
            addUpdateListener { animator ->
                val scale = animator.animatedValue as Float
                binding.aniWave.scaleY = scale
            }
            interpolator = AccelerateDecelerateInterpolator()
            this.duration = duration
            // 애니메이션이 끝난 후 필요한 작업이 있다면 여기에 추가
            binding.aniWave.playAnimation() // Lottie 애니메이션 시작
            binding.aniWave.visibility = View.VISIBLE
            start()
        }

    }

    private var stopAnimator: ValueAnimator? = null;
    private fun stopVoice() {
        val startScale = 1f
        // 종료 스케일 (1f는 원래 크기)
        val endScale = 0.1f
        // 애니메이션 지속 시간 (밀리초)
        val duration = 1000L

        stopAnimator = ValueAnimator.ofFloat(startScale, endScale).apply {
            addUpdateListener { animator ->
                val scale = animator.animatedValue as Float
                binding.aniWave.scaleY = scale
            }
            interpolator = AccelerateDecelerateInterpolator()
            this.duration = duration
            doOnEnd {
                binding.aniWave.cancelAnimation()
                binding.aniWave.visibility = View.GONE
            }
            start()
        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        tts.stop()
        tts.shutdown()
    }
}