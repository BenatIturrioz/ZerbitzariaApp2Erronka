package com.example.zerbitzariapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerbitzariapp.ui.theme.ZerbitzariAppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.*
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.log


@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZerbitzariAppTheme {
                NavigationHandler()
            }
        }
    }
}


var Erabiltzaileid: Int = 0;
var MahaiaId: Int = 0;
var ErreserbaId: Int = 0;
var ErreserbaIdToDelete: Int = 0;



data class EskaeraProduktua(
    val name: String,
    val quantity: Int,
    val price: Double
)

var eskaeraProduktuak: MutableList<EskaeraProduktua> = mutableListOf()



@SuppressLint("RememberReturnType")
@Composable
fun ChatScreen(onNavigateToChat: () -> Unit,
               onNavigateToMahaiakAukeratu: () -> Unit,

) {
    var messageText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<Pair<String, Boolean>>() }
    val chatClient = remember { ChatClient(messages) }

    // Conectar al servidor cuando se inicia la pantalla
    LaunchedEffect(true) {
        chatClient.connect()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Mostrar los mensajes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                border = BorderStroke(2.dp, Color.Black),
                onClick = { onNavigateToMahaiakAukeratu() },
                colors = ButtonDefaults.buttonColors(Color(0xFFFF6600))
            ) {
                Text(text = "Atzera", color = Color.Black)
            }

            Button(
                border = BorderStroke(2.dp, Color.Black),
                onClick = { onNavigateToChat() },  // Navegar a la pantalla de chat
                colors = ButtonDefaults.buttonColors(Color(0xFFFF6600))
            ) {
                Text(text = "Txat", color = Color.Black)
            }

        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(messages) { (message, isUser) ->
                MessageItem(message = message, isUser = isUser)
            }
        }

        // Campo de texto para el mensaje
        Row(modifier = Modifier.padding(16.dp)) {
            TextField(
                value = messageText,
                onValueChange = { messageText = it },
                label = { Text("Escribe tu mensaje...") },
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = {
                if (messageText.isNotBlank()) {
                    chatClient.sendMessage(messageText) // Enviar mensaje al servidor
                    messageText = "" // Limpiar campo de texto
                }
            }) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
fun MessageItem(message: String, isUser: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(8.dp)
        ) {
            Text(text = message, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}


class ChatClient(private val messages: MutableList<Pair<String, Boolean>>) {

    private var out: PrintWriter? = null
    private var reader: BufferedReader? = null
    private var socket: Socket? = null

    suspend fun connect() {
        try {
            println("Intentando conectar al servidor...")
            socket = withContext(Dispatchers.IO) {
                Socket("localhost", 5555)
            }
            println("Conectado al servidor.")

            out = PrintWriter(OutputStreamWriter(socket!!.getOutputStream()), true)
            reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))

            withContext(Dispatchers.IO) {
                while (true) {
                    val message = reader?.readLine()
                    if (message == null) {
                        println("Conexión cerrada por el servidor.")
                        break
                    }
                    println("Mensaje recibido: $message")
                    messages.add(Pair(message, false))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error en connect(): ${e.message}")
            messages.add(Pair("Error al conectar con el servidor: ${e.message}", false))
        }
    }

    fun sendMessage(message: String) {
        try {
            println("Intentando enviar mensaje: $message")

            if (socket == null || socket!!.isClosed || !socket!!.isConnected) {
                println("Error: El socket no está conectado.")
                messages.add(Pair("Error: El socket no está conectado.", false))
                return
            }

            if (out == null) {
                println("Error: PrintWriter no está inicializado.")
                messages.add(Pair("Error: PrintWriter no está inicializado.", false))
                return
            }

            val prefixedMessage = "[Zerbitzaria] $message"
            out!!.println(prefixedMessage)
            println("Mensaje enviado correctamente.")

            messages.add(Pair(message, true))
        } catch (e: IOException) {
            e.printStackTrace()
            println("Error en sendMessage(): ${e.message}")
            messages.add(Pair("Error al enviar el mensaje: ${e.message}", false))
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error inesperado en sendMessage(): ${e.message}")
            messages.add(Pair("Error inesperado: ${e.message}", false))
        }
    }

    fun disconnect() {
        try {
            println("Desconectando del servidor...")
            socket?.close()
            println("Desconexión completada.")
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error en disconnect(): ${e.message}")
        }
    }
}

fun fetchTables(onResult: (List<String>) -> Unit) {
    val client = OkHttpClient()
    val url = "http://10.0.2.2/getMesas.php" // Cambia esto si usas un dispositivo físico, usa la IP de tu PC en lugar de 10.0.2.2
    val request = Request.Builder()
        .url(url)
        .build()

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val jsonData = response.body?.string()

                val tableList = mutableListOf<String>()

                if (!jsonData.isNullOrEmpty()) {
                    val jsonArray = JSONArray(jsonData)
                    for (i in 0 until jsonArray.length()) {
                        val table = jsonArray.getJSONObject(i).getString("mahaia_id") // Aquí asumes que el JSON tiene un campo "id"
                        tableList.add(table)
                    }
                }

                // Log de las mesas obtenidas
                Log.d("FetchTables", "Table List: $tableList")

                // Llamar a onResult en el hilo principal
                withContext(Dispatchers.Main) {
                    onResult(tableList)
                }
            } else {
                Log.e("FetchTables", "Error: ${response.code}")
            }
        } catch (e: Exception) {
            // Manejo de excepciones
            e.printStackTrace()
            Log.e("FetchTables", "Request failed: ${e.message}")
        }
    }

}

