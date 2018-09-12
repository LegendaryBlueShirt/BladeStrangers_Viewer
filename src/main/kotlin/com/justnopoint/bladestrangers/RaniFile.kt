package com.justnopoint.bladestrangers

import java.io.ByteArrayOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.Channels
import java.util.zip.InflaterInputStream
import kotlin.concurrent.thread

class RaniFile(dataSource: RandomAccessFile, offset: Long) {
    companion object {
        const val MAGIC = "RZ"
        const val MAGIC_INFLATED = "RANI"
    }

    data class SpriteAssoc(val sprNo: Int, val flags: Int, val dims: IntArray)
    data class SpriteLoc(val offset: Int, val size: Int, val texWidth: Int, val texHeight: Int)
    data class SpriteDim(val texX: Int, val texY: Int, val width: Int, val height: Int, val axisX: Int, val axisY: Int)
    data class AnimDef(val unk1: Int, val frames: List<FrameDef>)
    data class FrameDef(val constructedSprNo: Int)

    val data: ByteBuffer
    var palettes: ArrayList<ByteArray>? = null
    private val spriteLocs = ArrayList<SpriteLoc>()
    private val spriteDims = ArrayList<SpriteDim>()
    private val spriteAssocs = ArrayList<SpriteAssoc>()
    private val animDefs = ArrayList<AnimDef>()

    fun getRawSpriteCount(): Int = spriteLocs.size

    fun getConstructedSpriteCount(): Int = spriteAssocs.size

    fun getPaletteCount(): Int = palettes?.size?:0

    fun getPalette(index: Int): ByteArray? {
        if(index < 0 || index >= getPaletteCount()) {
            return null
        }
        return palettes?.get(index)
    }

    fun getAnim(index: Int): AnimDef {
        return animDefs[index]
    }

    data class Sprite(val sprNo: Int, val dims: List<SpriteDim>)
    fun getSpriteData(index: Int): Sprite {
        val assoc = spriteAssocs[index]
        val dims = ArrayList<SpriteDim>()
        for(index in assoc.dims) {
            if(index != -1) {
                dims.add(spriteDims[index])
            }
        }
        return Sprite(sprNo = assoc.sprNo, dims = dims)
    }

    @Synchronized fun getRawSprite(index: Int, result: (data: ByteArray?, width: Int?, height: Int?) -> Unit) {
        val loc = spriteLocs[index]
        data.position(loc.offset)
        val sprdata = data.slice()
        sprdata.limit(loc.size)
        thread(start = true) {
            result(decompressSprite(sprdata), loc.texWidth, loc.texHeight)
        }
    }

    private fun decompressSprite(data: ByteBuffer): ByteArray {
        val baos = ByteArrayOutputStream()
        data.position(0)
        var dat: Int
        while(data.hasRemaining()) {
            dat = data.get().toInt()
            when(dat) {
                0x00 -> {
                    val cnt = data.short.toInt() and 0xFFFF
                    for(n in 0 until cnt) {
                        baos.write(0)
                    }
                }
                else -> { baos.write(dat) }
            }
        }
        return baos.toByteArray()
    }

    init {
        dataSource.seek(offset)
        val buffer = ByteArray(4096)
        dataSource.read(buffer, 0, 2)
        String(buffer, 0, 2).let {
            if (MAGIC != it) {
                throw Exception("Header mismatch!  Found $it")
            }
        }
        dataSource.skipBytes(4)
        val stream = Channels.newInputStream(dataSource.channel)
        val inflater = InflaterInputStream(stream)
        val baos = ByteArrayOutputStream()
        var bytesRead = 0
        while(bytesRead != -1) {
            bytesRead = inflater.read(buffer)
            if(bytesRead > 0) {
                baos.write(buffer, 0, bytesRead)
            }
        }
        data = ByteBuffer.wrap(baos.toByteArray())
        data.order(ByteOrder.LITTLE_ENDIAN)
        parse(data)
    }

