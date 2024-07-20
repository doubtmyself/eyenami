package team.eyenami
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StorageManager {
    private const val maxStorageSize = 300 * 1024 * 1024 // 300MB in bytes


    suspend fun manageStorage(context: Context) {
        withContext(Dispatchers.IO) {
            val externalFilesDir = context.getExternalFilesDir(null) ?: return@withContext
            val photoFiles = getPhotoFiles(externalFilesDir)
            val totalSize = calculateTotalSize(photoFiles)

            if (totalSize > maxStorageSize) {
                deleteOldestFiles(photoFiles, totalSize - maxStorageSize)
            }
        }
    }

    private fun getPhotoFiles(directory: File): List<File> {
        return directory.listFiles { file ->
            file.isFile && file.name.startsWith("PHOTO_") && file.name.endsWith(".jpg")
        }?.sortedBy { it.lastModified() } ?: emptyList()
    }

    private fun calculateTotalSize(files: List<File>): Long {
        return files.sumOf { it.length() }
    }

    private fun deleteOldestFiles(files: List<File>, sizeToFree: Long) {
        var freedSize = 0L
        for (file in files) {
            if (freedSize >= sizeToFree) break
            val fileSize = file.length()
            if (file.delete()) {
                freedSize += fileSize
            }
        }
    }

    fun createFile(context: Context, extension: String): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(null)
        val file = File.createTempFile("PHOTO_${timeStamp}_", ".$extension", storageDir)
        return file
    }
}