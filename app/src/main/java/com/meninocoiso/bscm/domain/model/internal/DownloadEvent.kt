import com.meninocoiso.bscm.domain.enums.ErrorType

sealed class DownloadEvent {
    abstract val chartId: String

    data class Started(override val chartId: String) : DownloadEvent()
    data class Progress(override val chartId: String, val progress: Float) : DownloadEvent()
    data class Extracting(override val chartId: String, val progress: Float) : DownloadEvent()
    data class Complete(override val chartId: String) : DownloadEvent()
    data class Error(
        override val chartId: String,
        val message: String,
        val type: ErrorType? = null
    ) : DownloadEvent()
    data class Cancelled(override val chartId: String) : DownloadEvent()
}