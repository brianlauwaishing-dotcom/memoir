package com.mcis.memoir

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mcis.memoir.data.MockData
import com.mcis.memoir.data.TemplateData
import com.mcis.memoir.ui.components.BottomNavigationBar
import com.mcis.memoir.ui.components.UntitledIcon
import com.mcis.memoir.ui.icons.*
import com.mcis.memoir.ui.theme.AppTheme
import com.mcis.memoir.ui.theme.inter

/**
 * Screen for previewing and editing a memory.
 * Shows the chosen template as a mask overlay with imported photos placed underneath.
 */
@Composable
fun MemoryEditScreen(
    selectedLanguage: String = "en",
    templateId: String = "old_street",
    photoResIds: List<Int> = emptyList(),
    onBackClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToSaved: () -> Unit = {},
    onNavigateToMemories: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isChinese = selectedLanguage == "zh"
    val template = remember(templateId) { MockData.templates.find { it.id == templateId } }

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
                    .padding(top = 54.dp, bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isChinese) stringResource(R.string.memory_flow_preview_edit_zh) else stringResource(R.string.memory_flow_preview_edit),
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

                // Save Button
                Text(
                    text = if (isChinese) stringResource(R.string.save_button_zh) else stringResource(R.string.save_button),
                    style = TextStyle(
                        fontFamily = inter,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DesignTokens.colorMaroon
                    ),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 17.dp)
                        .clickable { onSaveClick() }
                )
            }

            // Main Preview Area (The Canvas to be saved)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                if (template != null) {
                    TemplateCanvas(template, photoResIds)
                }
            }

            // Editing Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(DesignTokens.colorLanguageSelectionBackground)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EditToolItem(UntitledIcons.MoveIcon, if (isChinese) stringResource(R.string.memory_flow_move_zh) else stringResource(R.string.memory_flow_move))
                EditToolItem(UntitledIcons.FiltersIcon, if (isChinese) stringResource(R.string.memory_flow_filters_zh) else stringResource(R.string.memory_flow_filters))
                EditToolItem(UntitledIcons.StickersIcon, if (isChinese) stringResource(R.string.memory_flow_stickers_zh) else stringResource(R.string.memory_flow_stickers))
                EditToolItem(UntitledIcons.FontsIcon, if (isChinese) stringResource(R.string.memory_flow_fonts_zh) else stringResource(R.string.memory_flow_fonts))
                EditToolItem(UntitledIcons.StampsIcon, if (isChinese) stringResource(R.string.memory_flow_stamps_zh) else stringResource(R.string.memory_flow_stamps))
            }

            // Bottom Navigation Bar
            BottomNavigationBar(
                isChinese = isChinese,
                onHomeClick = onNavigateToHome,
                onSavedClick = onNavigateToSaved,
                onMemoriesClick = onNavigateToMemories,
                currentDestination = "memories"
            )
        }
    }
}

@Composable
fun TemplateCanvas(template: TemplateData, photoResIds: List<Int>) {
    val maskPainter = template.maskRes?.let { painterResource(it) }
    
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = maxWidth
        val canvasHeight = maxHeight

        // 1. Photos Layer (Drawn FIRST - placed BEHIND the template)
        Box(modifier = Modifier.fillMaxSize()) {
            template.slots.forEachIndexed { index, slot ->
                if (index < photoResIds.size) {
                    val photoRes = photoResIds[index]
                    Box(
                        modifier = Modifier
                            .offset(x = canvasWidth * slot.xPercent, y = canvasHeight * slot.yPercent)
                            .width(canvasWidth * slot.widthPercent)
                            .height(canvasHeight * slot.heightPercent)
                            .rotate(slot.rotation)
                    ) {
                        Image(
                            painter = painterResource(photoRes),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }

        // 2. Template Mask Layer (Drawn LAST - placed ON TOP)
        // We use DstOut blending if a mask is provided to "punch holes" in the template image
        Image(
            painter = painterResource(template.imageRes),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    // Draw the template artwork
                    drawContent()
                    
                    // If a mask is provided, use it to punch holes in the artwork
                    // DstOut keeps destination (artwork) where source (mask) is NOT present (alpha 0)
                    if (maskPainter != null) {
                        drawIntoCanvas { canvas ->
                            val paint = Paint().apply {
                                blendMode = BlendMode.DstOut
                            }
                            canvas.saveLayer(size.toRect(), paint)
                            with(maskPainter) {
                                draw(size)
                            }
                            canvas.restore()
                        }
                    }
                },
            contentScale = ContentScale.FillBounds
        )
    }
}

@Composable
private fun EditToolItem(icon: ImageVector, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { /* Tool logic */ }
            .padding(8.dp)
    ) {
        UntitledIcon(
            imageVector = icon,
            contentDescription = label,
            tint = DesignTokens.colorMaroon,
            size = 32.dp
        )
        Text(
            text = label,
            style = TextStyle(
                fontFamily = inter,
                fontSize = 10.sp,
                color = DesignTokens.colorMaroon
            )
        )
    }
}

@Preview(showBackground = true, device = "spec:width=412dp,height=915dp")
@Composable
fun MemoryEditScreenPreview() {
    AppTheme {
        MemoryEditScreen(
            photoResIds = listOf(
                R.drawable.sounds_of_temple, 
                R.drawable.sea_protection, 
                R.drawable.faith_hidden
            )
        )
    }
}
