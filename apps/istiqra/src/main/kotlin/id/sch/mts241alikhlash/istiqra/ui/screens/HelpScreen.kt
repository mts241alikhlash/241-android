package id.sch.mts241alikhlash.istiqra.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.sch.mts241alikhlash.istiqra.ui.components.SectionCard
import id.sch.mts241alikhlash.istiqra.ui.theme.DMSansFontFamily
import id.sch.mts241alikhlash.istiqra.ui.theme.WebTokens

@Composable
fun HelpScreen(topics: List<HelpTopic>) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        topics.forEach { topic ->
            SectionCard(title = topic.title) {
                topic.points.forEachIndexed { index, point ->
                    if (index > 0) Spacer(Modifier.padding(top = 10.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "${index + 1}.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = DMSansFontFamily,
                            color = WebTokens.Primary,
                            modifier = Modifier.width(22.dp),
                        )
                        Text(
                            text = point,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            fontFamily = DMSansFontFamily,
                            color = WebTokens.Foreground,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.padding(bottom = 8.dp))
    }
}
