package com.meninocoiso.bscm.util

import org.junit.Assert.assertEquals
import org.junit.Test

class StorageUtilsTest {

    @Test
    fun `getChartFolderName uses the chart id`() {
        val result = StorageUtils.getChartFolderName("shape-of-you")

        assertEquals("bscm_shape-of-you", result)
    }

    @Test
    fun `getChartFolderName keeps raw ids as is`() {
        val result = StorageUtils.getChartFolderName("7d7dd9d0-8f7b-4cc1-9dd2-raw-id")

        assertEquals("bscm_7d7dd9d0-8f7b-4cc1-9dd2-raw-id", result)
    }
}
