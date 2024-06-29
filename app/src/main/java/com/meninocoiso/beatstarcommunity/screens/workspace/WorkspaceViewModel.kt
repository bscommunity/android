package com.meninocoiso.beatstarcommunity.screens.workspace

import androidx.lifecycle.ViewModel
import com.meninocoiso.beatstarcommunity.data.WorkspaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WorkspaceViewModel @Inject constructor(
	repository: WorkspaceRepository
) : ViewModel() {

}