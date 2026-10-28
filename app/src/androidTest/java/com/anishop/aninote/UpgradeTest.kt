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
   assertTrue(p.edit().putString("notes",note).putString("accounts",account).putBoolean("calendar_events",false).commit())
   // Migrate one record now; keep the other plaintext until after the APK upgrade.
   assertEquals(note,EncryptedStore(context).read("notes"))
   return
  }
  val repo=Repo(context)
  assertEquals("یادداشت قدیمی",repo.notes().single().parts.single().title)
  assertEquals(22500L,repo.accounts().single().entries.single().amount)
  assertFalse(p.getBoolean("calendar_events",true))
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
