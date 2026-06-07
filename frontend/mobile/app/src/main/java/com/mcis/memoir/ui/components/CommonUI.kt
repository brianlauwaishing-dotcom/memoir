package com.mcis.memoir.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mcis.memoir.DesignTokens
import com.mcis.memoir.R
import com.mcis.memoir.data.RouteData
import com.mcis.memoir.ui.icons.*
import com.mcis.memoir.ui.theme.inter
import com.mcis.memoir.ui.theme.judson

@Composable
fun BottomNavigationBar(
    modifier: Modifier = Modifier,
    isChinese: Boolean,
    onHomeClick: () -> Unit = {},
    onSavedClick: () -> Unit = {},
    onMemoriesClick: () -> Unit = {},
    currentDestination: String = "home"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color.White)
            .border(1.dp, Color(0xFFC1C1C1)),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(
            label = if (isChinese) stringResource(R.string.home_nav_home_zh) else stringResource(R.string.home_nav_home),
            icon = UntitledIcons.HomeIcon,
            isSelected = currentDestination == "home",
            onClick = onHomeClick
        )
        BottomNavItem(
            label = if (isChinese) stringResource(R.string.home_nav_saved_zh) else stringResource(R.string.home_nav_saved),
            icon = UntitledIcons.SavedIcon,
            isSelected = currentDestination == "saved",
            onClick = onSavedClick
        )
        BottomNavItem(
            label = if (isChinese) stringResource(R.string.home_nav_memories_zh) else stringResource(R.string.home_nav_memories),
            icon = UntitledIcons.MemoriesIcon,
            isSelected = currentDestination == "memories",
            onClick = onMemoriesClick
        )
    }
}

@Composable
fun BottomNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit = {}
) {
    val color = if (isSelected) DesignTokens.colorMaroon else Color(0xFF5C5C5C)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        UntitledIcon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            size = 24.dp
        )
        Text(
            text = label,
            style = TextStyle(
                fontFamily = inter,
                fontSize = 10.sp,
                color = color
            )
        )
    }
}

@Composable
fun RouteCard(route: RouteData, isChinese: Boolean, onMoreClick: (String) -> Unit = {}) {
    val routeTitle = if (isChinese) route.titleZh else route.titleEn
    val routeCategory = if (isChinese) route.categoryZh else route.categoryEn

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFD7D7D7))
            .background(Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(246.dp)
                .background(Color.LightGray)
        ) {
            Image(
                painter = painterResource(route.imageRes),
                contentDescription = routeTitle,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = routeTitle,
                style = TextStyle(
                    fontFamily = judson,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .border(1.dp, DesignTokens.colorBorderGray, RoundedCornerShape(50.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = routeCategory,
                        style = TextStyle(
                            fontFamily = inter,
                            fontSize = 11.sp,
                            color = Color.Black
                        )
                    )
                }

                val moreButtonShape = RoundedCornerShape(50.dp)
                Box(
                    modifier = Modifier
                        .background(Color.Black, moreButtonShape)
                        .clip(moreButtonShape)
                        .clickable { onMoreClick(route.id) }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isChinese) stringResource(R.string.home_more_zh) else stringResource(R.string.home_more),
                            style = TextStyle(
                                fontFamily = inter,
                                fontSize = 10.sp,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Image(
                            painter = painterResource(R.drawable.arrow_right),
                            contentDescription = null,
                            modifier = Modifier.size(9.dp),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
                        )
                    }
                }
            }
        }
    }
}
