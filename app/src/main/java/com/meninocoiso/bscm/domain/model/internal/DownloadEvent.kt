import com.meninocoiso.bscm.domain.enums.ErrorType

sealed class DownloadEvent {
    abstract val id: String

    data class Started(override val id: String) : DownloadEvent()
    data class Progress(override val id: String, val progress: Float) : DownloadEvent()
    data class Extracting(override val id: String, val progress: Float) : DownloadEvent()
    data class Complete(override val id: String) : DownloadEvent()
    data class Error(
        override val id: String,
        val message: String,
        val type: ErrorType? = null
    ) : DownloadEvent()
}