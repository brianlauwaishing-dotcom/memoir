package com.mcis.memoir

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mcis.memoir.ui.components.BottomNavigationBar
import com.mcis.memoir.ui.components.UntitledIcon
import com.mcis.memoir.ui.icons.*
import com.mcis.memoir.ui.memory.components.FilePhoto
import com.mcis.memoir.ui.memory.edit.EditEffect
import com.mcis.memoir.ui.memory.edit.EditIntent
import com.mcis.memoir.ui.memory.edit.EditState
import com.mcis.memoir.ui.memory.edit.EditViewModel
import com.mcis.memoir.ui.theme.inter

@Composable
fun MemoryEditScreen(
    viewModel: EditViewModel,
    onBackClick: () -> Unit = {},
    onNavigateToReflection: (String) -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToSaved: () -> Unit = {},
    onNavigateToMemories: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isChinese = LocalConfiguration.current.locales[0].language == "zh"

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is EditEffect.NavigateToReflection -> onNavigateToReflection(effect.memoryId)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DesignTokens.colorLanguageSelectionBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 54.dp, bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.memory_flow_preview_edit),
                    style = TextStyle(
                        fontFamily = inter,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = DesignTokens.colorMaroon
                    )
                )

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
                        contentDescription = stringResource(R.string.back_button),
                        size = 24.dp,
                        tint = DesignTokens.colorMaroon
                    )
                }

                Text(
                    text = stringResource(R.string.save_button),
                    style = TextStyle(
                        fontFamily = inter,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DesignTokens.colorMaroon
                    ),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 17.dp)
                        .clickable { viewModel.onIntent(EditIntent.SaveClicked) }
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                if (state.isSpotDraft) {
                    SpotBookmarkCanvas(state)
                } else if (state.templateImageRes != 0) {
                    TemplateCanvas(state)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(DesignTokens.colorLanguageSelectionBackground)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EditToolItem(UntitledIcons.MoveIcon, stringResource(R.string.memory_flow_move), "Move")
                EditToolItem(UntitledIcons.FiltersIcon, stringResource(R.string.memory_flow_filters), "Filters")
                EditToolItem(UntitledIcons.StickersIcon, stringResource(R.string.memory_flow_stickers), "Stickers")
                EditToolItem(UntitledIcons.FontsIcon, stringResource(R.string.memory_flow_fonts), "Fonts")
                EditToolItem(UntitledIcons.StampsIcon, stringResource(R.string.memory_flow_stamps), "Stamps")
            }

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
private fun SpotBookmarkCanvas(state: EditState) {
    val context = LocalContext.current
    val firstPhoto = state.photoPaths.firstOrNull()

    Box(modifier = Modifier.fillMaxSize()) {
        if (firstPhoto != null) {
            FilePhoto(
                relativePath = firstPhoto,
                filesDir = context.filesDir,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE9E3DA))
            )
        }

        if (state.spotTitle.isNotBlank() || state.spotDescription.isNotBlank()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.58f))
                    .padding(horizontal = 24.dp, vertical = 18.dp)
            ) {
                if (state.spotTitle.isNotBlank()) {
                    Text(
                        text = state.spotTitle,
                        style = TextStyle(
                            fontFamily = inter,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
                if (state.spotDescription.isNotBlank()) {
                    Text(
                        text = state.spotDescription,
                        style = TextStyle(
                            fontFamily = inter,
                            fontSize = 16.sp,
                            color = Color.White,
                            lineHeight = 22.sp
                        ),
                        maxLines = 5
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateCanvas(state: EditState) {
    val context = LocalContext.current
    val maskPainter = if (state.templateMaskRes != 0) painterResource(state.templateMaskRes) else null

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = maxWidth
        val canvasHeight = maxHeight

        Box(modifier = Modifier.fillMaxSize()) {
            state.templateSlots.forEachIndexed { index, slot ->
                val relativePath = state.photoPaths.getOrNull(index)
                if (relativePath != null) {
                    Box(
                        modifier = Modifier
                            .offset(x = canvasWidth * slot.x, y = canvasHeight * slot.y)
                            .width(canvasWidth * slot.width)
                            .height(canvasHeight * slot.height)
                            .rotate(slot.rotation)
                    ) {
                        FilePhoto(
                            relativePath = relativePath,
                            filesDir = context.filesDir,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }

        Image(
            painter = painterResource(state.templateImageRes),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
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
private fun EditToolItem(icon: ImageVector, label: String, logName: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { Log.w("memory-creation-flow", "$logName not yet implemented") }
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
