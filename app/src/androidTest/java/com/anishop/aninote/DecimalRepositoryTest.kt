package com.anishop.aninote

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import org.json.JSONObject

@RunWith(AndroidJUnit4::class)
class DecimalRepositoryTest {
 @Test fun saveReopenEditCalculateAndBackup() {
  assumeTrue(InstrumentationRegistry.getArguments().getString("phase")!="seed")
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val repo=Repo(context)
  val original=BackupCodec.export(repo)
  try {
   val credit=Entry(id="decimal-credit",subject="گردش اعشاری",amount=parseMoney("16500.5")!!)
   val debit=Entry(id="decimal-debit",subject="بدهکار",amount=BigDecimal("-0.25"))
   repo.saveAccounts(listOf(Account(id="decimal-account",title="حمید نعیمی",entries=listOf(credit,debit))))
   val reopened=Repo(context).accounts().single()
   assertEquals("16500.5",editableMoney(reopened.entries.first().amount))
   assertEquals("16,500.25 تومان",money(reopened.entries.sumOf{it.amount}))
   assertEquals("16500.5",org.json.JSONArray(repo.encrypted.read("accounts")).getJSONObject(0).getJSONArray("entries").getJSONObject(0).getString("amount"))
   repo.saveAccounts(listOf(reopened.copy(entries=reopened.entries.map{if(it.id==credit.id)it.copy(amount=parseMoney("16500.75")!!)else it})))
   val edited=Repo(context).accounts().single()
   assertEquals("16,500.5 تومان",money(edited.entries.sumOf{it.amount}))
   val backup=BackupCodec.export(repo)
   assertEquals(2,JSONObject(backup).getInt("version"))
   BackupCodec.validate(backup)
   repo.saveAccounts(emptyList())
   BackupCodec.restore(repo,backup)
   assertEquals("16500.75",editableMoney(Repo(context).accounts().single().entries.first().amount))
   // The same formatted decimal values feed the report and widgets.
   assertTrue(PdfReports.create(context,"گردش حساب",listOf(money(edited.entries.sumOf{it.amount}))).size>0)
   val legacy=JSONObject(original).put("version",1)
   BackupCodec.validate(legacy.toString())
  } finally {BackupCodec.restore(repo,original)}
 }
}
