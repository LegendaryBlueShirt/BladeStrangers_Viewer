package com.justnopoint.bladestrangers

import java.io.RandomAccessFile
import kotlin.experimental.and

class RboxFile(dataSource: RandomAccessFile, offset: Long) {
    companion object {
        const val MAGIC = "RBOX"
    }

    data class Box(val v1: Float, val v2: Float, val v3: Float, val v4: Float)
    data class SequenceInstance(val duration: Int, val frameInstances: IntArray)
    data class FrameInstance(val unk1: Int, val boxes: IntArray)
    data class BoxInstance(val duration: Int, val valid: Boolean, val type: Int, val params: List<Int>?, val attack: AttackDef?)
    data class AttackDef(val unk1: ByteArray, val unk2: List<Float>, val unk3: List<Int>, val unk4: ByteArray, val damage: Int, val onHit: Int, val onBlock: Int, val powergain: Int )

    val sequenceLabels = ArrayList<Int>()
    val frameInstances = ArrayList<FrameInstance>()
    val sequenceInstances = ArrayList<SequenceInstance>()
    val boxInstances = ArrayList<BoxInstance>()
    val boxes = ArrayList<Box>()

    init {
        dataSource.seek(offset)
        val buffer = ByteArray(4096)
        dataSource.read(buffer, 0, 4)
        String(buffer, 0, 4).let {
            if (MAGIC != it) {
                throw Exception("Header mismatch!  Found $it")
            }
        }
        val unk1 = dataSource.readIntLe()
        val filesize = dataSource.readIntLe()
        val table = LongArray(9)
        for(n in 0 until table.size) {
            table[n] = dataSource.readLongLe()
        }

        //First section
        dataSource.seek(table[0]+offset)
        val nEntries = dataSource.readIntLe()
        for(n in 0 until nEntries) {
            sequenceLabels.add(dataSource.readShortLe())
        }

        //Second section
        dataSource.seek(table[1]+offset)
        val nValues = dataSource.readIntLe()
        for(n in 0 until nValues) {
            boxes.add(dataSource.readBox())
        }

        //Third section
        dataSource.seek(table[5]+offset)
        val offsets = LongArray((table[3] and 0xFFFF).toInt())
        for(n in 0 until nEntries) {
            offsets[n] = dataSource.readLongLe()
        }
        for(n in 0 until nEntries) {
            dataSource.seek(offsets[n]+offset)
            sequenceInstances.add(dataSource.readSequenceInstance())
        }

        //Fourth section
        dataSource.seek(table[6]+offset)
        val mappings = LongArray((table[3] ushr 16).toInt())
        for(n in 0 until mappings.size) {
            mappings[n] = dataSource.readLongLe()
        }
        var lowest = 9999999
        var highest = -1
        for(n in 0 until mappings.size) {
            dataSource.seek(mappings[n]+offset)
            val frame = dataSource.readFrameInstance()
            frameInstances.add(frame)
        }

        //Fifth section
        dataSource.seek(table[7]+offset)
        val boxoffs = IntArray(table[4].toInt())
        for(n in 0 until boxoffs.size) {
            boxoffs[n] = dataSource.readIntLe()
        }
        for(n in 0 until boxoffs.size) {
            if(dataSource.filePointer != boxoffs[n]+offset) {
                System.out.println("Current filepointer ${dataSource.filePointer-offset}  Next filepointer ${boxoffs[n]}")
            }
            dataSource.seek(boxoffs[n]+offset)
            val box = dataSource.readBoxInstance()
            boxInstances.add(box)
            for(boxref in box.params?: emptyList()) {
                if (boxref < lowest) {
                    lowest = boxref
                }
                if (boxref > highest) {
                    highest = boxref
                }
            }
        }

        System.out.println("Sequence count ${sequenceLabels.size}")
        System.out.println("Frame count ${frameInstances.size}")
        System.out.println("OtherCount ${boxoffs.size}")
        System.out.println("Low $lowest High $highest")
    }
}

fun RandomAccessFile.readFrameInstance(): RboxFile.FrameInstance {
    val unk1 = readShortLe()
    val nsprites = readShortLe()
    val boxes = IntArray(nsprites)
    for(m in 0 until nsprites) {
        boxes[m] = readShortLe()
    }
    return RboxFile.FrameInstance(unk1 = unk1, boxes = boxes)
}

fun RandomAccessFile.readSequenceInstance(): RboxFile.SequenceInstance {
    val duration = readShortLe()
    val nframes = readShortLe()
    val frames = IntArray(nframes)
    for(m in 0 until nframes) {
        frames[m] = readShortLe()
    }
    return RboxFile.SequenceInstance(duration = duration, frameInstances = frames)
}

fun RandomAccessFile.readBoxInstance(): RboxFile.BoxInstance {
    val duration = read()
    val valid = ((read() and 0xFF) != 0x80)
    var type = -1
    var subdefs: ArrayList<Int>? = null
    var attack: RboxFile.AttackDef? = null
    if(valid) {
        val nSubDefs = read()
        type = read()
        subdefs = ArrayList()
        when(type) {
            0 -> {
                for(n in 0 until nSubDefs) {
                    subdefs.add(readShortLe())
                }
            }
            0x38 -> {
                System.out.println("Read attack type")
                val flags = ByteArray(20)
                read(flags)
                val unk2 = listOf(Float.fromBits(readIntLe()), Float.fromBits(readIntLe()), Float.fromBits(readIntLe()))
                val onHitFrames = readShortLe()
                val onBlockFrames = readShortLe()
                val unk3 = listOf(readShortLe(), readShortLe(), readShortLe(), readShortLe())
                val damage = readShortLe()
                val powergain = readShortLe()
                val vars2 = ByteArray(8)
                read(vars2)
                attack = RboxFile.AttackDef(unk1 = flags, unk2 = unk2, unk3 = unk3, unk4 = vars2, damage = damage, onHit = onHitFrames, onBlock = onBlockFrames, powergain = powergain)
            }
            else -> {
                System.out.println("Found type $type - Count is $nSubDefs")
                for(n in 0 until type/2) {
                    subdefs.add(readShortLe())
                }
            }
        }

    }
    return RboxFile.BoxInstance(duration = duration, valid = valid, type = type, params = subdefs, attack = attack)
}

fun RandomAccessFile.readBox(): RboxFile.Box {
    val v1 = Float.fromBits(readIntLe())
    val v2 = Float.fromBits(readIntLe())
    val zero = Float.fromBits(readIntLe())
    val v3 = Float.fromBits(readIntLe())
    val v4 = Float.fromBits(readIntLe())
    val point3 = Float.fromBits(readIntLe())
    return RboxFile.Box(v1, v2, v3, v4)
}

fun RboxFile.AttackDef.isOverhead(): Boolean {
    return (unk1[4].toInt() and 0x2) != 0
}
fun RboxFile.AttackDef.isLow(): Boolean {
    return (unk1[4].toInt() and 0x4) != 0
}
fun RboxFile.AttackDef.getDamageScaling(): Int {
    return unk4[2].toInt()
}