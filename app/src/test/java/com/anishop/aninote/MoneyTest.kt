package com.anishop.aninote

import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Test

class MoneyTest {
 private fun exact(expected:String,actual:BigDecimal?) {assertNotNull(actual);assertEquals(0,BigDecimal(expected).compareTo(actual))}
 @Test fun decimalsAndLocaleInput() {
  listOf("16500.5","16,500.5","۱۶۵۰۰٫۵","١٦٥٠٠٫٥","۱۶٬۵۰۰٫۵").forEach{exact("16500.5",parseMoney(it))}
  exact("22.5",parseMoney("22.500"));exact("0.5",parseMoney(".5"))
  exact("0.000000000000000001",parseMoney("0.000000000000000001"))
 }
 @Test fun totalsAreExactAndCanExceedLong() {
  exact("0.3",listOf("0.1","0.2").sumOf{parseMoney(it)!!})
  exact("16500.25",parseMoney("16500.5")!!-parseMoney("0.25")!!)
  exact("18446744073709551614",storedMoney("9223372036854775807")*BigDecimal(2))
 }
 @Test fun displayAndEditingKeepFractions() {
  assertEquals("16,500.5 تومان",money(BigDecimal("16500.5")))
  assertEquals("-0.25 تومان",money(BigDecimal("-0.25")))
  assertEquals("16500.5",editableMoney(BigDecimal("-16500.500")))
  exact("16500.5",parseMoney(editableMoney(BigDecimal("16500.5"))))
 }
 @Test fun legacyIntegersAndExactStorage() {
  exact("22500",storedMoney("22500"));exact("-9223372036854775808",storedMoney("-9223372036854775808"))
  val original=parseMoney("16500.123456789012345678")!!
  assertEquals(original,storedMoney(original.toPlainString()))
 }
 @Test fun malformedInputIsNotSilentlyChanged() {
  listOf("1x4","1.2.3","16,50.5","1,5","NaN","1e3","-1","","0.1234567890123456789","999999999999999999999").forEach{assertNull(it,parseMoney(it))}
 }
}
