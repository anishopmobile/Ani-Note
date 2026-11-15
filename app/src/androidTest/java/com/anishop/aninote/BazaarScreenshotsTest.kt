package com.anishop.aninote

import android.content.Context
import android.graphics.Bitmap
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.math.BigDecimal

/** Store artwork uses only synthetic data in an isolated emulator and the published APK. */
@RunWith(AndroidJUnit4::class)
class BazaarScreenshotsTest {
 @get:Rule val compose=createEmptyComposeRule()
 private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
 private val context get()=instrumentation.targetContext
 private fun capture(name:String) {
  compose.waitForIdle()
  android.os.SystemClock.sleep(450)
  val bitmap=instrumentation.uiAutomation.takeScreenshot() ?: error("Screenshot unavailable")
  try {
   assertEquals(1080,bitmap.width);assertEquals(1920,bitmap.height)
   val folder=File(context.getExternalFilesDir(null),"bazaar-screenshots").apply{mkdirs()}
   File(folder,"$name.png").outputStream().use{check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))}
  } finally {bitmap.recycle()}
 }
 private fun alignSectionTop(title:String) {
  val node=compose.onNodeWithText(title)
  node.performScrollTo()
  val delta=node.fetchSemanticsNode().boundsInRoot.top-300f
  if(delta>1f)node.onAncestors().filter(hasScrollAction()).onFirst().performSemanticsAction(SemanticsActions.ScrollBy){it(0f,delta)}
  compose.waitForIdle()
 }
 private fun allowCapture(scenario:ActivityScenario<MainActivity>) {
  scenario.onActivity {
   // Only this instrumentation session changes its own window. Production FLAG_SECURE stays intact.
   it.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
   it.window.decorView.postInvalidateOnAnimation()
  }
  compose.waitForIdle()
 }
 @Test fun capturePublishedAppSectionsWithDemoData() {
  assumeTrue(InstrumentationRegistry.getArguments().getString("phase")=="screenshots")
  val repo=Repo(context)
  val now=System.currentTimeMillis()
  fun entry(id:String,title:String,amount:String,days:Int)=Entry(id,title,BigDecimal(amount),now-days*86_400_000L)
  val notes=listOf(
   Note("week",listOf(Part("week-1","برنامه‌ریزی هفته","مرور کارهای مهم هفته\nتکمیل گزارش پروژه\nپیگیری خریدهای ضروری",now+2*86_400_000L),Part("week-2","قرارها و پیگیری‌ها","جلسهٔ هماهنگی پروژه\nبررسی برنامهٔ روز بعد"))),
   Note("shopping",listOf(Part("shopping-1","فهرست خرید خانه","میوه و سبزیجات\nمواد غذایی هفته\nلوازم موردنیاز خانه"))),
   Note("ideas",listOf(Part("ideas-1","ایده‌های تازه","ثبت ایده‌ها برای پروژه‌های بعدی\nمطالعه و یادگیری روزانه"),Part("ideas-2","نکته‌های کاربردی","هر ایدهٔ کوچک می‌تواند شروع یک کار بزرگ باشد."))),
   Note("books",listOf(Part("books-1","کتاب‌هایی که می‌خوانم","یادداشت نکته‌های مهم هر فصل\nانتخاب کتاب بعدی"))),
   Note("travel",listOf(Part("travel-1","برنامهٔ سفر","بررسی مسیر و محل اقامت\nآماده‌کردن وسایل ضروری")))
  )
  val accounts=listOf(
   Account("daily","حساب روزمره",listOf(entry("daily-1","دریافت هزینهٔ پروژه","1500000.5",0),entry("daily-2","خرید خانه","-325000",1),entry("daily-3","پرداخت اینترنت","-89500",2),entry("daily-4","واریز به حساب","750000",3))),
   Account("saving","پس‌انداز",listOf(entry("saving-1","پس‌انداز ماهانه","2000000",2),entry("saving-2","واریز جدید","500000",0))),
   Account("home","هزینه‌های خانه",listOf(entry("home-1","بودجهٔ ماهانه","3000000",4),entry("home-2","خریدهای ضروری","-680000",1))),
   Account("project","حساب پروژه",listOf(entry("project-1","پیش‌پرداخت","4000000",5),entry("project-2","هزینهٔ اجرا","-1250000",1)))
  )
  repo.saveNotes(notes);repo.saveAccounts(accounts)
  check(repo.p.edit().putInt("ring_duration",30).putBoolean("ring_repeat",true).putInt("repeat_minutes",5).putBoolean("calendar_events",true).putBoolean("calendar_holidays",true).putBoolean("calendar_fridays",true).commit())
  val access=SecureAccess(context);access.enabled=false;access.setPin("123456");access.setPattern("01245")
  ActivityScenario.launch(MainActivity::class.java).use { scenario ->
   allowCapture(scenario)
   compose.onNodeWithText("یادداشت‌های من").assertIsDisplayed()
   capture("01-notes")
   compose.onNodeWithText("برنامه‌ریزی هفته").performClick()
   compose.onNodeWithText("ذخیره خودکار فعال است").assertIsDisplayed()
   capture("02-note-editor")
   compose.onNodeWithContentDescription("بازگشت").performClick()
   compose.onNodeWithText("مدیریت حساب").performClick()
   compose.onNodeWithText("مدیریت حساب‌ها").assertIsDisplayed()
   capture("03-accounts")
   compose.onNodeWithText("حساب روزمره").performClick()
   compose.onNodeWithText("مانده حساب").assertIsDisplayed()
   capture("04-account-transactions")
   compose.onNodeWithText(" ثبت گردش جدید").performClick()
   compose.onNodeWithText("موضوع").performTextReplacement("دریافت هزینهٔ پروژه")
   compose.onNodeWithText("مبلغ (تومان؛ نقطه برای اعشار)").performTextReplacement("1500000.5")
   scenario.onActivity {
    (it.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(it.window.decorView.windowToken,0)
   }
   capture("05-decimal-entry")
   compose.onNodeWithText("انصراف").performClick()
   compose.onNodeWithContentDescription("بازگشت").performClick()
   compose.onNodeWithText("تقویم").performClick()
   val month=android.icu.util.Calendar.getInstance(android.icu.util.ULocale("fa_IR@calendar=persian")).get(android.icu.util.Calendar.MONTH)
   repeat(month){compose.onNodeWithContentDescription("ماه قبل").performClick()}
   capture("06-persian-calendar")
   compose.onNodeWithText("تنظیمات").performClick()
   alignSectionTop("تنظیمات پخش زنگ")
   capture("07-reminder-settings")
   alignSectionTop("ورود امن به برنامه")
   capture("08-security-settings")
  }
  access.enabled=true
  ActivityScenario.launch(MainActivity::class.java).use { scenario ->
   allowCapture(scenario)
   compose.onNodeWithText("ورود با الگوی اختصاصی").performClick()
   capture("09-app-lock")
  }
 }
}
