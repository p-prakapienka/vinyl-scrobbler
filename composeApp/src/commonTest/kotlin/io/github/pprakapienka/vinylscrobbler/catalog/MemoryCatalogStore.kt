package io.github.pprakapienka.vinylscrobbler.catalog

class MemoryCatalogStore : CatalogStore {
    var text: String? = null

    override suspend fun readText(): String? = text

    override suspend fun writeText(text: String) {
        this.text = text
    }
}
