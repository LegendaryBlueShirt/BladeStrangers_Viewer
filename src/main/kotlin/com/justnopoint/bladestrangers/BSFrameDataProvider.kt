package com.justnopoint.bladestrangers

import com.justnopoint.`interface`.*
import com.justnopoint.util.createDelayedLoadImages
import com.justnopoint.util.createIntPalette
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import java.io.File

class BSFrameDataProvider(bsHome: File): FrameDataProvider, FrameRenderer {
    private val archive: MemoryArchive
    var rani: RaniFile? = null
    private var rbox: RboxFile? = null
    private var sprites = HashMap<Int, List<Image>>()

    init {
        if(!File(bsHome, "chara.memfs").exists()) {
            throw Exception("Attempted to load folder without chara.memfs")
        }
        archive = MemoryArchive.get(File(bsHome, "chara.memfs"))
    }

    companion object {
        fun validateFolder(folder: File): File? {
            var currentFolder = folder
            if(File(currentFolder, "fsroot").exists()) {
                currentFolder = File(currentFolder, "fsroot")
            }
            if(File(currentFolder, "spec_win").exists()) {
                currentFolder = File(currentFolder, "spec_win")
            }
            if(File(currentFolder, "chara.memfs").exists()) {
                return currentFolder
            }
            return null
        }
    }

    override fun getCharacters(): List<Character> {
        return listOf(
                BSCharacter("Ali", "cop_ali"),
                BSCharacter("Liongate", "cop_lio"),
                BSCharacter("Master T", "cop_mas"),
                BSCharacter("Solange", "cop_sol"),
                BSCharacter("Gunvolt", "int_gun"),
                BSCharacter("Curly Brace", "nic_cul"),
                BSCharacter("Helena", "nic_hel"),
                BSCharacter("Isaac", "nic_isa"),
                BSCharacter("Quote", "nic_quo"),
                BSCharacter("Lina", "ori_lin"),
                BSCharacter("Emiko", "uhr_emi"),
                BSCharacter("Shakemaru", "uhr_emi_shake"),
                BSCharacter("Kawase", "uhr_kws"),
                BSCharacter("Noko", "uhr_nok"),
                BSCharacter("Shovel Knight", "ycg_sho")
        )
    }

    override fun loadCharacter(character: Character) {
        if(character !is BSCharacter) {
            return
        }
        sprites.clear()
        rani = archive.getRaniFile("${character.tag}.rani")
        rbox = archive.getRboxFile("${character.tag}.rbox")
    }

    override fun getSequences(): List<Sequence> {
        rani?.let { graphics ->
            rbox?.let { data ->
                val sequences = ArrayList<BSSequence>()
                for ((index, label) in data.sequenceLabels.withIndex()) {
                    sequences.add(BSSequence(animDef = graphics.getAnim(index = label), label = label, dataDef = data.sequenceInstances[index]))
                }
                return sequences
            } ?: return emptyList()
        } ?: return emptyList()
    }

    override fun getFrameRenderer(): FrameRenderer {
        return this
    }

    inner class BSCharacter(val fullname: String, val tag: String): Character() {
        override fun getFullName(): String {
            return fullname
        }
    }

    inner class BSSequence(private val label: Int, animDef: RaniFile.AnimDef, val dataDef: RboxFile.SequenceInstance): Sequence() {
        val frames = ArrayList<BSFrame>()
        init {
            rbox?.let {source ->
                if(dataDef.duration != animDef.frames.size) {
                    System.out.println("Duration does not match framecount. $label   ${dataDef.duration}/${animDef.frames.size}")
                }
                for ((index, f) in animDef.frames.withIndex()) {
                    frames.add(BSFrame(sprDef = f,sequence = this,startTime = index))
                }
            }?: throw Exception("RBOX file was not yet initialized!")
        }

        override fun getName(): String {
           return getSequenceName(label = label)
        }

        override fun getFramecount(): Int {
            return frames.size
        }

        override fun getFrames(): List<Frame> {
            return frames
        }

    }

    inner class BSFrame(val sprDef: RaniFile.FrameDef, private val sequence: Sequence, private val startTime: Int): Frame {
        override fun getSequence(): Sequence {
            return sequence
        }

        override fun getStartTime(): Int {
            return startTime
        }

        override fun getDuration(): Int {
            return 1
        }
    }

