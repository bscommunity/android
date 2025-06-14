package com.meninocoiso.bscm.domain.lists

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.enums.Role

data class RoleList(
	val id: Role,
	val name: String
)

@Composable
fun getRolesList(): List<RoleList> {
	return listOf(
		RoleList(Role.AUTHOR, stringResource(R.string.author_role)),
		RoleList(Role.CHART, stringResource(R.string.chart_role)),
		RoleList(Role.AUDIO, stringResource(R.string.audio_role)),
		RoleList(Role.REVISION, stringResource(R.string.revision_role)),
		RoleList(Role.EFFECTS, stringResource(R.string.effects_role)),
		RoleList(Role.SYNC, stringResource(R.string.sync_role)),
		RoleList(Role.GAMEPLAY, stringResource(R.string.gameplay_role)),
	)
}