package com.anishop.aninote
import android.appwidget.*
import android.content.*
import android.app.PendingIntent
import android.widget.RemoteViews
import android.graphics.*
import android.graphics.drawable.Icon
import android.content.pm.*
import android.net.Uri
import android.view.View
import android.icu.util.Calendar
import android.graphics.Color
import android.graphics.Paint

abstract class AniWidget:AppWidgetProvider(){
 protected abstract val type:String
 override fun onUpdate(context:Context,manager:AppWidgetManager,ids:IntArray){
  ids.forEach{render(context,manager,it,type)}
 }
 companion object {
  private fun calendarBitmap():Bitmap {
   val b=Bitmap.createBitmap(630,470,Bitmap.Config.ARGB_8888)
   val canvas=Canvas(b);canvas.drawColor(Color.rgb(231,235,252))
   val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.rgb(28,40,70);textAlign=Paint.Align.CENTER;textSize=31f;typeface=Typeface.DEFAULT_BOLD}
   val c=persianCal();val year=c.get(Calendar.YEAR);val month=c.get(Calendar.MONTH);val today=c.get(Calendar.DAY_OF_MONTH)
   val first=persianCal().apply{clear();set(Calendar.YEAR,year);set(Calendar.MONTH,month);set(Calendar.DAY_OF_MONTH,1)}
   val offset=first.get(Calendar.DAY_OF_WEEK)%7;val count=first.getActualMaximum(Calendar.DAY_OF_MONTH)
   listOf("ش","ی","د","س","چ","پ","ج").forEachIndexed{col,t->paint.color=Color.DKGRAY;canvas.drawText(t,45f+col*90f,55f,paint)}
   for(day in 1..count){val slot=day+offset-1;val x=45f+(slot%7)*90f;val y=112f+(slot/7)*60f
    if(day==today){paint.color=Color.rgb(90,110,215);canvas.drawCircle(x,y-10f,28f,paint)}
    paint.color=if(day==today)Color.WHITE else if(slot%7==6)Color.RED else Color.rgb(28,40,70)
    canvas.drawText(day.toString(),x,y,paint)
   }
   return b
  }

  fun render(c:Context,m:AppWidgetManager,id:Int,type:String){
   val preferences=c.getSharedPreferences("widget_prefs",0)
   val views=RemoteViews(c.packageName,R.layout.ani_widget)
   val repo=Repo(c)
   val reveal=preferences.getBoolean("reveal",false)
   val chosenNotes=try{repo.notes().filter{preferences.getStringSet("notes",emptySet()).orEmpty().contains(it.id)}}catch(_:Exception){emptyList()}
   val content=try{
    when(type){
     "note"->{
      val ids=preferences.getStringSet("notes",emptySet()).orEmpty()
      val selected=repo.notes().filter{ids.contains(it.id)}
      if(!reveal)selected.size.toString()+" یادداشت انتخاب شده (نمایش عنوان با فعال کردن تنظیمات ویجت)"
      else selected.take(3).joinToString("\n"){it.parts.firstOrNull()?.title.orEmpty()}
     }
     "account"->{
      val ids=preferences.getStringSet("accounts",emptySet()).orEmpty()
      val selected=repo.accounts().filter{ids.contains(it.id)}
      if(!reveal)selected.size.toString()+" حساب انتخاب شده"
      else selected.take(3).joinToString("\n"){it.title+": "+money(it.entries.sumOf{e->e.amount})}
     }
     else->{
      val now=persianCal()
      val y=now.get(android.icu.util.Calendar.YEAR)
      val month=now.get(android.icu.util.Calendar.MONTH)
      val first=persianCal().apply{clear();set(android.icu.util.Calendar.YEAR,y);set(android.icu.util.Calendar.MONTH,month);set(android.icu.util.Calendar.DAY_OF_MONTH,1)}
      val offset=first.get(android.icu.util.Calendar.DAY_OF_WEEK)%7
      val days=first.getActualMaximum(android.icu.util.Calendar.DAY_OF_MONTH)
      val lines=(0 until calendarRows(offset,days)).joinToString("\n"){row->
       (0..6).joinToString(" "){col->
        val d=row*7+col-offset+1
        if(d in 1..days) d.toString().padStart(2,' ') else "  "
       }
      }
      jalali(System.currentTimeMillis())+"\nش ی د س چ پ ج\n"+lines
     }
    }
   }catch(e:Exception){"برای نمایش اطلاعات، برنامه را باز کنید"}
   val label=when(type){"note"->"یادداشت‌ها";"account"->"حساب‌ها";else->"تقویم شمسی"}
   if(type=="note"){
    val rows=listOf(R.id.note_row1,R.id.note_row2,R.id.note_row3)
    rows.forEachIndexed{index,viewId->
     val note=chosenNotes.getOrNull(index)
     views.setViewVisibility(viewId,if(note==null)View.GONE else View.VISIBLE)
     if(note!=null){
      views.setTextViewText(viewId,if(reveal) note.parts.firstOrNull()?.title?.ifBlank{"بدون عنوان"} ?: "بدون عنوان" else "یادداشت "+(index+1))
      val intent=Intent(c,MainActivity::class.java).putExtra("open_note_id",note.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
      views.setOnClickPendingIntent(viewId,PendingIntent.getActivity(c,(id*17+index),intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
     }
    }
   }
   if(type=="calendar"){
    views.setViewVisibility(R.id.widget_calendar,View.VISIBLE)
    views.setImageViewBitmap(R.id.widget_calendar,calendarBitmap())
   }
   views.setTextViewText(R.id.widget_title,label)
   views.setTextViewText(R.id.widget_text,if(type=="calendar")"" else if(type=="note")"برای انتخاب یادداشت‌ها: تنظیمات ← ویجت‌ها" else content)
   views.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getActivity(c,id,Intent(c,MainActivity::class.java).putExtra("widget_settings",type=="note"),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
   m.updateAppWidget(id,views)
  }
  fun refresh(c:Context){
   val m=AppWidgetManager.getInstance(c)
   listOf(NoteWidget::class.java to "note",AccountWidget::class.java to "account",CalendarWidget::class.java to "calendar").forEach{(cl,type)->
    m.getAppWidgetIds(ComponentName(c,cl)).forEach{render(c,m,it,type)}
   }
  }
 }
}
class NoteWidget:AniWidget(){override val type="note"}
class AccountWidget:AniWidget(){override val type="account"}
class CalendarWidget:AniWidget(){override val type="calendar"}
