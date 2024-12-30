package github

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.example.github.getBaseBranchSha
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.test.Test

class GetBaseBranchShaTest {

    @Test
    fun `test getBaseBranchSha with success`() = runBlocking {
        // Mock response data
        val mockSha = "mockSha12345"
        val mockJson = """{
            "ref": "refs/heads/main",
            "node_id": "sample",
            "url": "sample_url",
            "object": {
                "sha": "$mockSha",
                "type": "commit",
                "url": "objectUrl"
            }
        }"""

        // Mock engine
        val mockEngine = MockEngine { _ ->
            respond(
                content = mockJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        // Mock client
        val mockClient = HttpClient(mockEngine)

        // Call the function
        val result = getBaseBranchSha(mockClient, "owner", "repo", "main")

        // Assertions
        assertTrue(result.isSuccess, "Result should be successful")
        assertEquals(mockSha, result.getOrNull(), "SHA should match mock value")
    }
}
