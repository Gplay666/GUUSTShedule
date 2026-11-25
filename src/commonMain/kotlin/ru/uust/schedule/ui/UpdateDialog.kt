package ru.uust.schedule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ru.uust.schedule.update.UpdateInfo
import ru.uust.schedule.update.UpdateState
import kotlin.math.roundToInt

@Composable
fun UpdateDialog(
    state: UpdateState,
    darkTheme: Boolean,
    palette: ThemePalette,
    systemAccent: Color?,
) {
    val update = state.available ?: return
    if (!state.visible) return

    Dialog(onDismissRequest = state::dismiss) {
        UustScheduleTheme(
            darkTheme = darkTheme,
            palette = palette,
            systemAccent = systemAccent,
        ) {
            BoxWithConstraints {
                val compact = maxWidth < 400.dp
                val dialogMaxHeight = if (maxHeight.value.isFinite()) {
                    maxHeight * 0.94f
                } else {
                    maxHeight
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp)
                        .heightIn(max = dialogMaxHeight),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 0.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    ),
                ) {
                    Column {
                        UpdateHeader(
                            update = update,
                            onClose = state::dismiss,
                            compact = compact,
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 26.dp, vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            VersionRow(
                                currentVersion = state.currentVersion,
                                update = update,
                            )

                            if (state.isDownloading) {
                                UpdateProgress(state)
                            }

                            state.error?.let { message ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        Icon(
                                            Icons.Rounded.ErrorOutline,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Text(
                                            message,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }
                        }

                        UpdateActions(
                            state = state,
                            update = update,
                            compact = compact,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateActions(
    state: UpdateState,
    update: UpdateInfo,
    compact: Boolean,
) {
    if (compact) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 26.dp, end = 26.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            UpdateButton(
                state = state,
                update = update,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(
                onClick = state::skipCurrentVersion,
                enabled = !state.isDownloading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Пропустить эту версию",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 26.dp, end = 26.dp, bottom = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = state::skipCurrentVersion,
                enabled = !state.isDownloading,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 10.dp,
                ),
            ) {
                Text(
                    "Пропустить эту версию",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Spacer(Modifier.weight(1f))
            UpdateButton(state = state, update = update)
        }
    }
}

@Composable
private fun UpdateButton(
    state: UpdateState,
    update: UpdateInfo,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = state::install,
        enabled = !state.isDownloading,
        shape = CircleShape,
        modifier = modifier,
    ) {
        Icon(
            if (update.supportsOneClick) {
                Icons.Rounded.Download
            } else {
                Icons.Rounded.OpenInNew
            },
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            if (update.supportsOneClick) "Обновить" else "Открыть релиз",
        )
    }
}

@Composable
private fun UpdateHeader(
    update: UpdateInfo,
    onClose: () -> Unit,
    compact: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(
                        scheme.primaryContainer,
                        scheme.tertiaryContainer.copy(alpha = 0.8f),
                    ),
                ),
            )
            .padding(
                start = 24.dp,
                top = if (compact) 18.dp else 22.dp,
                end = if (compact) 10.dp else 14.dp,
                bottom = if (compact) 18.dp else 22.dp,
            ),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (!compact) {
                Surface(
                    shape = CircleShape,
                    color = scheme.onPrimaryContainer.copy(alpha = 0.10f),
                    modifier = Modifier.size(46.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.SystemUpdateAlt,
                            contentDescription = null,
                            tint = scheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    "Доступно обновление",
                    style = if (compact) {
                        MaterialTheme.typography.titleLarge
                    } else {
                        MaterialTheme.typography.headlineSmall
                    },
                    fontWeight = FontWeight.Black,
                    color = scheme.onPrimaryContainer,
                )
                Text(
                    "Расписание УУНиТ ${update.version}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
            Row(verticalAlignment = Alignment.Top) {
                IconButton(
                    onClick = onClose,
                    modifier = if (compact) Modifier.size(38.dp) else Modifier,
                ) {
                    Icon(
                        Icons.Rounded.ArrowBack,
                        contentDescription = "Назад",
                        tint = scheme.onPrimaryContainer,
                        modifier = if (compact) Modifier.size(21.dp) else Modifier,
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = if (compact) Modifier.size(38.dp) else Modifier,
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Закрыть",
                        tint = scheme.onPrimaryContainer,
                        modifier = if (compact) Modifier.size(21.dp) else Modifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun VersionRow(
    currentVersion: String,
    update: UpdateInfo,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        VersionBadge(
            label = currentVersion,
            container = scheme.surfaceContainerHighest,
            content = scheme.onSurfaceVariant,
        )
        Text(
            "→",
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onSurfaceVariant,
        )
        VersionBadge(
            label = update.version,
            container = scheme.primaryContainer,
            content = scheme.onPrimaryContainer,
        )
        Spacer(Modifier.weight(1f))
        val size = formatAssetSize(update.assetSize)
        if (size.isNotBlank()) {
            Text(
                size,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VersionBadge(
    label: String,
    container: Color,
    content: Color,
) {
    Surface(
        shape = CircleShape,
        color = container,
        contentColor = content,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun UpdateProgress(state: UpdateState) {
    val progress = state.progress
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (progress == null) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
            )
        } else {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            )
        }
        Text(
            buildString {
                append(state.statusMessage)
                if (progress != null) {
                    append(" ")
                    append((progress * 100).roundToInt().coerceIn(0, 100))
                    append("%")
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Changelog(markdown: String) {
    val blocks = remember(markdown) { parseChangelog(markdown) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (blocks.isEmpty()) {
            Text(
                "Список изменений доступен на странице релиза.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        blocks.forEach { block ->
            when (block) {
                is ChangelogBlock.Heading -> Text(
                    block.text,
                    style = if (block.level <= 1) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.titleSmall
                    },
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp),
                )

                is ChangelogBlock.Bullet -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 7.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Text(
                        block.text,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                }

                is ChangelogBlock.Paragraph -> Text(
                    block.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private sealed interface ChangelogBlock {
    data class Heading(val text: String, val level: Int) : ChangelogBlock
    data class Bullet(val text: String) : ChangelogBlock
    data class Paragraph(val text: String) : ChangelogBlock
}

private fun parseChangelog(markdown: String): List<ChangelogBlock> {
    val blocks = mutableListOf<ChangelogBlock>()
    val paragraph = StringBuilder()

    fun flush() {
        val text = paragraph.toString().trim()
        if (text.isNotEmpty()) blocks += ChangelogBlock.Paragraph(stripInlineMarkdown(text))
        paragraph.clear()
    }

    markdown.lines().forEach { raw ->
        val trimmed = raw.trim()
        when {
            trimmed.isEmpty() || trimmed.startsWith("```") || trimmed == "---" -> flush()

            trimmed.startsWith("#") -> {
                flush()
                val level = trimmed.takeWhile { it == '#' }.length
                val text = trimmed.drop(level).trim()
                if (text.isNotEmpty()) {
                    blocks += ChangelogBlock.Heading(stripInlineMarkdown(text), level)
                }
            }

            trimmed.startsWith("- ") ||
                trimmed.startsWith("* ") ||
                trimmed.startsWith("+ ") -> {
                flush()
                val text = trimmed.drop(2).trim()
                if (text.isNotEmpty()) blocks += ChangelogBlock.Bullet(stripInlineMarkdown(text))
            }

            Regex("^\\d+[.)]\\s+.*").matches(trimmed) -> {
                flush()
                val text = trimmed.replaceFirst(Regex("^\\d+[.)]\\s+"), "")
                if (text.isNotEmpty()) blocks += ChangelogBlock.Bullet(stripInlineMarkdown(text))
            }

            trimmed.startsWith(">") -> {
                flush()
                val text = trimmed.trimStart('>').trim()
                if (text.isNotEmpty()) blocks += ChangelogBlock.Paragraph(stripInlineMarkdown(text))
            }

            else -> {
                if (paragraph.isNotEmpty()) paragraph.append(' ')
                paragraph.append(trimmed)
            }
        }
    }
    flush()
    return blocks
}

private fun stripInlineMarkdown(text: String): String = text
    .replace(Regex("!\\[([^]]*)]\\([^)]*\\)"), "$1")
    .replace(Regex("\\[([^]]+)]\\([^)]*\\)"), "$1")
    .replace(Regex("`([^`]*)`"), "$1")
    .replace("**", "")
    .replace("__", "")
    .replace("~~", "")
    .replace(Regex("<[^>]+>"), "")
    .replace("&amp;", "&")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .trim()

private fun formatAssetSize(bytes: Long): String {
    if (bytes <= 0) return ""
    if (bytes < 1024 * 1024) return "${bytes / 1024} КБ"
    val tenths = ((bytes / (1024.0 * 1024.0)) * 10).roundToInt()
    return "${tenths / 10}.${tenths % 10} МБ"
}
