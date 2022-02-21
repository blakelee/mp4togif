import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalComposeUiApi::class)
@Preview
@Composable
fun NumberPicker(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var integer by remember { mutableStateOf(1) }
    var decimal by remember { mutableStateOf(0) }

    fun performOperation(mod: Int) {
        if (mod > 0 && decimal + 1 >= 100) {
            decimal = 0
            integer += mod
        } else if (mod < 0 && decimal - 1 < 0) {
            if (integer != 0) {
                decimal = 99
                integer += mod
            }
        } else {
            decimal += mod
        }

        onValueChange("$integer.${String.format("%02d", decimal)}")
    }

    fun increment() = performOperation(1)

    fun decrement() = performOperation(-1)

    Row(
        modifier.border(1.dp, Color.Black)
            .background(Color.White)
            .height(IntrinsicSize.Min)
    ) {

        BasicTextField(
            value = value,
            onValueChange = {
                var newInteger = 0
                var newDecimal = 0
                val numbers = it.split('.')
                if (numbers.size == 1) {
                    newInteger = numbers[0].toInt()
                } else if (numbers.size == 2) {
                    newInteger = numbers[0].toInt()

                    if (numbers[1].isEmpty()) {
                        newDecimal = 0
                    } else {
                        newDecimal = numbers[1].toInt()
                    }
                }

                integer = newInteger
                decimal = newDecimal

                onValueChange("$integer.${String.format("%02d", decimal)}")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.onPreviewKeyEvent { keyEvent ->
                var handled = true
                when {
                    keyEvent.key == Key.DirectionUp && keyEvent.type == KeyEventType.KeyDown -> increment()
                    keyEvent.key == Key.DirectionDown && keyEvent.type == KeyEventType.KeyDown -> decrement()
                    else -> handled = false
                }
                handled
            },
            textStyle = TextStyle(fontSize = 18.sp)
        )

        Column {
            Card(elevation = 2.dp, shape = RectangleShape, modifier = Modifier.clickable { increment() }) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    modifier = Modifier.fillMaxHeight(0.5f)
                        .width(16.dp)
                        .scale(2f)
                )
            }
            Card(elevation = 2.dp, shape = RectangleShape, modifier = Modifier.clickable { decrement() }) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.fillMaxHeight(1.0f)
                        .width(16.dp)
                        .scale(2f)
                )
            }
        }
    }
}