fun fetchProduktua(onResult: (List<Produktua>) -> Unit) {
    val client = OkHttpClient()
    val url = "http://10.0.2.2/get.php"

    val request = Request.Builder()
        .url(url)
        .build()

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val jsonData = response.body?.string()

                val produktuak = mutableListOf<Produktua>()

                if (!jsonData.isNullOrEmpty()) {
                    val jsonArray = JSONArray(jsonData)
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val produktua = Produktua(
                            id = jsonObject.getInt("id"),
                            izena = jsonObject.getString("izena"),
                            deskribapena = jsonObject.getString("deskribapena"),
                            prezioa = jsonObject.getDouble("prezioa").toFloat(),
                            erosketaPrezioa = jsonObject.getDouble("erosketaPrezioa").toFloat(),
                            kantitatea = jsonObject.getInt("kantitatea"),
                            kantitateMinimoa = jsonObject.getInt("kantitateMinimoa"),
                            mota = jsonObject.getInt("mota")
                        )
                        produktuak.add(produktua)
                    }
                }

                // Log de los productos obtenidos
                Log.d("FetchProduktua", "Product List: $produktuak")

                // Llamar a onResult en el hilo principal
                withContext(Dispatchers.Main) {
                    onResult(produktuak)
                }
            } else {
                Log.e("FetchProduktua", "Error: ${response.code}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("FetchProduktua", "Request failed: ${e.message}")
        }
    }
}

fun fetchErabiltzaileak(onResult: (List<Zerbitzaria>) -> Unit) {
    val client = OkHttpClient()
    val url = "http://10.0.2.2/getZerbitzariak.php" // Cambia esto si usas un dispositivo físico, usa la IP de tu PC en lugar de 10.0.2.2
    val request = Request.Builder()
        .url(url)
        .build()

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val jsonData = response.body?.string()

                val zerbitzariak = mutableListOf<Zerbitzaria>()

                if (!jsonData.isNullOrEmpty()) {
                    val jsonArray = JSONArray(jsonData)
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val zerbitzaria = Zerbitzaria(
                            id = jsonObject.getInt("id"),
                            izenaZ = jsonObject.getString("erabiltzaileIzena")
                        )
                        zerbitzariak.add(zerbitzaria)
                    }
                }

                // Llamar a onResult en el hilo principal
                withContext(Dispatchers.Main) {
                    onResult(zerbitzariak)
                }
            } else {
                Log.e("FetchErabiltzaileak", "Error: ${response.code}")
            }
        } catch (e: Exception) {
            // Manejo de excepciones
            e.printStackTrace()
            Log.e("FetchErabiltzaileak", "Request failed: ${e.message}")
        }
    }
}

fun fetchHighestErreserbaId(onResult: (Int?) -> Unit) {
    val client = OkHttpClient()
    val url = "http://10.0.2.2/getErreserba.php" // Cambia esto si usas un dispositivo físico
    val request = Request.Builder()
        .url(url)
        .build()

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val jsonData = response.body?.string()

                var maxErreserbaId: Int? = null  // Establecer como null por defecto

                // Solo asignamos un valor a maxErreserbaId si tenemos un valor válido de la base de datos
                if (!jsonData.isNullOrEmpty()) {
                    val jsonObject = JSONObject(jsonData)
                    val fetchedId = jsonObject.optString("erreserba_id")?.toIntOrNull()
                    maxErreserbaId = fetchedId ?: null  // Si no es válido, se deja como null
                }

                // Si no se recibe nada válido, asignamos 1 como valor por defecto
                if (maxErreserbaId == null) {
                    maxErreserbaId = 1
                    ErreserbaId = maxErreserbaId
                } else {
                    // Si recibimos un valor válido, le sumamos 1
                    maxErreserbaId += 1
                    ErreserbaId = maxErreserbaId
                }

                Log.d("FetchHighestErreserbaId", "Highest ErreserbaId (after increment): $ErreserbaId")

                withContext(Dispatchers.Main) {
                    onResult(maxErreserbaId)
                }
            } else {
                Log.e("FetchHighestErreserbaId", "Error: ${response.code}")
                withContext(Dispatchers.Main) { onResult(null) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("FetchHighestErreserbaId", "Request failed: ${e.message}")
            withContext(Dispatchers.Main) { onResult(null) }
        }
    }
}



