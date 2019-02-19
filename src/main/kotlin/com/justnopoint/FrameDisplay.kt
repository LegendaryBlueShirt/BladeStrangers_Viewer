package com.justnopoint

import com.justnopoint.`interface`.Character
import com.justnopoint.`interface`.FrameDataProvider
import com.justnopoint.`interface`.Sequence
import com.justnopoint.bladestrangers.BSFrameDataProvider
import com.justnopoint.bladestrangers.MemoryArchive
import com.justnopoint.util.AnimHelper
import com.justnopoint.util.createBufferedImage
import com.justnopoint.util.createIndexColorModel
import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.beans.value.ChangeListener
import javafx.event.EventHandler
import javafx.stage.Stage
import java.io.File
import java.io.FileOutputStream
import javax.imageio.ImageIO
import javafx.scene.layout.VBox
import javafx.scene.layout.Pane
import javafx.scene.layout.HBox
import javafx.scene.layout.BorderPane
import javafx.scene.Scene
import javafx.scene.control.MenuBar
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.control.ComboBox
import javafx.scene.control.Menu
import javafx.scene.control.MenuItem
import java.io.IOException
import javafx.scene.control.ButtonType
import java.lang.Exception
import java.util.*
import java.util.concurrent.CountDownLatch

class FrameDisplay: Application() {
    private lateinit var view: ViewerWindow

    var frameDataProvider: FrameDataProvider? = null

    var currentFrame = 0
    var animating = false
    var sequenceTime = 0
    var menu: MenuBar? = null
    var sequence: Sequence? = null
    var characterSelect: ComboBox<Character>? = null
    var sequenceSelect: ComboBox<Sequence>? = null

    companion object {
        const val FPS = 60
        const val frameDurationNanos = (1000000000.0/FPS).toLong()
    }

    private var keyListener: EventHandler<KeyEvent> = EventHandler { event ->
        when (event.code) {
            KeyCode.LEFT -> {
                if (currentFrame > 0)
                    currentFrame--
                animating = false
            }
            KeyCode.RIGHT -> {
                if (currentFrame + 1 < sequence?.getFramecount()?:-1) {
                    currentFrame++
                }
                animating = false
            }
            KeyCode.SPACE -> animating = !animating
            else -> {}
        }
    }

    override fun start(primaryStage: Stage) {
        view = ViewerWindow()
        primaryStage.onCloseRequest = view.getWindowCloseHandler()
        primaryStage.title = "Blade Strangers Framedisplay"

        val theScene = Scene(VBox(), 800.0, 600.0)

        theScene.addEventFilter<KeyEvent>(KeyEvent.KEY_PRESSED) { event -> keyListener.handle(event) }

        createMenu(view)
        menu?.prefWidthProperty()?.bind(primaryStage.widthProperty())

        val border = BorderPane()
        val topMenu = HBox()

        topMenu.children.addAll(characterSelect, sequenceSelect)
        // setup our canvas size and put it into the content of the frame
        border.top = topMenu

        val pane = Pane()
        border.center = pane

        pane.children.add(view)

        view.widthProperty().bind(primaryStage.widthProperty())
        view.heightProperty().bind(primaryStage.heightProperty().subtract(topMenu.heightProperty()))

        theScene.addEventFilter<MouseEvent>(MouseEvent.MOUSE_PRESSED) { event -> view.onClick(event) }
        theScene.addEventFilter<MouseEvent>(MouseEvent.MOUSE_DRAGGED) { event -> view.onDrag(event) }
        theScene.addEventFilter<MouseEvent>(MouseEvent.MOUSE_RELEASED) { event -> view.onRelease(event) }

        (theScene.root as VBox).children.addAll(menu, border)

        view.prepareForRendering()

        primaryStage.scene = theScene
        primaryStage.show()
    }

