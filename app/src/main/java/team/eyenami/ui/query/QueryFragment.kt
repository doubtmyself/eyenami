package team.eyenami.ui.query

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.launch
import team.eyenami.BuildConfig
import team.eyenami.R
import team.eyenami.databinding.FragmentQueryBinding
import timber.log.Timber

class QueryFragment : Fragment(R.layout.fragment_query) {
    private val binding by viewBinding(FragmentQueryBinding::bind)

    private lateinit var chatAdapter: ChatAdapter
    private val generativeModel = GenerativeModel(
//        modelName = "gemini-pro-vision",  // TODO 이미지 전송 샘플
        modelName = "gemini-pro",  // 텍스트 전용 모델로 변경sdfsdfd
        apiKey = BuildConfig.apiKey
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initialize()
        addListener()
    }

    private fun initialize() {
        chatAdapter = ChatAdapter()
        binding.RecyclerViewChat.adapter = chatAdapter
    }

    private fun addListener() {
        binding.BtnQuery.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val response = generativeModel.generateContent(
                        content {
                            text(binding.ETxtMessage.text.toString())
                        }
//                        content {      TODO 이미지 전송 샘플
//                            image(bitmap)
//                            text(prompt)
//                        }
                    )

                    // 응답 처리
                    response.text?.let { responseText ->

                        chatAdapter.addMessage(ChatMessage(responseText, true))
//                        binding.TxtResponse.text = responseText
                    }
                } catch (e: Exception) {
                    // 오류 처리
                    Timber.e("Error generating content", e)
//                    binding.RecyclerViewChat.text = "오류가 발생했습니다: ${e.message}"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }


}