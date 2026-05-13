package com.example.pdfreader.ui

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pdfreader.utils.PdfUtils
import kotlinx.coroutines.*
import java.io.IOException

// ─────────────────────────────────────────────────────────────────────────────
// DESIGN TOKENS — warm paper aesthetic, premium feel
// ─────────────────────────────────────────────────────────────────────────────

// Light mode — "printed paper" warmth
private val LightBackground  = Color(0xFFF8F1E4)
private val LightText        = Color(0xFF222222)
private val LightAccent      = Color(0xFF8B6914)   // warm amber-gold
private val LightSurface     = Color(0xFFF0E8D8)
private val LightHud         = Color(0xEEF0E8D8)   // 93 % opaque

// Dark mode — charcoal, NOT pure black
private val DarkBackground   = Color(0xFF1C1C1E)
private val DarkText         = Color(0xFFEDE6D6)
private val DarkAccent       = Color(0xFFC4974A)
private val DarkSurface      = Color(0xFF2C2C2E)
private val DarkHud          = Color(0xEE2C2C2E)

// Timing
private const val HUD_AUTO_HIDE_MS   = 4_000L
private const val CHAPTER_OVERLAY_MS = 2_000L
private const val SWIPE_THRESHOLD    = 80f

// ─────────────────────────────────────────────────────────────────────────────
// DATA MODELS
// ─────────────────────────────────────────────────────────────────────────────

/** Maps a chapter title to its 0-based page index in the PDF. */
data class TocItem(val title: String, val pageIndex: Int)

// ─────────────────────────────────────────────────────────────────────────────
// READING PREFERENCES  (SharedPreferences wrapper — no Room dependency needed)
// ─────────────────────────────────────────────────────────────────────────────

private class ReadingPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("reading_prefs", Context.MODE_PRIVATE)

    fun saveScrollPosition(fileName: String, index: Int) =
        prefs.edit().putInt("pos_$fileName", index).apply()

    fun getScrollPosition(fileName: String): Int =
        prefs.getInt("pos_$fileName", 0)

    fun saveNightMode(enabled: Boolean) =
        prefs.edit().putBoolean("night_mode", enabled).apply()

    fun getNightMode(): Boolean =
        prefs.getBoolean("night_mode", false)

    fun getBookmarks(fileName: String): Set<Int> {
        val raw = prefs.getString("bm_$fileName", "") ?: ""
        return if (raw.isEmpty()) emptySet()
        else raw.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun saveBookmarks(fileName: String, bookmarks: Set<Int>) =
        prefs.edit()
            .putString("bm_$fileName", bookmarks.joinToString(","))
            .apply()
}

