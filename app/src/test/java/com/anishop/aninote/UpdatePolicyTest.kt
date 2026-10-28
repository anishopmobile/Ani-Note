package com.anishop.aninote
import org.junit.Assert.*
import org.junit.Test
class UpdatePolicyTest {
 @Test fun versions() {
  assertEquals(123,UpdatePolicy.code("preview-v123"))
  listOf("v123","preview-v0","preview-v2147483648","preview-v12x","preview-v-1").forEach{assertNull(UpdatePolicy.code(it))}
 }
 @Test fun assetOrigins() {
  val url="https://github.com/anishopmobile/Ani-Note/releases/download/preview-v123/Ani-Note-preview-123.apk"
  assertTrue(UpdatePolicy.safeAsset(url))
  listOf(url.replace("https:","http:"),url.replace("github.com","evil.example"),url+"?redirect=evil",url.replace("/Ani-Note/","/other/"),url.replace("github.com/","github.com@evil.example/")).forEach{assertFalse(UpdatePolicy.safeAsset(it))}
 }
}
