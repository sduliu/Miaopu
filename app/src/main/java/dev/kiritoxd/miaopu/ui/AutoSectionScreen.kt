package dev.kiritoxd.miaopu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.kiritoxd.miaopu.data.AdapterStatus
import dev.kiritoxd.miaopu.data.AutoThread
import dev.kiritoxd.miaopu.data.AutoTopics
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

@Composable
fun AutoSectionScreen(viewModel: MiaopuViewModel) {
    var threads by remember { mutableStateOf(emptyList<AutoThread>()) }
    var page by remember { mutableIntStateOf(1) }
    var cursor by remember { mutableStateOf("") }
    var hasMore by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loadMoreError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun load(first: Boolean) {
        if (isLoadingMore) return
        if (first) {
            isLoading = true
            error = null
        } else {
            isLoadingMore = true
            loadMoreError = null
        }
        val requestPage = if (first) 1 else page + 1
        val requestCursor = if (first) "" else cursor
        scope.launch {
            val result = viewModel.autoAdapter.getThreads(AutoTopics.CAR, requestPage, requestCursor)
            val data = if (result.status == AdapterStatus.SUCCESS) result.data else null
            if (data != null) {
                threads = if (first) data.threads else threads + data.threads
                page = requestPage
                cursor = data.cursor
                hasMore = data.hasMore
                error = null
            } else if (first) {
                error = result.error?.message ?: "加载失败"
            } else {
                loadMoreError = result.error?.message ?: "加载失败"
            }
            isLoading = false
            isLoadingMore = false
        }
    }

    LaunchedEffect(Unit) { load(first = true) }

    Scaffold(
        containerColor = MiuixTheme.colorScheme.surface,
        topBar = {
            SmallTopAppBar(
                title = "汽车区",
                navigationIcon = {
                    IconButton(onClick = viewModel::goBack) { Icon(LucideIcons.ChevronLeft, "返回") }
                },
                actions = {
                    IconButton(onClick = { load(first = true) }) { Icon(LucideIcons.RefreshCw, "刷新") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when {
                isLoading -> item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                }
                error != null -> item {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(error.orEmpty(), color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { load(first = true) },
                            colors = ButtonDefaults.buttonColorsPrimary(),
                        ) {
                            Text("重试")
                        }
                    }
                }
                threads.isEmpty() -> item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        Text("还没有帖子", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                }
                else -> {
                    items(threads, key = { it.tid }) { thread ->
                        AutoThreadCard(thread = thread, onClick = { viewModel.openAutoThread(thread) })
                    }
                    if (hasMore) {
                        item {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Button(
                                    onClick = { load(first = false) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(if (isLoadingMore) "加载中…" else "加载更多")
                                }
                                loadMoreError?.let { message ->
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        message,
                                        style = MiuixTheme.textStyles.footnote1,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoThreadCard(thread: AutoThread, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        insideMargin = PaddingValues(16.dp),
        onClick = onClick,
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    thread.title,
                    style = MiuixTheme.textStyles.title3,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    listOf(thread.username, thread.time).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "回复 ${thread.replies} · 点亮 ${thread.recommends}",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            if (thread.isVideo) {
                Text(
                    "视频",
                    style = MiuixTheme.textStyles.footnote1,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
