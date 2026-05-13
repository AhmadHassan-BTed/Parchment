package com.example.pdfreader.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfUtils {

    /**
     * Copies a PDF asset to the cache directory and returns a ParcelFileDescriptor.
     * PdfRenderer requires a FileDescriptor to open a PDF.
     */
    suspend fun getPdfFileDescriptor(context: Context, fileName: String): ParcelFileDescriptor? = withContext(Dispatchers.IO) {
        try {
            val file = File(context.cacheDir, fileName)
            // Always overwrite for development purposes to ensure latest asset is used, 
            // or check existence. For now, check existence to be efficient.
            if (!file.exists()) {
                context.assets.open(fileName).use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
