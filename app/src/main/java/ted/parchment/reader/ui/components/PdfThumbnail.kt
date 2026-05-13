package ted.parchment.reader.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ted.parchment.reader.utils.PdfUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Asynchronously renders the first page of a PDF as a thumbnail.
 *
 * Falls back to an icon placeholder while loading or on error.
 * Properly manages PdfRenderer lifecycle to avoid memory leaks.
 *
 * @param fileName Asset-relative filename of the PDF
 */
@Composable
fun PdfThumbnail(fileName: String) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    DisposableEffect(fileName) {
        val job = scope.launch(Dispatchers.IO) {
            var pfd: ParcelFileDescriptor? = null
            var pdfRenderer: PdfRenderer? = null
            var page: PdfRenderer.Page? = null

            try {
                pfd = PdfUtils.getPdfFileDescriptor(context, fileName)

                if (pfd != null) {
                    pdfRenderer = PdfRenderer(pfd)

                    if (pdfRenderer.pageCount > 0) {
                        page = pdfRenderer.openPage(0)

                        val width = 300
                        val height = (width.toFloat() / page.width * page.height).toInt()
                        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        android.graphics.Canvas(bmp).drawColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        withContext(Dispatchers.Main) {
                            bitmap = bmp
                            isLoading = false
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { isLoading = false }
            } finally {
                try {
                    page?.close()
                    pdfRenderer?.close()
                    pfd?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        onDispose { job.cancel() }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}
