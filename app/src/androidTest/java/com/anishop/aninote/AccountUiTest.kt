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

@RunWith(AndroidJUnit4::class)
class AccountUiTest {
 @get:Rule val compose=createEmptyComposeRule()
 @Test fun cancelAndConfirmOnlyOneEntryThenSaveAndEditGroupedAmount() {
  assumeTrue(InstrumentationRegistry.getArguments().getString("phase")!="seed")
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val repo=Repo(context)
  val original=BackupCodec.export(repo)
  try {
   repo.saveAccounts(listOf(Account(id="ui-account",title="آزمون گردش حساب",entries=listOf(
    Entry(id="first",subject="گردش اول",amount=parseMoney("16500.5")!!,date=2000),
    Entry(id="second",subject="گردش دوم",amount=parseMoney("0.25")!!,date=1000)
   ))))
   ActivityScenario.launch(MainActivity::class.java).use {
    compose.onNodeWithText("مدیریت حساب").performClick()
    compose.onNodeWithText("آزمون گردش حساب").performClick()
    compose.onNodeWithContentDescription("حذف گردش گردش اول").performClick()
    compose.onNodeWithText("حذف گردش حساب").assertIsDisplayed()
    assertEquals(2,repo.accounts().single().entries.size)
    compose.onNodeWithText("انصراف").performClick()
    compose.onNodeWithText("حذف گردش حساب").assertDoesNotExist()
    assertEquals(2,repo.accounts().single().entries.size)
    compose.onNodeWithContentDescription("حذف گردش گردش اول").performClick()
    compose.onNodeWithText("حذف این گردش").performClick()
    compose.waitForIdle()
    assertEquals("second",repo.accounts().single().entries.single().id)
    compose.onNodeWithText("گردش دوم").assertIsDisplayed()
    compose.onNodeWithText(" ثبت گردش جدید").performClick()
    compose.onNodeWithText("موضوع").performTextReplacement("گردش میلیونی")
    compose.onNodeWithText("مبلغ (تومان؛ نقطه برای اعشار)").performTextReplacement("1500000.5")
    compose.onNodeWithText("ذخیره").performClick()
    compose.onNodeWithText("+1,500,000.5 تومان").assertIsDisplayed()
    assertEquals("1500000.5",editableMoney(repo.accounts().single().entries.last().amount))
    compose.onNodeWithContentDescription("ویرایش گردش گردش میلیونی").performClick()
    compose.onNodeWithText("مبلغ (تومان؛ نقطه برای اعشار)").performTextReplacement("2500000.75")
    compose.onNodeWithText("ذخیره").performClick()
    compose.onNodeWithText("+2,500,000.75 تومان").assertIsDisplayed()
    assertEquals(2,repo.accounts().single().entries.size)
    assertEquals("2500000.75",editableMoney(repo.accounts().single().entries.last().amount))
   }
  } finally {BackupCodec.restore(repo,original)}
 }
}
