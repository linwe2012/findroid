package dev.jdtech.jellyfin.settings.domain

/** Playback speeds accepted by both supported internal players. */
object PlaybackSpeeds {
    const val MIN = 0.25f
    const val MAX = 4f
    const val DEFAULT = "0.5,0.75,1,1.25,1.5,1.75,2"

    fun parse(value: String): List<Float>? {
        val speeds =
            value.trim().split(Regex("[,\\s]+")).map { token ->
                token.toFloatOrNull() ?: return null
            }
        if (speeds.isEmpty() || speeds.size > 20) return null
        if (speeds.any { !it.isFinite() || it !in MIN..MAX }) return null
        if (speeds.distinct().size != speeds.size) return null
        return speeds
    }

    fun format(speed: Float): String = speed.toString().removeSuffix(".0")
}
