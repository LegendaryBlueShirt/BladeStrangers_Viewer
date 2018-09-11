package com.justnopoint.util

import com.justnopoint.bladestrangers.RaniFile
import javafx.scene.image.Image
import javafx.scene.image.PixelFormat
import java.awt.Point
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.awt.image.IndexColorModel
import java.awt.image.WritableRaster
import javafx.scene.image.WritableImage

fun createIndexColorModel(data: ByteArray?): IndexColorModel {
    val buffer = data?.copyOf()?: ByteArray(1024)
    for(m in 0 until 256) {
        var temp1 = buffer[m*4]
        buffer[m*4] = buffer[m*4+3]
        buffer[m*4+3] = temp1
        temp1 = buffer[m*4+1]
        buffer[m*4+1] = buffer[m*4+2]
        buffer[m*4+2] = temp1
    }

    return IndexColorModel(8, 256, buffer, 0, true, 0)
}

fun createIntPalette(data: ByteArray?): IntArray {
    val palette = IntArray(256)
    if(data == null) {
        return palette
    }
    for (m in 0..255) {
        palette[m] = (data[m * 4 + 0].toInt() and 0xFF shl 24) or (data[m * 4 +3].toInt() and 0xFF shl 16) or (data[m * 4 + 2].toInt() and 0xFF shl 8) or (data[m * 4 + 1].toInt() and 0xFF)
    }
    return palette
}

fun createBufferedImage(data: ByteArray, width: Int, height: Int, colorModel: IndexColorModel): BufferedImage {
    val buffer = DataBufferByte(data, width*height)
    val sampleModel = colorModel.createCompatibleSampleModel(width, height)
    val writableRaster = WritableRaster.createWritableRaster(sampleModel, buffer, Point(0,0))
    return BufferedImage(colorModel, writableRaster, false, null)
}

fun createDelayedLoadImages(texNo: Int, dims: List<RaniFile.SpriteDim>, palette: IntArray, source: RaniFile): List<Image> {
    val result = ArrayList<WritableImage>()
    for(dim in dims) {
        result.add(WritableImage(dim.width, dim.height))
    }
    source.getRawSprite(texNo) { data, texWidth, _ ->
        for((index, dim) in dims.withIndex()) {
            result[index].pixelWriter.setPixels(0, 0, dim.width, dim.height, PixelFormat.createByteIndexedInstance(palette), data, dim.texX + dim.texY*texWidth!!, texWidth)
        }
    }
    return result
}

fun createDelayedLoadImage(texNo: Int, width: Int, height: Int, palette: IntArray, source: RaniFile): Image {
    val img = WritableImage(width, height)
    source.getRawSprite(texNo) { data, texWidth, _ ->
        img.pixelWriter.setPixels(0, 0, width, height, PixelFormat.createByteIndexedInstance(palette), data, 0, texWidth!!)
    }
    return img
}