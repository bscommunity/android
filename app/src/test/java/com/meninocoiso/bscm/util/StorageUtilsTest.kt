package com.meninocoiso.bscm.util

import org.junit.Assert.assertEquals
import org.junit.Test

class StorageUtilsTest {

    @Test
    fun `getChartFolderName prefers contentId when available`() {
        val result = StorageUtils.getChartFolderName(
            chartId = "7d7dd9d0-8f7b-4cc1-9dd2-raw-id",
            contentId = "shape-of-you"
        )

        assertEquals("bscm_shape-of-you", result)
    }

    @Test
    fun `getChartFolderName falls back to chart id when contentId is missing`() {
        val result = StorageUtils.getChartFolderName(
            chartId = "7d7dd9d0-8f7b-4cc1-9dd2-raw-id",
            contentId = null
        )

        assertEquals("bscm_7d7dd9d0-8f7b-4cc1-9dd2-raw-id", result)
    }

    @Test
    fun `getChartFolderName falls back to chart id when contentId is blank`() {
        val result = StorageUtils.getChartFolderName(
            chartId = "7d7dd9d0-8f7b-4cc1-9dd2-raw-id",
            contentId = "   "
        )

        assertEquals("bscm_7d7dd9d0-8f7b-4cc1-9dd2-raw-id", result)
    }
}
