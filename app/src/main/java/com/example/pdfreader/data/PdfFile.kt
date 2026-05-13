package com.example.pdfreader.data

/**
 * Data class representing a PDF File.
 * @param name Display name
 * @param fileName Asset file name
 */
data class PdfFile(
    val name: String,
    val path: String,
    val lastAccessed: Long
)
