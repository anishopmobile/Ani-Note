package com.anishop.aninote

import android.icu.util.Calendar
import android.icu.util.ULocale
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*

internal fun persianCal(millis:Long=System.currentTimeMillis()):Calendar =
 Calendar.getInstance(ULocale("fa_IR@calendar=persian")).apply{timeInMillis=millis}
internal data class Holiday(val label:String,val official:Boolean=true)
internal fun fixedHoliday(month:Int,day:Int):Holiday?=when(month to day){
 0 to 1->Holiday("آغاز نوروز")
 0 to 2->Holiday("عید نوروز")
 0 to 3->Holiday("عید نوروز")
 0 to 4->Holiday("عید نوروز")
 0 to 12->Holiday("روز جمهوری اسلامی")
 0 to 13->Holiday("روز طبیعت")
 2 to 14->Holiday("رحلت امام خمینی")
 2 to 15->Holiday("قیام پانزده خرداد")
 10 to 22->Holiday("پیروزی انقلاب اسلامی")
 11 to 29->Holiday("روز ملی شدن صنعت نفت")
 else->null
}
internal fun calendarOffset(dayOfWeek:Int):Int=dayOfWeek%7
internal fun calendarRows(offset:Int,days:Int)=(offset+days+6)/7
private val months=listOf("فروردین","اردیبهشت","خرداد","تیر","مرداد","شهریور","مهر","آبان","آذر","دی","بهمن","اسفند")

@Composable fun CalendarPage(showHolidays:Boolean,showEvents:Boolean,showFridays:Boolean) {
 val initial=remember{persianCal()}
 var year by remember{mutableIntStateOf(initial.get(Calendar.YEAR))}
 var month by remember{mutableIntStateOf(initial.get(Calendar.MONTH))}
 var selected by remember{mutableIntStateOf(initial.get(Calendar.DAY_OF_MONTH))}
 fun shift(delta:Int){
  val c=persianCal().apply{clear();set(Calendar.YEAR,year);set(Calendar.MONTH,month);set(Calendar.DAY_OF_MONTH,1);add(Calendar.MONTH,delta)}
  year=c.get(Calendar.YEAR);month=c.get(Calendar.MONTH);selected=1
 }
 val first=remember(year,month){persianCal().apply{clear();set(Calendar.YEAR,year);set(Calendar.MONTH,month);set(Calendar.DAY_OF_MONTH,1)}}
 val offset=calendarOffset(first.get(Calendar.DAY_OF_WEEK))
 val count=first.getActualMaximum(Calendar.DAY_OF_MONTH)
 val rows=calendarRows(offset,count)
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)){
  Text("تقویم شمسی",style=MaterialTheme.typography.titleLarge)
  ElevatedCard {
   Column(Modifier.fillMaxWidth().padding(12.dp)){
    Row(verticalAlignment=Alignment.CenterVertically){
     IconButton(onClick={shift(-1)}){Icon(Icons.Default.ChevronRight,"ماه قبل")}
     Text(months[month]+" "+year,Modifier.weight(1f),textAlign=TextAlign.Center,style=MaterialTheme.typography.titleMedium)
     IconButton(onClick={shift(1)}){Icon(Icons.Default.ChevronLeft,"ماه بعد")}
    }
    Row(Modifier.fillMaxWidth()){
     listOf("ش","ی","د","س","چ","پ","ج").forEach{label->
      Box(Modifier.weight(1f).height(36.dp),contentAlignment=Alignment.Center){Text(label)}
     }
    }
    repeat(rows){row->
     Row(Modifier.fillMaxWidth()){
      repeat(7){col->
       val day=row*7+col-offset+1
       Box(Modifier.weight(1f).height(48.dp),contentAlignment=Alignment.Center){
        if(day in 1..count){
         val official=fixedHoliday(month,day)!=null && showHolidays
         val friday=col==6&&showFridays
         val color=if(official||friday)Color(0xFFCC3247)else MaterialTheme.colorScheme.onSurface
         Surface(onClick={selected=day},shape=MaterialTheme.shapes.medium,
          color=if(selected==day)MaterialTheme.colorScheme.primaryContainer else Color.Transparent){
          Column(Modifier.size(42.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
           Text(day.toString(),color=color,maxLines=1,softWrap=false)
           if(official)Text("•",color=Color.Red)
          }
         }
        }
       }
      }
     }
    }
   }
  }
  val date=persianCal().apply{clear();set(Calendar.YEAR,year);set(Calendar.MONTH,month);set(Calendar.DAY_OF_MONTH,selected)}
  ElevatedCard {
   Column(Modifier.fillMaxWidth().padding(16.dp)){
    Text("تاریخ انتخاب‌شده: "+jalali(date.timeInMillis),style=MaterialTheme.typography.titleMedium)
    val h=fixedHoliday(month,selected)
    if(showEvents&&h!=null)Text(h.label,color=MaterialTheme.colorScheme.error)
    if(showEvents&&h==null)Text("مناسبت ثبت‌شده‌ای در فهرست فعلی وجود ندارد.")
    Text("تعطیلات متغیر قمری و داده‌های سالانه هنوز در این نسخه پوشش داده نشده‌اند.",style=MaterialTheme.typography.bodySmall,color=Color.Gray)
   }
  }
 }
}
