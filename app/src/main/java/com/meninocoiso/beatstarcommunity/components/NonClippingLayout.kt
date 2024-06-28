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
		layout(constraints.maxWidth, 0) {
			placeable.placeRelative(0, 0)
		}
	}
}