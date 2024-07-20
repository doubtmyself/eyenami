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
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QueryFragment : Fragment(R.layout.fragment_query) {

    private val binding by viewBinding(FragmentQueryBinding::bind)
    private lateinit var chatAdapter: ChatAdapter
    private val model = GenerativeModel(
        "gemini-1.5-pro",
        // Retrieve API key as an environmental variable defined in a Build Configuration
        // see https://github.com/google/secrets-gradle-plugin for further instructions
        BuildConfig.apiKey,
        generationConfig = generationConfig {
            temperature = 1f
            topK = 64
            topP = 0.95f
            maxOutputTokens = 8192
            responseMimeType = "application/json"
        },
        safetySettings = listOf(
            SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.MEDIUM_AND_ABOVE)
        )
    )
    val chatHistory = listOf<Content>()
    val chat = model.startChat(chatHistory)

    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService

    val initialPrompt = """
    당신은 시각장애인을 위한 AI 도우미입니다. 모든 응답은 반드시 다음 JSON 형식을 정확히 따라야 합니다:
    {"response": {"category": "DANGER" | "INFO" | "GUIDE", "description": "간단한 설명"}}
    
    카테고리는 다음과 같이 사용하세요:
    1. DANGER: 즉각적인 위험 상황 (예: 장애물, 계단, 차량 등)
    2. INFO: 일반적인 환경 정보 (예: 방의 구조, 주변 물체 등)
    3. GUIDE: 방향 안내 (예: 문의 위치, 안전한 경로 등)
    
    설명은 항상 간결하고 명확해야 하며, 50자 이내로 제한하세요.
    
    다음은 올바른 응답의 예시입니다:
    
    1. 이미지: 바닥에 물웅덩이가 있는 복도
       응답: {"response": {"category": "DANGER", "description": "바닥에 물웅덩이, 미끄러질 위험"}}
    
    2. 이미지: 책상 위에 컴퓨터와 책들이 있는 사무실
       응답: {"response": {"category": "INFO", "description": "사무실 환경, 책상에 컴퓨터와 책이 있음"}}
    
    3. 이미지: 왼쪽에 출구 표지판이 보이는 복도
       응답: {"response": {"category": "GUIDE", "description": "왼쪽으로 10미터 지점에 출구 있음"}}
    
    항상 이 형식을 따라 응답하세요. 추가 정보나 설명이 필요하면 description 내에서 간결하게 제공하세요.
    """

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (allPermissionsGranted()) {
            initialize()
            addListener()
            setupCamera()
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
        lifecycleScope.launch {
            try {
                val response = chat.sendMessage(initialPrompt)
                Timber.d("Initial prompt response: ${response.text}")
            } catch (e: Exception) {
                Timber.e("Error setting initial prompt", e)
            }
        }

    }

    private fun addListener() {
        binding.BtnQuery.setOnClickListener {
            takePhoto()
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
        val photoFile = File(requireContext().externalMediaDirs.firstOrNull(), "${System.currentTimeMillis()}.jpg")
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
        lifecycleScope.launch {
            try {
                Timber.d("Processing captured image...")
                val response = model.generateContent(
                    content {
                        image(bitmap)
                        text("이 이미지를 분석하고, 시각장애인에게 가장 중요한 정보를 제공하세요. 반드시 이전에 제공된 JSON 형식과 예시를 따라 응답하세요.")
                    }
                )

                response.text?.let { responseText ->
                    Timber.d("AI response: $responseText")
                    try {
                        val jsonResponse = JSONObject(responseText)
                        val responseObject = jsonResponse.optJSONObject("response")
                        if (responseObject != null) {
                            val category = responseObject.optString("category", "INFO")
                            val description = responseObject.optString("description", "정보를 제공할 수 없습니다.")
                            chatAdapter.addMessage(ChatMessage("$category: $description", true))
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