package com.mcis.memoir

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mcis.memoir.i18n.LocaleController
import com.mcis.memoir.ui.components.BottomNavigationBar
import com.mcis.memoir.ui.components.UntitledIcon
import com.mcis.memoir.ui.icons.*
import com.mcis.memoir.ui.route.JourneyRowState
import com.mcis.memoir.ui.route.RouteDetailIntent
import com.mcis.memoir.ui.route.RouteDetailState
import com.mcis.memoir.ui.route.RouteDetailViewModel
import com.mcis.memoir.ui.theme.AppTheme
import com.mcis.memoir.ui.theme.inter
import com.mcis.memoir.ui.theme.judson

@Composable
fun RouteDetailScreen(
    viewModel: RouteDetailViewModel,
    onBackClick: () -> Unit,
    onNavigateToSaved: () -> Unit,
    onNavigateToMemories: () -> Unit,
    onSpotClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var isNavigating by remember { mutableStateOf(false) }

    RouteDetailContent(
        state = state,
        onIntent = viewModel::onIntent,
        onBackClick = {
            if (!isNavigating) {
                isNavigating = true
                onBackClick()
            }
        },
        onNavigateToSaved = onNavigateToSaved,
        onNavigateToMemories = onNavigateToMemories,
        onSpotClick = onSpotClick,
        modifier = modifier
    )
}

@Composable
internal fun RouteDetailContent(
    state: RouteDetailState,
    onIntent: (RouteDetailIntent) -> Unit,
    onBackClick: () -> Unit,
    onNavigateToSaved: () -> Unit,
    onNavigateToMemories: () -> Unit,
    onSpotClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isChinese = LocaleController.currentLocale().language == "zh"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DesignTokens.colorLanguageSelectionBackground)
    ) {
        when {
            state.error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.route_not_found))
                }
            }
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DesignTokens.colorMaroon)
                }
            }
            else -> {
                RouteDetailLoadedContent(
                    state = state,
                    isChinese = isChinese,
                    onIntent = onIntent,
                    onNavigateToSaved = onNavigateToSaved,
                    onNavigateToMemories = onNavigateToMemories,
                    onSpotClick = onSpotClick
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(top = 57.dp, start = 18.dp)
                .size(24.dp)
                .clickable(onClick = onBackClick),
            contentAlignment = Alignment.Center
        ) {
            UntitledIcon(
                imageVector = UntitledIcons.BackIcon,
                contentDescription = stringResource(R.string.back_button),
                tint = Color.White,
                size = 24.dp
            )
        }
    }
}

@Composable
private fun RouteDetailLoadedContent(
    state: RouteDetailState,
    isChinese: Boolean,
    onIntent: (RouteDetailIntent) -> Unit,
    onNavigateToSaved: () -> Unit,
    onNavigateToMemories: () -> Unit,
    onSpotClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(259.dp)
                    .background(Color.LightGray)
            ) {
                if (state.heroDrawableRes != 0) {
                    Image(
                        painter = painterResource(state.heroDrawableRes),
                        contentDescription = state.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 29.dp)
                    .padding(top = 24.dp)
            ) {
                Text(
                    text = state.title,
                    style = TextStyle(
                        fontFamily = judson,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        lineHeight = 44.sp
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(70.dp)
                            .background(Color.Black)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = state.description,
                        style = TextStyle(
                            fontFamily = inter,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.Black,
                            lineHeight = 24.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.route_detail_your_journey),
                    style = TextStyle(
                        fontFamily = inter,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Black
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                state.journey.forEachIndexed { index, row ->
                    TimelineItem(
                        number = row.order,
                        label = row.label,
                        hasLine = index < state.journey.size - 1,
                        onClick = {
                            onSpotClick(row.spotId)
                            onIntent(RouteDetailIntent.SpotClicked(row.spotId))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            val shape = RoundedCornerShape(50.dp)
            Box(
                modifier = Modifier
                    .width(239.dp)
                    .height(65.dp)
                    .background(
                        color = if (state.isSaved) Color(0xFF5C5C5C) else DesignTokens.colorMaroon,
                        shape = shape
                    )
                    .clip(shape)
                    .clickable { onIntent(RouteDetailIntent.BookmarkToggled) },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UntitledIcon(
                        imageVector = UntitledIcons.SavedIcon,
                        contentDescription = null,
                        tint = Color.White,
                        size = 24.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(
                            if (state.isSaved) {
                                R.string.route_detail_save_place_saved
                            } else {
                                R.string.route_detail_save_place
                            }
                        ),
                        style = TextStyle(
                            fontFamily = inter,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White
                        )
                    )
                }
            }
        }

        BottomNavigationBar(
            isChinese = isChinese,
            onSavedClick = onNavigateToSaved,
            onMemoriesClick = onNavigateToMemories,
            currentDestination = ""
        )
    }
}

@Composable
fun TimelineItem(
    number: Int,
    label: String,
    hasLine: Boolean,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(33.dp)
        ) {
            Box(
                modifier = Modifier.height(58.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                if (number > 1) {
                    Box(
                        modifier = Modifier
                            .height(8.5.dp)
                            .width(1.dp)
                            .background(Color(0xFFD2C7B3))
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(top = 12.5.dp)
                        .size(33.dp)
                        .background(Color(0xFFD2C7B3), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = number.toString(),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontFamily = inter
                    )
                }

                if (hasLine) {
                    Box(
                        modifier = Modifier
                            .padding(top = 49.5.dp)
                            .height(8.5.dp)
                            .width(1.dp)
                            .background(Color(0xFFD2C7B3))
                    )
                }
            }

            if (hasLine) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .width(1.dp)
                        .background(Color(0xFFD2C7B3))
                )
            }
        }

        Spacer(modifier = Modifier.width(22.dp))

        val shape = RoundedCornerShape(10.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .height(58.dp)
                .border(1.dp, Color(0xFFC1C1C1), shape)
                .background(Color.Transparent)
                .padding(horizontal = 16.dp)
                .clip(shape)
                .clickable { onClick() },
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = TextStyle(
                        fontFamily = inter,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                )
                Image(
                    painter = painterResource(R.drawable.arrow_right),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFFC1C1C1))
                )
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=412dp,height=915dp")
@Composable
fun RouteDetailScreenPreview() {
    AppTheme {
        RouteDetailContent(
            state = RouteDetailState(
                isLoading = false,
                routeId = "demo",
                title = "Demo Route",
                description = "A demo description",
                heroDrawableRes = R.drawable.sounds_of_temple,
                journey = listOf(JourneyRowState(1, "demo_spot", "Demo Spot")),
                isSaved = false
            ),
            onIntent = {},
            onBackClick = {},
            onNavigateToSaved = {},
            onNavigateToMemories = {},
            onSpotClick = {}
        )
    }
}
