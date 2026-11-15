package com.anishop.aninote

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UpdateSelectionTest {
 private fun release(code:Int,draft:Boolean=false,preview:Boolean=false,url:String?=null):JSONObject {
  val link=url?:"https://github.com/anishopmobile/Ani-Note/releases/download/recovery-v$code/Ani-Note-recovery-$code.apk"
  return JSONObject().put("tag_name","recovery-v$code").put("draft",draft).put("prerelease",preview)
   .put("assets",JSONArray().put(JSONObject().put("name","Ani-Note-recovery-$code.apk").put("browser_download_url",link)))
 }
 private fun onlyNewApp() {assumeTrue(InstrumentationRegistry.getArguments().getString("phase")=="verify")}
 @Test fun installed306402RecognizesNextMainReleaseAndSkipsOtherChannels() {
  onlyNewApp()
  val current=JSONArray().put(release(306402))
  assertEquals(306402,Updates.select(current.toString(),306202)!!.code)
  assertNull(Updates.select(current.toString(),306402))
  val candidates=JSONArray().put(release(306602)).put(release(306402)).put(release(306502))
   .put(release(306902,draft=true)).put(release(306802,preview=true))
   .put(release(306702,url="http://github.com/anishopmobile/Ani-Note/releases/download/recovery-v306702/Ani-Note-recovery-306702.apk"))
   .put(release(307002).put("tag_name","main-v307002"))
  assertEquals(306602,Updates.select(candidates.toString(),306402)!!.code)
  assertNull(Updates.select(candidates.toString(),306602))
  assertTrue(UpdatePolicy.safeAsset(Updates.select(candidates.toString(),306402)!!.url))
 }
 @Test fun malformedOrMismatchedAssetsAreNotOffered() {
  onlyNewApp()
  val wrong=release(306502,url="https://github.com/anishopmobile/Ani-Note/releases/download/recovery-v306402/Ani-Note-recovery-306402.apk")
  assertNull(Updates.select(JSONArray().put(wrong).toString(),306402))
  assertNull(Updates.select("[]",306402))
 }
 @Test fun productionGithubEndpointFindsPublishedCompatibleApk() {
  onlyNewApp()
  val available=Updates.check(306202)
  assertNotNull("The production endpoint must expose 306402 or a newer compatible main build",available)
  assertTrue(available!!.code>=306402)
  assertTrue(UpdatePolicy.safeAsset(available.url))
 }
}
