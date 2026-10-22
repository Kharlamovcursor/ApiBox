package ru.hdlabs.apibox.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubReleasesTest {
    @Test
    fun readsNewerRelease() {
        val info = GitHubReleases.parseLatest(RELEASE, currentVersionCode = 1)
        requireNotNull(info)
        assertEquals(2, info.versionCode)
        assertEquals("1.0.1", info.versionName)
        assertEquals("app-release.apk", info.apkFileName)
        assertTrue(info.apkUrl.startsWith("https://github.com/"))
        assertEquals("Исправлен индекс", info.changelog)
    }

    @Test
    fun hidesCurrentRelease() {
        assertNull(GitHubReleases.parseLatest(RELEASE, currentVersionCode = 2))
    }

    @Test
    fun readsVersionTag() {
        assertEquals(2, GitHubReleases.parseVersionCode("v2"))
        assertNull(GitHubReleases.parseVersionCode("1.0.1"))
    }

    @Test
    fun trustsOnlyGitHubHosts() {
        assertTrue(GitHubReleases.isTrustedUpdateUrl("https://github.com/Kharlamovcursor/ApiBox/releases/download/v2/app-release.apk"))
        assertTrue(GitHubReleases.isTrustedUpdateUrl("https://objects.githubusercontent.com/file.apk"))
        assertFalse(GitHubReleases.isTrustedUpdateUrl("http://github.com/file.apk"))
        assertFalse(GitHubReleases.isTrustedUpdateUrl("https://example.com/app-release.apk"))
    }

    @Test
    fun cleansFileName() {
        assertEquals("app-release.apk", GitHubReleases.safeFileName("../app-release.apk"))
        assertEquals("app-release.apk", GitHubReleases.safeFileName("app-release.apk"))
    }

    private companion object {
        val RELEASE = """
            {
              "tag_name": "v2",
              "name": "1.0.1",
              "body": "Исправлен индекс",
              "assets": [
                {
                  "name": "notes.txt",
                  "browser_download_url": "https://github.com/Kharlamovcursor/ApiBox/releases/download/v2/notes.txt"
                },
                {
                  "name": "app-release.apk",
                  "browser_download_url": "https://github.com/Kharlamovcursor/ApiBox/releases/download/v2/app-release.apk"
                }
              ]
            }
        """.trimIndent()
    }
}
