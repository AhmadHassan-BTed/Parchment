package ted.parchment.reader.ui.viewer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reading settings panel for display inside a ModalBottomSheet.
 *
 * Provides controls for reading preferences and a gesture reference guide.
 */
@Composable
fun SettingsPanel(
    isNightMode: Boolean,
    accentColor: Color,
    textColor: Color,
    onNightModeToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 52.dp)
    ) {
        Text(
            "Reading Settings",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = textColor,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        HorizontalDivider(color = textColor.copy(alpha = 0.10f))
        Spacer(Modifier.height(8.dp))

        // ── Night Mode Toggle ───────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isNightMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                    contentDescription = null,
                    tint = accentColor
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        "Night Mode",
                        color = textColor,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    )
                    Text(
                        "Reduce eye strain in dim light",
                        color = textColor.copy(alpha = 0.50f),
                        fontSize = 12.sp
                    )
                }
            }
            Switch(
                checked = isNightMode,
                onCheckedChange = { onNightModeToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accentColor,
                    checkedTrackColor = accentColor.copy(alpha = 0.30f)
                )
            )
        }

        HorizontalDivider(color = textColor.copy(alpha = 0.06f))
        Spacer(Modifier.height(16.dp))

        // ── Gesture Reference ───────────────────────────────────────────────
        Text(
            "GESTURES",
            color = textColor.copy(alpha = 0.40f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(10.dp))

        GestureTip("Tap screen", "Toggle reading controls", textColor)
        GestureTip("Pinch", "Zoom in or out", textColor)
        GestureTip("Double-tap", "Quick zoom toggle (1× / 2×)", textColor)
        GestureTip("Swipe right", "Open table of contents", textColor)
        GestureTip("Swipe left", "Open bookmarks", textColor)
    }
}

/**
 * A single row in the gesture reference list.
 */
@Composable
private fun GestureTip(gesture: String, description: String, textColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            gesture,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            description,
            color = textColor.copy(alpha = 0.50f),
            fontSize = 13.sp
        )
    }
}
