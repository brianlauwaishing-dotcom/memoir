package com.mcis.memoir

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import com.mcis.memoir.data.PreferenceManager

@Composable
fun MyAppNavigation() {
    val context = LocalContext.current
    val preferenceManager = remember { PreferenceManager(context) }

    // Load initial values from persistent storage
    var selectedLanguage by remember { mutableStateOf(preferenceManager.selectedLanguage) }
    var userInterests by remember { mutableStateOf(preferenceManager.userInterests) }
    var savedRouteIds by remember { mutableStateOf(preferenceManager.savedRouteIds) }

    // Flow State: Memory Creation
    var selectedPhotos by remember { mutableStateOf(listOf<Int>()) }
    
    // Decide starting destination
    val initialDestination = if (preferenceManager.onboardingCompleted) HomeDestination else WelcomeDestination
    val backStack = remember { mutableStateListOf<Any>(initialDestination) }

    BackHandler(enabled = backStack.size > 1) {
        backStack.removeLastOrNull()
    }
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        transitionSpec = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(400)
            ) togetherWith ExitTransition.None
        },
        popTransitionSpec = {
            EnterTransition.None
                .togetherWith (
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(400)
                    )
                )
        },
        entryProvider = { key ->
            when (key) {
                is WelcomeDestination -> NavEntry(key) {
                    WelcomeScreen(
                        selectedLanguage = selectedLanguage,
                        onBeginTourClick = {
                            backStack.add(LanguageSelectionDestination)
                        }
                    )
                }
                is LanguageSelectionDestination -> NavEntry(key) {
                    LanguageSelectionScreen(
                        initialLanguage = selectedLanguage,
                        onLanguageSelect = { languageCode ->
                            selectedLanguage = languageCode
                            preferenceManager.selectedLanguage = languageCode
                        },
                        onNextClick = {
                            preferenceManager.selectedLanguage = selectedLanguage
                            backStack.add(CultureInterestDestination)
                        }
                    )
                }
                is CultureInterestDestination -> NavEntry(key) {
                    CultureInterestScreen(
                        selectedLanguage = selectedLanguage,
                        initialInterests = userInterests,
                        onInterestSelect = { interestId, isSelected ->
                            userInterests = if (isSelected) {
                                userInterests + interestId
                            } else {
                                userInterests - interestId
                            }
                            preferenceManager.userInterests = userInterests
                        },
                        onStartExploringClick = {
                            preferenceManager.selectedLanguage = selectedLanguage
                            preferenceManager.userInterests = userInterests
                            preferenceManager.onboardingCompleted = true
                            backStack.add(HomeDestination)
                        },
                        onSkipClick = {
                            userInterests = emptySet()
                            preferenceManager.selectedLanguage = selectedLanguage
                            preferenceManager.userInterests = emptySet()
                            preferenceManager.onboardingCompleted = true
                            backStack.add(HomeDestination)
                        }
                    )
                }
                is HomeDestination -> NavEntry(key) {
                    HomeScreen(
                        selectedLanguage = selectedLanguage,
                        initialInterests = userInterests,
                        onNavigateToHome = {
                            if (backStack.last() != HomeDestination) {
                                backStack.clear()
                                backStack.add(HomeDestination)
                            }
                        },
                        onNavigateToSaved = {
                            backStack.add(SavedDestination)
                        },
                        onNavigateToMemories = {
                            backStack.add(MemoriesDestination)
                        },
                        onMoreClick = { routeId ->
                            backStack.add(RouteDetailDestination(routeId))
                        }
                    )
                }
                is SavedDestination -> NavEntry(key) {
                    SavedScreen(
                        selectedLanguage = selectedLanguage,
                        savedRouteIds = savedRouteIds,
                        onNavigateToHome = {
                            backStack.clear()
                            backStack.add(HomeDestination)
                        },
                        onNavigateToSaved = {
                            if (backStack.last() != SavedDestination) {
                                backStack.add(SavedDestination)
                            }
                        },
                        onNavigateToMemories = {
                            backStack.add(MemoriesDestination)
                        },
                        onMoreClick = { routeId ->
                            backStack.add(RouteDetailDestination(routeId))
                        }
                    )
                }
                is MemoriesDestination -> NavEntry(key) {
                    MemoriesScreen(
                        selectedLanguage = selectedLanguage,
                        onNavigateToHome = {
                            backStack.clear()
                            backStack.add(HomeDestination)
                        },
                        onNavigateToSaved = {
                            backStack.add(SavedDestination)
                        },
                        onNavigateToMemories = {
                            if (backStack.last() != MemoriesDestination) {
                                backStack.add(MemoriesDestination)
                            }
                        },
                        onCreateMemoryClick = {
                            selectedPhotos = emptyList() // Reset for new creation
                            backStack.add(MemoryTemplateDestination)
                        }
                    )
                }
                is MemoryTemplateDestination -> NavEntry(key) {
                    MemoryTemplateScreen(
                        selectedLanguage = selectedLanguage,
                        onBackClick = {
                            backStack.removeLastOrNull()
                        },
                        onNavigateToHome = {
                            backStack.clear()
                            backStack.add(HomeDestination)
                        },
                        onNavigateToSaved = {
                            backStack.add(SavedDestination)
                        },
                        onNavigateToMemories = {
                            while (backStack.lastOrNull() != MemoriesDestination && backStack.size > 1) {
                                backStack.removeLastOrNull()
                            }
                            if (backStack.isEmpty()) {
                                backStack.add(MemoriesDestination)
                            }
                        },
                        onTemplateSelect = { templateId ->
                            selectedPhotos = emptyList() // Clear selection when choosing a new template
                            backStack.add(MemoryPhotoSelectionDestination(templateId))
                        }
                    )
                }
                is MemoryPhotoSelectionDestination -> NavEntry(key) {
                    MemoryPhotoSelectionScreen(
                        selectedLanguage = selectedLanguage,
                        templateId = key.templateId,
                        initialPhotos = selectedPhotos,
                        onPhotosChange = { selectedPhotos = it },
                        onBackClick = {
                            backStack.removeLastOrNull()
                        },
                        onNextClick = { photos ->
                            selectedPhotos = photos
                            backStack.add(MemoryEditDestination(key.templateId, photos))
                        },
                        onNavigateToHome = {
                            backStack.clear()
                            backStack.add(HomeDestination)
                        },
                        onNavigateToSaved = {
                            backStack.add(SavedDestination)
                        },
                        onNavigateToMemories = {
                            while (backStack.lastOrNull() != MemoriesDestination && backStack.size > 1) {
                                backStack.removeLastOrNull()
                            }
                            if (backStack.isEmpty()) {
                                backStack.add(MemoriesDestination)
                            }
                        }
                    )
                }
                is MemoryEditDestination -> NavEntry(key) {
                    MemoryEditScreen(
                        selectedLanguage = selectedLanguage,
                        templateId = key.templateId,
                        photoResIds = selectedPhotos,
                        onBackClick = {
                            backStack.removeLastOrNull()
                        },
                        onSaveClick = {
                            backStack.add(MemoryReflectionDestination)
                        },
                        onNavigateToHome = {
                            backStack.clear()
                            backStack.add(HomeDestination)
                        },
                        onNavigateToSaved = {
                            backStack.add(SavedDestination)
                        },
                        onNavigateToMemories = {
                            while (backStack.lastOrNull() != MemoriesDestination && backStack.size > 1) {
                                backStack.removeLastOrNull()
                            }
                            if (backStack.isEmpty()) {
                                backStack.add(MemoriesDestination)
                            }
                        }
                    )
                }
                is MemoryReflectionDestination -> NavEntry(key) {
                    MemoryReflectionScreen(
                        selectedLanguage = selectedLanguage,
                        onBackClick = {
                            backStack.removeLastOrNull()
                        },
                        onNextClick = {
                            // Clear backstack to Memories
                            while (backStack.lastOrNull() != MemoriesDestination && backStack.size > 1) {
                                backStack.removeLastOrNull()
                            }
                            if (backStack.isEmpty()) {
                                backStack.add(MemoriesDestination)
                            }
                        },
                        onNavigateToHome = {
                            backStack.clear()
                            backStack.add(HomeDestination)
                        },
                        onNavigateToSaved = {
                            backStack.add(SavedDestination)
                        },
                        onNavigateToMemories = {
                            while (backStack.lastOrNull() != MemoriesDestination && backStack.size > 1) {
                                backStack.removeLastOrNull()
                            }
                            if (backStack.isEmpty()) {
                                backStack.add(MemoriesDestination)
                            }
                        }
                    )
                }
                is RouteDetailDestination -> NavEntry(key) {
                    RouteDetailScreen(
                        selectedLanguage = selectedLanguage,
                        routeId = key.routeId,
                        isSaved = savedRouteIds.contains(key.routeId),
                        onBackClick = {
                            backStack.removeLastOrNull()
                        },
                        onNavigateToHome = {
                            backStack.clear()
                            backStack.add(HomeDestination)
                        },
                        onNavigateToSaved = {
                            backStack.add(SavedDestination)
                        },
                        onNavigateToMemories = {
                            backStack.add(MemoriesDestination)
                        },
                        onToggleSave = { routeId ->
                            savedRouteIds = if (savedRouteIds.contains(routeId)) {
                                savedRouteIds - routeId
                            } else {
                                savedRouteIds + routeId
                            }
                            preferenceManager.savedRouteIds = savedRouteIds
                        },
                        onSpotClick = { spotId ->
                            backStack.add(SpotIntroDestination(spotId))
                        }
                    )
                }
                is SpotIntroDestination -> NavEntry(key) {
                    SpotIntroScreen(
                        selectedLanguage = selectedLanguage,
                        spotId = key.spotId,
                        onBackClick = {
                            backStack.removeLastOrNull()
                        },
                        onInfoClick = { spotId ->
                            backStack.add(SpotDetailDestination(spotId))
                        },
                        onDiscoveryItemClick = { artifactId ->
                            backStack.add(ArtifactDiscoveryDestination(key.spotId, artifactId))
                        }
                    )
                }
                is ArtifactDiscoveryDestination -> NavEntry(key) {
                    ArtifactDiscoveryScreen(
                        selectedLanguage = selectedLanguage,
                        spotId = key.spotId,
                        artifactId = key.artifactId,
                        onBackClick = {
                            backStack.removeLastOrNull()
                        },
                        onInfoClick = { spotId ->
                            backStack.add(SpotDetailDestination(spotId))
                        },
                        onMoreClick = { spotId, artifactId ->
                            backStack.add(ArtifactDetailDestination(spotId, artifactId))
                        },
                        onCameraClick = {
                            backStack.add(CameraPreviewDestination)
                        }
                    )
                }
                is ArtifactDetailDestination -> NavEntry(key) {
                    ArtifactDetailScreen(
                        selectedLanguage = selectedLanguage,
                        spotId = key.spotId,
                        artifactId = key.artifactId,
                        onBackClick = {
                            backStack.removeLastOrNull()
                        },
                        onInfoClick = { spotId ->
                            backStack.add(SpotDetailDestination(spotId))
                        },
                        onCameraClick = {
                            backStack.add(CameraPreviewDestination)
                        },
                        onNavigateToHome = {
                            backStack.clear()
                            backStack.add(HomeDestination)
                        },
                        onNavigateToSaved = {
                            backStack.add(SavedDestination)
                        },
                        onNavigateToMemories = {
                            backStack.add(MemoriesDestination)
                        }
                    )
                }
                is CameraPreviewDestination -> NavEntry(key) {
                    CameraPreviewScreen(
                        onBackClick = {
                            backStack.removeLastOrNull()
                        },
                        onCaptureClick = {
                            backStack.removeLastOrNull()
                        }
                    )
                }
                is SpotDetailDestination -> NavEntry(key) {
                    SpotDetailScreen(
                        selectedLanguage = selectedLanguage,
                        spotId = key.spotId,
                        onBackClick = {
                            backStack.removeLastOrNull()
                        },
                        onNavigateToHome = {
                            backStack.clear()
                            backStack.add(HomeDestination)
                        },
                        onNavigateToSaved = {
                            backStack.add(SavedDestination)
                        },
                        onNavigateToMemories = {
                            backStack.add(MemoriesDestination)
                        },
                        onDiscoveryItemClick = { artifactId ->
                            backStack.add(ArtifactDiscoveryDestination(key.spotId, artifactId))
                        }
                    )
                }
                else -> NavEntry(Unit) { Text("Unknown destination") }
            }
        }
    )

}
