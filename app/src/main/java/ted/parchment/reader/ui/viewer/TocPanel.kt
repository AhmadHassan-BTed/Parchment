package ted.parchment.reader.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ted.parchment.reader.data.model.TocItem

/**
 * Full-height Table of Contents panel for display inside a ModalBottomSheet.
 *
 * Highlights the currently active chapter with an accent-colored indicator bar
 * and provides one-tap navigation to any chapter.
 */
@Composable
fun TocPanel(
    tableOfContents: List<TocItem>,
    currentPage: Int,
    accentColor: Color,
    textColor: Color,
    onChapterSelected: (TocItem) -> Unit
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
            Icon(Icons.Default.MenuBook, contentDescription = null, tint = accentColor)
            Spacer(Modifier.width(12.dp))
            Text(
                "Contents",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = textColor
            )
        }
        HorizontalDivider(color = textColor.copy(alpha = 0.10f))

        // ── Chapter List ────────────────────────────────────────────────────
        LazyColumn {
            items(tableOfContents) { chapter ->
                val isActive = chapter == tableOfContents
                    .filter { it.pageIndex < currentPage }
                    .lastOrNull()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isActive) accentColor.copy(alpha = 0.08f) else Color.Transparent)
                        .clickable { onChapterSelected(chapter) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Active chapter accent bar + title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(22.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(accentColor)
                            )
                            Spacer(Modifier.width(10.dp))
                        } else {
                            Spacer(Modifier.width(13.dp))
                        }
                        Text(
                            text = chapter.title,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) accentColor else textColor,
                            fontSize = 15.sp,
                            maxLines = 2,
                            lineHeight = 22.sp,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "p. ${chapter.pageIndex + 1}",
                        fontSize = 12.sp,
                        color = textColor.copy(alpha = 0.45f)
                    )
                }

                HorizontalDivider(
                    color = textColor.copy(alpha = 0.06f),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }
}
