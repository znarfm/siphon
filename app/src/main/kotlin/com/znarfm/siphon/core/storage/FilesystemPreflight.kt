package com.znarfm.siphon.core.storage

import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.StatFs
import android.system.Os
import java.io.File
import java.io.IOException

const val FAT32_MAX_FILE_BYTES = 4_294_967_295L // 4 GiB - 1 byte

data class PreflightReport(
    val availableBytes: Long,
    val fsType: String,
    val isSparseSupported: Boolean,
    val recommendedWorkers: Int
)

class InsufficientStorageException(
    val requiredBytes: Long,
    val availableBytes: Long
) : IOException("Insufficient storage: required $requiredBytes bytes, available $availableBytes bytes")

class Fat32FileSizeLimitException(
    val totalBytes: Long
) : IOException("File size ($totalBytes bytes) exceeds FAT32 4GB limit ($FAT32_MAX_FILE_BYTES bytes)")

object FilesystemPreflight {

    private val FAT32_TYPES = setOf("vfat", "msdos", "fat")
    private val NON_SPARSE_TYPES = setOf("vfat", "msdos", "fat", "exfat", "sdfat")

    /**
     * Verifies free space and filesystem constraints for an open [pfd].
     * Uses `Os.fstatvfs` directly on the file descriptor so external SD card SAF trees
     * and internal MediaStore URIs are checked against their actual backing mount.
     */
    fun check(
        pfd: ParcelFileDescriptor,
        totalBytes: Long,
        alreadyDownloadedBytes: Long = 0L,
        requestedWorkers: Int = 6
    ): PreflightReport {
        val availableBytes = queryAvailableBytes(pfd)
        val remainingBytes = (totalBytes - alreadyDownloadedBytes).coerceAtLeast(0L)

        if (totalBytes > 0L && remainingBytes > availableBytes) {
            throw InsufficientStorageException(remainingBytes, availableBytes)
        }

        val resolvedPath = resolveFdPath(pfd)
        val fsType = detectFilesystemType(resolvedPath)

        if (totalBytes > FAT32_MAX_FILE_BYTES && isFat32(fsType, resolvedPath)) {
            throw Fat32FileSizeLimitException(totalBytes)
        }

        val isSparse = isSparseSupported(fsType, resolvedPath)
        val recommendedWorkers = if (isSparse) {
            requestedWorkers.coerceIn(1, 8)
        } else {
            // Non-sparse FAT32/exFAT mounts zero-fill high-offset writes synchronously;
            // cap concurrency to prevent multi-GB kernel zero-fill stalls.
            minOf(requestedWorkers, 2).coerceAtLeast(1)
        }

        return PreflightReport(
            availableBytes = availableBytes,
            fsType = fsType,
            isSparseSupported = isSparse,
            recommendedWorkers = recommendedWorkers
        )
    }

    private fun queryAvailableBytes(pfd: ParcelFileDescriptor): Long {
        return try {
            val stat = Os.fstatvfs(pfd.fileDescriptor)
            stat.f_bavail * stat.f_frsize
        } catch (_: Throwable) {
            val fallbackPath = Environment.getExternalStorageDirectory().absolutePath
            StatFs(fallbackPath).availableBytes
        }
    }

    private fun resolveFdPath(pfd: ParcelFileDescriptor): String? {
        return try {
            Os.readlink("/proc/self/fd/${pfd.fd}")
        } catch (_: Throwable) {
            null
        }
    }

    internal fun detectFilesystemType(filePath: String?): String {
        val mountsFile = File("/proc/mounts")
        if (!mountsFile.canRead() || filePath.isNullOrBlank()) return "unknown"

        var bestMountPoint = ""
        var bestFsType = "unknown"
        var underlyingExternalFs: String? = null

        return try {
            mountsFile.forEachLine { line ->
                val parts = line.split(' ')
                if (parts.size >= 3) {
                    val mountPoint = parts[1].replace("\\040", " ")
                    val fsType = parts[2].lowercase()

                    if (fsType in NON_SPARSE_TYPES &&
                        (mountPoint.startsWith("/mnt/media_rw/") || mountPoint.startsWith("/mnt/pass_through/"))
                    ) {
                        val volumeId = mountPoint.substringAfterLast('/')
                        if (volumeId.isNotEmpty() && filePath.contains("/$volumeId/")) {
                            underlyingExternalFs = fsType
                        }
                    }

                    if (filePath.startsWith(mountPoint) && mountPoint.length >= bestMountPoint.length) {
                        bestMountPoint = mountPoint
                        bestFsType = fsType
                    }
                }
            }
            underlyingExternalFs ?: bestFsType
        } catch (_: Throwable) {
            "unknown"
        }
    }

    private fun isFat32(fsType: String, resolvedPath: String?): Boolean {
        if (fsType in FAT32_TYPES) return true
        // External removable storage mounted via FUSE that wasn't matched to f2fs/ext4/exfat
        if (resolvedPath != null &&
            !resolvedPath.startsWith("/storage/emulated/") &&
            !resolvedPath.startsWith("/data/") &&
            fsType == "vfat"
        ) {
            return true
        }
        return false
    }

    private fun isSparseSupported(fsType: String, resolvedPath: String?): Boolean {
        if (fsType in NON_SPARSE_TYPES) return false
        if (resolvedPath != null &&
            !resolvedPath.startsWith("/storage/emulated/") &&
            !resolvedPath.startsWith("/data/")
        ) {
            // Removable SD cards / USB OTG drives are almost universally FAT32/exFAT
            return false
        }
        return true
    }
}
