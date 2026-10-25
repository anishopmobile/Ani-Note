package com.anishop.aninote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
class CalendarLayoutTest {
 @Test fun monthFitsAllWeekdayOffsets() {
  for (offset in 0..6) for (days in 28..31) {
   val weeks=calendarWeekCount(offset,days)
   val finalCell=offset+days-1
   assertEquals((finalCell/7)+1,weeks)
   assertEquals(true,weeks in 4..6)
  }
 }
 @Test fun saturdayMonthBeginsInFirstColumn() {
  assertEquals(5,calendarWeekCount(0,31))
 }
 @Test fun fridayMonthCanNeedSixRows() {
  assertEquals(6,calendarWeekCount(6,31))
 }
 @Test fun rejectsInvalidOffset() {
  assertThrows(IllegalArgumentException::class.java){calendarWeekCount(7,30)}
 }
}