package com.anishop.aninote

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object UpdatePolicy {
 const val releases = "https://api.github.com/repos/anishopmobile/Ani-Note/releases?per_page=100"
 fun code(tag:String):Int? = Regex("main-v([1-9][0-9]{0,9})").matchEntire(tag)
  ?.groupValues?.get(1)?.toIntOrNull()?.takeIf{it<=2100000000}
 fun safeAsset(url:String):Boolean = try {
  val u=java.net.URI(url)
  u.scheme=="https" && u.host=="github.com" && u.port==-1 && u.userInfo==null && u.query==null && u.fragment==null &&
   Regex("/anishopmobile/Ani-Note/releases/download/main-v[0-9]+/Ani-Note-main-[0-9]+\\.apk").matches(u.rawPath)
 } catch(_:Exception){false}
}
data class AvailableUpdate(val code:Int,val url:String)
object Updates {
 fun check(installed:Long):AvailableUpdate? {
  val connection=URL(UpdatePolicy.releases).openConnection() as HttpsURLConnection
  connection.connectTimeout=15000; connection.readTimeout=15000
  connection.instanceFollowRedirects=false
  connection.setRequestProperty("Accept","application/vnd.github+json")
  connection.setRequestProperty("User-Agent","Ani-Note-Main")
  connection.useCaches=false
  try {
   check(connection.responseCode==200)
   val bytes=connection.inputStream.use{it.readBytesLimited(2_000_000)}
   val releases=JSONArray(bytes.toString(Charsets.UTF_8))
   val candidates=mutableListOf<AvailableUpdate>()
   for(i in 0 until releases.length()) {
    val release=releases.getJSONObject(i)
    if(release.optBoolean("draft") || release.optBoolean("prerelease"))continue
    val code=UpdatePolicy.code(release.optString("tag_name"))?:continue
    if(code<=installed)continue
    val assets=release.optJSONArray("assets")?:continue
    for(j in 0 until assets.length()) {
     val asset=assets.getJSONObject(j)
     val url=asset.optString("browser_download_url")
     if(asset.optString("name")=="Ani-Note-main-$code.apk" && UpdatePolicy.safeAsset(url) &&
      url.endsWith("/main-v$code/Ani-Note-main-$code.apk"))candidates.add(AvailableUpdate(code,url))
    }
   }
   return candidates.maxByOrNull{it.code}
  } finally {connection.disconnect()}
 }
 private fun java.io.InputStream.readBytesLimited(limit:Int):ByteArray {
  val out=java.io.ByteArrayOutputStream(); val buffer=ByteArray(8192)
  while(true){val n=read(buffer);if(n<0)break;check(out.size()+n<=limit);out.write(buffer,0,n)}
  return out.toByteArray()
 }
}
@Composable fun UpdateSettings() {
 val context=LocalContext.current
 val scope=rememberCoroutineScope()
 val info=remember{context.packageManager.getPackageInfo(context.packageName,0)}
 @Suppress("DEPRECATION")
 val code=if(android.os.Build.VERSION.SDK_INT>=28)info.longVersionCode else info.versionCode.toLong()
 var busy by remember{mutableStateOf(false)}
 var message by remember{mutableStateOf("")}
 var update by remember{mutableStateOf<AvailableUpdate?>(null)}
 Text("بروزرسانی برنامه",style=MaterialTheme.typography.titleMedium)
 Text("نسخه ${info.versionName} • کد $code\n${context.packageName}")
 Button(enabled=!busy,onClick={
  busy=true;update=null
  scope.launch {
   try{update=withContext(Dispatchers.IO){Updates.check(code)};message=if(update==null)"نسخه جدیدتری برای برنامه اصلی منتشر نشده است." else "نسخه جدید آماده دریافت است."}
   catch(_:Exception){message="بررسی بروزرسانی ممکن نشد؛ اتصال اینترنت را بررسی و دوباره تلاش کنید."}
   finally{busy=false}
  }
 }){Text(if(busy)"در حال بررسی…" else "بررسی بروزرسانی")}
 if(message.isNotEmpty())Text(message)
 update?.let { available ->
  Text("ابتدا بکاپ بگیرید؛ سپس APK را دریافت و بدون حذف برنامه نصب کنید. نصب نیازمند تأیید شما و امضای سازگار است.")
  Button(onClick={try{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(available.url)))}catch(_:Exception){message="مرورگر برای دریافت فایل در دسترس نیست."}}){Text("دریافت نسخه ${available.code}")}
 }
}
