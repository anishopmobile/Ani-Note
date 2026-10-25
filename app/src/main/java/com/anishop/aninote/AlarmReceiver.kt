package com.anishop.aninote
import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
object Alarms {
 fun stopped(c:Context,id:String,at:Long)=c.getSharedPreferences("stopped",0).getLong(id,-1L)==at
 fun stop(c:Context,id:String,at:Long){
  c.getSharedPreferences("stopped",0).edit().putLong(id,at).apply()
  val am=c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
  PendingIntent.getBroadcast(c,id.hashCode(),Intent(c,AlarmReceiver::class.java),PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let{am.cancel(it);it.cancel()}
 }
 fun schedule(c:Context,s:Part,time:Long){
  val i=Intent(c,AlarmReceiver::class.java).putExtra("title",s.title).putExtra("body",s.body).putExtra("id",s.id).putExtra("at",s.alarm).putExtra("at",s.alarm)
  val p=PendingIntent.getBroadcast(c,s.id.hashCode(),i,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val manager=c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
  if(Build.VERSION.SDK_INT>=31&&!manager.canScheduleExactAlarms())manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,time,p)
  else manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,time,p)
 }

 fun sync(c:Context,notes:List<Note>) {
  val am=c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
  val prefs=c.getSharedPreferences("scheduled",0)
  val parts=notes.flatMap{it.parts}.filter{it.alarm>System.currentTimeMillis()}
  val ids=parts.map{it.id}
  (prefs.getString("ids","")?:"").split("|").filter{it.isNotEmpty()&&!ids.contains(it)}.forEach{id->
   val p=PendingIntent.getBroadcast(c,id.hashCode(),Intent(c,AlarmReceiver::class.java),PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
   if(p!=null){am.cancel(p);p.cancel()}
  }
  parts.filterNot{stopped(c,it.id,it.alarm)}.forEach{s->
   val i=Intent(c,AlarmReceiver::class.java).putExtra("title",s.title).putExtra("body",s.body).putExtra("id",s.id).putExtra("at",s.alarm)
   val p=PendingIntent.getBroadcast(c,s.id.hashCode(),i,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
   if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms())am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,s.alarm,p)
   else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,s.alarm,p)
  }
  prefs.edit().putString("ids",ids.joinToString("|")).apply()
 }
}
class AlarmReceiver:BroadcastReceiver(){
 override fun onReceive(c:Context,i:Intent){
  val id=i.getStringExtra("id")?:return
  val at=i.getLongExtra("at",0L)
  if(Alarms.stopped(c,id,at))return
  val part=Repo(c).notes().flatMap{it.parts}.firstOrNull{it.id==id&&it.alarm==at}?:return
  val intent=Intent(c,ReminderService::class.java).setAction("start")
   .putExtra("id",part.id).putExtra("at",part.alarm).putExtra("title",part.title).putExtra("body",part.body)
  try{c.startForegroundService(intent)}catch(e:Exception){
   android.util.Log.e("AniNoteAlarm","Foreground reminder failed",e)
   val mgr=c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
   mgr.createNotificationChannel(NotificationChannel("ani_fallback","یادآورهای Ani Note",NotificationManager.IMPORTANCE_HIGH))
   if(Build.VERSION.SDK_INT<33 || c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED){
    val stop=PendingIntent.getBroadcast(c,id.hashCode(),Intent(c,ReminderStopReceiver::class.java).putExtra("id",id).putExtra("at",at),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    mgr.notify(id.hashCode().let{if(it==0)1 else it},Notification.Builder(c,"ani_fallback").setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(part.title.ifBlank{"یادآوری"}).setContentText(part.body).addAction(Notification.Action.Builder(android.R.drawable.ic_media_pause,"توقف یادآوری",stop).build()).build())
   }
  }
  val pref=c.getSharedPreferences("data",0)
  if(pref.getBoolean("ring_repeat",false)&&!Alarms.stopped(c,id,at)){
   Alarms.schedule(c,part,System.currentTimeMillis()+pref.getInt("repeat_minutes",2).coerceIn(1,1440)*60_000L)
  }
 }
}
class ReminderStopReceiver:BroadcastReceiver(){
 override fun onReceive(c:Context,i:Intent){
  val id=i.getStringExtra("id")?:return
  Alarms.stop(c,id,i.getLongExtra("at",0L))
  c.startService(Intent(c,ReminderService::class.java).setAction("stop").putExtra("id",id))
  (c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(id.hashCode().let{if(it==0)1 else it})
 }
}
class BootReceiver:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){if(i.action==Intent.ACTION_BOOT_COMPLETED)Alarms.sync(c,Repo(c).notes())}}
