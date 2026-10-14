package com.example.airstreamclone

import java.io.OutputStreamWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

object SonosController {

    fun discoverSonosIp(timeoutMs: Int = 3000): String? {
        val searchMessage = buildString {
            append("M-SEARCH * HTTP/1.1\r\n")
            append("HOST: 239.255.255.250:1900\r\n")
            append("MAN: \"ssdp:discover\"\r\n")
            append("MX: 2\r\n")
            append("ST: urn:schemas-upnp-org:device:ZonePlayer:1\r\n\r\n")
        }

        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.soTimeout = timeoutMs
            val data = searchMessage.toByteArray()
            val packet = DatagramPacket(
                data, data.size,
                InetAddress.getByName("239.255.255.250"), 1900
            )
            socket.send(packet)

            val rxBuf = ByteArray(2048)
            val rxPacket = DatagramPacket(rxBuf, rxBuf.size)
            while (true) {
                socket.receive(rxPacket)
                val response = String(rxPacket.data, 0, rxPacket.length)
                if (response.contains("Sonos", ignoreCase = true)) {
                    return rxPacket.address.hostAddress
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            socket?.close()
        }
        return null
    }

    fun startSonosStream(sonosIp: String, phoneIp: String, httpPort: Int) {
        val streamUrl = "http://$phoneIp:$httpPort/live.wav"

        val setUriBody = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                <s:Body>
                    <u:SetAVTransportURI xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                        <InstanceID>0</InstanceID>
                        <CurrentURI>$streamUrl</CurrentURI>
                        <CurrentURIMetaData></CurrentURIMetaData>
                    </u:SetAVTransportURI>
                </s:Body>
            </s:Envelope>
        """.trimIndent()

        val playBody = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                <s:Body>
                    <u:Play xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                        <InstanceID>0</InstanceID>
                        <Speed>1</Speed>
                    </u:Play>
                </s:Body>
            </s:Envelope>
        """.trimIndent()

        sendSoapAction(sonosIp, "SetAVTransportURI", setUriBody)
        sendSoapAction(sonosIp, "Play", playBody)
    }

    fun stopSonosStream(sonosIp: String) {
        val stopBody = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                <s:Body>
                    <u:Stop xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                        <InstanceID>0</InstanceID>
                    </u:Stop>
                </s:Body>
            </s:Envelope>
        """.trimIndent()
        sendSoapAction(sonosIp, "Stop", stopBody)
    }

    private fun sendSoapAction(sonosIp: String, action: String, xmlBody: String) {
        try {
            val url = URL("http://$sonosIp:1400/MediaRenderer/AVTransport/Control")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"")
            conn.setRequestProperty("SOAPAction", "\"urn:schemas-upnp-org:service:AVTransport:1#$action\"")

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(xmlBody)
                writer.flush()
            }
            conn.responseCode
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
