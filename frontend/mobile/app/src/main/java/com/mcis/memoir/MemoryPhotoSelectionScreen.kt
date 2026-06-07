package com.mcis.memoir

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mcis.memoir.ui.components.BottomNavigationBar
import com.mcis.memoir.ui.components.UntitledIcon
import com.mcis.memoir.ui.icons.*
import com.mcis.memoir.ui.theme.AppTheme
import com.mcis.memoir.ui.theme.inter

/**
 * Screen for importing photos for a new memory.
 * Supports selecting multiple photos and displaying them in a numbered grid.
 */
@Composable
fun MemoryPhotoSelectionScreen(
    selectedLanguage: String = "en",
    templateId: String = "",
    initialPhotos: List<Int> = emptyList(),
    onPhotosChange: (List<Int>) -> Unit = {},
    onBackClick: () -> Unit = {},
    onNextClick: (List<Int>) -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToSaved: () -> Unit = {},
    onNavigateToMemories: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isChinese = selectedLanguage == "zh"
    
    // Use hoisted state via callback
    val selectedPhotos = initialPhotos

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DesignTokens.colorLanguageSelectionBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 54.dp, bottom = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isChinese) stringResource(R.string.memory_flow_import_photos_zh) else stringResource(R.string.memory_flow_import_photos),
                    style = TextStyle(
                        fontFamily = inter,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = DesignTokens.colorMaroon
                    )
                )
                
                // Back Button
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 17.dp)
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    UntitledIcon(
                        imageVector = UntitledIcons.BackIcon,
                        contentDescription = if (isChinese) stringResource(R.string.back_button_zh) else stringResource(R.string.back_button),
                        size = 24.dp,
                        tint = DesignTokens.colorMaroon
                    )
                }

                // Next Button
                Text(
                    text = if (isChinese) stringResource(R.string.next_button_zh) else stringResource(R.string.next_button),
                    style = TextStyle(
                        fontFamily = inter,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DesignTokens.colorMaroon
                    ),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 17.dp)
                        .clickable { if (selectedPhotos.isNotEmpty()) onNextClick(selectedPhotos) }
                )
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 27.dp)
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Text(
                    text = if (isChinese) stringResource(R.string.memory_flow_photos_selected_zh, selectedPhotos.size) else stringResource(R.string.memory_flow_photos_selected, selectedPhotos.size),
                    style = TextStyle(
                        fontFamily = inter,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
                Text(
                    text = if (isChinese) stringResource(R.string.memory_flow_hold_to_reorder_zh) else stringResource(R.string.memory_flow_hold_to_reorder),
                    style = TextStyle(
                        fontFamily = inter,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )

                Spacer(modifier = Modifier.height(33.dp))

                // Photos Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                    verticalArrangement = Arrangement.spacedBy(11.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(selectedPhotos) { index, photoRes ->
                        SelectedPhotoItem(index + 1, photoRes)
                    }
                    
                    // Add Photos Button as a grid item
                    item {
                        AddPhotosBox(isChinese) {
                            // Simulate selecting 5 photos
                            onPhotosChange(listOf(
                                R.drawable.sounds_of_temple,
                                R.drawable.sea_protection,
                                R.drawable.layers_of_colonial,
                                R.drawable.brick_arches_and_time,
                                R.drawable.faith_hidden
                            ))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(80.dp)) // Space for bottom nav
        }

        // Bottom Navigation Bar
        BottomNavigationBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            isChinese = isChinese,
            onHomeClick = onNavigateToHome,
            onSavedClick = onNavigateToSaved,
            onMemoriesClick = onNavigateToMemories,
            currentDestination = "memories"
        )
    }
}

@Composable
fun SelectedPhotoItem(number: Int, photoRes: Int) {
    Box(
        modifier = Modifier
            .size(178.dp)
            .clip(RoundedCornerShape(15.dp))
    ) {
        Image(
            painter = painterResource(photoRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Number Overlay
        Box(
            modifier = Modifier
                .padding(8.dp)
                .size(31.dp)
                .background(DesignTokens.colorMaroon, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                style = TextStyle(
                    fontFamily = inter,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }
    }
}

@Composable
fun AddPhotosBox(isChinese: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(178.dp)
            .clip(RoundedCornerShape(15.dp))
            .clickable { onClick() }
            .drawDashedBorder(
                color = Color(0xFF868782),
                strokeWidth = 2.dp,
                dashLength = 10.dp,
                gapLength = 10.dp,
                cornerRadius = 15.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            UntitledIcon(
                imageVector = UntitledIcons.CameraIcon,
                contentDescription = null,
                size = 81.dp,
                tint = Color(0xFF868782)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isChinese) stringResource(R.string.memory_flow_add_photos_zh) else stringResource(R.string.memory_flow_add_photos),
                style = TextStyle(
                    fontFamily = inter,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF868782)
                )
            )
        }
    }
}

/**
 * Custom modifier to draw a dashed border.
 */
fun Modifier.drawDashedBorder(
    color: Color,
    strokeWidth: androidx.compose.ui.unit.Dp,
    dashLength: androidx.compose.ui.unit.Dp,
    gapLength: androidx.compose.ui.unit.Dp,
    cornerRadius: androidx.compose.ui.unit.Dp
) = this.drawBehind {
    val stroke = Stroke(
        width = strokeWidth.toPx(),
        pathEffect = PathEffect.dashPathEffect(
            floatArrayOf(dashLength.toPx(), gapLength.toPx()),
            0f
        )
    )
    drawRoundRect(
        color = color,
        style = stroke,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx())
    )
}

@Preview(showBackground = true, device = "spec:width=412dp,height=915dp")
@Composable
fun MemoryPhotoSelectionScreenPreview() {
    AppTheme {
        MemoryPhotoSelectionScreen()
    }
}
