package com.anishop.aninote
import org.junit.Assert.*
import org.junit.Test
class CalendarFeatureTest {
 @Test fun startsSaturdayAtZero() { assertEquals(0,calendarOffset(7)) }
 @Test fun startsFridayAtSix() { assertEquals(6,calendarOffset(6)) }
 @Test fun monthGridIsBounded(){ for(days in 29..31)for(offset in 0..6){assertTrue(calendarRows(offset,days) in 5..6 || calendarRows(offset,days)==4)} }
 @Test fun fixedHolidaySample(){assertEquals("آغاز نوروز",fixedHoliday(0,1)?.label);assertNull(fixedHoliday(0,8))}
}