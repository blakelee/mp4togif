import androidx.compose.ui.awt.ComposeWindow
import java.awt.FileDialog
import java.io.File

fun saveFile(window: ComposeWindow, title: String, fileName: String): File {
    return FileDialog(window, title, FileDialog.SAVE).apply {
        isVisible = true
    }.run { File(directory + file) }
}