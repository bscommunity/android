package com.meninocoiso.bscm.data.local.dao

import androidx.room.ColumnInfo

/**
 * Row returned by [CollectionDao.getItemCounts]: item counts per collection,
 * computed from the locally synced cross-ref table.
 */
data class CollectionItemCounts(
    @ColumnInfo(name = "collectionId") val collectionId: String,
    @ColumnInfo(name = "chartCount") val chartCount: Int,
    @ColumnInfo(name = "tourPassCount") val tourPassCount: Int,
    @ColumnInfo(name = "themeCount") val themeCount: Int,
)