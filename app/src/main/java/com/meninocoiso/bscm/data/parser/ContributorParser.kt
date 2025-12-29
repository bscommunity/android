package com.meninocoiso.bscm.data.parser

import android.util.Log
import com.meninocoiso.bscm.data.remote.dto.ContributorUserDto
import com.meninocoiso.bscm.domain.enums.Role
import com.meninocoiso.bscm.domain.model.Contributor
import java.time.LocalDateTime
import javax.inject.Inject

private const val TAG = "ContributorParser"

/**
 * Service for parsing contributors from serialized format
 * Format: "userId|username|imageUrl|role1,role2||userId|username|imageUrl|role1,role2||..."
 * Role IDs: 0=AUTHOR, 1=CHART, 2=AUDIO, 3=REVISION, 4=EFFECTS, 5=SYNC, 6=GAMEPLAY
 */
class ContributorParser @Inject constructor() {

    fun parseContributors(contributors: String?, chartId: String): List<Contributor> {
        if (contributors.isNullOrBlank()) return emptyList()

        return try {
            contributors.split("||")
                .filter { it.isNotBlank() }
                .mapNotNull { item ->
                    val parts = item.split("|", limit = 3)
                    if (parts.size >= 2) {
                        val username = parts[0]
                        val imageUrl = parts[1].takeIf { it.isNotBlank() }
                        val rolesStr = parts.getOrNull(2)

                        val roles = rolesStr?.split(",")
                            ?.mapNotNull { roleId ->
                                mapRole(roleId.toIntOrNull())
                            } ?: emptyList()

                        if (username.isNotBlank()) {
                            Contributor(
                                user = ContributorUserDto(
                                    id = username,
                                    username = username,
                                    imageUrl = imageUrl,
                                    createdAt = null
                                ),
                                chartId = chartId,
                                roles = roles,
                                joinedAt = LocalDateTime.now()
                            )
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing contributors: $contributors", e)
            emptyList()
        }
    }

    private fun mapRole(roleId: Int?): Role? {
        return when (roleId) {
            0 -> Role.AUTHOR
            1 -> Role.CHART
            2 -> Role.AUDIO
            3 -> Role.REVISION
            4 -> Role.EFFECTS
            5 -> Role.SYNC
            6 -> Role.GAMEPLAY
            else -> null
        }
    }
}

