package com.anishop.aninote

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.net.Uri
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.app.NotificationChannel
import android.app.NotificationManager
import org.json.JSONObject
import org.json.JSONArray
import java.io.ByteArrayOutputStream

object BackupCodec {
 fun export(repo:Repo):String = JSONObject()
  .put("format","ani-note-backup")
  .put("version",1)
  .put("created",System.currentTimeMillis())
  .put("notes",JSONArray(repo.encrypted.read("notes")))
  .put("accounts",JSONArray(repo.encrypted.read("accounts")))
  .put("sound",repo.p.getString("sound_uri",""))
  .put("ring_duration",repo.p.getInt("ring_duration",20))
  .put("ring_repeat",repo.p.getBoolean("ring_repeat",false))
  .put("repeat_minutes",repo.p.getInt("repeat_minutes",2))
  .toString(2)
 fun validate(text:String):JSONObject {
  val json=JSONObject(text)
  require(json.getString("format")=="ani-note-backup" && json.getInt("version")==1)
  val notes=json.getJSONArray("notes")
  val accounts=json.getJSONArray("accounts")
  require(notes.length()<=10000 && accounts.length()<=10000)
  for(i in 0 until notes.length()){
   val n=notes.getJSONObject(i)
   require(n.getString("id").isNotBlank())
   val parts=n.getJSONArray("parts")
   require(parts.length() in 1..1000)
   for(j in 0 until parts.length()){
    val p=parts.getJSONObject(j)
    p.getString("id");p.getString("title");p.getString("body");p.getLong("alarm")
   }
  }
  for(i in 0 until accounts.length()){
   val a=accounts.getJSONObject(i)
   a.getString("id");a.getString("title")
   val entries=a.getJSONArray("entries")
   require(entries.length()<=100000)
   for(j in 0 until entries.length()){
    val e=entries.getJSONObject(j)
    e.getString("id");e.getString("subject");e.getLong("amount");e.getLong("date")
   }
  }
  require(json.optInt("ring_duration",20) in 1..300)
  require(json.optInt("repeat_minutes",2) in 1..1440)
  return json
 }
 fun restore(repo:Repo,text:String){
  val j=validate(text)
  repo.encrypted.write("notes",j.getJSONArray("notes").toString())
  repo.encrypted.write("accounts",j.getJSONArray("accounts").toString())
  val ok=repo.p.edit().putString("sound_uri",j.optString("sound",""))
   .putInt("ring_duration",j.optInt("ring_duration",20))
   .putBoolean("ring_repeat",j.optBoolean("ring_repeat",false))
   .putInt("repeat_minutes",j.optInt("repeat_minutes",2)).commit()
  check(ok){"Failed to write backup"}
  Alarms.sync(repo.context,repo.notes())
 }
}
object NotificationSounds {
 fun channelId(sound:String):String = "reminder_"+sound.hashCode().toUInt().toString(16)
 fun channel(context:Context,sound:String):String {
  val id=channelId(sound)
  val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  val uri=if(sound.isBlank())RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE) else Uri.parse(sound)
  val attributes=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
   .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
  val channel=NotificationChannel(id,"یادآورهای Ani Note",NotificationManager.IMPORTANCE_HIGH)
  channel.setSound(uri,attributes)
  manager.createNotificationChannel(channel)
  return id
 }
}
object PdfReports {
 fun create(context:Context,title:String,lines:List<String>):ByteArray {
  val pdf=PdfDocument()
  val pageWidth=595
  val pageHeight=842
  val margin=40
  val textPaint=TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
   color=android.graphics.Color.rgb(28,33,62)
   textSize=14f
   typeface=context.resources.getFont(R.font.vazirmatn)
  }
  val titlePaint=TextPaint(textPaint).apply{textSize=20f;typeface=Typeface.create(textPaint.typeface,Typeface.BOLD)}
  val layoutWidth=pageWidth-2*margin
  var page:PdfDocument.Page?=null
  var y=margin
  var pageNumber=0
  fun startPage(){
   pageNumber++
   page=pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth,pageHeight,pageNumber).create())
   y=margin
  }
  fun line(value:String,paint:TextPaint){
   val layout=StaticLayout.Builder.obtain(value,0,value.length,paint,layoutWidth)
    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
    .setTextDirection(TextDirectionHeuristics.RTL)
    .setIncludePad(false)
    .build()
   if(page==null || y+layout.height+16>pageHeight-margin){
    if(page!=null)pdf.finishPage(page!!)
    startPage()
   }
   val canvas=page!!.canvas
   canvas.save()
   canvas.translate(margin.toFloat(),y.toFloat())
   layout.draw(canvas)
   canvas.restore()
   y+=layout.height+16
  }
  line(title,titlePaint)
  line("Ani Note | "+jalali(System.currentTimeMillis()),textPaint)
  for(row in lines)line(row.ifBlank{" "},textPaint)
  if(page!=null)pdf.finishPage(page!!)
  val out=ByteArrayOutputStream()
  pdf.writeTo(out)
  pdf.close()
  return out.toByteArray()
 }
}
