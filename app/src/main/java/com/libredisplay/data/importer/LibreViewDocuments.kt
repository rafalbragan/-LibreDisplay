package com.libredisplay.data.importer

import android.content.ContentResolver
import android.net.Uri
import android.os.CancellationSignal
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class VerifiedCsvDocument(val uri: Uri, val name: String, val summary: CsvSummary)

class LibreViewDocuments(private val resolver: ContentResolver) {
    /** Only direct children of the explicitly granted tree; never walks filesystem paths. */
    suspend fun discover(tree: Uri, options: CsvTimeOptions, found: suspend (VerifiedCsvDocument) -> Unit): Int = withContext(Dispatchers.IO) {
        require(DocumentsContract.isTreeUri(tree)) { "Wybierz folder przez systemowy selektor." }
        val signal = CancellationSignal()
        val cancellationWatcher = launch(Dispatchers.Default) {
            try { awaitCancellation() } finally { signal.cancel() }
        }
        var checked = 0
        try {
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            resolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE), null, null, null, signal)?.use { cursor ->
                while (cursor.moveToNext()) {
                    currentCoroutineContext().ensureActive()
                    if (cursor.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) continue
                    require(++checked <= 2000) { "Folder zawiera ponad 2000 plików. Wybierz mniejszy folder." }
                    if (!cursor.isNull(3) && cursor.getLong(3) > 128L * 1024 * 1024) continue
                    val uri = DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0))
                    try {
                        val descriptor = resolver.openAssetFileDescriptor(uri, "r", signal) ?: continue
                        val summary = descriptor.use { asset -> asset.createInputStream().use { input ->
                            LibreViewCsvParser().parse(input, options) { }
                        } }
                        found(VerifiedCsvDocument(uri, cursor.getString(1).orEmpty(), summary))
                    } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                    catch (_: CsvFormatException) { /* Not a verified LibreView file. */ }
                    catch (_: java.io.IOException) { /* An unreadable document is not a verified file. */ }
                    catch (_: SecurityException) { /* Access may be revoked by the provider. */ }
                }
            } ?: throw java.io.IOException("Nie można odczytać folderu.")
            checked
        } finally { cancellationWatcher.cancel() }
    }
}