fun getEskaerak(onResult: (List<String>?) -> Unit) {
    val client = OkHttpClient()
    val url = "http://10.0.2.2/getEskaera.php" // Cambia esto si usas un dispositivo físico
    val request = Request.Builder()
        .url(url)
        .build()

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val jsonData = response.body?.string()

                val eskaerakList = mutableListOf<String>()

                if (!jsonData.isNullOrEmpty()) {
                    val jsonArray = JSONArray(jsonData)
                    for (i in 0 until jsonArray.length()) {
                        val eskaera = jsonArray.getJSONObject(i).optString("eskaera", "")
                        if (eskaera.isNotEmpty()) {
                            eskaerakList.add(eskaera)
                        }
                    }
                }

                Log.d("GetEskaerak", "Eskaerak: $eskaerakList")

                withContext(Dispatchers.Main) {
                    onResult(eskaerakList)
                }
            } else {
                Log.e("GetEskaerak", "Error: ${response.code}")
                withContext(Dispatchers.Main) { onResult(null) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("GetEskaerak", "Request failed: ${e.message}")
            withContext(Dispatchers.Main) { onResult(null) }
        }
    }
}



fun fetchMaxEskaera(onResult: (List<EskaeraProduktua>?) -> Unit) {
    val client = OkHttpClient()
    val url = "http://10.0.2.2/getEskaeraProduktua.php"

    val request = Request.Builder()
        .url(url)
        .get()
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
            onResult(null)
        }

        override fun onResponse(call: Call, response: Response) {
            response.body?.string()?.let { responseBody ->
                try {
                    val jsonResponse = JSONObject(responseBody)
                    if (jsonResponse.getBoolean("success")) {
                        val data = jsonResponse.getJSONArray("data1")
                         eskaeraProduktuak = mutableListOf<EskaeraProduktua>()
                        for (i in 0 until data.length()) {
                            val item = data.getJSONObject(i)
                            eskaeraProduktuak.add(
                                EskaeraProduktua(
                                    name = item.getString("produktu_izena"),
                                    quantity = item.getInt("produktuaKop"),
                                    price = item.getDouble("prezioa")
                                )
                            )
                        }
                        onResult(eskaeraProduktuak)
                    } else {
                        Log.e("fetchMaxErreserba", jsonResponse.getString("message"))
                        onResult(null)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    onResult(null)
                }
            }
        }
    })
}

fun insertEskaera() {
    val client = OkHttpClient()
    val url = "http://10.0.2.2/insertEskaeraOsoa.php"

    val ordaindua = 0

    // Log de los productos que estás insertando
    Log.d("insertKomanda", "EskaeraProduktua: ${eskaeraProduktuak.joinToString(", ") { "Name: ${it.name}, Quantity: ${it.quantity}, Price: ${it.price}" }}")

    // Log de la ID de la mesa
    Log.d("Mahaia", MahaiaId.toString())

    // Log del valor total de la venta
    val prezioTotala = PrezioTotalaKalkulatu(eskaeraProduktuak)
    Log.d("PrezioTotala", "Total price calculated: $prezioTotala")

    // Verifica que `ErreserbaId` es el esperado
    Log.d("ErreserbaId", "ErreserbaId before insert: $ErreserbaId")

    val jsonBody = JSONArray().apply {
        val eskaeraJson = JSONObject().apply {
            put("langilea_id", Erabiltzaileid)
            put("erreserba_id", ErreserbaId)  // Verifica que aquí esté pasando el valor correcto
            put("mahaia_id", MahaiaId)
            put("prezioTotala", prezioTotala)
            put("ordaindua", ordaindua)
        }
        put(eskaeraJson)
    }

    // Log del cuerpo de la solicitud para asegurar que los datos son correctos
    Log.d("RequestBody", "Request body to be sent: $jsonBody")

    val requestBody = jsonBody.toString()
        .toRequestBody("application/json; charset=utf-8".toMediaType())

    val request = Request.Builder()
        .url(url)
        .post(requestBody)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
            Log.e("insertKomanda", "Error: ${e.localizedMessage}")
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                response.body?.string()?.let { responseBody ->
                    val jsonResponse = JSONObject(responseBody)
                    if (jsonResponse.getBoolean("success")) {
                        Log.i("insertKomanda", "Insert successful: ${jsonResponse.getString("message")}")
                    } else {
                        Log.e("insertKomanda", "Error: ${jsonResponse.getString("message")}")
                    }
                }
            } else {
                Log.e("insertKomanda", "Response failed with code ${response.code}")
            }
        }
    })
}




