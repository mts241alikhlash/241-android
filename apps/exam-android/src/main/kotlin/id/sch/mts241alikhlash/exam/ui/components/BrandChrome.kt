package id.sch.mts241alikhlash.exam.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.sch.mts241alikhlash.exam.R
import id.sch.mts241alikhlash.exam.ui.theme.DMSansFontFamily
import id.sch.mts241alikhlash.exam.ui.theme.WebTokens

val BrandCardOverlap = 40.dp
val BrandEmblemSize = 72.dp
private val AppLogo = R.drawable.logo

@Composable
fun BrandHero(
    height: Dp,
    title: String,
    tagline: String,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(height),
        contentAlignment = Alignment.TopCenter,
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                WebTokens.Primary.copy(alpha = 0.78f),
                                WebTokens.Primary.copy(alpha = 0.94f),
                            ),
                        ),
                    ),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, WebTokens.Background)),
                    ),
        )
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(bottom = BrandCardOverlap + BrandEmblemSize / 2),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.padding(horizontal = 24.dp),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DMSansFontFamily,
                letterSpacing = (-0.8).sp,
                color = WebTokens.PrimaryForeground,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier =
                    Modifier
                        .width(32.dp)
                        .height(2.dp)
                        .background(WebTokens.BrandGold),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = tagline,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = DMSansFontFamily,
                letterSpacing = 1.6.sp,
                color = WebTokens.PrimaryForeground.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
fun BrandEmblem() {
    Box(
        modifier =
            Modifier
                .pullUp(BrandEmblemSize / 2)
                .size(BrandEmblemSize)
                .background(WebTokens.Card, CircleShape)
                .border(1.dp, WebTokens.Border, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = AppLogo),
            contentDescription = "Logo 241-exam",
            modifier = Modifier.height(44.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

fun Modifier.pullUp(amount: Dp): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val pull = amount.roundToPx().coerceAtMost(placeable.height)
        layout(placeable.width, placeable.height - pull) { placeable.place(0, -pull) }
    }

@Composable
fun BrandWatermark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = AppLogo),
        contentDescription = null,
        colorFilter = ColorFilter.tint(WebTokens.Foreground.copy(alpha = 0.045f), BlendMode.SrcIn),
        contentScale = ContentScale.FillWidth,
        alignment = Alignment.TopStart,
        modifier = modifier,
    )
}
