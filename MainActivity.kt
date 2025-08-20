package com.example.calcnow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalcNowScientificApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalcNowScientificApp() {
    val client = remember { HttpClient(CIO) }
    val scope = rememberCoroutineScope()
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    val history = remember { mutableStateListOf<String>() }
    var showHistory by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("CalcNow - Scientific") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF8F8F8))
                .padding(12.dp)
        ) {
            // Display area
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = expression.ifEmpty { "0" },
                    fontSize = 30.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
                Text(
                    text = result,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Keypad grid (6 columns)
            val buttons = listOf(
                listOf("sin","cos","tan","^","(",")"),
                listOf("log","ln","sqrt","÷","AC","DEL"),
                listOf("7","8","9","×","%","pi"),
                listOf("4","5","6","-","ans","e"),
                listOf("1","2","3","+",".",""),
                listOf("0","00","","=","","" )
            )

            // Render rows
            for (row in buttons) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (label in row) {
                        CalculatorButton(label = label, modifier = Modifier
                            .weight(1f)
                            .height(56.dp)) {
                            when(label) {
                                "" -> { /* empty slot */ }
                                "AC" -> { expression = ""; result = "" }
                                "DEL" -> { if (expression.isNotEmpty()) expression = expression.dropLast(1) }
                                "=" -> {
                                    val exprConverted = formatExpressionForApi(expression)
                                    scope.launch {
                                        try {
                                            val resp: String = client.get("https://api.mathjs.org/v4/") {
                                                parameter("expr", exprConverted)
                                                accept(ContentType.Text.Plain)
                                            }.body()
                                            result = resp
                                            history.add(0, "$expression = $resp")
                                        } catch (e: Exception) {
                                            result = "Error: " + (e.message ?: e.toString())
                                        }
                                    }
                                }
                                "ans" -> { if (history.isNotEmpty()) expression += history.first().substringAfter(" = ") }
                                "sqrt" -> expression += "sqrt("
                                "×" -> expression += "*"
                                "÷" -> expression += "/"
                                "%" -> expression += "%"
                                "pi" -> expression += "pi"
                                "e" -> expression += "e"
                                else -> expression += label
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { showHistory = !showHistory }) {
                    Text(if (showHistory) "Hide History" else "Show History" )
                }
                Text(text = "${'$'}{history.size} items", modifier = Modifier.align(Alignment.CenterVertically))
            }

            if (showHistory) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                ) {
                    for (h in history) {
                        Text(text = h, modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp)
                            .clickable {
                                expression = h.substringBefore(" = ")
                            })
                        HorizontalDivider(
                            Modifier,
                            DividerDefaults.Thickness,
                            DividerDefaults.color
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier) {
        Text(text = label.ifEmpty { "" }, fontSize = 16.sp)
    }
}

fun formatExpressionForApi(expr: String): String {
    if (expr.isBlank()) return "0"
    val out = expr.replace("×","*").replace("÷","/").replace("√","sqrt")
    // safe: ensure percent handled by mathjs as (x/100) if desired; leave as % which mathjs interprets as modulus
    return out
}
