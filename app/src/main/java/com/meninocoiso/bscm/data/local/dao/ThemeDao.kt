package com.meninocoiso.bscm.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.meninocoiso.bscm.domain.model.Theme
import kotlinx.coroutines.flow.Flow

@Dao
interface ThemeDao {
    @Query("""
        SELECT * FROM themes 
        WHERE (:query IS NULL OR 
               name LIKE '%' || :query || '%' OR 
               replaces LIKE '%' || :query || '%') 
        ORDER BY updated_at DESC 
        LIMIT CASE WHEN :limit IS NULL THEN -1 ELSE :limit END OFFSET :offset
    """)
    fun getThemes(query: String? = null, limit: Int? = null, offset: Int = 0): List<Theme>

    @Query("SELECT * FROM themes WHERE id = :id")
    fun getTheme(id: String): Theme?

    @Query("SELECT * FROM themes WHERE id IN (:ids)")
    fun getThemesByIds(ids: List<String>): List<Theme>

    @Query("""
        SELECT * FROM themes 
        WHERE liked_at IS NOT NULL 
        ORDER BY liked_at DESC
    """)
    fun observeLikedThemes(): Flow<List<Theme>>

    @Query("""
        SELECT t.* FROM themes t
        INNER JOIN collection_item_cross_ref ref 
            ON t.id = ref.content_id
        WHERE ref.collection_id = 'bookmarks'
        AND ref.content_type = 'THEME'
        ORDER BY ref.added_at DESC
    """)
    fun observeBookmarkedThemes(): Flow<List<Theme>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(themes: List<Theme>)

    @Upsert
    fun update(theme: Theme)

    @Upsert
    fun update(themes: List<Theme>)

    @Query("UPDATE themes SET liked_at = :likedAt WHERE id = :themeId")
    suspend fun updateLikedAt(themeId: String, likedAt: String?)

    @Query("UPDATE themes SET bookmarked_at = :bookmarkedAt WHERE id = :themeId")
    suspend fun updateBookmarkedAt(themeId: String, bookmarkedAt: String?)

    @Delete
    fun delete(theme: Theme)

    @Delete
    fun delete(themes: List<Theme>)
}
