package com.anishop.aninote

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UpgradeTest {
 private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
 @Test fun preservedData() {
  val phase=InstrumentationRegistry.getArguments().getString("phase")
  val p=context.getSharedPreferences("data",0)
  val note="""[{"id":"legacy-note","parts":[{"id":"part","title":"یادداشت قدیمی","body":"متن محفوظ","alarm":0}]}]"""
  val account="""[{"id":"legacy-account","title":"حساب قدیمی","entries":[{"id":"entry","subject":"تست","amount":22500,"date":1234}]}]"""
  if(phase=="seed") {
   assertTrue(p.edit().putString("notes",note).putString("accounts",account).putBoolean("calendar_events",false).putBoolean("calendar_holidays",false).putBoolean("calendar_fridays",false)
    .putInt("ring_duration",37).putBoolean("ring_repeat",true).putInt("repeat_minutes",7).putString("sound_uri","").commit())
   // Seed encrypted legacy records plus a genuine decimal entry before the next APK upgrade.
   assertEquals(note,EncryptedStore(context).read("notes"))
   val repo=Repo(context)
   val legacy=repo.accounts().single()
   repo.saveAccounts(listOf(legacy.copy(entries=legacy.entries+Entry(id="decimal-upgrade",subject="اعشار قبل از آپدیت",amount=parseMoney("16500.5")!!))))
   val widget=context.getSharedPreferences("widget_prefs",0)
   check(widget.edit().putBoolean("reveal",true).putStringSet("notes",setOf("legacy-note")).putStringSet("accounts",setOf("legacy-account")).commit())
   val access=SecureAccess(context);access.setPin("8642")
   access.enabled=InstrumentationRegistry.getArguments().getString("lock")=="true"
   return
  }
  val repo=Repo(context)
  assertEquals("یادداشت قدیمی",repo.notes().single().parts.single().title)
  assertEquals(0,java.math.BigDecimal("22500").compareTo(repo.accounts().single().entries.first().amount))
  assertEquals("16500.5",editableMoney(repo.accounts().single().entries.last().amount))
  assertFalse(p.getBoolean("calendar_events",true))
  assertFalse(p.getBoolean("calendar_holidays",true))
  assertFalse(p.getBoolean("calendar_fridays",true))
  assertEquals(37,p.getInt("ring_duration",0))
  assertTrue(p.getBoolean("ring_repeat",false))
  assertEquals(7,p.getInt("repeat_minutes",0))
  val widget=context.getSharedPreferences("widget_prefs",0)
  assertTrue(widget.getBoolean("reveal",false))
  assertEquals(setOf("legacy-note"),widget.getStringSet("notes",emptySet()))
  assertEquals(setOf("legacy-account"),widget.getStringSet("accounts",emptySet()))
  val access=SecureAccess(context)
  assertTrue(access.hasPin());assertTrue(access.check("pin","8642"))
  assertEquals(InstrumentationRegistry.getArguments().getString("lock")=="true",access.enabled)
  assertEquals("NotePlus",context.packageManager.getApplicationLabel(context.applicationInfo).toString())
  assertTrue(p.getString("notes","")!!.startsWith("gcm:"))
  assertTrue(p.getString("accounts","")!!.startsWith("gcm:"))
 }
 @Test fun corruptDataIsNotSilentlyEmpty() {
  if(InstrumentationRegistry.getArguments().getString("phase")=="seed")return
  val p=context.getSharedPreferences("data",0)
  val original=p.getString("notes",null)
  try {
   p.edit().putString("notes","gcm:broken").commit()
   try{Repo(context).notes();fail("Corruption must be reported")}catch(_:IllegalStateException){}
   assertEquals("gcm:broken",p.getString("notes",null))
  } finally {p.edit().putString("notes",original).commit()}
 }
}
