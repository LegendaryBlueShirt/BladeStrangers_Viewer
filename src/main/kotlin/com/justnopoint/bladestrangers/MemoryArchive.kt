package com.justnopoint.bladestrangers

import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets

class MemoryArchive(memfs: File, membody: File) {
    data class MemFile(val fname: String, val offset: Long, val size: Int)

    private val archive: RandomAccessFile = RandomAccessFile(membody, "r")
    private val fileSystem: Map<String, MemFile>
    private val files: List<MemFile>

    companion object {
        const val MAGIC = "RSON"

        fun get(memfs: File): MemoryArchive {
            val folder = memfs.parentFile
            val name = memfs.nameWithoutExtension
            val body = File(folder, "$name.membody")
            if(body.exists()) {
                return MemoryArchive(memfs, body)
            }
            throw FileNotFoundException("Could not find file ${body.path}")
        }
    }

    init {
        val buffer = ByteArray(4)
        val fsInput = RandomAccessFile(memfs, "r")
        fsInput.seek(0)
        fsInput.read(buffer)
        if(MAGIC != String(buffer)) {
            throw Exception("Invalid file header!  Found ${String(buffer)}")
        }
        val fsize = fsInput.readIntLe()
        if(fsize != fsInput.length().toInt()) {
            throw Exception("File size mismatch!  Found $fsize instead of ${fsInput.length()}")
        }
        fsInput.skipBytes(12)
        var check: Byte = 0x00
        var count = 0
        val offsets = mutableListOf<Long>()
        val sizes = mutableListOf<Int>()
        while(check != 0x88.toByte()) {
            check = fsInput.readByte()
            if(check != 0x88.toByte()) {
                fsInput.skipBytes(3)
                offsets.add(fsInput.readLongLe())
                sizes.add(fsInput.readIntLe())
                count++
            }
        }
        val strbuffer = ByteArray(256)
        val names = mutableListOf<String>()
        for(n in 0 until count) {
            val strlen = fsInput.readUnsignedByte()
            fsInput.read(strbuffer, 0, strlen+2)
            val fname = String(strbuffer, 0, strlen, StandardCharsets.UTF_16LE)
            names.add(fname)
            fsInput.skipBytes(1)
        }
        files = (0 until count).map { MemFile(names[it], offsets[it], sizes[it]) }
        fileSystem = files.map { it.fname.substring(1).trim() to it }.toMap()
    }

    fun getNumFiles(): Int = files.size

    fun getFilename(index: Int): String? {
        if(index < 0 || index >= files.size) {
            return null
        }
        return files[index].fname
    }

    private fun getFile(path: String): MemFile? {
        return fileSystem[path]
    }

    fun getFile(index: Int): MemFile? {
        if(index < 0 || index >= files.size) {
            return null
        }
        return files[index]
    }

    fun getRaniFile(path: String): RaniFile? {
        getFile(path)?.let {
            return RaniFile(archive, it.offset)
        }?: return null
    }

    fun getRboxFile(path: String): RboxFile? {
        getFile(path)?.let {
            return RboxFile(archive, it.offset)
        }?: return null
    }
}

fun RandomAccessFile.readLongLe(): Long {
    return readUnsignedByte().toLong() or (readUnsignedByte().toLong() shl 8) or (readUnsignedByte().toLong() shl 16) or (readUnsignedByte().toLong() shl 24)
}

fun RandomAccessFile.readIntLe(): Int {
    return readUnsignedByte() or (readUnsignedByte() shl 8) or (readUnsignedByte() shl 16) or (readUnsignedByte() shl 24)
}

fun RandomAccessFile.readShortLe(): Int {
    return readUnsignedByte() or (readUnsignedByte() shl 8)
}