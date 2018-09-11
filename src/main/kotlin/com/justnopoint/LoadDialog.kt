package com.justnopoint

import com.justnopoint.bladestrangers.BSFrameDataProvider
import com.justnopoint.util.SteamHelper
import java.io.File
import java.io.IOException
import javafx.event.ActionEvent
import javafx.scene.control.ButtonType
import javafx.fxml.FXMLLoader
import javafx.fxml.FXML
import javafx.scene.Parent
import javafx.stage.DirectoryChooser
import javafx.scene.control.Dialog
import javafx.scene.control.TextField


class LoadDialog : Dialog<ButtonType>() {
    private var folderOk = false

    @FXML
    internal var bsHome: TextField? = null

    internal var fileChooser = DirectoryChooser()

    val folder: File
        get() = File(bsHome!!.text)

    @FXML
    protected fun handleLoadAction(event: ActionEvent) {
        if (folderOk) {
            result = ButtonType.OK
        }
        close()
    }

    @FXML
    protected fun handleCancelAction(event: ActionEvent) {
        result = ButtonType.CLOSE
        close()
    }

    @FXML
    protected fun showFolderChooser(event: ActionEvent) {
        fileChooser.title = "Open Resource File"
        fileChooser.initialDirectory = SteamHelper.bsDirectory?:File(".")
        val selectedFolder = fileChooser.showDialog(dialogPane.scene.window)
        if (selectedFolder != null) {
            bsHome!!.text = selectedFolder.path
        }
    }

    init {
        title = "Loading Options"
        result = ButtonType.CANCEL

        try {
            val loader = FXMLLoader(javaClass.getResource("/LoadDialog.fxml"))
            loader.setController(this)
            val root = loader.load<Parent>()
            dialogPane.content = root
            dialogPane.buttonTypes.add(ButtonType.CLOSE)
            val closeButton = dialogPane.lookupButton(ButtonType.CLOSE)
            closeButton.managedProperty().bind(closeButton.visibleProperty())
            closeButton.isVisible = false

            bsHome!!.textProperty().addListener { _, _, newValue ->
                val validatedFolder = validateFolder(File(newValue))
                folderOk = if (validatedFolder == null) {
                    false
                } else {
                    bsHome!!.text = validatedFolder.path
                    true
                }
            }
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

    }

    private fun validateFolder(folder: File): File? {
        return BSFrameDataProvider.validateFolder(folder)
    }
}