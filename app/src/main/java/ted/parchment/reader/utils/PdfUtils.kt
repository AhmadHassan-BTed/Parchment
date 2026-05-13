package ted.parchment.reader.utils

import android.content.Context
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Utility for preparing PDF assets for [android.graphics.pdf.PdfRenderer].
 *
 * PdfRenderer requires a [ParcelFileDescriptor] backed by a real file (not a stream),
 * so this utility copies bundled assets to the app's cache directory on first access.
 */
object PdfUtils {

    /**
     * Copies a PDF from the assets directory to the cache and returns a read-only
     * [ParcelFileDescriptor] suitable for [android.graphics.pdf.PdfRenderer].
     *
     * The cached copy is reused on subsequent calls for efficiency.
     *
     * @param context  Application or activity context
     * @param fileName Asset-relative filename (e.g. "Full_Book.pdf")
     * @return A read-only file descriptor, or `null` if the asset cannot be opened
     */
    suspend fun getPdfFileDescriptor(
        context: Context,
        fileName: String
    ): ParcelFileDescriptor? = withContext(Dispatchers.IO) {
        try {
            val file = File(context.cacheDir, fileName)
            if (!file.exists()) {
                context.assets.open(fileName).use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to open PDF asset: $fileName", e)
            null
        }
    }

    private const val TAG = "PdfUtils"
}