fun PrezioTotalaKalkulatu(eskaeraProduktuak: List<EskaeraProduktua>): Double {
    val grupadoPorReserva = eskaeraProduktuak.groupBy { it.name }
    Log.d(eskaeraProduktuak.toString(), "PrezioTotalaKalkulatu: ")
    return grupadoPorReserva.values.sumOf { grupo ->
        grupo.sumOf { it.quantity * it.price }
    }
}




sealed class Screen {
    object CharlieApp : Screen()
    object MainScreen : Screen()
    object MahaiakAukeratu : Screen()
    object KomandaAukeratu : Screen()
    object EntranteakScreen : Screen()
    object LehenPlateraScreen : Screen()
    object PostreakScreen : Screen()
    object EdariakScreen : Screen()
    object KomandaScreen : Screen()
    object ChatScreen : Screen()
}


@Composable
fun NavigationHandler() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.CharlieApp) }

    when (currentScreen) {
        is Screen.CharlieApp -> CharlieApp(onNavigateToMainScreen = { currentScreen = Screen.MainScreen }, onNavigateToChat = { currentScreen = Screen.ChatScreen })
        is Screen.MainScreen -> MainScreen(onNavigateToMahaiakAukeratu = { currentScreen = Screen.MahaiakAukeratu }, onNavigateToChat = { currentScreen = Screen.ChatScreen }, onNavigateToCharliesApp = { currentScreen = Screen.MainScreen })
        is Screen.MahaiakAukeratu -> MahaiakAukeratu(onNavigateToKomandaAukeratu = { currentScreen = Screen.KomandaAukeratu }, onNavigateToChat = { currentScreen = Screen.ChatScreen }, onNavigateToMainScreen = { currentScreen = Screen.MainScreen })
        is Screen.KomandaAukeratu -> KomandaAukeratuScreen(
            onNavigateToEntranteak = { currentScreen = Screen.EntranteakScreen },
            onNavigateToLehenPlatera = { currentScreen = Screen.LehenPlateraScreen },
            onNavigateToPostreak = { currentScreen = Screen.PostreakScreen },
            onNavigateToEdariak = { currentScreen = Screen.EdariakScreen },
            onNavigateToKomanda = { currentScreen = Screen.KomandaScreen },
            onNavigateToChat = { currentScreen = Screen.ChatScreen },
            onNavigateToMahaiakAukeratu = { currentScreen = Screen.MahaiakAukeratu } // Añadir navegación al chat
        )
        is Screen.EntranteakScreen -> EntranteakScreen(onNavigateToKomandaAukeratu = { currentScreen = Screen.KomandaAukeratu }, onNavigateToChat = { currentScreen = Screen.ChatScreen })
        is Screen.LehenPlateraScreen -> LehenPlateraScreen(onNavigateToKomandaAukeratu = { currentScreen = Screen.KomandaAukeratu }, onNavigateToChat = { currentScreen = Screen.ChatScreen })
        is Screen.PostreakScreen -> PostreakScreen(onNavigateToKomandaAukeratu = { currentScreen = Screen.KomandaAukeratu }, onNavigateToChat = { currentScreen = Screen.ChatScreen })
        is Screen.EdariakScreen -> EdariakScreen(onNavigateToKomandaAukeratu = { currentScreen = Screen.KomandaAukeratu }, onNavigateToChat = { currentScreen = Screen.ChatScreen })
        is Screen.KomandaScreen -> KomandaScreen(onNavigateToChat = { currentScreen = Screen.ChatScreen }, onNavigateToKomandaAukeratu = {currentScreen = Screen.KomandaAukeratu}, onNavigateToCharlieApp = {currentScreen = Screen.CharlieApp})
        is Screen.ChatScreen -> ChatScreen(onNavigateToChat = {currentScreen = Screen.ChatScreen}, onNavigateToMahaiakAukeratu = {currentScreen = Screen.MahaiakAukeratu})  // Mostramos la pantalla del chat
        else -> {}
    }
}


