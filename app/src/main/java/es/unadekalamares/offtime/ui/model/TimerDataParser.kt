package es.unadekalamares.offtime.ui.model

object TimerDataParser {

    fun toReadableTime(time: Long): String {
        val seconds = getSeconds(time)
        val minutes = getMinutes(time)
        val hours = getHours(time)
        val roundedSeconds = seconds % 60
        val roundedMinutes = minutes % 60
        val formattedTime = String.format(
            "%02d:%02d:%02d",
            hours,
            roundedMinutes,
            roundedSeconds
        )
        return formattedTime
    }

    fun getSeconds(time: Long): Long = time / 1000
    private fun getMinutes(time: Long): Long = getSeconds(time) / 60
    private fun getHours(time: Long): Long = getMinutes(time) / 60

}