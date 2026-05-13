package ted.parchment.reader.ui.viewer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Modal dialog for jumping to a specific page number.
 *
 * Validates input to ensure only valid page numbers within the book's range
 * are accepted. Uses a number keyboard for optimal mobile input.
 */
@Composable
fun PageJumpDialog(
    pageCount: Int,
    surfaceColor: Color,
    textColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onPageSelected: (Int) -> Unit
) {
    var inputVal by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                "Go to Page",
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        },
        text = {
            Column {
                Text(
                    "Enter a number between 1 and $pageCount",
                    fontSize = 13.sp,
                    color = textColor.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = inputVal,
                    onValueChange = { if (it.all { char -> char.isDigit() }) inputVal = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = textColor.copy(alpha = 0.2f),
                        cursorColor = accentColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val targetPage = inputVal.toIntOrNull()
                    if (targetPage != null && targetPage in 1..pageCount) {
                        onPageSelected(targetPage)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Go", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textColor.copy(alpha = 0.5f))
            }
        }
    )
}