fun insertEskaeraProduktua(
    selectedItems: List<EskaeraProduktua>,
    erreserbaId: Int,
    onResult: (Boolean) -> Unit
) {
    if (selectedItems.isEmpty()) {
        Log.e("insertErreserba", "No hay productos seleccionados para insertar.")
        onResult(false)
        return
    }

    val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    val url = "http://10.0.2.2/insertEskaera.php"

    val jsonBody = JSONArray().apply {
        selectedItems.forEach { item ->
            val productoJson = JSONObject().apply {
                put("erreserba_id", ErreserbaId)
                put("produktu_izena", item.name)
                put("produktuaKop", item.quantity)
                put("prezioa", item.price)
            }
            put(productoJson)
            Log.e("produktuak gordetzen","Gorde dira!")
        }
    }

    Log.d("JSONBody", "JSON Enviado: $jsonBody")

    val requestBody = jsonBody.toString()
        .toRequestBody("application/json; charset=utf-8".toMediaType())

    val request = Request.Builder()
        .url(url)
        .post(requestBody)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("insertErreserba", "Error al realizar la solicitud: ${e.message}")
            onResult(false)
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                Log.d("insertErreserba", "Inserción exitosa")
                onResult(true)
            } else {
                Log.e("insertErreserba", "Error en la respuesta del servidor: ${response.code}")
                onResult(false)
            }
        }
    })
}
@Composable
fun ProduktuaListScreen(
    mota: Int,
    title: String,
    onNavigateToKomandaAukeratu: () -> Unit,
    onNavigateToChat: () -> Unit
) {
    var produktuak by remember { mutableStateOf<List<Produktua>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val selectedQuantities = remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }

    LaunchedEffect(Unit) {
        fetchProduktua { result ->
            produktuak = result.filter { it.mota == mota }
            isLoading = false
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                border = BorderStroke(2.dp, Color.Black),
                onClick = { onNavigateToKomandaAukeratu()},
                colors = ButtonDefaults.buttonColors(Color(0xFFFF6600))
            ) {
                Text(text = "Atzera", color = Color.Black)
            }

            Button(
                border = BorderStroke(2.dp, Color.Black),
                onClick = { onNavigateToChat() },  // Navegar a la pantalla de chat
                colors = ButtonDefaults.buttonColors(Color(0xFFFF6600))
            ) {
                Text(text = "Txat", color = Color.Black)
            }

        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = title, color = Color.Black)

        if (isLoading) {
            CircularProgressIndicator(color = Color(0xFFFF6600))
        } else {
            produktuak.forEach { produktua ->
                KomandaItemRow(
                    itemName = produktua.izena,
                    initialQuantity = 0,
                    onQuantityChanged = { quantity ->
                        selectedQuantities.value = selectedQuantities.value.toMutableMap().apply {
                            put(produktua.id, quantity)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                val selectedItems = selectedQuantities.value.mapNotNull { (id, quantity) ->
                    val produktua = produktuak.find { it.id == id }
                    if (produktua != null && quantity > 0) {
                        EskaeraProduktua(
                            name = produktua.izena,
                            quantity = quantity,
                            price = produktua.prezioa.toDouble()
                        )
                    } else null
                }

                ErreserbaId?.let {
                    insertEskaeraProduktua(
                        erreserbaId = ErreserbaId.plus(1),

                        selectedItems = selectedItems
                    ) { success ->
                        if (success) {
                            // Navegar o mostrar éxito
                            Log.e("ID","$ErreserbaId");
                            onNavigateToKomandaAukeratu()

                        } else {
                            // Mostrar error
                            Log.e("InsertErreserba", "Error al insertar la comanda")
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(Color(0xFFFF6600))
        ) {
            Text(text = "Komanda bukatu", color = Color.Black)
        }
    }
}



@Composable
fun EntranteakScreen(onNavigateToKomandaAukeratu: () -> Unit, onNavigateToChat: () -> Unit) {
    ProduktuaListScreen(
        mota = 1,
        title = "Entranteak",
        onNavigateToKomandaAukeratu = onNavigateToKomandaAukeratu,
        onNavigateToChat = onNavigateToChat
    )
}

@Composable
fun LehenPlateraScreen(onNavigateToKomandaAukeratu: () -> Unit, onNavigateToChat: () -> Unit) {
    ProduktuaListScreen(
        mota = 2,
        title = "Lehen Platerak",
        onNavigateToKomandaAukeratu = onNavigateToKomandaAukeratu,
        onNavigateToChat = onNavigateToChat
    )
}

@Composable
fun PostreakScreen(onNavigateToKomandaAukeratu: () -> Unit, onNavigateToChat: () -> Unit) {
    ProduktuaListScreen(
        mota = 3,
        title = "Postreak",
        onNavigateToKomandaAukeratu = onNavigateToKomandaAukeratu,
        onNavigateToChat = onNavigateToChat
    )
}

@Composable
fun EdariakScreen(onNavigateToKomandaAukeratu: () -> Unit, onNavigateToChat: () -> Unit) {
    ProduktuaListScreen(
        mota = 4,
        title = "Edariak",
        onNavigateToKomandaAukeratu = onNavigateToKomandaAukeratu,
        onNavigateToChat = onNavigateToChat
    )
}

@Composable
fun CharlieApp(onNavigateToMainScreen: () -> Unit, onNavigateToChat: () -> Unit) {
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        fetchHighestErreserbaId { result ->
            //ErreserbaId = ErreserbaId.plus(1); // Incrementar en 1 después de obtener el valor
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    border = BorderStroke(2.dp, Color.Black),
                    onClick = { /* Acción de cerrar */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6600))
                ) {
                    Text("Itxi", color = Color.Black)
                }
                Button(
                    border = BorderStroke(2.dp, Color.Black),
                    onClick = { onNavigateToChat() },  // Navegar a la pantalla de chat
                    colors = ButtonDefaults.buttonColors(Color(0xFFFF6600))
                ) {
                    Text(text = "Txat", color = Color.Black)
                }

            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color(0xFFFF6600))
            } else {

                Spacer(modifier = Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Button(
                        border = BorderStroke(2.dp, Color.Black),
                        onClick = {
                            onNavigateToMainScreen()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6600))
                    ) {
                        Text("Zerbitzaria", fontSize = 16.sp, color = Color.Black)
                    }
                    Button(
                        border = BorderStroke(2.dp, Color.Black),
                        onClick = { /* Acción para Sukaldaria */ },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6600))
                    ) {
                        Text("Sukaldaria", fontSize = 16.sp, color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(onNavigateToMahaiakAukeratu: () -> Unit, onNavigateToChat: () -> Unit, onNavigateToCharliesApp: () -> Unit) {
    var zerbitzariak by remember { mutableStateOf<List<Zerbitzaria>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        fetchErabiltzaileak { result ->
            zerbitzariak = result
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    border = BorderStroke(2.dp, Color.Black),
                    onClick = { onNavigateToCharliesApp() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6600))
                ) { Text("Atzera", color = Color.Black) }
                Button(
                    border = BorderStroke(2.dp, Color.Black),
                    onClick = { onNavigateToChat() },  // Navegar a la pantalla de chat
                    colors = ButtonDefaults.buttonColors(Color(0xFFFF6600))
                ) {
                    Text(text = "Txat", color = Color.Black)
                }

            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                zerbitzariak.forEach { zerbitzaria ->
                    ZerbitzariaButton(
                        id = zerbitzaria.id,
                        text = zerbitzaria.izenaZ,
                        onNavigateToMahaiakAukeratu = onNavigateToMahaiakAukeratu,

                    )
                }
            }
        }
    }
}

@Composable
fun MahaiakAukeratu(onNavigateToKomandaAukeratu: () -> Unit, onNavigateToChat: () -> Unit, onNavigateToMainScreen: () -> Unit) {
    val tableList = remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        fetchTables { fetchedTables ->
            tableList.value = fetchedTables
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    border = BorderStroke(2.dp, Color.Black),
                    onClick = { onNavigateToMainScreen() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6600))
                ) {
                    Text("Atzera", color = Color.Black)
                }
                Button(
                    border = BorderStroke(2.dp, Color.Black),
                    onClick = { onNavigateToChat() },  // Navegar a la pantalla de chat
                    colors = ButtonDefaults.buttonColors(Color(0xFFFF6600))
                ) {
                    Text(text = "Txat", color = Color.Black)
                }

            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)  // Espaciado entre los elementos
        ) {
            tableList.value.chunked(2).forEach { rowTables ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    rowTables.forEach { table ->
                        Box(
                            modifier = Modifier.weight(1f)
                        ) {
                            MahaiaButton(
                                text = table,
                                function = onNavigateToKomandaAukeratu,
                                onNavigateToChat = onNavigateToChat
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))


        }
    }
}

