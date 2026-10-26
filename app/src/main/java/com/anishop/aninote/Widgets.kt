package com.anishop.aninote
import android.appwidget.*
import android.content.*
import android.app.PendingIntent
import android.widget.RemoteViews
import android.graphics.*
import android.graphics.drawable.Icon
import android.content.pm.*
import android.net.Uri

object CustomShortcut {
 fun pin(context:Context,uri:Uri):Boolean {
  val manager=context.getSystemService(ShortcutManager::class.java)
  if(!manager.isRequestPinShortcutSupported)return false
  val original=context.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it)}?:return false
  val square=Bitmap.createScaledBitmap(original,192,192,true)
  val shortcut=ShortcutInfo.Builder(context,"ani_personal_icon")
   .setShortLabel("Ani Note").setIcon(Icon.createWithBitmap(square))
   .setIntent(Intent(context,MainActivity::class.java).setAction(Intent.ACTION_MAIN)).build()
  return manager.requestPinShortcut(shortcut,null)
 }
}
abstract class AniWidget:AppWidgetProvider(){
 protected abstract val type:String
 override fun onUpdate(context:Context,manager:AppWidgetManager,ids:IntArray){
  ids.forEach{render(context,manager,it,type)}
 }
 companion object {
  fun render(c:Context,m:AppWidgetManager,id:Int,type:String){
   val preferences=c.getSharedPreferences("widget_prefs",0)
   val views=RemoteViews(c.packageName,R.layout.ani_widget)
   val repo=Repo(c)
   val reveal=preferences.getBoolean("reveal",false)
   val content=try{
    when(type){
     "note"->{
      val ids=preferences.getStringSet("notes",emptySet()).orEmpty()
      val selected=repo.notes().filter{ids.contains(it.id)}
      if(!reveal)selected.size.toString()+" یادداشت انتخاب شده"
      else selected.take(3).joinToString("\n"){it.parts.firstOrNull()?.title.orEmpty()}
     }
     "account"->{
      val ids=preferences.getStringSet("accounts",emptySet()).orEmpty()
      val selected=repo.accounts().filter{ids.contains(it.id)}
      if(!reveal)selected.size.toString()+" حساب انتخاب شده"
      else selected.take(3).joinToString("\n"){it.title+": "+money(it.entries.sumOf{e->e.amount})}
     }
     else->jalali(System.currentTimeMillis())
    }
   }catch(e:Exception){"برای نمایش اطلاعات، برنامه را باز کنید"}
   val label=when(type){"note"->"یادداشت‌ها";"account"->"حساب‌ها";else->"تقویم شمسی"}
   views.setTextViewText(R.id.widget_title,label)
   views.setTextViewText(R.id.widget_text,content)
   views.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getActivity(c,id,Intent(c,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
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
