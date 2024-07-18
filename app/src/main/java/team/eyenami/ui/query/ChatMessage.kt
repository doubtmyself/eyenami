package team.eyenami.ui.query

data class ChatMessage(
    val message: String,
    /**
     * true면 사용자 메시지, false면 AI 응답
     */
    val isUser: Boolean
)