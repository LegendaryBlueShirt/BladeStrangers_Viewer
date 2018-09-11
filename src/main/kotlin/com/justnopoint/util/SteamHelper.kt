package com.justnopoint.util

import java.io.IOException
import java.io.StringWriter
import java.io.File
import java.io.InputStream


object SteamHelper {
    val bsDirectory: File?
        get() {
            val steamDirectory = steamDirectory
            return if (steamDirectory != null) {
                val steamDirFile = File(steamDirectory)
                File(steamDirFile, "SteamApps/common/Blade Strangers")
            } else {
                null
            }
        }

    val steamDirectory: String?
        get() {
            try {
                val process = Runtime.getRuntime().exec(STEAM_FOLDER_CMD)
                val reader = StreamReader(process.inputStream)

                reader.start()
                process.waitFor()
                reader.join()

                val result = reader.result
                val p = result.indexOf(REGSTR_TOKEN)

                return if (p == -1) null else result.substring(p + REGSTR_TOKEN.length).trim { it <= ' ' }

            } catch (e: Exception) {
                return null
            }

        }

    private val REGQUERY_UTIL = "reg query "
    private val REGSTR_TOKEN = "REG_SZ"

    private val STEAM_FOLDER_CMD = "$REGQUERY_UTIL\"HKCU\\Software\\Valve\\Steam\" /v SteamPath"

    internal class StreamReader(private val stream: InputStream) : Thread() {
        private val sw: StringWriter = StringWriter()

        val result: String
            get() = sw.toString()

        override fun run() {
            try {
                var c = stream.read()
                do {
                    sw.write(c)
                    c = stream.read()
                } while (c != -1)
            } catch (e: IOException) {
            }

        }
    }

    @JvmStatic
    fun main(s: Array<String>) {
        println("Steam directory : " + steamDirectory!!)
    }
}