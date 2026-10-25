package com.anishop.aninote
import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
object Alarms {
 fun sync(c:Context,notes:List<Note>) {
  val am=c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
  val prefs=c.getSharedPreferences("scheduled",0)
  val parts=notes.flatMap{it.parts}.filter{it.alarm>System.currentTimeMillis()}
  val ids=parts.map{it.id}
  (prefs.getString("ids","")?:"").split("|").filter{it.isNotEmpty()&&!ids.contains(it)}.forEach{id->
   val p=PendingIntent.getBroadcast(c,id.hashCode(),Intent(c,AlarmReceiver::class.java),PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
   if(p!=null){am.cancel(p);p.cancel()}
  }
  parts.forEach{s->
   val i=Intent(c,AlarmReceiver::class.java).putExtra("title",s.title).putExtra("body",s.body).putExtra("id",s.id)
   val p=PendingIntent.getBroadcast(c,s.id.hashCode(),i,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
   if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms())am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,s.alarm,p)
   else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,s.alarm,p)
  }
  prefs.edit().putString("ids",ids.joinToString("|")).apply()
 }
}
class AlarmReceiver:BroadcastReceiver(){
 override fun onReceive(c:Context,i:Intent){
  val mgr=c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  val sound=c.getSharedPreferences("data",0).getString("sound_uri","") ?: ""
  val channel=NotificationSounds.channel(c,sound)
  if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
  val open=PendingIntent.getActivity(c,0,Intent(c,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  mgr.notify((i.getStringExtra("id")?:"").hashCode(),Notification.Builder(c,channel).setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle(i.getStringExtra("title")?:"یادآوری").setContentText(i.getStringExtra("body")).setStyle(Notification.BigTextStyle().bigText(i.getStringExtra("body"))).setContentIntent(open).setAutoCancel(true).build())
 }
}
class BootReceiver:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){if(i.action==Intent.ACTION_BOOT_COMPLETED)Alarms.sync(c,Repo(c).notes())}}
