package ted.parchment.reader.ui.viewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import ted.parchment.reader.ui.theme.DesignTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Renders a single PDF page as a Compose [Image].
 *
 * Key design decisions:
 * - Renders at 2× native resolution to keep text crisp when zoomed
 * - Draws a white background before rendering (PDF pages are transparent)
 * - Applies a colour-inversion matrix in night mode for comfortable reading
 * - Uses synchronized access to [PdfRenderer] since it only allows one page open at a time
 * - Properly cancels and cleans up bitmap on dispose to prevent memory leaks
 */
@Composable
fun PdfPageRenderer(
    pdfRenderer: PdfRenderer,
    pageIndex: Int,
    isNightMode: Boolean,
    bgColor: Color
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val scope = rememberCoroutineScope()

    // Invert colours for night mode — keeps text readable, inverts white→charcoal
    val colorFilter: ColorFilter? = if (isNightMode) {
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    } else null

    DisposableEffect(pageIndex) {
        val job = scope.launch(Dispatchers.IO) {
            val rendered = synchronized(pdfRenderer) {
                runCatching {
                    if (pageIndex >= pdfRenderer.pageCount) return@runCatching null
                    val page = pdfRenderer.openPage(pageIndex)
                    // 2× resolution keeps text crisp when zoomed
                    val w = page.width * 2
                    val h = page.height * 2
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    android.graphics.Canvas(bmp).drawColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bmp
                }.getOrNull()
            }
            if (rendered != null) withContext(Dispatchers.Main) { bitmap = rendered }
        }
        onDispose {
            job.cancel()
            bitmap = null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Page ${pageIndex + 1}",
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .aspectRatio(bitmap!!.width.toFloat() / bitmap!!.height.toFloat()),
            contentScale = ContentScale.FillWidth,
            colorFilter = colorFilter
        )
    } else {
        // Per-page loading skeleton — matches the book's background tone
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(28.dp),
                color = if (isNightMode) DesignTokens.DarkAccent else DesignTokens.LightAccent
            )
        }
    }
}
