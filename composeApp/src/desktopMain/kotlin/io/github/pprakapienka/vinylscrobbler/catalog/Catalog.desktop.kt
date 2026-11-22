package io.github.pprakapienka.vinylscrobbler.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
actual fun rememberCatalog(): Catalog {
    val store = remember { DesktopCatalogStore() }
    return remember(store) { FileCatalog(store) }
}

internal class DesktopCatalogStore : CatalogStore {
    private val file = File(System.getProperty("user.home"), ".$DIRECTORY/$FILE_NAME")

    override suspend fun readText(): String? = withContext(Dispatchers.IO) {
        if (!file.exists()) null else file.readText()
    }

    override suspend fun writeText(text: String) = withContext(Dispatchers.IO) {
        file.parentFile?.mkdirs()
        file.writeText(text)
    }

    private companion object {
        const val DIRECTORY = "vinyl-scrobbler"
        const val FILE_NAME = "catalog.json"
    }
}
