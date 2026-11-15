package com.anishop.aninote

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject

@RunWith(AndroidJUnit4::class)
class MigrationBackupTest {
 @Test fun importVersionOneWithoutChangingRecordValues() {
  assumeTrue(InstrumentationRegistry.getArguments().getString("phase")!="seed")
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  assertEquals("com.anishop.aninote.recovery",context.packageName)
  val repo=Repo(context)
  val original=BackupCodec.export(repo)
  val backup="""{
   "format":"ani-note-backup","version":1,"created":1234,
   "notes":[{"id":"old-note","parts":[{"id":"old-part","title":"یادداشت قبلی","body":"متن فارسی محفوظ","alarm":0}]}],
   "accounts":[{"id":"old-account","title":"حساب قبلی","entries":[{"id":"old-credit","subject":"دریافت","amount":22500,"date":1234},{"id":"old-debit","subject":"پرداخت","amount":-500,"date":1235}]}],
   "sound":"","ring_duration":45,"ring_repeat":false,"repeat_minutes":3
  }"""
  try {
   BackupCodec.restore(repo,backup)
   val reopened=Repo(context)
   assertEquals("متن فارسی محفوظ",reopened.notes().single().parts.single().body)
   assertEquals("old-note",reopened.notes().single().id)
   val account=reopened.accounts().single()
   assertEquals("old-account",account.id)
   assertEquals("old-credit",account.entries.first().id)
   assertEquals(1234L,account.entries.first().date)
   assertEquals("22,000 تومان",money(account.entries.sumOf{it.amount}))
   assertEquals(45,repo.p.getInt("ring_duration",0))
   assertEquals(3,repo.p.getInt("repeat_minutes",0))
   val beforeNotes=repo.encrypted.read("notes")
   val beforeAccounts=repo.encrypted.read("accounts")
   val invalid=JSONObject(backup)
   invalid.getJSONArray("accounts").getJSONObject(0).getJSONArray("entries").getJSONObject(0).put("amount","NaN")
   try {BackupCodec.restore(repo,invalid.toString());fail("Invalid backup must fail before changing data")}catch(_:IllegalArgumentException){}
   assertEquals(beforeNotes,repo.encrypted.read("notes"))
   assertEquals(beforeAccounts,repo.encrypted.read("accounts"))
  } finally {BackupCodec.restore(repo,original)}
 }
}
