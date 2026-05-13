package ted.parchment.reader.ui.viewer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ted.parchment.reader.data.model.TocItem

/**
 * Bookmarks panel for display inside a ModalBottomSheet.
 *
 * Shows all saved bookmarks with their associated chapter context,
 * and supports one-tap navigation and individual bookmark removal.
 */
@Composable
fun BookmarksPanel(
    bookmarks: List<Int>,
    tableOfContents: List<TocItem>,
    accentColor: Color,
    textColor: Color,
    onBookmarkSelected: (Int) -> Unit,
    onBookmarkRemoved: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 40.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Bookmark, contentDescription = null, tint = accentColor)
            Spacer(Modifier.width(12.dp))
            Text(
                "Bookmarks",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = textColor
            )
        }
        HorizontalDivider(color = textColor.copy(alpha = 0.10f))

        // ── Content ─────────────────────────────────────────────────────────
        if (bookmarks.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.Bookmark,
                        contentDescription = null,
                        tint = textColor.copy(alpha = 0.25f),
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "No bookmarks yet.\nTap the bookmark icon while reading.",
                        color = textColor.copy(alpha = 0.40f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }
        } else {
            LazyColumn {
                items(bookmarks) { pageIndex ->
                    val chapter = tableOfContents
                        .filter { it.pageIndex <= pageIndex }
                        .lastOrNull()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBookmarkSelected(pageIndex) }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Page ${pageIndex + 1}",
                                color = textColor,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp
                            )
                            chapter?.let {
                                Text(it.title, fontSize = 12.sp, color = accentColor)
                            }
                        }
                        IconButton(onClick = { onBookmarkRemoved(pageIndex) }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove bookmark",
                                tint = textColor.copy(alpha = 0.45f)
                            )
                        }
                    }
                    HorizontalDivider(
                        color = textColor.copy(alpha = 0.06f),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }
    }
}
