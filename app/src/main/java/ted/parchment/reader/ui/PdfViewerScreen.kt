package ted.parchment.reader.ui

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
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
import ted.parchment.reader.data.model.TocItem
import ted.parchment.reader.data.preferences.ReadingPreferences
import ted.parchment.reader.data.repository.BookRepository
import ted.parchment.reader.ui.theme.DesignTokens
import ted.parchment.reader.ui.theme.ReaderColorScheme
import ted.parchment.reader.ui.viewer.*
import ted.parchment.reader.utils.PdfUtils
import kotlinx.coroutines.*
import java.io.IOException

/**
 * Main reading screen — the core of Parchment.
 *
 * Orchestrates the PDF rendering pipeline, navigation HUD, zoom controls,
 * and bottom-sheet panels (TOC, Bookmarks, Settings) into a cohesive,
 * gesture-driven reading experience.
 *
 * Architecture:
 * - State management is local (remember/derivedStateOf) since the viewer
 *   is the sole consumer. A ViewModel is unnecessary overhead here.
 * - Rendering is delegated to [PdfPageRenderer] per-page composables
 * - Data is sourced from [BookRepository] and [ReadingPreferences]
 * - Design tokens come from [ReaderColorScheme]
 *
 * @param fileName Asset filename of the PDF to render
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(fileName: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val prefs = remember { ReadingPreferences(context) }

    // ── Load book config from repository ────────────────────────────────────
    val bookConfig = remember {
        BookRepository.getAllBooks().find { it.assetFileName == fileName }
            ?: BookRepository.getDefaultBook()
    }
    val tableOfContents = bookConfig.tableOfContents

    var showJumpDialog by remember { mutableStateOf(false) }

    // ── PDF engine ──────────────────────────────────────────────────────────
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pageCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    // ── Theme state ─────────────────────────────────────────────────────────
    var isNightMode by remember { mutableStateOf(prefs.getNightMode()) }
    val colors = ReaderColorScheme.resolve(isNightMode)

    // ── HUD visibility & auto-hide ──────────────────────────────────────────
    var showHud by remember { mutableStateOf(true) }
    var autoHideJob by remember { mutableStateOf<Job?>(null) }

    fun resetAutoHide() {
        autoHideJob?.cancel()
        if (showHud) {
            autoHideJob = scope.launch {
                delay(DesignTokens.HUD_AUTO_HIDE_MS)
                showHud = false
            }
        }
    }

    // ── Panel visibility ────────────────────────────────────────────────────
    var showTocSheet by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    // ── Bookmarks ───────────────────────────────────────────────────────────
    val bookmarks = remember {
        mutableStateListOf<Int>().also { it.addAll(prefs.getBookmarks(fileName)) }
    }

    // ── Scroll & zoom ───────────────────────────────────────────────────────
    val listState = rememberLazyListState()
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // ── Chapter-change toast overlay ────────────────────────────────────────
    var showChapterOverlay by remember { mutableStateOf(false) }
    var chapterOverlayTitle by remember { mutableStateOf("") }

    // ── Derived state ───────────────────────────────────────────────────────
    val currentPage by remember {
        derivedStateOf { listState.firstVisibleItemIndex + 1 }
    }

    val currentChapter by remember {
        derivedStateOf {
            tableOfContents
                .filter { it.pageIndex < currentPage }
                .lastOrNull() ?: tableOfContents.firstOrNull()
        }
    }

    // ── Chapter-change side effects ─────────────────────────────────────────
    var previousChapter by remember { mutableStateOf<TocItem?>(null) }
    LaunchedEffect(currentChapter) {
        if (previousChapter != null && previousChapter != currentChapter) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            chapterOverlayTitle = currentChapter?.title ?: ""
            showChapterOverlay = true
            delay(DesignTokens.CHAPTER_OVERLAY_MS)
            showChapterOverlay = false
        }
        previousChapter = currentChapter
    }

    // ── Persist scroll position on every page change ────────────────────────
    LaunchedEffect(currentPage) {
        prefs.saveScrollPosition(fileName, listState.firstVisibleItemIndex)
    }

    // ── Persist night mode preference ───────────────────────────────────────
    LaunchedEffect(isNightMode) {
        prefs.saveNightMode(isNightMode)
    }

    // ── Load PDF and restore reading position ───────────────────────────────
    LaunchedEffect(fileName) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val pfd = PdfUtils.getPdfFileDescriptor(context, fileName)
            if (pfd != null) {
                fileDescriptor = pfd
                try {
                    pdfRenderer = PdfRenderer(pfd)
                    pageCount = pdfRenderer?.pageCount ?: 0
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        isLoading = false
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

    // ── Bottom sheet states ──────────────────────────────────────────────────
    val tocSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val bookmarksSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ════════════════════════════════════════════════════════════════════════
    // ROOT LAYOUT
    // ════════════════════════════════════════════════════════════════════════
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()

        fun applyZoom(newScale: Float) {
            scale = newScale.coerceIn(1f, 5f)
            val maxX = ((screenWidth * scale) - screenWidth) / 2f
            val maxY = ((screenHeight * scale) - screenHeight) / 2f
            offset = Offset(
                x = offset.x.coerceIn(-maxX.coerceAtLeast(0f), maxX.coerceAtLeast(0f)),
                y = offset.y.coerceIn(-maxY.coerceAtLeast(0f), maxY.coerceAtLeast(0f))
            )
        }

        val transformState = rememberTransformableState { zoomChange, panChange, _ ->
            val newScale = (scale * zoomChange).coerceIn(1f, 5f)
            scale = newScale

            val maxX = ((screenWidth * scale) - screenWidth) / 2f
            val maxY = ((screenHeight * scale) - screenHeight) / 2f
            offset = Offset(
                x = (offset.x + panChange.x * scale).coerceIn(-maxX.coerceAtLeast(0f), maxX.coerceAtLeast(0f)),
                y = (offset.y + panChange.y * scale).coerceIn(-maxY.coerceAtLeast(0f), maxY.coerceAtLeast(0f))
            )
        }

        // ── 1. READING AREA ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .transformable(state = transformState)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            showHud = !showHud
                            if (showHud) resetAutoHide()
                        },
                        onDoubleTap = {
                            scope.launch {
                                applyZoom(if (scale > 1f) 1f else 2f)
                                offset = Offset.Zero
                            }
                        }
                    )
                }
                .pointerInput(scale) {
                    if (scale != 1f) return@pointerInput
                    detectHorizontalDragGestures { _, dragAmount ->
                        when {
                            dragAmount > DesignTokens.SWIPE_THRESHOLD -> {
                                if (!showTocSheet) { showTocSheet = true; showHud = false }
                            }
                            dragAmount < -DesignTokens.SWIPE_THRESHOLD -> {
                                if (!showBookmarksSheet) { showBookmarksSheet = true; showHud = false }
                            }
                        }
                    }
                }
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = colors.accent, strokeWidth = 2.dp)
                    Text("Opening book…", color = colors.text.copy(alpha = 0.6f), fontSize = 14.sp)
                }
            } else if (pdfRenderer != null) {
                LazyColumn(
                    state = listState,
                    userScrollEnabled = (scale == 1f),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        },
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    contentPadding = PaddingValues(
                        top = if (showHud) 72.dp else 0.dp,
                        bottom = if (showHud) 96.dp else 0.dp
                    )
                ) {
                    items(pageCount) { index ->
                        PdfPageRenderer(
                            pdfRenderer = pdfRenderer!!,
                            pageIndex = index,
                            isNightMode = isNightMode,
                            bgColor = colors.background
                        )
                    }
                }
            } else {
                Text(
                    "Could not open book.",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // ── 2. TOP HUD ──────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showHud,
            enter = slideInVertically(tween(220)) { -it } + fadeIn(tween(220)),
            exit = slideOutVertically(tween(220)) { -it } + fadeOut(tween(220)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 6.dp)
                    .background(colors.hud)
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* pop back stack */ }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = colors.text)
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = bookConfig.displayName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    currentChapter?.let { chapter ->
                        Text(
                            text = chapter.title,
                            fontSize = 11.sp,
                            color = colors.accent,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            lineHeight = 14.sp,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                val isBookmarked = bookmarks.contains(currentPage - 1)
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isBookmarked) bookmarks.remove(currentPage - 1)
                    else bookmarks.add(currentPage - 1)
                    prefs.saveBookmarks(fileName, bookmarks.toSet())
                    resetAutoHide()
                }) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Outlined.Bookmark,
                        contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
                        tint = if (isBookmarked) colors.accent else colors.text
                    )
                }

                IconButton(onClick = { isNightMode = !isNightMode; resetAutoHide() }) {
                    Icon(
                        imageVector = if (isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle night mode",
                        tint = colors.text
                    )
                }

                IconButton(onClick = { showSettingsSheet = true; resetAutoHide() }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Settings", tint = colors.text)
                }
            }
        }

        // ── 3. BOTTOM HUD ───────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showHud,
            enter = slideInVertically(tween(220)) { it } + fadeIn(tween(220)),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(220)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 8.dp)
                    .background(colors.hud)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showTocSheet = true; resetAutoHide() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.FormatListBulleted, contentDescription = "Table of Contents", tint = colors.text)
                    }

                    currentChapter?.let { chapter ->
                        Text(
                            text = chapter.title,
                            fontSize = 11.sp,
                            color = colors.accent,
                            maxLines = 2,
                            lineHeight = 14.sp,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
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
                            text = "$currentPage / $pageCount",
                            fontSize = 12.sp,
                            color = if (isNightMode) Color.White else colors.text,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (pageCount > 0) {
                    Slider(
                        value = currentPage.toFloat(),
                        onValueChange = { newPage ->
                            scope.launch {
                                listState.scrollToItem((newPage.toInt() - 1).coerceAtLeast(0))
                            }
                            resetAutoHide()
                        },
                        valueRange = 1f..pageCount.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = colors.accent,
                            activeTrackColor = colors.accent,
                            inactiveTrackColor = colors.accent.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                    )
                }
            }
        }

        // ── 4. CHAPTER-CHANGE OVERLAY ───────────────────────────────────────
        AnimatedVisibility(
            visible = showChapterOverlay,
            enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.88f),
            exit = fadeOut(tween(500)) + scaleOut(tween(500), targetScale = 0.88f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface.copy(alpha = 0.95f))
                    .padding(horizontal = 28.dp, vertical = 16.dp)
            ) {
                Text(
                    text = chapterOverlayTitle,
                    color = colors.accent,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }

        // ── 5. ZOOM BAR ────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showHud,
            enter = slideInHorizontally(tween(220)) { it } + fadeIn(tween(220)),
            exit = slideOutHorizontally(tween(220)) { it } + fadeOut(tween(220)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp, top = 72.dp, bottom = 100.dp)
        ) {
            VerticalZoomBar(
                scale = scale,
                onScaleChange = { applyZoom(it) },
                accentColor = colors.accent,
                surfaceColor = colors.surface,
                textColor = colors.text
            )
        }
    }

    // ── TOC BOTTOM SHEET ────────────────────────────────────────────────────
    if (showTocSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTocSheet = false },
            sheetState = tocSheetState,
            containerColor = colors.surface
        ) {
            TocPanel(
                tableOfContents = tableOfContents,
                currentPage = currentPage,
                accentColor = colors.accent,
                textColor = colors.text,
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

    // ── BOOKMARKS BOTTOM SHEET ──────────────────────────────────────────────
    if (showBookmarksSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBookmarksSheet = false },
            sheetState = bookmarksSheetState,
            containerColor = colors.surface
        ) {
            BookmarksPanel(
                bookmarks = bookmarks.sorted(),
                tableOfContents = tableOfContents,
                accentColor = colors.accent,
                textColor = colors.text,
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

    // ── SETTINGS BOTTOM SHEET ───────────────────────────────────────────────
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = settingsSheetState,
            containerColor = colors.surface
        ) {
            SettingsPanel(
                isNightMode = isNightMode,
                accentColor = colors.accent,
                textColor = colors.text,
                onNightModeToggle = { isNightMode = !isNightMode }
            )
        }
    }

    // ── PAGE JUMP DIALOG ────────────────────────────────────────────────────
    if (showJumpDialog) {
        PageJumpDialog(
            pageCount = pageCount,
            surfaceColor = colors.surface,
            textColor = colors.text,
            accentColor = colors.accent,
            onDismiss = { showJumpDialog = false },
            onPageSelected = { targetPage ->
                scope.launch {
                    listState.scrollToItem(targetPage - 1)
                    showJumpDialog = false
                }
            }
        )
    }
}