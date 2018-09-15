package com.justnopoint.`interface`

import com.justnopoint.bladestrangers.RaniFile
import com.justnopoint.bladestrangers.RboxFile

interface FileSystem {
    fun getRaniFile(path: String): RaniFile?
    fun getRboxFile(path: String): RboxFile?
}