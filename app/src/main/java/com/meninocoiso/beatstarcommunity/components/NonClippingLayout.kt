import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun NonClippingLayout(
	modifier: Modifier? = Modifier,
	content: @Composable () -> Unit
) {
	val screenHeight = LocalConfiguration.current.screenHeightDp

	Layout(
		modifier = (modifier ?: Modifier),
		content = content
	) { measurables, constraints ->
		val placeable = measurables[0].measure(
			constraints.copy(
				maxWidth = constraints.maxWidth,
				// TODO: Temporary workaround since using infinity values doesn't work (e.g. Constraints.Infinity and Int.MAX_VALUE)
				maxHeight = screenHeight * 3
			)
		)
		layout(constraints.maxWidth, constraints.maxHeight) {
			placeable.placeRelative(0, 0)
		}
	}
}

/*
	.layout { measurable, constraints ->
		// Measure the composable
		val placeable = measurable.measure(
			constraints.copy(
				maxWidth = constraints.maxWidth,
				maxHeight = teste
			)
		)

		// Define the width and height of the layout
		val width = constraints.maxWidth
		val height = constraints.maxHeight

		// Define the layout
		layout(width, height) {
			// Place the composable at (0, 0) coordinates
			placeable.placeRelative(0, 0)
		}
	}
*/