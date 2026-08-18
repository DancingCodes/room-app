package love.moonc.room.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

object RoomSpacing {
    val ScreenHorizontal = 16.dp
    val ScreenTop = 24.dp
    val ScreenBottom = 16.dp
    val ScreenPadding = PaddingValues(
        start = ScreenHorizontal,
        top = ScreenTop,
        end = ScreenHorizontal,
        bottom = ScreenBottom,
    )
    val ItemGap = 12.dp
    val CompactGap = 8.dp
    val SectionGap = 20.dp
    val LoginLift = 48.dp
    val CardPadding = 16.dp
    val ProfileCardPadding = 20.dp
    val FieldMinHeight = 56.dp
    val AvatarSize = 88.dp
    val ContentMaxWidth = 420.dp
    val SwipeLeaveThreshold = 96.dp
}

@Composable
fun ScreenColumn(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(RoomSpacing.ItemGap),
    contentPadding: PaddingValues = RoomSpacing.ScreenPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
    ) {
        content()
    }
}

@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
    )
}

@Composable
fun PrimaryButton(
    text: String,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().heightIn(min = RoomSpacing.FieldMinHeight),
    ) {
        if (loading) {
            CircularProgressIndicator()
        } else {
            Text(text)
        }
    }
}

@Composable
fun FormColumn(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    ScreenColumn(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}

@Composable
fun CenteredFormColumn(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = RoomSpacing.ScreenPadding,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = verticalArrangement,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().widthIn(max = RoomSpacing.ContentMaxWidth),
            verticalArrangement = Arrangement.spacedBy(RoomSpacing.ItemGap),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}