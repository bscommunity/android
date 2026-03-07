package com.meninocoiso.bscm.data.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChartStorageScannerTest {

    @Test
    fun `normalizeIdentifier keeps non blank values`() {
        val result = ChartStorageScanner.normalizeIdentifier("content-123")

        assertEquals("content-123", result)
    }

    @Test
    fun `normalizeIdentifier returns null for blank values`() {
        val result = ChartStorageScanner.normalizeIdentifier("   ")

        assertNull(result)
    }

    @Test
    fun `normalizeIdentifier returns null for null values`() {
        val result = ChartStorageScanner.normalizeIdentifier(null)

        assertNull(result)
    }
}
