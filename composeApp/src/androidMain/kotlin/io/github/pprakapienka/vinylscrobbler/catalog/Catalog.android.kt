package io.github.pprakapienka.vinylscrobbler.catalog

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
actual fun rememberCatalog(): Catalog {
    val context = LocalContext.current.applicationContext
    val store = remember(context) { AndroidCatalogStore(context) }
    return remember(store) { FileCatalog(store) }
}

internal class AndroidCatalogStore(
    context: Context,
) : CatalogStore {
    private val file = File(context.filesDir, FILE_NAME)

    override suspend fun readText(): String? = withContext(Dispatchers.IO) {
        if (!file.exists()) null else file.readText()
    }

    override suspend fun writeText(text: String) = withContext(Dispatchers.IO) {
        file.writeText(text)
    }

    private companion object {
        const val FILE_NAME = "catalog.json"
    }
}
