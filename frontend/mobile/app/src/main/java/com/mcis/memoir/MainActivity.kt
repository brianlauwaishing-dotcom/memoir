package com.mcis.memoir

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.mcis.memoir.ui.theme.AppTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import kotlinx.coroutines.delay

/**
 * Navigation destination for the Welcome screen.
 */
data object WelcomeDestination

/**
 * Navigation destination for the Language Selection screen.
 */
data object LanguageSelectionDestination

/**
 * Navigation destination for the Culture Interest screen.
 */
data object CultureInterestDestination

/**
 * Navigation destination for the Home screen.
 */
data object HomeDestination

/**
 * Navigation destination for the Saved screen.
 */
data object SavedDestination

/**
 * Navigation destination for the Memories screen.
 */
data object MemoriesDestination

/**
 * Navigation destination for the Memory Template screen.
 */
data object MemoryTemplateDestination

/**
 * Navigation destination for the Memory Photo Selection screen.
 */
data class MemoryPhotoSelectionDestination(val templateId: String)

/**
 * Navigation destination for the Memory Edit (Preview & Edit) screen.
 */
data class MemoryEditDestination(val templateId: String, val photoResIds: List<Int>)

/**
 * Navigation destination for the Memory Reflection screen.
 */
data object MemoryReflectionDestination

/**
 * Navigation destination for the Route Detail screen.
 */
data class RouteDetailDestination(val routeId: String)

/**
 * Navigation destination for the Spot Detail screen.
 */
data class SpotDetailDestination(val spotId: String)

/**
 * Navigation destination for the Spot Intro (Discovery Mode) screen.
 */
data class SpotIntroDestination(val spotId: String)

/**
 * Navigation destination for the Artifact Discovery (Look Closer) screen.
 */
data class ArtifactDiscoveryDestination(val spotId: String, val artifactId: Int)

/**
 * Navigation destination for the Artifact Detail screen.
 */
data class ArtifactDetailDestination(val spotId: String, val artifactId: Int)

/**
 * Navigation destination for the Camera Preview screen.
 */
data object CameraPreviewDestination

/**
 * Navigation destination for the Spot Explore (Continue Exploring) screen.
 */
data class SpotExploreDestination(val spotId: String)

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navigationEventDispatcherOwner = rememberNavigationEventDispatcherOwner(parent = null)
            CompositionLocalProvider(
                LocalNavigationEventDispatcherOwner provides navigationEventDispatcherOwner
            ) {
                AppTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        // Show the launch splash only on a genuine cold start. rememberSaveable
                        // keeps this flag across Activity recreation (e.g. the recreation
                        // triggered by applying a per-app locale), so the logo does not flash
                        // again mid-flow after the user picks a language.
                        var showSplash by rememberSaveable { mutableStateOf(true) }

                        LaunchedEffect(Unit) {
                            if (showSplash) {
                                delay(2000)
                                showSplash = false
                            }
                        }

                        if (showSplash) {
                            SplashScreen()
                        } else {
                            MyAppNavigation()
                        }
                    }
                }
            }
        }
    }
}