    private fun parse(data: ByteBuffer) {
        val strbuf = ByteArray(4)
        data.get(strbuf)
        String(strbuf).let {
            if(MAGIC_INFLATED != it) {
                throw Exception("Inflated Header mismatch!  Found $it")
            }
        }

        data.int
        val fsize1 = data.int
        val fsize2 = data.int
        if(fsize1 != data.limit() || fsize2 != data.limit()) {
            throw Exception("Inflated data is wrong size? $fsize1 $fsize2 ${data.limit()}")
        }
        val firstOffset = data.int
        val nvalues = (firstOffset/4)-4
        val table = IntArray(nvalues)
        table[0] = firstOffset
        for(n in 1 until nvalues) {
            table[n] = data.int
        }

        //First Section
        data.position(firstOffset)
        val nVars = data.int
        for(m in 0 until nVars) {
            spriteLocs.add(data.readSpriteLoc())
        }

        //Palettes
        data.position(table[6])
        val nPalettes = data.short.toInt()
        val palSize = data.short.toInt()
        data.int
        val colorCount = data.int
        palettes = ArrayList(nPalettes)
        for(n in 0 until nPalettes) {
            val buffer = ByteArray(palSize)
            data.get(buffer)
            palettes?.add(buffer)
        }

        //Third section
        data.position(table[1])
        val nEntries = data.int
        for(n in 0 until nEntries) {
            spriteDims.add(data.readSpriteDim())
        }

        //Fourth section
        data.position(table[2])
        val nFields = data.short
        val dataSize = data.short //38
        val nDims = data.int //16

        for(n in 0 until nFields) {
            spriteAssocs.add(data.readSpriteAssoc(nDims))
        }

        //Fifth section
        data.position(table[3])
        val nAnimDefs = data.int
        for(n in 0 until nAnimDefs) {
            animDefs.add(data.readAnimDef())
        }

        System.out.println("Unique sprites ${getRawSpriteCount()}")
        System.out.println("Real sprite count ${getConstructedSpriteCount()}")
    }
}

fun ByteBuffer.readSpriteAssoc(nDims: Int): RaniFile.SpriteAssoc {
    val dims = IntArray(nDims)
    val sprNo = short.toInt()
    val flags = int
    for(m in 0 until nDims) {
        dims[m] = short.toInt()
    }
    return RaniFile.SpriteAssoc(sprNo = sprNo, flags = flags, dims = dims)
}

fun ByteBuffer.readSpriteDim(): RaniFile.SpriteDim {
    val texX = short.toInt() //Sometimes a sprite is split into pieces.  These are the origin coordinates for this piece.
    val texY = short.toInt()
    val axisX = short.toInt() //Drawing axis for this piece
    val axisY = short.toInt()
    val width = short.toInt() //Dimensions of this piece
    val height = short.toInt()
    return RaniFile.SpriteDim(width = width, height = height, axisX = axisX, axisY = axisY, texX = texX, texY = texY)
}

fun ByteBuffer.readSpriteLoc(): RaniFile.SpriteLoc {
    val offset = int
    val size = int
    val unk = int
    val texWidth = short.toInt()
    val texHeight = short.toInt()
    val unk2 = int
    return RaniFile.SpriteLoc(offset = offset, size = size, texWidth = texWidth, texHeight = texHeight)
}

fun ByteBuffer.readAnimDef(): RaniFile.AnimDef {
    val unk1 = short.toInt()
    val frameTotal = short
    val offset = int
    val frames = ArrayList<RaniFile.FrameDef>()
    if(frameTotal > 0) {
        val temp = position()
        position(offset)
        for(n in 0 until frameTotal) {
            frames.add(readFrameDef())
        }
        position(temp)
    }
    return RaniFile.AnimDef(unk1 = unk1, frames = frames)
}

fun ByteBuffer.readFrameDef(): RaniFile.FrameDef {
    val spr = short
    val unk1 = short
    val unk2 = short
    val unk3 = short
    return RaniFile.FrameDef(constructedSprNo = spr.toInt())
}