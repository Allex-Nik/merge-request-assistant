package github

import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.date.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.example.ResponseStatus
import org.example.github.parseGitHubResponse
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals

class ParseGitHubResponseTest {

    @OptIn(InternalAPI::class)
    @Test
    fun `parseGitHubResponse returns Success for 200 OK`() = runBlocking{
        val mockResponse = MockHttpResponse(HttpStatusCode.OK)
        val result = parseGitHubResponse(mockResponse)
        assertEquals(ResponseStatus.Success, result)
    }

    @OptIn(InternalAPI::class)
    @Test
    fun `parseGitHubResponse returns NotFound for 404 Not Found`() = runBlocking{
        val mockResponse = MockHttpResponse(HttpStatusCode.NotFound)
        val result = parseGitHubResponse(mockResponse)
        assertEquals(ResponseStatus.NotFound, result)
    }

    @OptIn(InternalAPI::class)
    @Test
    fun `parseGitHubResponse returns Forbidden for 403 Forbidden`() = runBlocking{
        val mockResponse = MockHttpResponse(HttpStatusCode.Forbidden)
        val result = parseGitHubResponse(mockResponse)
        assertEquals(ResponseStatus.Forbidden, result)
    }


}

class MockHttpResponse @InternalAPI constructor(private val statusCode: HttpStatusCode
) : HttpResponse() {
    override val call: HttpClientCall
        get() = throw NotImplementedError("Not needed for this test.")
    override val status: HttpStatusCode
        get() = statusCode
    override val version: HttpProtocolVersion
        get() = HttpProtocolVersion.HTTP_1_1
    override val requestTime: GMTDate
        get() = GMTDate()
    override val responseTime: GMTDate
        get() = GMTDate()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default
    @InternalAPI
    override val rawContent: ByteReadChannel
        get() = ByteReadChannel("")
    override val headers: Headers
        get() = Headers.Empty
    }