@Composable
fun MahaiaButton(text: String, function: () -> Unit, onNavigateToChat: () -> Unit) {


    Button(
        onClick ={function()
                MahaiaId = text.toInt()},
        modifier = Modifier
            .padding(16.dp)  // Ajusta el padding
            .clip(CircleShape)  // Hace el botón redondo (si lo prefieres redondo)
            .size(100.dp)  // Hace que el botón sea cuadrado (puedes ajustar el valor)
            .height(100.dp),  // Define la altura y el ancho igual
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6600))
    ) {
        Text(text, color = Color.Black)
    }
}

@Composable
fun KomandaAukeratuScreen(
    onNavigateToEntranteak: () -> Unit,
    onNavigateToLehenPlatera: () -> Unit,
    onNavigateToPostreak: () -> Unit,
    onNavigateToEdariak: () -> Unit,
    onNavigateToKomanda: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToMahaiakAukeratu: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                border = BorderStroke(2.dp, Color.Black),
                onClick = { onNavigateToMahaiakAukeratu() },
                colors = ButtonDefaults.buttonColors(Color(0xFFFF6600))
            ) {
                Text(text = "Atzera", color = Color.Black)
            }

            Button(
                border = BorderStroke(2.dp, Color.Black),
                onClick = { onNavigateToChat() },
                colors = ButtonDefaults.buttonColors(Color(0xFFFF6600))
            ) {
                Text(text = "Txat", color = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNavigateToEntranteak,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(Color(0xFFFF6600))
        ) {
            Text("Entranteak", color = Color.Black)
        }


        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNavigateToLehenPlatera,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(Color(0xFFFF6600))
        ) {
            Text("1. Platera", color = Color.Black)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNavigateToPostreak,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(Color(0xFFFF6600))
        ) {
            Text("Postreak", color = Color.Black)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNavigateToEdariak,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(Color(0xFFFF6600))
        ) {
            Text("Edaria", color = Color.Black)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                ErreserbaIdToDelete = ErreserbaId;
                Log.d("DeleteEskaeraProduktua", "cuando se pasa de erreserbaId a ErreserbaToDelete: $ErreserbaId")
                insertEskaera()  // Llamar a la función primero
                onNavigateToKomanda()  // Luego navegar
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(Color(0xFFFF6600))
        ) {
            Text("Komanda", color = Color.Black)
        }

    }
}

