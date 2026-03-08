package es.unadekalamares.offtime.ui.model

enum class RunningTimer {
    None, TopTimer, BottomTimer;

    fun isRunning(): Boolean = this != None
    fun isTopTimer(): Boolean = this == TopTimer
    fun isBottomTimer(): Boolean = this == BottomTimer
}