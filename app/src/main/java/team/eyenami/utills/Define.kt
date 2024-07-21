package team.eyenami.utills

enum class SettingProp {
    VIBRATERUN
}




data class AIResponse(
    val response: Response
)

data class Response(
    val category: String,
    val description: String
)
