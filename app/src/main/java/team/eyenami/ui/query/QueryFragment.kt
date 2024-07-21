package team.eyenami.ui.query

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import kotlinx.coroutines.launch
import team.eyenami.BuildConfig
import team.eyenami.R
import team.eyenami.databinding.FragmentQueryBinding
import timber.log.Timber
import com.google.ai.client.generativeai.type.generationConfig
import org.json.JSONObject

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.speech.tts.TextToSpeech
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import team.eyenami.obj.SettingManager
import team.eyenami.utills.Util
import java.io.File
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QueryFragment : Fragment(R.layout.fragment_query) {

    private val binding by viewBinding(FragmentQueryBinding::bind)
    private lateinit var chatAdapter: ChatAdapter
    private val model = GenerativeModel(
        "gemini-1.5-flash",
        // Retrieve API key as an environmental variable defined in a Build Configuration
        // see https://github.com/google/secrets-gradle-plugin for further instructions
        BuildConfig.apiKey,
//        generationConfig = generationConfig {
//            temperature = 1f
//            topK = 64
//            topP = 0.95f
//            maxOutputTokens = 8192
//            responseMimeType = "application/json"
//        },
        generationConfig = generationConfig {
            temperature = 0.7f  // 낮은 temperature 값
            topK = 32  // 낮은 topK 값
            topP = 0.9f  // 낮은 topP 값
            maxOutputTokens = 512  // 최대 출력 토큰 수 줄임
            responseMimeType = "application/json"
        },
        safetySettings = listOf(
            SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.MEDIUM_AND_ABOVE)
        )
    )
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService

    private val initializationPrompt = """
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
Analyze image and respond per rules.
Confirm: "Rules accepted."
""".trimIndent()
    private lateinit var wkJob: Job

    private lateinit var tts: TextToSpeech

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (allPermissionsGranted()) {

            initialize()
            addListener()
            setupCamera()
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
        } else {
            requestPermissions()
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
    }

    private fun initialize() {
        chatAdapter = ChatAdapter()
        binding.RecyclerViewChat.adapter = chatAdapter
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun addListener() {

        binding.btnQuery.setOnClickListener {
            takePhoto()
        }

        binding.btnQueryContinue.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                wkJob = lifecycleScope.launch {
                    while (isActive) {
                        launch {
                            takePhoto()
                        }

                        delay(SettingManager.getDetectionCountMS()) // 작업을 반복할 간격을 설정 (예: 1초)
                    }
                }
            } else {
                if (::wkJob.isInitialized && wkJob.isActive) {
                    wkJob.cancel()
                }
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
            "${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                Timber.e("Photo capture failed: ${exc.message}", exc)
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                processCapturedImage(bitmap)
            }
        })
    }

    private fun processCapturedImage(bitmap: Bitmap) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Timber.d("Processing captured image...")
                val response = model.generateContent(
                    content {
                        text(initializationPrompt)
                        image(bitmap)
                        text("Analyze this image based on the rules provided above.")
                    }
                )
                withContext(Dispatchers.Main){
                    response.text?.let { responseText ->
                        Timber.d("AI response: $responseText")
                        try {
                            val jsonResponse = JSONObject(responseText)
                            val responseObject = jsonResponse.optJSONObject("response")
                            if (responseObject != null) {
                                val category = responseObject.optString("category", "INFO")
                                val description = responseObject.optString("description", "정보를 제공할 수 없습니다.")
                                chatAdapter.addMessage(ChatMessage("$category: $description", true))
                                tts.speak("$category: $description", TextToSpeech.QUEUE_FLUSH, null, null)
                            } else {
                                // JSON 형식이 맞지 않을 경우 전체 응답을 표시
                                chatAdapter.addMessage(ChatMessage("INFO: $responseText", true))
                            }
                        } catch (e: Exception) {
                            Timber.e("Error parsing JSON response: ${e.message}")
                            chatAdapter.addMessage(ChatMessage("INFO: $responseText", true))
                        }
                    } ?: run {
                        Timber.e("Empty response from AI")
                        chatAdapter.addMessage(ChatMessage("AI로부터 빈 응답을 받았습니다.", true))
                    }
                }
            } catch (e: Exception) {
                Timber.e("Error generating content from image: ${e.message}")
                chatAdapter.addMessage(ChatMessage("이미지 처리 중 오류: ${e.message}", true))
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }


}