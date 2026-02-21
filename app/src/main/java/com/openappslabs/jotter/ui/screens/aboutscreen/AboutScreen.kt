/*
 * Copyright (c) 2026 Open Apps Labs
 *
 * This file is part of Jotter
 *
 * Jotter is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Jotter is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Jotter.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package com.openappslabs.jotter.ui.screens.aboutscreen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.openappslabs.jotter.BuildConfig
import com.openappslabs.jotter.R
import com.openappslabs.jotter.ui.theme.rememberJotterHaptics
import java.time.Year
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val haptics = rememberJotterHaptics()
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val primary = MaterialTheme.colorScheme.primary
    val iconBackgroundBrush = remember(primaryContainer, primary) {
        Brush.linearGradient(
            colors = listOf(primaryContainer, primary.copy(alpha = 0.1f))
        )
    }
    val copyrightText = remember {
        val year = Year.now().toString()
        "Made with ❤️ | © $year Open Apps Labs"
    }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    Surface(
                        onClick = {
                            haptics.click()
                            onBackClick()
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp,
                tonalElevation = 8.dp,
                modifier = Modifier.size(128.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.background(iconBackgroundBrush)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.app_icon),
                        contentDescription = "App Icon",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(72.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Jotter",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Simple. Secure. Open.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    AboutItem(label = "Version", value = BuildConfig.VERSION_NAME)
                    AboutDivider()
                    AboutItem(label = "Developer", value = "Open Apps Labs")
                    AboutDivider()
                    AboutItem(label = "License", value = "GNU GPL v3.0")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column {
                    ActionItem(
                        icon = Icons.Outlined.Code,
                        label = "Source Code",
                        onClick = { uriHandler.openUri("https://github.com/openappslabs/Jotter") }
                    )
                    AboutDivider()
                    ActionItem(
                        icon = Icons.Outlined.Gavel,
                        label = "View License",
                        onClick = { uriHandler.openUri("https://www.gnu.org/licenses/gpl-3.0.en.html") }
                    )
                    AboutDivider()
                    ActionItem(
                        icon = Icons.Outlined.Email,
                        label = "Support",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = "mailto:openappslabs@gmail.com".toUri()
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = copyrightText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 16.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AboutItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    val haptics = rememberJotterHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptics.tick()
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun AboutDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    )
}