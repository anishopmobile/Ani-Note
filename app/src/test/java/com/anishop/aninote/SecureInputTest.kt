package com.anishop.aninote
import org.junit.Assert.*
import org.junit.Test
class SecureInputTest {
 @Test fun moneyThousandSeparators() {
  assertEquals(22500L,parseMoney("22.500"))
  assertEquals(22500L,parseMoney("22,500"))
  assertEquals(22500L,parseMoney("22500"))
 }
 @Test fun rejectInvalidAmounts() {
  assertNull(parseMoney(""))
  assertNull(parseMoney("1x4"))
  assertNull(parseMoney("99999999999999999999999999"))
 }
 @Test fun pinRules() {
  assertTrue(Regex("[0-9]{4,6}").matches("1234"))
  assertTrue(Regex("[0-9]{4,6}").matches("123456"))
  assertFalse(Regex("[0-9]{4,6}").matches("123"))
  assertFalse(Regex("[0-9]{4,6}").matches("1234567"))
 }
 @Test fun independentPatternRules() {
  val pattern="01245"
  assertTrue(Regex("[0-8]{4,9}").matches(pattern) && pattern.toSet().size==pattern.length)
  val duplicated="0112"
  assertFalse(duplicated.toSet().size==duplicated.length)
 }
}
