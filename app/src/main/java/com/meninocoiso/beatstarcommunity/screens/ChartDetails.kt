package com.meninocoiso.beatstarcommunity.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.components.Carousel
import kotlinx.serialization.Serializable

@Serializable
object ChartDetailsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartDetails(
	onReturn: () -> Unit
) {
	val imageUrls = listOf(
		"https://th.bing.com/th/id/OIP.Fnrr1lh0QpG1bhKXSptqzwAAAA?rs=1&pid=ImgDetMain",
		"https://images.pexels.com/photos/19780240/pexels-photo-19780240/free-photo-of-a-forest-with-trees-and-fog-in-the-background.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=1",
	)

	Scaffold(topBar = {
		TopAppBar(
			modifier = Modifier.padding(horizontal = 16.dp),
			navigationIcon = {
				Icon(
					modifier = Modifier
						.padding(end = 12.dp)
						.clickable { onReturn() },
					imageVector = Icons.AutoMirrored.Filled.ArrowBack,
					tint = MaterialTheme.colorScheme.onSurface,
					contentDescription = "Return"
				)
			},
			actions = {
				Icon(
					imageVector = Icons.Outlined.FavoriteBorder,
					contentDescription = "Like content"
				)
			},
			title = {
				Column {
					Text("We Live Forever", style = MaterialTheme.typography.titleLarge)
					Text("The Prodigy", style = MaterialTheme.typography.titleMedium)
				}
			}
		)
	}) { innerPadding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(innerPadding),
			verticalArrangement = Arrangement.Top,
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Carousel(imageUrls)
		}
	}
}