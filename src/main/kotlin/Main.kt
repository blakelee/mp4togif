import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        this.window.setSize(300, 300)
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

    ffmpeg.setExecutable(true)

    file?.let {
        state = Processed(file)
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

                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    var linked by remember { mutableStateOf(true) }
                    var width by remember { mutableStateOf("1.00") }
                    var height by remember { mutableStateOf("1.00") }
                    val linkedState by derivedStateOf { if (linked) Icons.Default.Link else Icons.Default.LinkOff }

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
                        val directory = currentState.originalFile.parent
                        val name = currentState.originalFile.nameWithoutExtension
                        val fileName: String = directory + File.separatorChar + name + ".gif"

                        val cmd = "${ffmpeg.absolutePath} -y -i ${currentState.originalFile.absolutePath} -vf " +
                                "scale=iw*$width:ih*$height,fps=50 $fileName"

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

                        process.waitFor()
                        process.inputStream.close()

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
data class Processed(val originalFile: File) : State()
data class Processing(val command: String) : State()
object Converted : State()