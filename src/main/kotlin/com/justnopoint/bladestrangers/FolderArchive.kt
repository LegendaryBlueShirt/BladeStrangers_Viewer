package com.justnopoint.bladestrangers

import com.justnopoint.`interface`.FileSystem
import java.io.File
import java.io.RandomAccessFile

class FolderArchive(val folder: File): FileSystem {
    override fun getRaniFile(path: String): RaniFile? {
        val myFile = File(folder, path)
        if(!myFile.exists()) {
            System.out.println("${myFile.path} does not exist.")
            return null
        }
        return RaniFile(RandomAccessFile(myFile, "r"), 0)
    }

    override fun getRboxFile(path: String): RboxFile? {
        val myFile = File(folder, path)
        if(!myFile.exists()) {
            System.out.println("${myFile.path} does not exist.")
            return null
        }
        return RboxFile(RandomAccessFile(myFile, "r"), 0)
    }

}