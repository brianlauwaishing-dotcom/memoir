package com.mcis.memoir

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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

@Composable
fun MemoryReflectionScreen(
    selectedLanguage: String = "en",
    onBackClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToSaved: () -> Unit = {},
    onNavigateToMemories: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isChinese = selectedLanguage == "zh"
    var reflectionText by remember { mutableStateOf("") }
    var aiPolishedText by remember { mutableStateOf("") }
    var addToLandmarks by remember { mutableStateOf(false) }

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
                    text = if (isChinese) stringResource(R.string.memory_reflection_headline_zh) else stringResource(R.string.memory_reflection_headline),
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
                        .clickable { onNextClick() }
                )
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 35.dp)
                    .fillMaxWidth()
            ) {
                // Section 1: My Reflection
                Text(
                    text = if (isChinese) stringResource(R.string.memory_reflection_my_reflection_zh) else stringResource(R.string.memory_reflection_my_reflection),
                    style = TextStyle(fontFamily = inter, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                )
                Spacer(modifier = Modifier.height(15.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(216.dp)
                        .shadow(12.dp, RoundedCornerShape(15.dp))
                        .background(DesignTokens.colorLanguageSelectionBackground, RoundedCornerShape(15.dp))
                        .padding(16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Number indicator
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("1", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            BasicTextField(
                                value = reflectionText,
                                onValueChange = { if (it.length <= 300) reflectionText = it },
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(fontFamily = inter, fontSize = 12.sp, color = Color.Black),
                                decorationBox = { innerTextField ->
                                    if (reflectionText.isEmpty()) {
                                        Text(
                                            text = if (isChinese) stringResource(R.string.memory_reflection_placeholder_zh) else stringResource(R.string.memory_reflection_placeholder),
                                            style = TextStyle(fontFamily = inter, fontSize = 12.sp, color = Color(0xFF868782))
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            Text(
                                text = "${reflectionText.length}/300",
                                style = TextStyle(fontFamily = inter, fontSize = 12.sp, color = Color(0xFF868782)),
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Section 3: Polish with AI
                Text(
                    text = if (isChinese) stringResource(R.string.memory_reflection_polish_ai_zh) else stringResource(R.string.memory_reflection_polish_ai),
                    style = TextStyle(fontFamily = inter, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                )
                Spacer(modifier = Modifier.height(15.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(283.dp)
                        .shadow(12.dp, RoundedCornerShape(15.dp))
                        .background(Color(0xFFFCE4D9), RoundedCornerShape(15.dp)) // Specific light peach color
                        .padding(16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("3", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        // Placeholder for AI result
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            // Buttons at bottom
                            Row(
                                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Regenerate
                                Row(
                                    modifier = Modifier.clickable { /* AI logic */ },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    UntitledIcon(UntitledIcons.RegenerateIcon, null, size = 20.dp, tint = DesignTokens.colorMaroon)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isChinese) stringResource(R.string.memory_reflection_regenerate_zh) else stringResource(R.string.memory_reflection_regenerate), color = DesignTokens.colorMaroon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(20.dp))
                                // Copy
                                Row(
                                    modifier = Modifier.clickable { /* Copy logic */ },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    UntitledIcon(UntitledIcons.CopyIcon, null, size = 20.dp, tint = DesignTokens.colorMaroon)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isChinese) stringResource(R.string.memory_reflection_copy_zh) else stringResource(R.string.memory_reflection_copy), color = DesignTokens.colorMaroon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Bottom: Add to landmarks
                Text(
                    text = if (isChinese) stringResource(R.string.memory_reflection_add_landmarks_zh) else stringResource(R.string.memory_reflection_add_landmarks),
                    style = TextStyle(fontFamily = inter, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(83.dp)
                        .shadow(12.dp, RoundedCornerShape(15.dp))
                        .background(DesignTokens.colorLanguageSelectionBackground, RoundedCornerShape(15.dp))
                        .clickable { addToLandmarks = !addToLandmarks }
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        UntitledIcon(UntitledIcons.LandmarkIcon, null, size = 44.dp, tint = Color.Black)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(if (isChinese) stringResource(R.string.memory_reflection_add_landmarks_zh) else stringResource(R.string.memory_reflection_add_landmarks), style = TextStyle(fontFamily = inter, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .border(2.dp, if (addToLandmarks) DesignTokens.colorMaroon else Color.Gray, RoundedCornerShape(4.dp))
                                .background(if (addToLandmarks) DesignTokens.colorMaroon else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            if (addToLandmarks) {
                                UntitledIcon(UntitledIcons.CheckIcon, null, size = 16.dp, tint = Color.White)
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))

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

@Preview(showBackground = true, device = "spec:width=412dp,height=915dp")
@Composable
fun MemoryReflectionScreenPreview() {
    AppTheme {
        MemoryReflectionScreen()
    }
}