@Composable
fun ZerbitzariaButton(
    id: Int,
    text: String,
    onNavigateToMahaiakAukeratu: () -> Unit,
) {
    Button(
        border = BorderStroke(2.dp, Color.Black),
        onClick = {
            Erabiltzaileid = id
            onNavigateToMahaiakAukeratu()
        },
        shape = RoundedCornerShape(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6600)),
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .height(60.dp)
    ) {
        Text(text, fontSize = 16.sp, color = Color.Black)
    }
}

fun deleteEskaeraProduktua(erreserbaId: Int, onResult: (Boolean, String) -> Unit) {
    val client = OkHttpClient()
    val url = "http://10.0.2.2/deleteEskaeraProduktua.php" // Cambia esta URL si es necesario

    // Crear el cuerpo de la solicitud POST con el erreserba_id
    val body: RequestBody = FormBody.Builder()
        .add("erreserba_id", ErreserbaId.toString())
        .build()

    // Crear la solicitud
    val request = Request.Builder()
        .url(url)
        .post(body)
        .build()

    Log.d("DeleteEskaeraProduktua", "Solicitud enviada a: $url")
    Log.d("DeleteEskaeraProduktua", "Cuerpo de la solicitud: erreserba_id=$ErreserbaId")

    // Realizar la solicitud en un hilo en segundo plano
    CoroutineScope(Dispatchers.IO).launch {
        try {
            Log.d("DeleteEskaeraProduktua", "Ejecutando la solicitud...")
            val response = client.newCall(request).execute()

            Log.d("DeleteEskaeraProduktua", "Respuesta recibida: ${response.code} - ${response.message}")

            if (response.isSuccessful) {
                val jsonResponse = response.body?.string()
                Log.d("DeleteEskaeraProduktua", "Respuesta JSON del servidor: $jsonResponse")

                // Comprobar si la respuesta es exitosa
                if (jsonResponse != null && jsonResponse.contains("\"success\":true")) {
                    Log.d("DeleteEskaeraProduktua", "La eskaera fue eliminada con éxito")
                    withContext(Dispatchers.Main) {
                        onResult(true, "Eskaera eliminada correctamente")
                    }
                } else {
                    Log.e("DeleteEskaeraProduktua", "No se encontró la eskaera o hubo un problema al eliminar: $jsonResponse")
                    withContext(Dispatchers.Main) {
                        onResult(false, "No se encontró la eskaera o hubo un problema al eliminar")
                    }
                }
            } else {
                Log.e("DeleteEskaeraProduktua", "Error en la solicitud: Código de respuesta ${response.code}")
                withContext(Dispatchers.Main) {
                    onResult(false, "Error en la solicitud: ${response.code}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("DeleteEskaeraProduktua", "Excepción durante la solicitud: ${e.message}")
            withContext(Dispatchers.Main) {
                onResult(false, "Error en la solicitud")
            }
        }
    }
}






@Composable
fun KomandaScreen(onNavigateToChat: () -> Unit, onNavigateToKomandaAukeratu: () -> Unit, onNavigateToCharlieApp: () -> Unit) {
    // Estado para almacenar los productos obtenidos
    val products = remember { mutableStateOf<List<EskaeraProduktua>>(emptyList()) }

    // Cargar los productos al iniciar la pantalla
    LaunchedEffect(Unit) {
        fetchMaxEskaera { result ->
            result?.let {
                products.value = it
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                border = BorderStroke(2.dp, Color.Black),
                onClick = { onNavigateToKomandaAukeratu() },
                colors = ButtonDefaults.buttonColors(Color(0xFFFF6600))
            ) {
                Text(text = "Atzera", color = Color.Black)
            }

            Button(
                border = BorderStroke(2.dp, Color.Black),
                onClick = { onNavigateToChat() },  // Navegar a la pantalla de chat
                colors = ButtonDefaults.buttonColors(Color(0xFFFF6600))
            ) {
                Text(text = "Txat", color = Color.Black)
            }

        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = "Komanda bidali",
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(16.dp))

        // List of items
        LazyColumn(modifier = Modifier
            .fillMaxSize()
            .weight(1f)) {
            items(products.value) { product ->
                // Log each product's details to see in Logcat
                Log.d("KomandaScreen", "Producto: ${product.name}, Cantidad: ${product.quantity}, Precio: ${product.price}")

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(text = "Izena: ${product.name}", color = Color.Black)
                    Text(text = "Kopurua: ${product.quantity}", color = Color.Black)
                    Text(text = "Prezioa: ${product.price}€", color = Color.Black)
                }

                Divider(color = Color.Gray, thickness = 1.dp)
            }
        }

        Button(
            border = BorderStroke(2.dp, Color.Black),
            onClick = {
                Log.d("DeleteEskaeraProduktua", "El erreserbaId que se pasa es:$ErreserbaIdToDelete")
                // Llamada a la función para eliminar la eskaera
                 val erreserbaId = ErreserbaIdToDelete
                deleteEskaeraProduktua(erreserbaId) { success, message ->
                    if (success) {
                        // Si la eliminación fue exitosa, navegar a la siguiente página
                        onNavigateToKomandaAukeratu()
                        Log.d("DeleteEskaeraProduktua", "Eskaera eliminada correctamente.")
                    } else {
                        // Si la eliminación falló, mostrar mensaje de error
                        Log.e("DeleteEskaeraProduktua", message)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(Color(0xFFFF6600)),
            shape = RoundedCornerShape(64.dp)
        ) {
            Text(text = "Komanda modifikatu", color = Color.Black)
        }



        // Bottom Button
        Button(
            border = BorderStroke(2.dp, Color.Black),
            onClick = {  onNavigateToCharlieApp()
                ErreserbaId = ErreserbaId.plus(1)
                },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(Color(0xFFFF6600)),
            shape = RoundedCornerShape(32.dp)
        ) {
            Text(text = "Komanda bidali", color = Color.Black)
        }
    }
}
@Composable
fun KomandaItemRow(itemName: String, initialQuantity: Int, onQuantityChanged: (Int) -> Unit) {
    var quantity by remember { mutableStateOf(initialQuantity) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = itemName, color = Color.Black)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (quantity > 0) quantity-- }) {
                Text("-", color = Color.Red, fontSize = 24.sp)
            }
            Text(quantity.toString(), color = Color.Black)
            IconButton(onClick = { quantity++ }) {
                Icon(Icons.Default.Add, contentDescription = "Increment", tint = Color.Green)
            }
        }
    }

    // Notify the parent when the quantity changes
    LaunchedEffect(quantity) {
        onQuantityChanged(quantity)
    }
}

@Preview(showBackground = true)
@Composable
fun KomandaAukeratuScreenPreview() {
    ZerbitzariAppTheme {
        KomandaAukeratuScreen(
            onNavigateToEntranteak = {},
            onNavigateToLehenPlatera = {},
            onNavigateToPostreak = {},
            onNavigateToEdariak = {},
            onNavigateToKomanda = {},
            onNavigateToChat = {},
            onNavigateToMahaiakAukeratu = {}

        )
    }
}

@Preview(showBackground = true)
@Composable
fun MahaiakAukeratuPreview() {
    ZerbitzariAppTheme {
        MahaiakAukeratu(onNavigateToKomandaAukeratu = {},
            onNavigateToChat = {},
            onNavigateToMainScreen = {}
        )
    }
}


@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    ZerbitzariAppTheme {
        MainScreen(onNavigateToMahaiakAukeratu = { },
            onNavigateToChat = {},
            onNavigateToCharliesApp = {})
    }
}


