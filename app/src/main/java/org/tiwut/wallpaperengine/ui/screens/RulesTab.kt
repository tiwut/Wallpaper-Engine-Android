package org.tiwut.wallpaperengine.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import org.tiwut.wallpaperengine.data.TriggerType
import org.tiwut.wallpaperengine.data.WallpaperImage
import org.tiwut.wallpaperengine.data.WallpaperRule
import org.tiwut.wallpaperengine.viewmodel.WallpaperViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesTab(viewModel: WallpaperViewModel) {
    val rules by viewModel.rules.collectAsState()
    val images by viewModel.images.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (rules.isEmpty()) {
            Text(
                "No active rules. Tap + to create one.",
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val wallpaperManager = android.app.WallpaperManager.getInstance(context)
                val info = wallpaperManager.wallpaperInfo
                val isLiveWallpaperActive = info != null && info.packageName == context.packageName

                if (!isLiveWallpaperActive) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .clickable {
                                try {
                                    val intent = android.content.Intent(android.app.WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                                    intent.putExtra(android.app.WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, android.content.ComponentName(context, org.tiwut.wallpaperengine.service.LiveWallpaperService::class.java))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Engine Inactive", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Tap here to set Atmosphere as your system Live Wallpaper for smooth animations.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE TRIGGERS",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "${rules.count { it.isEnabled }} Enabled",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(rules) { rule ->
                        RuleCard(rule, 
                            images = images.filter { rule.imageIds.split(",").contains(it.id.toString()) },
                            onToggle = {
                                viewModel.toggleRule(rule.id, it)
                            }, onDelete = {
                                viewModel.deleteRule(rule.id)
                            }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            shape = androidx.compose.foundation.shape.CircleShape,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(64.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Rule", tint = MaterialTheme.colorScheme.onPrimary)
        }
    }

    if (showDialog) {
        if (images.isEmpty()) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("No Images") },
                text = { Text("Please add images to the Gallery first.") },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) { Text("OK") }
                }
            )
        } else {
            AddRuleDialog(
                images = images,
                onDismiss = { showDialog = false },
                onSave = { rule ->
                    viewModel.addRule(rule)
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun RuleCard(rule: WallpaperRule, images: List<WallpaperImage>, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (images.isNotEmpty()) {
                 Image(
                    painter = rememberAsyncImagePainter(File(images.first().localUri)),
                    contentDescription = "Wallpaper",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp))
                 )
            } else {
                 Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface))
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.triggerType.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (rule.triggerType == TriggerType.TIME) {
                    Text(
                        text = "Time: ${rule.triggerValue}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (rule.triggerType == TriggerType.SHAKE) {
                    Text("Randomize", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (rule.triggerType == TriggerType.UNLOCK) {
                    Text("On Wake", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Switch(
                checked = rule.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surface
                )
            )
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Rule", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRuleDialog(images: List<WallpaperImage>, onDismiss: () -> Unit, onSave: (WallpaperRule) -> Unit) {
    var selectedTrigger by remember { mutableStateOf(TriggerType.UNLOCK) }
    var timeValue by remember { mutableStateOf("08:00") }
    var selectedImageIds by remember { mutableStateOf(setOf<Int>(images.first().id)) }
    
    val animations = listOf("FADE", "SLIDE_LEFT", "SLIDE_RIGHT", "SLIDE_UP", "SLIDE_DOWN", "ZOOM", "WIPE", "DIAGONAL_WIPE", "CROSSFADE_ZOOM", "CIRCULAR_REVEAL", "FLIP_X", "FLIP_Y", "DRIFT", "SHRINK_GROW", "SPIN", "PANORAMA_360")
    var selectedAnimation by remember { mutableStateOf(animations[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Wallpaper Rule", color = MaterialTheme.colorScheme.onSurface) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Select Trigger:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 4.dp))
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TriggerType.values().forEach { type ->
                        FilterChip(
                            selected = type == selectedTrigger,
                            onClick = { selectedTrigger = type },
                            label = { Text(type.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                if (selectedTrigger == TriggerType.TIME) {
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = timeValue,
                        onValueChange = { timeValue = it },
                        label = { Text("Time (HH:mm)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text("Select Transition:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 4.dp))
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    animations.forEach { anim ->
                        FilterChip(
                            selected = anim == selectedAnimation,
                            onClick = { selectedAnimation = anim },
                            label = { Text(anim) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("Select Images (for rotation):", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(images) { img ->
                        val isSelected = selectedImageIds.contains(img.id)
                        Image(
                            painter = rememberAsyncImagePainter(File(img.localUri)),
                            contentDescription = "Image Selection",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { 
                                    selectedImageIds = if (isSelected && selectedImageIds.size > 1) {
                                        selectedImageIds - img.id
                                    } else {
                                        selectedImageIds + img.id
                                    }
                                }
                                .then(
                                    if (isSelected) Modifier.border(4.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
                                    else Modifier
                                )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        WallpaperRule(
                            triggerType = selectedTrigger,
                            triggerValue = timeValue,
                            imageIds = selectedImageIds.joinToString(","),
                            transitionAnimation = selectedAnimation
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) { Text("Cancel") }
        }
    )
}
