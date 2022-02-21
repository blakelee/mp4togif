import androidx.compose.runtime.Composable
import androidx.compose.ui.window.FrameWindowScope
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDropEvent
import java.io.File

@Composable
fun FrameWindowScope.DropHandler(onFileDropped: (File) -> Unit) {
    window.contentPane.dropTarget = object : DropTarget() {
        override fun drop(event: DropTargetDropEvent) {
            try {
                event.acceptDrop(DnDConstants.ACTION_REFERENCE)
                val droppedFiles = event
                    .transferable.getTransferData(
                        DataFlavor.javaFileListFlavor
                    ) as List<*>
                droppedFiles.first()?.let {
                    onFileDropped(it as File)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}