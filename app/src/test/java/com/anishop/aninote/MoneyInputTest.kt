package com.anishop.aninote

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.*
import org.junit.Test

class MoneyInputTest {
 private fun transformed(raw:String)=AmountVisualTransformation.filter(AnnotatedString(raw))
 @Test fun groupWholePartWithoutChangingFraction() {
  mapOf("" to "", "1500000" to "1,500,000", "1500000.5" to "1,500,000.5",
   "1500000." to "1,500,000.", "0.000123" to "0.000123", ".5" to ".5",
   "۱۵۰۰۰۰۰٫۵" to "۱,۵۰۰,۰۰۰٫۵", "1,500,000.5" to "1,500,000.5",
   "1.2.3" to "1.2.3").forEach{(input,expected)->assertEquals(expected,transformed(input).text.text)}
 }
 @Test fun caretMapsSafelyAtEveryInsertionAndDeletionBoundary() {
  listOf("", "1", "123", "1234", "1234567", "1234567.50", "۱۵۰۰۰۰۰٫۵").forEach { raw ->
   val result=transformed(raw);val map=result.offsetMapping
   val originals=(0..raw.length).map{map.originalToTransformed(it)}
   val displays=(0..result.text.length).map{map.transformedToOriginal(it)}
   assertEquals(originals.sorted(),originals);assertEquals(displays.sorted(),displays)
   originals.forEach{assertTrue(it in 0..result.text.length)}
   displays.forEach{assertTrue(it in 0..raw.length)}
   (0..raw.length).forEach{assertEquals(it,map.transformedToOriginal(map.originalToTransformed(it)))}
   assertEquals(raw.length,map.transformedToOriginal(result.text.length))
  }
 }
}