    private val loadDialog by lazy {
        LoadDialog()
    }
    private fun showDirectoryChooser() {
        val result = loadDialog.showAndWait()
        if (result.get() == ButtonType.OK) {
            loadDialog.folder?.let {
                frameDataProvider = BSFrameDataProvider(it)
                frameDataProvider?.let { frameData ->
                    view.renderer = frameData.getFrameRenderer()
                    characterSelect?.items?.clear()
                    characterSelect?.items?.addAll(frameData.getCharacters())
                    characterSelect?.isDisable = false
                    characterSelect?.selectionModel?.select(0)
                    characterSelect?.selectionModel?.selectedItem?.let {
                        characterChange(it)
                    }
                }
            }
            try {
                start()
            } catch (e: IOException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            } catch (e: InterruptedException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }

        }
    }

    private fun createMenu(view: ViewerWindow) {
        menu = MenuBar().apply {

            val fileMenu = Menu("File")
            val loadDirectory = MenuItem("Load Directory")
            loadDirectory.setOnAction { showDirectoryChooser() }
            fileMenu.items.add(loadDirectory)

            val viewMenu = Menu("View")
            val resetPosition = MenuItem("Reset Position")
            resetPosition.setOnAction { view.resetPosition() }
            viewMenu.items.add(resetPosition)

            menus.addAll(fileMenu, viewMenu)

            characterSelect = ComboBox()
            characterSelect?.valueProperty()?.addListener(ChangeListener<Character> { _, _, newValue ->
                if (newValue == null)
                    return@ChangeListener
                if (characterSelect?.isDisabled != false)
                    return@ChangeListener
                characterChange(newValue)
            })

            sequenceSelect = ComboBox()
            sequenceSelect?.valueProperty()?.addListener(ChangeListener<Sequence> { _, _, newValue ->
                if (newValue == null)
                    return@ChangeListener
                if (sequenceSelect?.isDisabled != false)
                    return@ChangeListener
                sequenceChange(newValue)
            })

            characterSelect?.isDisable = true
            sequenceSelect?.isDisable = true
        }
    }

    private fun characterChange(newCharacter: Character) {
        frameDataProvider?.loadCharacter(newCharacter)
        sequenceSelect?.items?.clear()
        frameDataProvider?.getSequences()?.let {
            sequenceSelect?.items?.addAll(it)
        }
        sequenceSelect?.isDisable = false
        sequenceSelect?.selectionModel?.select(0)
        sequenceSelect?.selectionModel?.selectedItem?.let {
            sequenceChange(it)
        }
    }

    private fun sequenceChange(newSequence: Sequence) {
        sequence = newSequence
        currentFrame = 0
        sequenceTime = 0
    }

    private var looper: AnimationTimer? = null
    @Throws(IOException::class, InterruptedException::class)
	fun start()  {
		view.running = false
		looper?.stop()
		looper = object: AnimationTimer() {
			var lastFrameNanos = 0L
			var framecount = 0
			var framesSkipped = 0
            var skipFrame = false

            override fun handle(now: Long) {
				framecount++
				if(animating) {
					sequenceTime++
					currentFrame = AnimHelper.getFrameForTime(sequence, sequenceTime)
				}

                if(sequence?.getFrames()?.isNotEmpty() == true) {
                    view.currentFrame = sequence?.getFrames()?.get(currentFrame)
                } else {
                    view.currentFrame = null
                }

				val currentFrameNanos = now-lastFrameNanos
				if(currentFrameNanos > frameDurationNanos) { //We're on time.
					skipFrame=true
				}

				if(!skipFrame) {
					view.render()
				} else {
					framesSkipped++
					skipFrame = false
				}

				lastFrameNanos = now
			}}
		looper?.start()
	}
}

fun main(args: Array<String>) {
    Application.launch(FrameDisplay::class.java, *args)
    //sprDump()
    //imgDump()
}

