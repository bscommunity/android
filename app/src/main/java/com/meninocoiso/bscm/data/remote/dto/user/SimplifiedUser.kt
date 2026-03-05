package com.meninocoiso.bscm.data.remote.dto.user

import android.os.Parcelable
import androidx.room.Entity
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Entity(tableName = "users")
@Serializable
@Parcelize
data class SimplifiedUser(
    val id: String,
    val username: String,
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    val accentColor: Long? = null,
    val isVerified: Boolean? = false
) : Parcelable