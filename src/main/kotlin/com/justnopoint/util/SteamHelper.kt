package com.justnopoint.util

import com.technofovea.hl2parse.vdf.SloppyParser
import com.technofovea.hl2parse.vdf.ValveTokenLexer
import com.technofovea.hl2parse.vdf.VdfRoot
import org.antlr.runtime.ANTLRInputStream
import org.antlr.runtime.CommonTokenStream
import java.io.*
import java.util.*


object SteamHelper {
    const val bsSteamID = 565170
    const val bsAppManifest = "appmanifest_$bsSteamID.acf"
    const val libraryManifest = "libraryfolders.vdf"

    val libraryDirectory: File?
        get() {
            steamDirectory?.let { dir ->
                val steamLibraries = ArrayList<File>()
                val steamDirFile = File(dir)
                steamLibraries.add(steamDirFile)
                val libraryFile = File(steamDirFile, "SteamApps/$libraryManifest")
                val dataRoot = doSloppyParse(FileInputStream(libraryFile))
                dataRoot.children.find { it.name == "LibraryFolders" }?.let { libraryFoldersNode ->
                    for (attribute in libraryFoldersNode.attributes) {
                        when(attribute.name) {
                            "TimeNextStatsReport", "ContentStatsID" -> {}
                            else -> {
                                steamLibraries.add(File(attribute.value))
                            }
                        }
                    }
                }

                for(library in steamLibraries) {
                    val appsfolder = File(library, "SteamApps")
                    val manifest = appsfolder.listFiles { file ->
                        return@listFiles (file.name == bsAppManifest)
                    }.firstOrNull()
                    if(manifest != null) {
                        return library
                    }
                }
            }
            return null
        }

    val bsDirectory: File?
        get() {
            val libraryDirectory = libraryDirectory
            return if (libraryDirectory != null) {
                val manifest = appManifest
                val dataRoot = doSloppyParse(FileInputStream(manifest))
                val installDir = dataRoot.children.find { it.name == "AppState" }?.let { libraryFoldersNode ->
                   libraryFoldersNode.attributes.find { it.name == "installdir" }?.value?:"Blade Strangers"
                }
                File(libraryDirectory, "SteamApps/common/$installDir")
            } else {
                null
            }
        }

    val appManifest: File?
        get() {
            val libraryDirectory = libraryDirectory
            return if (libraryDirectory != null) {
                File(libraryDirectory, "SteamApps/$bsAppManifest")
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

    fun doSloppyParse(inputStream: InputStream): VdfRoot {
        val ais = ANTLRInputStream(inputStream)
        val lexer = ValveTokenLexer(ais)
        val parser = SloppyParser(CommonTokenStream(lexer))
        return parser.main()
    }

    @JvmStatic
    fun main(s: Array<String>) {
        println("Steam directory : " + steamDirectory!!)
        println("Library directory : $libraryDirectory")
        println("BS directory : $bsDirectory")
    }
}