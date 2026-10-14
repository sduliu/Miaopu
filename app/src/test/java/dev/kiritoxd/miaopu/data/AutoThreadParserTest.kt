package dev.kiritoxd.miaopu.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoThreadParserTest {
    @Test
    fun `autoThreads maps thread fields and nextCursor`() {
        val page = HupuJsonParser.autoThreads(THREADS_JSON)

        assertEquals(2, page.threads.size)
        val first = page.threads[0]
        assertEquals("63302113", first.tid)
        assertEquals("提车三个月，分享一下用车感受", first.title)
        assertEquals("JR老司机", first.username)
        assertEquals(45, first.replies)
        assertEquals(10, first.recommends)
        assertEquals("2026-10-14 18:22", first.time)
        assertFalse(first.isVideo)
        val second = page.threads[1]
        assertEquals("63302088", second.tid)
        assertTrue(second.isVideo)
        assertEquals(3, second.recommends)
        assertEquals("cursor_abc123", page.cursor)
        assertFalse(page.hasMore)
    }

    @Test
    fun `autoThreads falls back to cursor and treats full page as having more`() {
        val items = (1..20).joinToString(",") { index ->
            """{"tid":${63000000 + index},"title":"帖子$index","type":"post","replies":$index,"username":"用户$index","recommendNum":1,"certTitle":"","time":"2026-10-14 12:00","isVideo":false,"isVote":false,"url":"https://bbs.hupu.com/${63000000 + index}.html"}"""
        }
        val json = """{"code":200,"msg":"success","data":{"topicThreads":[$items],"cursor":"legacy_cursor","count":100}}"""
        val page = HupuJsonParser.autoThreads(json)

        assertEquals(20, page.threads.size)
        assertEquals("63000001", page.threads.first().tid)
        assertEquals("legacy_cursor", page.cursor)
        assertTrue(page.hasMore)
    }

    @Test
    fun `autoThreads tolerates empty and malformed payloads`() {
        val empty = HupuJsonParser.autoThreads(EMPTY_JSON)
        assertTrue(empty.threads.isEmpty())
        assertEquals("", empty.cursor)
        assertFalse(empty.hasMore)

        val noData = HupuJsonParser.autoThreads("""{"code":200,"msg":"success"}""")
        assertTrue(noData.threads.isEmpty())
        assertFalse(noData.hasMore)

        // Entries without a usable tid are skipped, not fatal.
        val missingTid = HupuJsonParser.autoThreads(
            """{"code":200,"data":{"topicThreads":[{"title":"没有tid"}]}}""",
        )
        assertTrue(missingTid.threads.isEmpty())
    }

    private companion object {
        const val THREADS_JSON = """
            {
                "code": 200,
                "msg": "success",
                "data": {
                    "count": 2,
                    "nextCursor": "cursor_abc123",
                    "topicThreads": [
                        {
                            "tid": 63302113,
                            "title": "提车三个月，分享一下用车感受",
                            "type": "post",
                            "replies": 45,
                            "username": "JR老司机",
                            "recommendNum": 10,
                            "certTitle": "",
                            "time": "2026-10-14 18:22",
                            "isVideo": false,
                            "isVote": false,
                            "url": "https://bbs.hupu.com/63302113.html"
                        },
                        {
                            "tid": 63302088,
                            "title": "高速实测续航打折多少",
                            "type": "video",
                            "replies": 12,
                            "username": "电车研究员",
                            "recommendNum": 3,
                            "certTitle": "认证车主",
                            "time": "2026-10-14 17:05",
                            "isVideo": true,
                            "isVote": false,
                            "url": "https://bbs.hupu.com/63302088.html"
                        }
                    ]
                }
            }
        """
        const val EMPTY_JSON = """
            {"code": 200, "msg": "success", "data": {"topicThreads": [], "nextCursor": "", "count": 0}}
        """
    }
}
