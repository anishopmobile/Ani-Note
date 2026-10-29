package com.anishop.aninote
import org.junit.Assert.*
import org.junit.Test
class UpdatePolicyTest {
 @Test fun versions() {
  assertEquals(123,UpdatePolicy.code("main-v123"))
  listOf("v123","main-v0","main-v2147483648","main-v12x","main-v-1").forEach{assertNull(UpdatePolicy.code(it))}
 }
 @Test fun assetOrigins() {
  val url="https://github.com/anishopmobile/Ani-Note/releases/download/main-v123/Ani-Note-main-123.apk"
  assertTrue(UpdatePolicy.safeAsset(url))
  listOf(url.replace("https:","http:"),url.replace("github.com","evil.example"),url+"?redirect=evil",url.replace("/Ani-Note/","/other/"),url.replace("github.com/","github.com@evil.example/")).forEach{assertFalse(UpdatePolicy.safeAsset(it))}
 }
}
