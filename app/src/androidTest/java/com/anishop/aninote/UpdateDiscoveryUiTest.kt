package com.anishop.aninote

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Run after publication, against the real installed 306402 APK and its original updater. */
@RunWith(AndroidJUnit4::class)
class UpdateDiscoveryUiTest {
 @get:Rule val compose=createEmptyComposeRule()
 @Test fun installed306402UpdateButtonDiscoversPublishedNotePlusBuild() {
  val args=InstrumentationRegistry.getArguments()
  assumeTrue(args.getString("phase")=="published")
  val expected=args.getString("expected_code")!!.toInt()
  assertTrue(expected>306402)
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  @Suppress("DEPRECATION")
  val installed=context.packageManager.getPackageInfo(context.packageName,0).versionCode
  assertEquals(306402,installed)
  ActivityScenario.launch(MainActivity::class.java).use {
   compose.onNodeWithText("تنظیمات").performClick()
   val download="دریافت نسخه $expected"
   var found=false
   for(attempt in 0..2) {
    compose.onNodeWithText("بررسی بروزرسانی").performScrollTo().performClick()
    compose.waitUntil(30_000) {
     compose.onAllNodesWithText("در حال بررسی…").fetchSemanticsNodes().isEmpty() &&
      (compose.onAllNodesWithText(download).fetchSemanticsNodes().isNotEmpty() ||
       compose.onAllNodesWithText("نسخه جدیدتری برای برنامه جدید منتشر نشده است.").fetchSemanticsNodes().isNotEmpty() ||
       compose.onAllNodesWithText("بررسی بروزرسانی ممکن نشد؛ اتصال اینترنت را بررسی و دوباره تلاش کنید.").fetchSemanticsNodes().isNotEmpty())
    }
    found=compose.onAllNodesWithText(download).fetchSemanticsNodes().isNotEmpty()
    if(found)break
    if(attempt<2)Thread.sleep(15_000) // Allow GitHub's release listing to become consistent.
   }
   assertTrue("The original 306402 updater must discover NotePlus $expected through GitHub Releases",found)
   compose.onNodeWithText(download).performScrollTo().assertIsDisplayed()
  }
 }
}
