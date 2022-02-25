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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


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

    val ffmpeg = File(System.getProperty("compose.application.resources.dir")!!)
        .listFiles()!!
        .first { it.name.contains("ffmpeg") }

    val tempDir = File(System.getProperty("java.io.tmpdir") + "temp.jpg")

    file?.let {
        state = Loading
        rememberCoroutineScope().launch {
            withContext(Dispatchers.IO) {

                val command = "${ffmpeg.absolutePath} -y -ss 0.0 -i ${file.absolutePath} -vframes 1 -f image2 ${tempDir.absolutePath}"

                val process = ProcessBuilder()
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectErrorStream(true)
                    .command(command.split(' '))
                    .start()

                process.inputStream.close()
                process.waitFor()

                state = Processed(tempDir, file)
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

                        val process = ProcessBuilder()
                            .redirectOutput(ProcessBuilder.Redirect.PIPE)
                            .redirectErrorStream(true)
                            .command(currentState.command.split(' '))
                            .start()

                        process.inputStream.close()
                        process.waitFor()

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