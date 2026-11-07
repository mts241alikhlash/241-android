package id.sch.mts241alikhlash.exam.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.sch.mts241alikhlash.exam.ui.theme.DMSansFontFamily
import id.sch.mts241alikhlash.exam.ui.theme.WebTokens

private val HeaderFontSize = 14.sp
private val HeaderWeight = FontWeight.SemiBold
private val HeaderPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)

@Composable
fun CardHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(WebTokens.Foreground.copy(alpha = 0.03f))
                .padding(HeaderPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        HeaderTitle(title)
        trailing?.invoke()
    }
}

@Composable
fun HeaderTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        fontSize = HeaderFontSize,
        fontWeight = HeaderWeight,
        fontFamily = DMSansFontFamily,
        color = WebTokens.Foreground,
        modifier = modifier,
    )
}

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(WebTokens.Card, shape)
                .border(1.dp, WebTokens.Border, shape)
                .clip(shape),
    ) {
        CardHeader(title)
        Box(Modifier.fillMaxWidth().height(1.dp).background(WebTokens.Border))
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun SectionedCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(WebTokens.Card, shape)
                .border(1.dp, WebTokens.Border, shape)
                .clip(shape),
        content = content,
    )
}

@Composable
fun SectionHeader(
    title: String,
    divider: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
) {
    if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(WebTokens.Border))
    CardHeader(title = title, trailing = trailing)
    Box(Modifier.fillMaxWidth().height(1.dp).background(WebTokens.Border))
}
