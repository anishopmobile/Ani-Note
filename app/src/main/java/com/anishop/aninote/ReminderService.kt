package com.anishop.aninote
import android.app.*
import android.content.*
import android.media.*
import android.os.*
import android.net.Uri

class ReminderService:Service(){
 private val handler=Handler(Looper.getMainLooper())
 private var player:MediaPlayer?=null
 private var active:String?=null
 private var stopTask:Runnable?=null
 override fun onBind(i:Intent?):IBinder?=null
 override fun onStartCommand(i:Intent?,flags:Int,startId:Int):Int {
  if(i?.action=="stop"){
   if(i.getStringExtra("id")==active)release()
   stopSelf(startId)
   return START_NOT_STICKY
  }
  val id=i?.getStringExtra("id")?:return START_NOT_STICKY
  val at=i.getLongExtra("at",0L)
  if(Alarms.stopped(this,id,at))return START_NOT_STICKY
  release()
  active=id
  val manager=getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  manager.createNotificationChannel(NotificationChannel("ani_ringing","زنگ یادآوری فعال",NotificationManager.IMPORTANCE_HIGH).apply{setSound(null,null)})
  val stop=Intent(this,ReminderStopReceiver::class.java).putExtra("id",id).putExtra("at",at)
  val stopPi=PendingIntent.getBroadcast(this,id.hashCode(),stop,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val openPi=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val notif=Notification.Builder(this,"ani_ringing")
   .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
   .setContentTitle(i.getStringExtra("title")?.ifBlank{"یادآوری"}?:"یادآوری")
   .setContentText(i.getStringExtra("body"))
   .setContentIntent(openPi)
   .setOngoing(true)
   .addAction(Notification.Action.Builder(android.R.drawable.ic_media_pause,"توقف یادآوری",stopPi).build())
   .build()
  startForeground(id.hashCode().let{if(it==0)1 else it},notif)
  val p=getSharedPreferences("data",0)
  val sound=p.getString("sound_uri","").orEmpty()
  val uri=if(sound.isBlank())RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)else Uri.parse(sound)
  try{
   val attrs=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
   player=MediaPlayer().apply{
    setAudioAttributes(attrs)
    try{setDataSource(this@ReminderService,uri)}
    catch(_:Exception){setDataSource(this@ReminderService,RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))}
    isLooping=true
    prepare()
    start()
   }
  }catch(_:Exception){release()}
  val task=Runnable{release();stopSelf()}
  stopTask=task
  handler.postDelayed(task,p.getInt("ring_duration",20).coerceIn(1,300)*1000L)
  return START_NOT_STICKY
 }
 private fun release(){
  stopTask?.let{handler.removeCallbacks(it)}
  stopTask=null
  try{player?.stop()}catch(_:Exception){}
  player?.release();player=null
  active=null
 }
 override fun onDestroy(){release();super.onDestroy()}
}
