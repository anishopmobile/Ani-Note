package com.anishop.aninote
import org.junit.Assert.*
import org.junit.Test
class UpdatePolicyTest {
 @Test fun versions() {
  assertEquals(123,UpdatePolicy.code("recovery-v123"))
  listOf("v123","recovery-v0","recovery-v2147483648","recovery-v12x","recovery-v-1").forEach{assertNull(UpdatePolicy.code(it))}
 }
 @Test fun assetOrigins() {
  val url="https://github.com/anishopmobile/Ani-Note/releases/download/recovery-v123/Ani-Note-recovery-123.apk"
  assertTrue(UpdatePolicy.safeAsset(url))
  listOf(url.replace("https:","http:"),url.replace("github.com","evil.example"),url+"?redirect=evil",url.replace("/Ani-Note/","/other/"),url.replace("github.com/","github.com@evil.example/")).forEach{assertFalse(UpdatePolicy.safeAsset(it))}
 }
}
