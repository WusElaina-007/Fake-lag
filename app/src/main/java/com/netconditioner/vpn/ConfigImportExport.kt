package com.netconditioner.vpn

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class ConfigImportExport(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver

    suspend fun exportConfig(uri: Uri, payload: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                resolver.openOutputStream(uri)?.use { output ->
                    OutputStreamWriter(output).use { writer ->
                        writer.write(payload)
                    }
                } ?: error("Unable to open output stream")
            }
        }
    }

    suspend fun importConfig(uri: Uri): Result<String> {
        return withContext(Dispatchers.IO) {
            runCatching {
                resolver.openInputStream(uri)?.use { input ->
                    BufferedReader(InputStreamReader(input)).use { reader ->
                        reader.readText()
                    }
                } ?: error("Unable to open input stream")
            }
        }
    }
}
