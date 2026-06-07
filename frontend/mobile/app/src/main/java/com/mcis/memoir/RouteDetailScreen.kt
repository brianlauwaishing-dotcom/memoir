package com.mcis.memoir

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.mcis.memoir.data.MockData
import com.mcis.memoir.data.RouteData
import com.mcis.memoir.ui.components.BottomNavigationBar
import com.mcis.memoir.ui.components.UntitledIcon
import com.mcis.memoir.ui.icons.*
import com.mcis.memoir.ui.theme.AppTheme
import com.mcis.memoir.ui.theme.inter
import com.mcis.memoir.ui.theme.judson

/**
 * Detailed screen for a cultural route.
 * Balanced to fill the screen while remaining non-scrollable on standard devices.
 */
@Composable
fun RouteDetailScreen(
    selectedLanguage: String = "en",
    routeId: String = "temples",
    isSaved: Boolean = false,
    onBackClick: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToSaved: () -> Unit = {},
    onNavigateToMemories: () -> Unit = {},
    onToggleSave: (String) -> Unit = {},
    onSpotClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isChinese = selectedLanguage == "zh"

    // Mimic backend fetch
    val route = remember(routeId) { MockData.routes.find { it.id == routeId } }

    // Debounce state to prevent multiple rapid clicks
    var isNavigating by remember { mutableStateOf(false) }

    if (route == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Route not found")
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DesignTokens.colorLanguageSelectionBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Main Content Area - Scrollable
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Hero Image Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(259.dp)
                ) {
                    Image(
                        painter = painterResource(route.imageRes),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(horizontal = 29.dp)
                        .padding(top = 24.dp)
                ) {
                    // Title
                    val title = if (isChinese) route.titleZh else route.titleEn
                    Text(
                        text = title,
                        style = TextStyle(
                            fontFamily = judson,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            lineHeight = 44.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Description with Vertical Line
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
                        
                        val description = if (isChinese) route.descriptionZh else route.descriptionEn
                        
                        Text(
                            text = description,
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

                    // Your Journey Section
                    Text(
                        text = if (isChinese) stringResource(R.string.route_detail_your_journey_zh) else stringResource(R.string.route_detail_your_journey),
                        style = TextStyle(
                            fontFamily = inter,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.Black
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Timeline Items
                    route.journeyItems.forEachIndexed { index, item ->
                        val label = if (isChinese) item.labelZh else item.labelEn
                        TimelineItem(
                            number = item.id,
                            label = label,
                            hasLine = index < route.journeyItems.size - 1,
                            onClick = { onSpotClick(item.spotId) }
                        )
                    }
                    
                    // Extra spacing at the end of scrollable content to ensure content isn't blocked by the fixed button
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }

            // Fixed Bottom Section (Save Place Button)
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
                            color = if (isSaved) Color(0xFF5C5C5C) else DesignTokens.colorMaroon, 
                            shape = shape
                        )
                        .clip(shape)
                        .clickable { onToggleSave(routeId) },
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
                            text = if (isChinese) {
                                if (isSaved) "取消收藏" else stringResource(R.string.route_detail_save_place_zh)
                            } else {
                                if (isSaved) "Saved" else stringResource(R.string.route_detail_save_place)
                            },
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

            // Bottom Navigation
            BottomNavigationBar(
                isChinese = isChinese,
                onHomeClick = onNavigateToHome,
                onSavedClick = onNavigateToSaved,
                onMemoriesClick = onNavigateToMemories,
                currentDestination = ""
            )
        }

        // Fixed Back Button (Top Left)
        Box(
            modifier = Modifier
                .padding(top = 57.dp, start = 18.dp)
                .size(24.dp)
                .clickable { 
                    if (!isNavigating) {
                        isNavigating = true
                        onBackClick()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            UntitledIcon(
                imageVector = UntitledIcons.BackIcon,
                contentDescription = "Back",
                tint = Color.White,
                size = 24.dp
            )
        }
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
            // Left column container for circle and lines
            Box(
                modifier = Modifier.height(58.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                if (number > 1) {
                    Box(
                        modifier = Modifier
                            .height(8.5.dp) // 4dp gap from circle top (12.5 - 4)
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
                            .padding(top = 49.5.dp) // 4dp gap from circle bottom (45.5 + 4)
                            .height(8.5.dp) // To fill the rest of the 58dp box (58 - 49.5)
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
        
        // Text Box
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
        RouteDetailScreen()
    }
}
