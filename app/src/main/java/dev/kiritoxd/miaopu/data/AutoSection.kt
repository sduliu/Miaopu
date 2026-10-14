package dev.kiritoxd.miaopu.data

data class AutoTopic(
    val topicId: Int,
    val name: String,
)

object AutoTopics {
    val CAR = AutoTopic(topicId = 278, name = "汽车区")
    val ALL = listOf(CAR)
}

data class AutoThread(
    val tid: String,
    val title: String,
    val username: String,
    val replies: Int,
    val recommends: Int,
    val time: String,
    val isVideo: Boolean,
)

data class AutoThreadPage(
    val threads: List<AutoThread>,
    val cursor: String,
    val hasMore: Boolean,
)
