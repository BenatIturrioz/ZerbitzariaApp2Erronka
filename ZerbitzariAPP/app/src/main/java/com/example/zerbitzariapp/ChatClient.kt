package com.example.chatapp

import java.io.*
import java.net.Socket
import kotlinx.coroutines.*

class ChatClient(private val messages: MutableList<Pair<String, Boolean>>, private val username: String) {

    private var socket: Socket? = null
    private var out: PrintWriter? = null
    private var `in`: BufferedReader? = null
    private var dataOut: DataOutputStream? = null

    private val serverAddress = "localhost"
    private val serverPort = 5555

    // Conectar al servidor
    suspend fun connect() {
        withContext(Dispatchers.IO) {
            try {
                socket = Socket(serverAddress, serverPort)
                out = PrintWriter(socket!!.getOutputStream(), true)
                `in` = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                dataOut = DataOutputStream(socket!!.getOutputStream())

                // Escuchar mensajes del servidor
                while (true) {
                    val message = `in`?.readLine()
                    message?.let {
                        messages.add(Pair(it, false))  // Mensaje recibido
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    // Desconectar del servidor
    suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            try {
                socket?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    // Enviar un mensaje
    suspend fun sendMessage(message: String) {
        withContext(Dispatchers.IO) {
            try {
                out?.println(message)
                messages.add(Pair(message, true))  // Mensaje enviado
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    // Enviar un archivo
    suspend fun sendFile(file: File) {
        withContext(Dispatchers.IO) {
            try {
                val fileBytes = file.readBytes()
                dataOut?.write(fileBytes)
                dataOut?.flush()
                messages.add(Pair("Archivo enviado: ${file.name}", true))
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