// ─────────────────────────────────────────────────────────────────────────────
// MAIN SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(fileName: String) {
    val context  = LocalContext.current
    val scope    = rememberCoroutineScope()
    val haptic   = LocalHapticFeedback.current
    val prefs    = remember { ReadingPreferences(context) }

    var showJumpDialog by remember { mutableStateOf(false) }

    // ── PDF engine ──────────────────────────────────────────────────────────
    var pdfRenderer   by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pageCount     by remember { mutableStateOf(0) }
    var isLoading     by remember { mutableStateOf(true) }

    // ── Theme state ─────────────────────────────────────────────────────────
    var isNightMode by remember { mutableStateOf(prefs.getNightMode()) }

    // ── HUD visibility & auto-hide ──────────────────────────────────────────
    var showHud        by remember { mutableStateOf(true) }
    var autoHideJob    by remember { mutableStateOf<Job?>(null) }

    /** Re-starts the 4-second auto-hide countdown. Call on every user interaction. */
    fun resetAutoHide() {
        autoHideJob?.cancel()
        if (showHud) {
            autoHideJob = scope.launch {
                delay(HUD_AUTO_HIDE_MS)
                showHud = false
            }
        }
    }

    // ── Panel visibility ────────────────────────────────────────────────────
    var showTocSheet       by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showSettingsSheet  by remember { mutableStateOf(false) }

    // ── Bookmarks ───────────────────────────────────────────────────────────
    val bookmarks = remember {
        mutableStateListOf<Int>().also { it.addAll(prefs.getBookmarks(fileName)) }
    }

    // ── Scroll & zoom ───────────────────────────────────────────────────────
    val listState = rememberLazyListState()
    var scale  by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // ── Chapter-change toast overlay ────────────────────────────────────────
    var showChapterOverlay  by remember { mutableStateOf(false) }
    var chapterOverlayTitle by remember { mutableStateOf("") }

    // ── Derived state ───────────────────────────────────────────────────────
    /** 1-based current page number, updates every scroll tick. */
    val currentPage by remember {
        derivedStateOf { listState.firstVisibleItemIndex + 1 }
    }

    // ── Hardcoded TOC — adjust pageIndex values to match your PDF ──────────
    val tableOfContents = remember {
        listOf(
            TocItem("Durood-e-Ibrahimi\nدرودِ ابراہیمی",                pageIndex = 283),
            TocItem("Durood-e-Jilani\nدرودِ جیلانی",                  pageIndex = 283),
            TocItem("Durood-e-Taj\nدرودِ تاج",                        pageIndex = 284),
            TocItem("Durood-e-Imam Shafi'i\nدرودِ امام شافعی",        pageIndex = 284),
            TocItem("Durood-e-Mohi\nدرودِ ماہی",                      pageIndex = 284),
            TocItem("Durood-e-Jal Shukrat\nدرودِ جَلّ شُکرَت",          pageIndex = 285),
            TocItem("Salat-o-Salam\nصلوٰۃ و سلام",                   pageIndex = 285),
            TocItem("Durood-e-Khidr\nدرودِ خضری",                    pageIndex = 285),
            TocItem("Durood-e-Habib Rasool\nدرودِ حبیبِ رسول",        pageIndex = 286),
            TocItem("Durood-e-Tahir\nدرودِ طاہر",                     pageIndex = 286),
            TocItem("Durood-e-Azmat\nدرودِ عظمت",                    pageIndex = 286),
            TocItem("Durood-e-Ghausia\nدرودِ غوثیہ",                  pageIndex = 287),
            TocItem("Durood-e-Tunaji\nدرودِ تنجی",                    pageIndex = 287),
            TocItem("Durood-e-Shifa Quloob\nدرودِ شفاء القلوب",       pageIndex = 288),
            TocItem("Durood-e-Qurani\nدرودِ قرآنی",                  pageIndex = 289),
            TocItem("Durood-e-Khaas\nدرودِ خاص",                     pageIndex = 290),
            TocItem("Durood-e-Sa'adat\nدرودِ سعادت",                 pageIndex = 290),
            TocItem("Durood-e-Aali Qadr\nدرودِ عالی قدر",             pageIndex = 291),
            TocItem("Durood-e-Didar\nدرودِ دیدار",                   pageIndex = 292),
            TocItem("Durood-e-Habib\nدرودِ حبیب",                    pageIndex = 292),
            TocItem("Durood-e-Noor\nدرودِ نور",                      pageIndex = 293),
            TocItem("Aik Mukammal Durood Shareef\nایک مکمل درود شریف", pageIndex = 293),
            TocItem("Durood-e-Uloom wa Asrar\nدرودِ علوم و اسرار",    pageIndex = 296),
            TocItem("Durood-e-Ruhi ya Bazurgi\nدرودِ روحی یا بزرگی",  pageIndex = 297),
            TocItem("Durood-e-Tanjeena\nدرودِ تنجینا",                pageIndex = 299),
            TocItem("Durood-e-Taj (Variant)\nدرودِ تاج",              pageIndex = 300),
            TocItem("Durood-e-Aali\nدرودِ عالی",                     pageIndex = 303),
            TocItem("Asma-e-Mubarak\nاسماءِ مبارک",                  pageIndex = 306),
            TocItem("Huzoor ke Naam-e-Mubarak\nحضورﷺ کے نامِ مبارک",  pageIndex = 307),
            TocItem("Iraad-o-Dua Nafl\nاوراد و دعا نفل",              pageIndex = 309)
        )
    }

    /** The TOC entry whose chapter the reader is currently inside. */
    val currentChapter by remember {
        derivedStateOf {
            tableOfContents
                .filter { it.pageIndex < currentPage }
                .lastOrNull() ?: tableOfContents.first()
        }
    }

    // ── Chapter-change side effects ─────────────────────────────────────────
    var previousChapter by remember { mutableStateOf<TocItem?>(null) }
    LaunchedEffect(currentChapter) {
        if (previousChapter != null && previousChapter != currentChapter) {
            // Subtle haptic pulse on chapter change
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            // Brief chapter name overlay
            chapterOverlayTitle = currentChapter.title
            showChapterOverlay = true
            delay(CHAPTER_OVERLAY_MS)
            showChapterOverlay = false
        }
        previousChapter = currentChapter
    }

    // ── Persist scroll position on every page change ─────────────────────
    LaunchedEffect(currentPage) {
        prefs.saveScrollPosition(fileName, listState.firstVisibleItemIndex)
    }

    // ── Persist night mode preference ────────────────────────────────────
    LaunchedEffect(isNightMode) {
        prefs.saveNightMode(isNightMode)
    }

    // ── Load PDF and restore reading position ────────────────────────────
    LaunchedEffect(fileName) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val pfd = PdfUtils.getPdfFileDescriptor(context, fileName)
            if (pfd != null) {
                fileDescriptor = pfd
                try {
                    pdfRenderer = PdfRenderer(pfd)
                    pageCount   = pdfRenderer?.pageCount ?: 0
                } catch (e: IOException) { e.printStackTrace() }
            }
        }
        isLoading = false
        // Restore exact saved position with a small delay for layout
        val savedPos = prefs.getScrollPosition(fileName)
        if (savedPos > 0) {
            delay(200)
            listState.scrollToItem(savedPos)
        }
        resetAutoHide()
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                pdfRenderer?.close()
                fileDescriptor?.close()
            }
        }
    }

    // ── Resolved color tokens ────────────────────────────────────────────
    val bgColor      = if (isNightMode) DarkBackground else LightBackground
    val textColor    = if (isNightMode) DarkText       else LightText
    val accentColor  = if (isNightMode) DarkAccent     else LightAccent
    val surfaceColor = if (isNightMode) DarkSurface    else LightSurface
    val hudColor     = if (isNightMode) DarkHud        else LightHud

    // ── Bottom sheet states ──────────────────────────────────────────────
    val tocSheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val bookmarksSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val settingsSheetState  = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ────────────────────────────────────────────────────────────────────────
    // ROOT LAYOUT
    // ────────────────────────────────────────────────────────────────────────
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        val screenWidth  = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()

        // ── applyZoom — clamps scale and re-constrains offset ────────────────
        fun applyZoom(newScale: Float) {
            scale  = newScale.coerceIn(1f, 5f)
            val maxX = ((screenWidth  * scale) - screenWidth)  / 2f
            val maxY = ((screenHeight * scale) - screenHeight) / 2f
            offset = Offset(
                x = offset.x.coerceIn(-maxX.coerceAtLeast(0f), maxX.coerceAtLeast(0f)),
                y = offset.y.coerceIn(-maxY.coerceAtLeast(0f), maxY.coerceAtLeast(0f))
            )
        }

        // ── Pinch-to-zoom transform state ────────────────────────────────
        val transformState = rememberTransformableState { zoomChange, panChange, _ ->
            val newScale = (scale * zoomChange).coerceIn(1f, 5f)
            scale = newScale

            val maxX = ((screenWidth  * scale) - screenWidth)  / 2f
            val maxY = ((screenHeight * scale) - screenHeight) / 2f
            offset = Offset(
                x = (offset.x + panChange.x * scale).coerceIn(-maxX.coerceAtLeast(0f), maxX.coerceAtLeast(0f)),
                y = (offset.y + panChange.y * scale).coerceIn(-maxY.coerceAtLeast(0f), maxY.coerceAtLeast(0f))
            )
        }

        // ── 1. READING AREA ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .transformable(state = transformState)
                // Tap gestures: toggle HUD, double-tap to zoom reset
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            // Toggles the HUD menus regardless of current zoom level
                            showHud = !showHud
                            if (showHud) resetAutoHide()
                        },
                        onDoubleTap = {
                            scope.launch {
                                // Toggle between 1× and 2×
                                applyZoom(if (scale > 1f) 1f else 2f)
                                offset = Offset.Zero
                            }
                        }
                    )
                }
                // Swipe gestures for panels — ONLY active at 1× zoom.
                // Keyed on `scale`: when scale != 1f the block exits immediately
                // without calling any detector, so transformable wins the gesture
                // arena and panning works correctly while zoomed in.
                .pointerInput(scale) {
                    if (scale != 1f) return@pointerInput   // yield to transformable
                    detectHorizontalDragGestures { _, dragAmount ->
                        when {
                            dragAmount >  SWIPE_THRESHOLD -> {
                                if (!showTocSheet) { showTocSheet = true; showHud = false }
                            }
                            dragAmount < -SWIPE_THRESHOLD -> {
                                if (!showBookmarksSheet) { showBookmarksSheet = true; showHud = false }
                            }
                        }
                    }
                }
        ) {
            // ── Loading state ────────────────────────────────────────────
            if (isLoading) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = accentColor, strokeWidth = 2.dp)
                    Text("Opening book…", color = textColor.copy(alpha = 0.6f), fontSize = 14.sp)
                }

                // ── Page list ────────────────────────────────────────────────
            } else if (pdfRenderer != null) {
                LazyColumn(
                    state       = listState,
                    // Lock vertical scrolling while pinch-zoomed — pan handles it
                    userScrollEnabled = (scale == 1f),
                    modifier    = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX      = scale
                            scaleY      = scale
                            translationX = offset.x
                            translationY = offset.y
                        },
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    contentPadding = PaddingValues(
                        top    = if (showHud) 72.dp else 0.dp,
                        bottom = if (showHud) 96.dp else 0.dp
                    )
                ) {
                    items(pageCount) { index ->
                        PdfPage(
                            pdfRenderer = pdfRenderer!!,
                            pageIndex   = index,
                            isNightMode = isNightMode,
                            bgColor     = bgColor
                        )
                    }
                }

                // ── Error state ──────────────────────────────────────────────
            } else {
                Text(
                    "Could not open book.",
                    modifier = Modifier.align(Alignment.Center),
                    color    = MaterialTheme.colorScheme.error
                )
            }
        }

        // ── 2. TOP HUD ───────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = showHud,
            enter    = slideInVertically(tween(220)) { -it } + fadeIn(tween(220)),
            exit     = slideOutVertically(tween(220)) { -it } + fadeOut(tween(220)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 6.dp)
                    .background(hudColor)
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                IconButton(onClick = { /* pop back stack */ }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
                }

                // Book + chapter title (centred, fills available space)
                Column(
                    modifier            = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text       = "Full Book",
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 15.sp,
                        color      = textColor,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Text(
                        text       = currentChapter.title,
                        fontSize   = 11.sp,
                        color      = accentColor,
                        maxLines   = 2,                        // Changed from 1
                        textAlign  = TextAlign.Center,         // Added so both lines center align
                        lineHeight = 14.sp,                    // Added for breathing room
                        overflow   = TextOverflow.Ellipsis
                    )
                }

                // Bookmark current page
                val isBookmarked = bookmarks.contains(currentPage - 1)
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isBookmarked) bookmarks.remove(currentPage - 1)
                    else             bookmarks.add(currentPage - 1)
                    prefs.saveBookmarks(fileName, bookmarks.toSet())
                    resetAutoHide()
                }) {
                    Icon(
                        imageVector     = if (isBookmarked) Icons.Default.Bookmark else Icons.Outlined.Bookmark,
                        contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
                        tint            = if (isBookmarked) accentColor else textColor
                    )
                }

                // Night-mode toggle
                IconButton(onClick = { isNightMode = !isNightMode; resetAutoHide() }) {
                    Icon(
                        imageVector        = if (isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle night mode",
                        tint               = textColor
                    )
                }

                // Settings / more
                IconButton(onClick = { showSettingsSheet = true; resetAutoHide() }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Settings", tint = textColor)
                }
            }
        }

        // ── 3. BOTTOM HUD ────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = showHud,
            enter    = slideInVertically(tween(220)) { it }  + fadeIn(tween(220)),
            exit     = slideOutVertically(tween(220)) { it } + fadeOut(tween(220)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 8.dp)
                    .background(hudColor)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Meta row: TOC icon | chapter name | page counter
                Row(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment   = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick  = { showTocSheet = true; resetAutoHide() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.FormatListBulleted, contentDescription = "Table of Contents", tint = textColor)
                    }

                    Text(
                        text      = currentChapter.title,
                        fontSize  = 11.sp,
                        color     = accentColor,
                        maxLines  = 2,                        // Changed from 1
                        lineHeight = 14.sp,                   // Added for breathing room
                        overflow  = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            // A subtle background defines the "tapping area" for better UX
                            .background(
                                if (isNightMode) Color.White.copy(alpha = 0.1f)
                                else Color.Black.copy(alpha = 0.05f)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showJumpDialog = true
                                resetAutoHide()
                            }
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                    ) {
                        Text(
                            text       = "$currentPage / $pageCount",
                            fontSize   = 12.sp,
                            // Ensures the text is pure white in night mode for maximum contrast
                            color      = if (isNightMode) Color.White else textColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Progress scrubber
                if (pageCount > 0) {
                    Slider(
                        value         = currentPage.toFloat(),
                        onValueChange = { newPage ->
                            scope.launch {
                                listState.scrollToItem((newPage.toInt() - 1).coerceAtLeast(0))
                            }
                            resetAutoHide()
                        },
                        valueRange = 1f..pageCount.toFloat(),
                        colors     = SliderDefaults.colors(
                            thumbColor        = accentColor,
                            activeTrackColor  = accentColor,
                            inactiveTrackColor = accentColor.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                    )
                }
            }
        }

        // ── 4. CHAPTER-CHANGE OVERLAY (center toast) ─────────────────────
        AnimatedVisibility(
            visible  = showChapterOverlay,
            enter    = fadeIn(tween(300))  + scaleIn(tween(300), initialScale = 0.88f),
            exit     = fadeOut(tween(500)) + scaleOut(tween(500), targetScale = 0.88f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(surfaceColor.copy(alpha = 0.95f))
                    .padding(horizontal = 28.dp, vertical = 16.dp)
            ) {
                Text(
                    text       = chapterOverlayTitle,
                    color      = accentColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 16.sp
                )
            }
        }

        // ── 5. ZOOM BAR — hides/shows with HUD, right edge ───────────────
        AnimatedVisibility(
            visible  = showHud,
            enter    = slideInHorizontally(tween(220)) { it } + fadeIn(tween(220)),
            exit     = slideOutHorizontally(tween(220)) { it } + fadeOut(tween(220)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(
                    end    = 10.dp,
                    top    = 72.dp,   // clear top HUD
                    bottom = 100.dp   // clear bottom HUD
                )
        ) {
            VerticalZoomBar(
                scale         = scale,
                onScaleChange = { applyZoom(it) },
                accentColor   = accentColor,
                surfaceColor  = surfaceColor,
                textColor     = textColor
            )
        }
    }   // end BoxWithConstraints

    // ── TOC BOTTOM SHEET ─────────────────────────────────────────────────
    if (showTocSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTocSheet = false },
            sheetState       = tocSheetState,
            containerColor   = surfaceColor
        ) {
            TocPanel(
                tableOfContents  = tableOfContents,
                currentPage      = currentPage,
                accentColor      = accentColor,
                textColor        = textColor,
                onChapterSelected = { chapter ->
                    scope.launch {
                        showTocSheet = false
                        tocSheetState.hide()
                        listState.animateScrollToItem(chapter.pageIndex)
                    }
                }
            )
        }
    }

    // ── BOOKMARKS BOTTOM SHEET ───────────────────────────────────────────
    if (showBookmarksSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBookmarksSheet = false },
            sheetState       = bookmarksSheetState,
            containerColor   = surfaceColor
        ) {
            BookmarksPanel(
                bookmarks        = bookmarks.sorted(),
                tableOfContents  = tableOfContents,
                accentColor      = accentColor,
                textColor        = textColor,
                onBookmarkSelected = { pageIndex ->
                    scope.launch {
                        showBookmarksSheet = false
                        bookmarksSheetState.hide()
                        listState.animateScrollToItem(pageIndex)
                    }
                },
                onBookmarkRemoved = { pageIndex ->
                    bookmarks.remove(pageIndex)
                    prefs.saveBookmarks(fileName, bookmarks.toSet())
                }
            )
        }
    }

    // ── SETTINGS BOTTOM SHEET ────────────────────────────────────────────
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState       = settingsSheetState,
            containerColor   = surfaceColor
        ) {
            SettingsPanel(
                isNightMode      = isNightMode,
                accentColor      = accentColor,
                textColor        = textColor,
                onNightModeToggle = { isNightMode = !isNightMode }
            )
        }
    }

    // ── PAGE JUMP DIALOG ──────────────────────────────────────────────────
    if (showJumpDialog) {
        var inputVal by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
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
                            scope.launch {
                                listState.scrollToItem(targetPage - 1)
                                showJumpDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Go", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) {
                    Text("Cancel", color = textColor.copy(alpha = 0.5f))
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TABLE OF CONTENTS PANEL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TocPanel(
    tableOfContents  : List<TocItem>,
    currentPage      : Int,
    accentColor      : Color,
    textColor        : Color,
    onChapterSelected: (TocItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 40.dp)
    ) {
        // Header
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.MenuBook, contentDescription = null, tint = accentColor)
            Spacer(Modifier.width(12.dp))
            Text(
                "Contents",
                fontWeight = FontWeight.Bold,
                fontSize   = 20.sp,
                color      = textColor
            )
        }
        HorizontalDivider(color = textColor.copy(alpha = 0.10f))

        LazyColumn {
            items(tableOfContents) { chapter ->
                // This entry is "active" if it is the last chapter before the current page
                val isActive = chapter == tableOfContents
                    .filter { it.pageIndex < currentPage }
                    .lastOrNull()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isActive) accentColor.copy(alpha = 0.08f) else Color.Transparent)
                        .clickable { onChapterSelected(chapter) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment   = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Active chapter accent bar + title
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
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
                            text       = chapter.title,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color      = if (isActive) accentColor else textColor,
                            fontSize   = 15.sp,
                            maxLines   = 2,                   // Changed from 1
                            lineHeight = 22.sp,               // Added for clear separation
                            overflow   = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text    = "p. ${chapter.pageIndex + 1}",
                        fontSize = 12.sp,
                        color   = textColor.copy(alpha = 0.45f)
                    )
                }

                HorizontalDivider(color = textColor.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 20.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BOOKMARKS PANEL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BookmarksPanel(
    bookmarks        : List<Int>,
    tableOfContents  : List<TocItem>,
    accentColor      : Color,
    textColor        : Color,
    onBookmarkSelected: (Int) -> Unit,
    onBookmarkRemoved : (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 40.dp)
    ) {
        // Header
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Bookmark, contentDescription = null, tint = accentColor)
            Spacer(Modifier.width(12.dp))
            Text(
                "Bookmarks",
                fontWeight = FontWeight.Bold,
                fontSize   = 20.sp,
                color      = textColor
            )
        }
        HorizontalDivider(color = textColor.copy(alpha = 0.10f))

        if (bookmarks.isEmpty()) {
            // Empty state
            Box(
                modifier        = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.Bookmark,
                        contentDescription = null,
                        tint     = textColor.copy(alpha = 0.25f),
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "No bookmarks yet.\nTap the bookmark icon while reading.",
                        color     = textColor.copy(alpha = 0.40f),
                        fontSize  = 14.sp,
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
                        verticalAlignment   = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Page ${pageIndex + 1}",
                                color      = textColor,
                                fontWeight = FontWeight.Medium,
                                fontSize   = 15.sp
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
                    HorizontalDivider(color = textColor.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 20.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SETTINGS PANEL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsPanel(
    isNightMode      : Boolean,
    accentColor      : Color,
    textColor        : Color,
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
            fontSize   = 20.sp,
            color      = textColor,
            modifier   = Modifier.padding(vertical = 16.dp)
        )
        HorizontalDivider(color = textColor.copy(alpha = 0.10f))
        Spacer(Modifier.height(8.dp))

        // Night Mode row
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = if (isNightMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                    contentDescription = null,
                    tint               = accentColor
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Night Mode", color = textColor, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Text("Reduce eye strain in dim light", color = textColor.copy(alpha = 0.50f), fontSize = 12.sp)
                }
            }
            Switch(
                checked         = isNightMode,
                onCheckedChange = { onNightModeToggle() },
                colors          = SwitchDefaults.colors(
                    checkedThumbColor  = accentColor,
                    checkedTrackColor  = accentColor.copy(alpha = 0.30f)
                )
            )
        }

        HorizontalDivider(color = textColor.copy(alpha = 0.06f))
        Spacer(Modifier.height(16.dp))

        // Gesture reference
        Text(
            "GESTURES",
            color      = textColor.copy(alpha = 0.40f),
            fontSize   = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(10.dp))

        GestureTip("Tap screen",        "Toggle reading controls",     textColor)
        GestureTip("Pinch",             "Zoom in or out",              textColor)
        GestureTip("Double-tap",        "Quick zoom toggle (1× / 2×)", textColor)
        GestureTip("Swipe right",       "Open table of contents",      textColor)
        GestureTip("Swipe left",        "Open bookmarks",              textColor)
    }
}

