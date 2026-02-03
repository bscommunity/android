package com.meninocoiso.bscm.presentation.viewmodel

class PaginationState(
    val pageSize: Int = 20
) {
    var currentPage: Int = 0
    var isLoadingMore: Boolean = false
    var hasMore: Boolean = true

    fun reset() {
        currentPage = 0
        isLoadingMore = false
        hasMore = true
    }

    fun nextOffset(): Int = currentPage * pageSize

    fun advancePage() {
        currentPage += 1
    }
}