fun test() {
    val file = File("C:\\Program Files (x86)\\Steam\\steamapps\\common\\Blade Strangers\\fsroot\\spec_win\\chara.memfs")
    System.out.println("Now reading file.")
    if(file.exists()) {
        val archive = MemoryArchive.get(file)
        System.out.println("Attempting inflate.")
        archive.getRaniFile("ycg_sho.rani")?.let { rani ->
            FileOutputStream("output2.bin").use {
                it.write(rani.data.array())
            }

            rani.getRawSprite(310) { sprdata, width, height ->
                if(sprdata == null || width == null || height == null) {
                    return@getRawSprite
                }
                System.out.println("Got image.")
                for(n in 0 until 9) {
                    val palette = createIndexColorModel(rani.getPalette(n))
                    val image = createBufferedImage(data = sprdata, width = width, height = height, colorModel = palette)
                    ImageIO.write(image, "png", File("pal$n.png"))
                }
            }
        }
    }
}

fun imgDump() {
    val outFolder = File("output")
    outFolder.mkdir()
    val file = File("C:\\Program Files (x86)\\Steam\\steamapps\\common\\Blade Strangers\\fsroot\\spec_win")
    val adv = MemoryArchive.get(File(file, "adv.memfs"))
    for(n in 0 until adv.getNumFiles()) {
        val outfile = File(outFolder, adv.getFilename(n))
        outfile.parentFile?.mkdirs()
        val bytes = adv.getFileBytes(n)
        try {
            val output = FileOutputStream(outfile)
            output.use {
                it.write(bytes)
            }
        }catch (e: Exception) {}
    }
    val ssdata = MemoryArchive.get(File(file, "ssdata.memfs"))
    for(n in 0 until ssdata.getNumFiles()) {
        val outfile = File(outFolder, ssdata.getFilename(n))
        outfile.parentFile?.mkdirs()
        val bytes = ssdata.getFileBytes(n)
        try {
            val output = FileOutputStream(outfile)
            output.use {
                it.write(bytes)
            }
        }catch (e: Exception) {}
    }
    val ui = MemoryArchive.get(File(file, "ui.memfs"))
    for(n in 0 until ui.getNumFiles()) {
        val outfile = File(outFolder, ui.getFilename(n))
        outfile.parentFile?.mkdirs()
        val bytes = ui.getFileBytes(n)
        try {
            val output = FileOutputStream(outfile)
            output.use {
                it.write(bytes)
            }
        }catch (e: Exception) {}
    }
}

fun sprDump() {
    val file = File("C:\\Program Files (x86)\\Steam\\steamapps\\common\\Blade Strangers\\fsroot\\spec_win")
    val frames = BSFrameDataProvider(file)
    val chars = frames.getCharacters()
    val outFolder = File("output")
    outFolder.mkdir()
    var lock = CountDownLatch(1)
    for(character in chars) {
        frames.loadCharacter(character)
        val charFolder = File(outFolder, (character as BSFrameDataProvider.BSCharacter).tag)
        charFolder.mkdir()
        frames.rani?.let {
            val nSprites = it.getRawSpriteCount()
            it.getRawSprite(0) { data, width, height ->
                for(n in 0 until 9) {
                    val paldata = it.palettes?.get(n)
                    val palette = createIndexColorModel(paldata)
                    val image = createBufferedImage(data!!, width!!, height!!, palette)
                    val outFile = File(charFolder, "pal$n.png")
                    ImageIO.write(image, "png", outFile)
                }
            }

            val paldata = it.palettes?.get(1)
            val palette = createIndexColorModel(paldata)
            for(n in 0 until nSprites) {
                it.getRawSprite(n) { data, width, height ->
                    val image = createBufferedImage(data!!, width!!, height!!, palette)
                    val outFile = File(charFolder, String.format("%04d.png", n))
                    ImageIO.write(image, "png", outFile)
                    lock.countDown()
                }
                lock.await()
                lock = CountDownLatch(1)
            }
        }
    }
}