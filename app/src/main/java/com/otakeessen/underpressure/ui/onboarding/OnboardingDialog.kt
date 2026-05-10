package com.otakeessen.underpressure.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.otakeessen.underpressure.BuildConfig
import com.otakeessen.underpressure.R

@Composable
fun OnboardingDialog(
    onDismiss: () -> Unit
) {
    val slides = listOf(
        OnboardingSlideData(
            icon = Icons.Default.Security,
            caption = stringResource(R.string.onboarding_slide1_caption),
            description = stringResource(R.string.onboarding_slide1_description),
            version = stringResource(R.string.onboarding_version_label, BuildConfig.VERSION_NAME)
        ),
        OnboardingSlideData(
            icon = Icons.Default.Schedule,
            caption = stringResource(R.string.onboarding_slide2_caption),
            description = stringResource(R.string.onboarding_slide2_description)
        ),
        OnboardingSlideData(
            icon = Icons.Default.BarChart,
            caption = stringResource(R.string.onboarding_slide3_caption),
            description = stringResource(R.string.onboarding_slide3_description)
        ),
        OnboardingSlideData(
            icon = Icons.Default.Share,
            caption = stringResource(R.string.onboarding_slide4_caption),
            description = stringResource(R.string.onboarding_slide4_description)
        ),
        OnboardingSlideData(
            icon = Icons.Default.Analytics,
            caption = stringResource(R.string.onboarding_slide5_caption),
            description = stringResource(R.string.onboarding_slide5_description)
        )
    )

    val pagerState = rememberPagerState(pageCount = { slides.size })

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    OnboardingSlideView(slide = slides[page])
                }

                Row(
                    Modifier
                        .height(50.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(slides.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        }
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(8.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.button_onboarding_close))
                    }
                }
            }
        }
    }
}
