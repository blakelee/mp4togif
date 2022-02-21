import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption


fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        var file by remember { mutableStateOf<File?>(null) }
        DropHandler { file = null; file = it }
        App(file, this)
    }
}

@Composable
@Preview
fun App(file: File? = null, windowScope: FrameWindowScope) {

    var state by remember { mutableStateOf<State>(Initialize) }

    lateinit var ffmpeg: File

    useResource("ffmpeg") {
        val path = Paths.get("/tmp", "ffmpeg")
        Files.copy(it, path, StandardCopyOption.REPLACE_EXISTING)
        ffmpeg = path.toFile()
        ffmpeg.setReadable(true)
        ffmpeg.setExecutable(true)
    }

    file?.let {
        state = Loading
        rememberCoroutineScope().launch {
            withContext(Dispatchers.IO) {

                val command = "${ffmpeg.absolutePath} -y -ss 0.0 -i ${file.path} -vframes 1 -f image2 temp.jpg"

                ProcessBuilder()
                    .command(command.split(' '))
                    .start()
                    .waitFor()

                withContext(Dispatchers.Main) {
                    state = Processed(File("temp.jpg"), file)
                }
            }
        }
    }

    MaterialTheme {

        when (val currentState = state) {
            Initialize -> Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text("Drag and drop a video to begin")
            }
            Loading -> Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.size(96.dp))
            }
            is Processed -> Row {
                val imageBitmap: ImageBitmap = remember(currentState.stillImage) {
                    loadImageBitmap(currentState.stillImage.inputStream())
                }

                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(0.5f)
                )

                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    var linked by remember { mutableStateOf(true) }
                    var width by remember { mutableStateOf("1.00") }
                    var height by remember { mutableStateOf("1.00") }
                    val linkedState by derivedStateOf { if (linked) Icons.Default.Link else Icons.Default.LinkOff }

                    Text(
                        "Original dimensions: ${imageBitmap.height}x${imageBitmap.width}",
                        modifier = Modifier.padding(top = 32.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {

                        Column {
                            Text("Width Multiplier")
                            NumberPicker(
                                value = width,
                                onValueChange = { value ->
                                    width = value
                                    if (linked) {
                                        height = value
                                    }
                                }
                            )
                        }

                        Icon(linkedState, null,
                            modifier = Modifier.clickable { linked = !linked }
                                .padding(horizontal = 16.dp)
                                .align(Alignment.CenterVertically)
                        )

                        Column {
                            Text("Height Multiplier")
                            NumberPicker(
                                value = height,
                                onValueChange = { value ->
                                    height = value
                                    if (linked) {
                                        width = value
                                    }
                                }
                            )
                        }
                    }

                    Button(onClick = {
                        val file =
                            saveFile(windowScope.window, "Save Gif", currentState.stillImage.nameWithoutExtension)
                        val width = (width.toDouble() * imageBitmap.width).toInt()
                        val height = (height.toDouble() * imageBitmap.height).toInt()

                        val cmd = "${ffmpeg.absolutePath} -y -i ${currentState.originalFile.absolutePath} -vf " +
                                "scale=$width:$height,fps=50 ${file.absolutePath}"

                        state = Processing(cmd)
                    }) {
                        Text("Process")
                    }
                }
            }
            is Processing -> {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.size(96.dp))
                }

                rememberCoroutineScope().launch {
                    withContext(Dispatchers.IO) {

                        ProcessBuilder()
                            .command(currentState.command.split(' '))
                            .start()
                            .waitFor()

                        state = Converted
                    }
                }
            }
            Converted -> Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text("File converted to gif\nDrag and drop a video to begin", textAlign = TextAlign.Center)
            }
        }
    }
}

sealed class State
object Initialize : State()
object Loading : State()
data class Processed(val stillImage: File, val originalFile: File) : State()
data class Processing(val command: String) : State()
object Converted : State()