/** A single row in the gesture reference list. */
@Composable
private fun GestureTip(gesture: String, description: String, textColor: Color) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(gesture,     color = textColor,                  fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text(description, color = textColor.copy(alpha = 0.50f), fontSize = 13.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// VERTICAL ZOOM BAR  — persistent right-edge zoom control
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A pill-shaped vertical slider always visible on the right edge of the screen.
 *
 *  ┌────┐
 *  │ +  │  ← tap to step zoom in  (+0.25×)
 *  │    │
 *  │ ●  │  ← draggable thumb  (top = 5×, bottom = 1×)
 *  │    │
 *  │ −  │  ← tap to step zoom out (−0.25×)
 *  │100%│  ← live zoom label
 *  └────┘
 */
@Composable
private fun VerticalZoomBar(
    scale        : Float,
    onScaleChange: (Float) -> Unit,
    accentColor  : Color,
    surfaceColor : Color,
    textColor    : Color
) {
    val minZoom = 1f
    val maxZoom = 5f
    val step    = 0.25f
    val density = LocalDensity.current

    val currentScale by rememberUpdatedState(scale)
    val currentOnScaleChange by rememberUpdatedState(onScaleChange)

    var trackHeightPx by remember { mutableIntStateOf(1) }

    val fraction = ((scale - minZoom) / (maxZoom - minZoom)).coerceIn(0f, 1f)

    // Calculate a safe margin so the thumb stays far away from the buttons
    val verticalMarginDp = 12.dp
    val verticalMarginPx = with(density) { verticalMarginDp.toPx() }

    Column(
        modifier = Modifier
            .shadow(10.dp, RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            .background(surfaceColor.copy(alpha = 0.94f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .padding(horizontal = 10.dp, vertical = 12.dp)
            .width(44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        // ── Zoom-in button ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accentColor.copy(alpha = if (scale < maxZoom) 0.15f else 0.05f))
                .clickable(enabled = scale < maxZoom) {
                    onScaleChange((scale + step).coerceAtMost(maxZoom))
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "+",
                color      = if (scale < maxZoom) accentColor else textColor.copy(alpha = 0.25f),
                fontWeight = FontWeight.Bold,
                fontSize   = 18.sp,
                lineHeight = 18.sp
            )
        }

        // ── Draggable track ───────────────────────────────────────────────
        val trackColor  = textColor.copy(alpha = 0.12f)
        val filledColor = accentColor.copy(alpha = 0.35f)
        val thumbColor  = accentColor
        val thumbGlow   = accentColor.copy(alpha = 0.20f)

        Canvas(
            modifier = Modifier
                .width(24.dp)
                .height(180.dp)
                .onSizeChanged { trackHeightPx = it.height }
                .pointerInput(trackHeightPx, minZoom, maxZoom, verticalMarginPx) {
                    @Suppress("DEPRECATION")
                    forEachGesture {
                        awaitPointerEventScope {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()

                            var didDrag = false
                            var prevY   = down.position.y
                            // Calculate the restricted track length
                            val activeTrackPx = (trackHeightPx - 2 * verticalMarginPx).coerceAtLeast(1f)

                            while (true) {
                                val event  = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) break

                                val dy = change.position.y - prevY
                                if (kotlin.math.abs(dy) > 0f && activeTrackPx > 0) {
                                    change.consume()
                                    didDrag = true
                                    val delta = -(dy / activeTrackPx) * (maxZoom - minZoom)
                                    currentOnScaleChange((currentScale + delta).coerceIn(minZoom, maxZoom))
                                }
                                prevY = change.position.y
                            }

                            if (!didDrag && activeTrackPx > 0) {
                                // Clamp tap to inside the restricted track
                                val tapY = (down.position.y - verticalMarginPx).coerceIn(0f, activeTrackPx)
                                val newFraction = 1f - (tapY / activeTrackPx)
                                currentOnScaleChange(
                                    (minZoom + newFraction * (maxZoom - minZoom)).coerceIn(minZoom, maxZoom)
                                )
                            }
                        }
                    }
                }
        ) {
            val cx         = size.width  / 2f
            val totalH     = size.height
            val activeH    = totalH - 2 * verticalMarginPx
            val trackW     = with(density) { 4.dp.toPx() }
            val thumbR     = with(density) { 6.dp.toPx() }
            val glowR      = with(density) { 10.dp.toPx() }
            val halfTrack  = trackW / 2f

            // Thumb is firmly restricted between the top and bottom margins
            val thumbY     = verticalMarginPx + activeH * (1f - fraction)

            // Track background
            drawRoundRect(
                color        = trackColor,
                topLeft      = Offset(cx - halfTrack, verticalMarginPx),
                size         = Size(trackW, activeH),
                cornerRadius = CornerRadius(halfTrack)
            )

            // Filled segment
            if (thumbY < totalH - verticalMarginPx) {
                drawRoundRect(
                    color        = filledColor,
                    topLeft      = Offset(cx - halfTrack, thumbY),
                    size         = Size(trackW, (totalH - verticalMarginPx) - thumbY),
                    cornerRadius = CornerRadius(halfTrack)
                )
            }

            drawCircle(color = thumbGlow, radius = glowR, center = Offset(cx, thumbY))
            drawCircle(color = thumbColor, radius = thumbR, center = Offset(cx, thumbY))
            drawCircle(
                color  = Color.White.copy(alpha = 0.28f),
                radius = thumbR * 0.48f,
                center = Offset(cx - thumbR * 0.18f, thumbY - thumbR * 0.22f)
            )
        }

        // ── Zoom-out button ───────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accentColor.copy(alpha = if (scale > minZoom) 0.15f else 0.05f))
                .clickable(enabled = scale > minZoom) {
                    onScaleChange((scale - step).coerceAtLeast(minZoom))
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "−",
                color      = if (scale > minZoom) accentColor else textColor.copy(alpha = 0.25f),
                fontWeight = FontWeight.Bold,
                fontSize   = 18.sp,
                lineHeight = 18.sp
            )
        }

        // ── Live zoom label ───────────────────────────────────────────────
        Text(
            text       = "${(scale * 100).toInt()}%",
            fontSize   = 10.sp,
            color      = accentColor,
            fontWeight = FontWeight.SemiBold,
            textAlign  = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PDF PAGE RENDERER  (warm background, smart colour-invert for night mode)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PdfPage(
    pdfRenderer: PdfRenderer,
    pageIndex  : Int,
    isNightMode: Boolean,
    bgColor    : Color
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val scope  = rememberCoroutineScope()

    // Invert colours for night mode — keeps text readable, inverts white→charcoal
    val colorFilter: ColorFilter? = if (isNightMode) {
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    -1f,  0f,  0f,  0f, 255f,
                    0f, -1f,  0f,  0f, 255f,
                    0f,  0f, -1f,  0f, 255f,
                    0f,  0f,  0f,  1f,   0f
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
                    // 2× resolution keeps text crisp when the user zooms in
                    val w   = page.width  * 2
                    val h   = page.height * 2
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
            bitmap             = bitmap!!.asImageBitmap(),
            contentDescription = "Page ${pageIndex + 1}",
            modifier           = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .aspectRatio(bitmap!!.width.toFloat() / bitmap!!.height.toFloat()),
            contentScale       = ContentScale.FillWidth,
            colorFilter        = colorFilter
        )
    } else {
        // Per-page loading skeleton — matches the book's background tone
        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier    = Modifier.size(28.dp),
                color       = if (isNightMode) DarkAccent else LightAccent
            )
        }
    }
}