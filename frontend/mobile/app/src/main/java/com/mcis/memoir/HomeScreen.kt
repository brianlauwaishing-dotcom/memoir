package com.mcis.memoir

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.mcis.memoir.ui.components.RouteCard
import com.mcis.memoir.ui.components.UntitledIcon
import com.mcis.memoir.ui.icons.*
import com.mcis.memoir.ui.theme.AppTheme
import com.mcis.memoir.ui.theme.inter
import com.mcis.memoir.ui.theme.judson

data class CategoryItem(val id: String, val labelRes: Int)

/**
 * Home screen displaying cultural routes and categories.
 * Filters content based on [initialInterests] and search query.
 * Supports multiple category selection.
 */
@Composable
fun HomeScreen(
    selectedLanguage: String = "en",
    initialInterests: Set<String> = emptySet(),
    onNavigateToHome: () -> Unit = {},
    onNavigateToSaved: () -> Unit = {},
    onNavigateToMemories: () -> Unit = {},
    onMoreClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isChinese = selectedLanguage == "zh"
    val context = LocalContext.current

    // Mimic process of getting data from backend
    val routes = remember { MockData.routes }

    // Search state
    var searchQuery by remember { mutableStateOf("") }

    // Category tracking using IDs (Multi-select)
    var selectedCategoryIds by remember { 
        mutableStateOf(if (initialInterests.isEmpty()) setOf("all") else initialInterests) 
    }

    // Category list definition using string resources
    val categoryList = remember {
        listOf(
            CategoryItem("all", R.string.home_category_all),
            CategoryItem("temples", R.string.culture_temples),
            CategoryItem("old_streets", R.string.culture_old_streets),
            CategoryItem("architecture", R.string.culture_architecture),
            CategoryItem("trade", R.string.culture_trade),
            CategoryItem("colonial", R.string.culture_colonial),
            CategoryItem("crafts", R.string.culture_crafts)
        )
    }

    // Combined filtering logic
    val filteredRoutes = routes.filter { route ->
        val matchesCategory = if (selectedCategoryIds.contains("all")) {
            true
        } else {
            selectedCategoryIds.any { id ->
                val categoryResId = when(id) {
                    "temples" -> R.string.culture_temples
                    "old_streets" -> R.string.culture_old_streets
                    "architecture" -> R.string.culture_architecture
                    "trade" -> R.string.culture_trade
                    "colonial" -> R.string.culture_colonial
                    "crafts" -> R.string.culture_crafts
                    else -> 0
                }
                if (categoryResId == 0) return@any false
                
                // RESTORED: Get original English string for backend-matching comparison
                // Since MockData.kt categoryEn uses these original labels, we compare against English
                val categoryLabel = context.getString(categoryResId)
                val routeLabel = route.categoryEn
                
                categoryLabel == routeLabel
            }
        }

        val matchesSearch = if (searchQuery.isBlank()) {
            true
        } else {
            val title = if (isChinese) route.titleZh else route.titleEn
            val words = title.split(Regex("\\s+|(?<=\\p{IsHan})|(?=\\p{IsHan})"))
            words.any { word -> 
                word.startsWith(searchQuery.trim(), ignoreCase = true) 
            }
        }

        matchesCategory && matchesSearch
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
                    .padding(top = 60.dp)
            ) {
                Text(
                    text = if (isChinese) stringResource(R.string.home_subtitle_zh) else stringResource(R.string.home_subtitle),
                    style = TextStyle(
                        fontFamily = inter,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Black
                    )
                )
                Text(
                    text = if (isChinese) stringResource(R.string.home_headline_zh) else stringResource(R.string.home_headline),
                    style = TextStyle(
                        fontFamily = judson,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Black
                    )
                )

                Spacer(modifier = Modifier.height(11.dp))

                // Interactive Search Bar
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = if (isChinese) stringResource(R.string.home_search_placeholder_zh) else stringResource(R.string.home_search_placeholder)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Categories (Multi-select)
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categoryList) { category ->
                    CategoryChip(
                        label = if (category.id == "all") {
                            if (isChinese) stringResource(R.string.home_category_all_zh) else stringResource(R.string.home_category_all)
                        } else {
                            if (isChinese) {
                                val resName = context.resources.getResourceEntryName(category.labelRes)
                                val zhResId = context.resources.getIdentifier("${resName}_zh", "string", context.packageName)
                                if (zhResId != 0) stringResource(zhResId) else stringResource(category.labelRes)
                            } else {
                                stringResource(category.labelRes)
                            }
                        },
                        isSelected = selectedCategoryIds.contains(category.id),
                        onClick = { 
                            if (category.id == "all") {
                                selectedCategoryIds = setOf("all")
                            } else {
                                val current = selectedCategoryIds - "all"
                                selectedCategoryIds = if (current.contains(category.id)) {
                                    val next = current - category.id
                                    if (next.isEmpty()) setOf("all") else next
                                } else {
                                    current + category.id
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Routes List
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 29.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                filteredRoutes.forEach { route ->
                    RouteCard(route, isChinese, onMoreClick)
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        BottomNavigationBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            isChinese = isChinese,
            onHomeClick = onNavigateToHome,
            onSavedClick = onNavigateToSaved,
            onMemoriesClick = onNavigateToMemories,
            currentDestination = "home"
        )
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(Color(0xFFD7D7D7), RoundedCornerShape(50.dp))
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UntitledIcon(
                imageVector = UntitledIcons.SearchIcon,
                contentDescription = null,
                tint = Color(0xFF7B7B7B),
                size = 24.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = TextStyle(
                    fontFamily = inter,
                    fontSize = 16.sp,
                    color = Color.Black
                ),
                cursorBrush = SolidColor(Color.Black),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = TextStyle(
                                fontFamily = inter,
                                fontSize = 16.sp,
                                color = Color(0xFF7B7B7B)
                            )
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(50.dp)
    Box(
        modifier = Modifier
            .height(30.dp)
            .background(
                color = if (isSelected) DesignTokens.colorMaroon else Color.Transparent,
                shape = shape
            )
            .border(
                width = if (isSelected) 0.dp else 1.dp,
                color = if (isSelected) Color.Transparent else DesignTokens.colorBorderGray,
                shape = shape
            )
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = inter,
                fontSize = 12.sp,
                color = if (isSelected) Color.White else Color.Black
            )
        )
    }
}

@Preview(showBackground = true, device = "spec:width=412dp,height=915dp")
@Composable
fun HomeScreenPreview() {
    AppTheme {
        HomeScreen()
    }
}
