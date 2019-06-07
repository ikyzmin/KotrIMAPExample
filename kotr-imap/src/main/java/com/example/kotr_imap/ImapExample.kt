package com.example.kotr_imap

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.tls.tls
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.cio.write
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.io.cancel
import kotlinx.coroutines.io.readUTF8Line
import kotlinx.coroutines.io.readUTF8LineTo
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress

const val TAG = "test"
var tagCounter = 0

@KtorExperimentalAPI
fun main() {
    runBlocking {
        val socket = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().connect(InetSocketAddress("imap.collabio.team", 993)).tls(Dispatchers.IO)
        val w = socket.openWriteChannel(autoFlush = false)
        val r = socket.openReadChannel()
        w.write("$TAG${tagCounter++} LOGIN ilya.kyzmin@myoffice.team 09gw7vkx\r\n")
        w.flush()
        w.write("$TAG${tagCounter++} LIST \"\" \"*\"\r\n")
        w.flush()
        w.write("$TAG${tagCounter++} LIST \"\" \"*\" RETURN (STATUS (MESSAGES UNSEEN))\r\n")
        w.flush()
        w.write("$TAG${tagCounter++} ENABLE QRESYNC\r\n")
        w.flush()
        w.write("$TAG${tagCounter++} SELECT INBOX (QRESYNC (0 0))\r\n")
        w.flush()
        w.write("$TAG${tagCounter++} SEARCH RETURN (PARTIAL 1:20) ALL \r\n")
        w.flush()
       // w.write("$TAG${tagCounter++} UID FETCH \$ (ENVELOPE) \r\n")
        w.flush()
        while (true){
            val line  = r.readUTF8Line().toString()
            println(line)
            if (line.contains("PARTIAL")){
                if(line.indexOf("(")>=0) {
                    var response = line.substring(line.lastIndexOf("(")+1, line.lastIndexOf(")"))
                    response = response.split(" ")[1]
                    w.write("$TAG${tagCounter++} FETCH $response (ENVELOPE) \r\n")
                    w.flush()
                    while(r.readUTF8LineTo(System.out)){
                        println()
                    }
                }
            }
        }
    }
}
