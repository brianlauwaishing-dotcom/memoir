package com.mcis.memoir

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mcis.memoir.data.MockData
import com.mcis.memoir.ui.components.BottomNavigationBar
import com.mcis.memoir.ui.components.RouteCard
import com.mcis.memoir.ui.theme.AppTheme
import com.mcis.memoir.ui.theme.judson

/**
 * Screen displaying the user's saved cultural routes.
 */
@Composable
fun SavedScreen(
    selectedLanguage: String = "en",
    savedRouteIds: Set<String> = emptySet(),
    onNavigateToHome: () -> Unit = {},
    onNavigateToSaved: () -> Unit = {},
    onNavigateToMemories: () -> Unit = {},
    onMoreClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isChinese = selectedLanguage == "zh"
    
    // Filter routes to only those that are saved
    val savedRoutes = remember(savedRouteIds) {
        MockData.routes.filter { it.id in savedRouteIds }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DesignTokens.colorLanguageSelectionBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Section
            Column(
                modifier = Modifier
                    .padding(horizontal = 29.dp)
                    .padding(top = 62.dp)
            ) {
                Text(
                    text = if (isChinese) "我的旅遊計畫" else "My travel plan",
                    style = TextStyle(
                        fontFamily = judson,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Black
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Routes List
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 29.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(20.dp)
            ) {
                if (savedRoutes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isChinese) "目前還沒有收藏的路徑" else "No saved routes yet",
                            style = TextStyle(
                                fontSize = 16.sp,
                                color = Color.Gray
                            )
                        )
                    }
                } else {
                    savedRoutes.forEach { route ->
                        RouteCard(route, isChinese, onMoreClick)
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Bottom Navigation Bar
        BottomNavigationBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            isChinese = isChinese,
            onHomeClick = onNavigateToHome,
            onSavedClick = onNavigateToSaved,
            onMemoriesClick = onNavigateToMemories,
            currentDestination = "saved"
        )
    }
}

@Preview(showBackground = true, device = "spec:width=412dp,height=915dp")
@Composable
fun SavedScreenPreview() {
    AppTheme {
        SavedScreen(savedRouteIds = setOf("sounds_of_temple", "sea_protection"))
    }
}