    override fun renderFrame(g: GraphicsContext, frame: Frame, axisX: Int, axisY: Int) {
        if(frame !is BSFrame) {
           return
        }
        g.save()
        g.translate(axisX.toDouble(), axisY.toDouble())
        rani?.let {
            val sprdata = it.getSpriteData(frame.sprDef.constructedSprNo)
            if(!sprites.containsKey(frame.sprDef.constructedSprNo)) {
                sprites[frame.sprDef.constructedSprNo] = createDelayedLoadImages(
                        texNo = sprdata.sprNo,
                        dims = sprdata.dims,
                        source = it,
                        palette = createIntPalette(it.palettes!![1]))
            }
            val sprite = sprites[frame.sprDef.constructedSprNo]!!
            for((index, dim) in sprdata.dims.withIndex()) {
                g.drawImage(sprite[index], dim.axisX.toDouble(), dim.axisY.toDouble())
            }
        }
        g.restore()
        g.restore()
    }

    override fun renderFrameData(g: GraphicsContext, frame: Frame, axisX: Int, axisY: Int) {
        g.save()

        g.fill = Color(1.0,1.0,1.0,1.0)
        g.fillText("Frame ${frame.getStartTime()+1}/${frame.getSequence().getFramecount()}", 30.0, 30.0)

        if(frame is BSFrame) {
            val seq = frame.getSequence()
            var yval = 50.0
            if(seq is BSSequence) {
                for(value in seq.dataDef.frameInstances) {
                    g.fill = Color(1.0,1.0,1.0,1.0)
                    g.fillText("$value", 30.0, yval)

                    var xval = 30.0
                    yval += 20.0
                    val boxes =  rbox!!.frameInstances[value].boxes.map { rbox!!.boxInstances[it] }
                    val activeBox = getActiveBoxDef(frame.getStartTime(), boxes)
                    for(box in rbox!!.frameInstances[value].boxes) {
                        val boxinstance = rbox!!.boxInstances[box]
                        if(boxinstance == activeBox) {
                            g.fill = Color(1.0,0.0,0.0,1.0)
                        } else {
                            g.fill = Color(1.0,1.0,1.0,1.0)
                        }
                        g.fillText("$box(${boxinstance.duration})", xval, yval)
                        xval += 40.0
                    }
                    yval += 20.0
                }
            }
        }

        g.restore()
        g.save()

        g.translate(axisX.toDouble(), axisY.toDouble())
        g.fill = Color(1.0,1.0,1.0,1.0)
        if(frame is BSFrame) {
            val seq = frame.getSequence()
            if(seq is BSSequence) {
                for(value in seq.dataDef.frameInstances) {
                    val boxes =  rbox!!.frameInstances[value].boxes.map { rbox!!.boxInstances[it] }
                    val activeBox = getActiveBoxDef(frame.getStartTime(), boxes)
                    if(activeBox.valid) {
                        val actualBoxes: List<RboxFile.Box>?
                        when(activeBox.type) {
                            0 -> {
                                g.stroke = Color(0.0,0.0,1.0,0.8)
                                actualBoxes = activeBox.params?.map { rbox!!.boxes[it] }
                            }
                            4 -> {
                                g.stroke = Color(1.0,1.0,0.0,0.8)
                                actualBoxes = activeBox.params?.map { rbox!!.boxes[it] }
                            }
                            6 -> {
                                g.stroke = Color(0.0,1.0,1.0,0.8)
                                actualBoxes = activeBox.params?.map { rbox!!.boxes[it] }
                            }
                            0x38 -> {
                                g.stroke = Color(1.0,0.0,0.0,0.8)
                                actualBoxes = activeBox.attack?.unk3?.map { rbox!!.boxes[it] }
                            }
                            else -> {
                                g.stroke = Color(1.0,1.0,1.0,0.8)
                                actualBoxes = activeBox.params?.map { rbox!!.boxes[it] }
                            }
                        }


                        for(box in actualBoxes?: emptyList()) {
                            val scaleValue = 256.0
                            g.strokeRect(box.v1*scaleValue, -box.v2*scaleValue, box.v3*scaleValue, box.v4*scaleValue)
                        }
                    }
                }
            }
        }

        g.restore()
    }

    private fun getActiveBoxDef(frameNo: Int, boxdefs: List<RboxFile.BoxInstance>): RboxFile.BoxInstance {
        var index = 0
        var timeLeft = frameNo
        while(timeLeft >= boxdefs[index].duration) {
            timeLeft -= boxdefs[index].duration
            index++
        }
        return boxdefs[index]
    }
}