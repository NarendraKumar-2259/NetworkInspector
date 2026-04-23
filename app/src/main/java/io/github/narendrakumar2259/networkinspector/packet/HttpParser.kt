package io.github.narendrakumar2259.networkinspector.packet

data class HttpRequest(
    val method: String,
    val path: String,
    val headers: Map<String, String>,
    val body: String?
)

class HttpParser {
    fun parseHttpRequest(data: ByteArray): HttpRequest? {

        //Convert the ByteArray to a String
        val requestString = String(data)

        //Split the request into lines
        val lines = requestString.split("\r\n")

        if (lines.isEmpty()) return null

        //The first line should contain the method and path
        val requestLine = lines[0].split(" ")
        if (requestLine.size < 2) return null

        val method = requestLine[0]
        val path = requestLine[1]

        //Parse headers and body
        val headers = mutableMapOf<String, String>()
        var body: String? = null
        var isBody = false

        // Headers are separated from the body by an empty line
        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.isEmpty()) {
                isBody = true
                continue
            }
            // If we're not yet in the body, parse headers
            if (!isBody) {
                val headerParts = line.split(": ", limit = 2)
                if (headerParts.size == 2) {
                    headers[headerParts[0]] = headerParts[1]
                }
                // If we are in the body, append lines to the body string
            } else {
                body = (body ?: "") + line + "\n"
            }
        }

        return HttpRequest(
            method = method,
            path = path,
            headers = headers,
            body = body?.trim()
        )
    }
}