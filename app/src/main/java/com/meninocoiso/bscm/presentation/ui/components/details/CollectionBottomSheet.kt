package com.meninocoiso.bscm.presentation.ui.components.details

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.SecureFlagPolicy
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.model.Collection
import com.meninocoiso.bscm.domain.model.SimplifiedCollection
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageSize
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI
import com.meninocoiso.bscm.presentation.ui.components.SwitchUI
import com.meninocoiso.bscm.presentation.ui.components.layout.CoverArt
import kotlinx.coroutines.launch

private const val MAX_NAME_LENGTH = 30

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionCreateBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onClose: () -> Unit,
    collections: List<Collection>,
    isLoading: Boolean = false,
    isMutating: Boolean = false,
    errorMessage: String? = null,
    onCollectionSelected: (collectionId: String, collectionName: String) -> Unit,
    onCreateCollection: suspend (name: String, isPublic: Boolean) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val horizontalPagerState = rememberPagerState { 2 }

    val currentTitle = when (horizontalPagerState.currentPage) {
        0 -> stringResource(R.string.collection_add)
        1 -> stringResource(R.string.collection_create)
        else -> ""
    }

    println("Is Mutating: $isMutating, Is Loading: $isLoading")

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = { onDismissRequest() },
        properties = ModalBottomSheetProperties(
            securePolicy = SecureFlagPolicy.SecureOn,
            shouldDismissOnBackPress = !isMutating,
            shouldDismissOnClickOutside = !isMutating,
        ),
        sheetGesturesEnabled = !isMutating
    ) {
        BackHandler(enabled = isMutating) { /* block back press */ }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (horizontalPagerState.currentPage > 0) {
                IconButton(
                    enabled = !isMutating,
                    onClick = {
                        coroutineScope.launch {
                            horizontalPagerState.scrollToPage(
                                horizontalPagerState.currentPage - 1
                            )
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
            Text(
                text = currentTitle,
                style = MaterialTheme.typography.titleMedium
            )
            if (horizontalPagerState.currentPage == 0 && !isLoading) {
                IconButton(enabled = !isMutating, onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close_bottomsheet)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            HorizontalPager(
                state = horizontalPagerState,
                key = { it }, // Recompose the pager when the page changes
                beyondViewportPageCount = 0,
                userScrollEnabled = false,
                modifier = Modifier
                    .wrapContentHeight(),
                verticalAlignment = Alignment.Top
            ) { index ->
                when (index) {
                    0 -> CollectionsListSection(
                        collections = collections,
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        onCollectionClick = onCollectionSelected,
                        onCreateNewCollectionClick = {
                            coroutineScope.launch {
                                horizontalPagerState.scrollToPage(
                                    1
                                )
                            }
                        }
                    )

                    1 -> CollectionFormSection(
                        isLoading = isMutating,
                        onSave = { name, isPublic ->
                            onCreateCollection(name, isPublic)
                            coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    onDismissRequest()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionEditBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onClose: () -> Unit,
    collection: SimplifiedCollection,
    isLoading: Boolean = false,
    onSaveChanges: suspend (name: String, isPublic: Boolean) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = { onDismissRequest() },
        properties = ModalBottomSheetProperties(
            securePolicy = SecureFlagPolicy.SecureOn,
            shouldDismissOnBackPress = !isLoading,
            shouldDismissOnClickOutside = !isLoading,
        ),
        sheetGesturesEnabled = !isLoading
    ) {
        BackHandler(enabled = isLoading) { /* block back press */ }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.size(48.dp))
            Text(
                text = stringResource(R.string.collection_edit),
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(enabled = !isLoading, onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close_bottomsheet)
                )
            }
        }
        CollectionFormSection(
            isLoading = isLoading,
            initialName = collection.name,
            initialIsPublic = collection.isPublic,
            onSave = { name, isPublic ->
                onSaveChanges(name, isPublic)
                coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        onDismissRequest()
                    }
                }
            }
        )
    }
}

@Composable
fun CollectionsListSection(
    modifier: Modifier = Modifier,
    collections: List<Collection>,
    isLoading: Boolean,
    errorMessage: String? = null,
    onCollectionClick: (String, String) -> Unit,
    onCreateNewCollectionClick: () -> Unit
) {
    LazyColumn(modifier = modifier) {
        item {
            val interactionSource = remember { MutableInteractionSource() }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = onCreateNewCollectionClick,
                        interactionSource = interactionSource,
                        indication = ripple()
                    )
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_add_2_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(stringResource(R.string.create_new_collection), style = MaterialTheme.typography.titleMedium)
            }
        }
        if (errorMessage != null) {
            item {
                StatusMessageUI(
                    modifier = Modifier.padding(vertical = 32.dp),
                    icon = R.drawable.rounded_error_24,
                    title = stringResource(R.string.error),
                    message = errorMessage,
                    size = StatusMessageSize.Small
                )
            }
        } else if (isLoading && collections.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        } else if (collections.isEmpty()) {
            item {
                StatusMessageUI(
                    modifier = Modifier.padding(vertical = 32.dp),
                    icon = R.drawable.outline_deployed_code_24,
                    title = stringResource(R.string.no_collections_yet),
                    message = stringResource(R.string.collections_will_appear),
                    size = StatusMessageSize.Small
                )
            }
        } else {
            items(collections.size) { index ->
                val collection = collections[index]

                CollectionItem(
                    name = collection.name,
                    coverUrl = collection.coverUrl ?: "",
                    isPublic = collection.isPublic,
                    contentCounts = Triple(0, 0, 0),
                    onClick = { onCollectionClick(collection.id, collection.name) }
                )
            }
        }
    }
}

@Composable
fun CollectionFormSection(
    isLoading: Boolean,
    initialName: String = "",
    initialIsPublic: Boolean = true,
    onSave: suspend (name: String, isPublic: Boolean) -> Unit
) {
    var isPublic by rememberSaveable { mutableStateOf(initialIsPublic) }
    var name by rememberSaveable { mutableStateOf(initialName) }

    val scope = rememberCoroutineScope()

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
    ) {
                OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            value = name,
            onValueChange = { if (it.length <= MAX_NAME_LENGTH) name = it },
            enabled = !isLoading,
            label = { Text(stringResource(R.string.collection_name_label)) },
            singleLine = true,
            supportingText = { Text("${name.length}/${MAX_NAME_LENGTH}") },
            placeholder = { Text(stringResource(R.string.enter_collection_name)) }
        )
        ListItem(
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            ),
            headlineContent = { Text(stringResource(R.string.make_public)) },
            supportingContent = { Text(stringResource(R.string.make_public_description)) },
            trailingContent = {
                SwitchUI(
                    checked = isPublic,
                    onCheckedChange = { isPublic = !isPublic },
                    enabled = !isLoading
                )
            },
        )
        Button(
            enabled = !isLoading && name.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            onClick = {
                val trimmedName = name.trim()
                if (trimmedName.isNotEmpty()) {
                    scope.launch {
                        onSave(trimmedName, isPublic)
                    }
                }
            })
        {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = ButtonDefaults.buttonColors().disabledContentColor,
                )
            } else {
                Text(stringResource(R.string.save))
            }
        }
    }
}

@Composable
fun CollectionItem(
    name: String,
    coverUrl: String,
    isPublic: Boolean = false,
    contentCounts: Triple<Int, Int, Int>,
    onClick: () -> Unit = { }
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = ripple()
            )
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverArt(url = coverUrl, size = 56.dp, borderRadius = 8.dp)
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                if (!isPublic) {
                    Icon(
                        modifier = Modifier.size(14.dp),
                        painter = painterResource(R.drawable.rounded_lock_24),
                        contentDescription = null
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.charts_count, contentCounts.first),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = stringResource(R.string.tourpasses_count, contentCounts.second),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = stringResource(R.string.themes_count, contentCounts.third),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}