@Preview(showBackground = true)
@Composable
fun KomandaScreenPreview() {
    ZerbitzariAppTheme {
        KomandaScreen(onNavigateToChat = {},
            onNavigateToKomandaAukeratu = {},
            onNavigateToCharlieApp = {},
             )
    }
}

@Preview(showBackground = true)
@Composable
fun EntranteakScreenPreview() {
    ZerbitzariAppTheme {
        EntranteakScreen(onNavigateToKomandaAukeratu = {}, onNavigateToChat = {})
    }
}

@Preview(showBackground = true)
@Composable
fun CharlieAppPreview() {
    ZerbitzariAppTheme {
        CharlieApp(onNavigateToMainScreen = {}, onNavigateToChat = {})
    }
}

@Preview(showBackground = true)
@Composable
fun LehenPlateraScreenPreview() {
    ZerbitzariAppTheme {
        LehenPlateraScreen(onNavigateToKomandaAukeratu = {}, onNavigateToChat = {})
    }
}

@Preview(showBackground = true)
@Composable
fun PostreakScreenPreview() {
    ZerbitzariAppTheme {
        PostreakScreen(onNavigateToKomandaAukeratu = {}, onNavigateToChat = {})
    }
}

@Preview(showBackground = true)
@Composable
fun EdariakScreenPreview() {
    ZerbitzariAppTheme {
        EdariakScreen(onNavigateToKomandaAukeratu = {}, onNavigateToChat = {})
    }
}