package com.anishop.aninote

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.media.RingtoneManager
import android.net.Uri
import android.icu.util.Calendar as PersianCalendar
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.Locale

data class Part(val id:String=UUID.randomUUID().toString(), val title:String="", val body:String="", val alarm:Long=0)
data class Note(val id:String=UUID.randomUUID().toString(), val parts:List<Part> = listOf(Part()))
data class Entry(val id:String=UUID.randomUUID().toString(), val subject:String="", val amount:Long=0, val date:Long=System.currentTimeMillis())
data class Account(val id:String=UUID.randomUUID().toString(), val title:String="", val entries:List<Entry> = emptyList())
fun jalali(m:Long):String { val c=PersianCalendar.getInstance(android.icu.util.ULocale("fa_IR@calendar=persian")).apply { timeInMillis=m }; return "%04d/%02d/%02d".format(Locale.US,c.get(1),c.get(2)+1,c.get(5)) }
fun clock(m:Long):String { val c=PersianCalendar.getInstance(android.icu.util.ULocale("fa_IR@calendar=persian")).apply { timeInMillis=m }; return "%02d:%02d".format(Locale.US,c.get(11),c.get(12)) }
fun money(m:Long)=String.format(Locale.US,"%,d",m)+" تومان"
val blue=Color(0xFF4654C8)
class Repo(val context:Context) {
 val p=context.getSharedPreferences("data",0)
 fun notes():List<Note> = try { val a=JSONArray(p.getString("notes","[]")); (0 until a.length()).map { i ->
  val n=a.getJSONObject(i); val x=n.getJSONArray("parts"); Note(n.getString("id"),(0 until x.length()).map { j -> val s=x.getJSONObject(j); Part(s.getString("id"),s.optString("title"),s.optString("body"),s.optLong("alarm")) })
 } } catch(_:Exception) { emptyList() }
 fun saveNotes(ns:List<Note>) { val a=JSONArray(); ns.forEach { n -> val ps=JSONArray(); n.parts.forEach { ps.put(JSONObject().put("id",it.id).put("title",it.title).put("body",it.body).put("alarm",it.alarm)) }; a.put(JSONObject().put("id",n.id).put("parts",ps)) }; p.edit().putString("notes",a.toString()).apply(); Alarms.sync(context,ns) }
 fun accounts():List<Account> = try { val a=JSONArray(p.getString("accounts","[]")); (0 until a.length()).map { i ->
  val n=a.getJSONObject(i); val x=n.getJSONArray("entries"); Account(n.getString("id"),n.getString("title"),(0 until x.length()).map { j -> val e=x.getJSONObject(j); Entry(e.getString("id"),e.getString("subject"),e.getLong("amount"),e.getLong("date")) })
 } } catch(_:Exception) { emptyList() }
 fun saveAccounts(ns:List<Account>) { val a=JSONArray(); ns.forEach { n -> val es=JSONArray(); n.entries.forEach { es.put(JSONObject().put("id",it.id).put("subject",it.subject).put("amount",it.amount).put("date",it.date)) }; a.put(JSONObject().put("id",n.id).put("title",n.title).put("entries",es)) }; p.edit().putString("accounts",a.toString()).apply() }
}
class MainActivity:ComponentActivity() {
 override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState); setContent { MainScreen(Repo(this)) } }
}
@Composable fun MainScreen(repo:Repo) {
 val context=LocalContext.current
 val font=FontFamily(Font(R.font.vazirmatn))
 val type=Typography(bodyLarge=TextStyle(fontFamily=font),bodyMedium=TextStyle(fontFamily=font),titleLarge=TextStyle(fontFamily=font,fontWeight=FontWeight.Bold,fontSize=22.sp),titleMedium=TextStyle(fontFamily=font,fontWeight=FontWeight.Bold,fontSize=17.sp),labelLarge=TextStyle(fontFamily=font),labelMedium=TextStyle(fontFamily=font))
 val notes=remember { mutableStateListOf<Note>().apply { addAll(repo.notes()) } }
 val accounts=remember { mutableStateListOf<Account>().apply { addAll(repo.accounts()) } }
 var tab by remember { mutableIntStateOf(0) }; var openNote by remember { mutableStateOf<String?>(null) }; var openAccount by remember { mutableStateOf<String?>(null) }
 var newAccount by remember { mutableStateOf(false) }; var accountName by remember { mutableStateOf("") }
 var deletingNote by remember { mutableStateOf<String?>(null) }
 var deletingAccount by remember { mutableStateOf<String?>(null) }
 var pendingExport by remember { mutableStateOf<ByteArray?>(null) }
 var importedBackup by remember { mutableStateOf<String?>(null) }
 var sound by remember { mutableStateOf(repo.p.getString("sound_uri","") ?: "") }
 val pdfLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")){uri->
  if(uri!=null)try { context.contentResolver.openOutputStream(uri)?.use{it.write(pendingExport ?: byteArrayOf())};android.widget.Toast.makeText(context,"گزارش PDF ذخیره شد",0).show() }catch(e:Exception){android.widget.Toast.makeText(context,"خطا در ذخیره PDF",1).show()}
 }
 val backupLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->
  if(uri!=null)try {context.contentResolver.openOutputStream(uri)?.use{it.write(BackupCodec.export(repo).toByteArray(Charsets.UTF_8))};android.widget.Toast.makeText(context,"بکاپ ذخیره شد",0).show()}catch(e:Exception){android.widget.Toast.makeText(context,"خطا در تهیه بکاپ",1).show()}
 }
 val importLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
  if(uri!=null)try {val bytes=context.contentResolver.openInputStream(uri)?.use{it.readBytes()} ?: byteArrayOf();require(bytes.size<=10_000_000){"فایل بیش از حد بزرگ است"};val json=bytes.toString(Charsets.UTF_8);BackupCodec.validate(json);importedBackup=json}catch(e:Exception){android.widget.Toast.makeText(context,"فایل بکاپ معتبر نیست",1).show()}
 }
 val ringtoneLauncher=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
  if(result.resultCode==android.app.Activity.RESULT_OK){
   @Suppress("DEPRECATION") val uri=if(Build.VERSION.SDK_INT>=33)result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI,Uri::class.java)else result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
   sound=uri?.toString() ?: ""
   repo.p.edit().putString("sound_uri",sound).apply()
   NotificationSounds.channel(context,sound)
  }
 }
 fun exportPdf(title:String,lines:List<String>){
  try {pendingExport=PdfReports.create(context,title,lines);pdfLauncher.launch("AniNote-"+System.currentTimeMillis()+".pdf")}catch(e:Exception){android.widget.Toast.makeText(context,"خطا در تولید PDF",1).show()}
 }

 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){}
 LaunchedEffect(Unit) { if(Build.VERSION.SDK_INT>=33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) permission.launch(Manifest.permission.POST_NOTIFICATIONS) }
 fun changeNote(n:Note) { val i=notes.indexOfFirst { it.id==n.id }; if(i>=0){ notes[i]=n; repo.saveNotes(notes) } }
 fun changeAccount(a:Account) { val i=accounts.indexOfFirst { it.id==a.id }; if(i>=0){ accounts[i]=a; repo.saveAccounts(accounts) } }
 MaterialTheme(colorScheme=lightColorScheme(primary=blue,background=Color(0xFFF6F7FC)),typography=type) {
 CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
 Scaffold(topBar={ Row(Modifier.fillMaxWidth().background(Color.White).padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
  if(openNote!=null || openAccount!=null) IconButton(onClick={openNote=null;openAccount=null}){ Icon(Icons.Default.ArrowForward,"بازگشت") }
  Column(Modifier.weight(1f)) { Text("Ani Note",style=MaterialTheme.typography.titleLarge,color=blue); Text("دفتر هوشمند فارسی",style=MaterialTheme.typography.bodyMedium) }
  Icon(Icons.Default.AutoAwesome,null,tint=blue)
 } },bottomBar={ if(openNote==null && openAccount==null) NavigationBar { NavigationBarItem(selected=tab==0,onClick={tab=0},icon={Icon(Icons.Default.Description,null)},label={Text("یادداشت‌ها")}); NavigationBarItem(selected=tab==1,onClick={tab=1},icon={Icon(Icons.Default.AccountBalanceWallet,null)},label={Text("مدیریت حساب")}); NavigationBarItem(selected=tab==2,onClick={tab=2},icon={Icon(Icons.Default.Settings,null)},label={Text("تنظیمات")}) } },
 floatingActionButton={ if(openNote==null && openAccount==null && tab!=2) FloatingActionButton(onClick={ if(tab==0){ val n=Note(); notes.add(0,n);repo.saveNotes(notes);openNote=n.id } else {accountName="";newAccount=true} },containerColor=blue,contentColor=Color.White){Icon(Icons.Default.Add,"افزودن")} }
 ) { padding -> Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
 when {
 openNote!=null -> { val n=notes.firstOrNull{it.id==openNote}; if(n!=null) NoteEditor(n,::changeNote,{notes.removeAll{it.id==n.id};repo.saveNotes(notes);openNote=null},{exportPdf(n.parts.firstOrNull()?.title ?: "یادداشت",n.parts.flatMapIndexed{index,p->listOf("بخش "+(index+1)+" — "+p.title,p.body)+ (if(p.alarm>0)listOf("یادآوری: "+jalali(p.alarm)+" "+clock(p.alarm))else emptyList())})}) }
 openAccount!=null -> { val a=accounts.firstOrNull{it.id==openAccount}; if(a!=null) AccountEditor(a,::changeAccount,{accounts.removeAll{it.id==a.id};repo.saveAccounts(accounts);openAccount=null},{exportPdf(a.title,listOf("مانده حساب: "+money(a.entries.sumOf{it.amount})) + a.entries.map{it.subject+" | "+money(it.amount)+" | "+jalali(it.date)})}) }
 tab==0 -> { var q by remember { mutableStateOf("") }; Text("یادداشت‌های من",style=MaterialTheme.typography.titleLarge); Spacer(Modifier.height(10.dp))
  OutlinedTextField(q,{q=it},Modifier.fillMaxWidth(),placeholder={Text("جستجو...")},leadingIcon={Icon(Icons.Default.Search,null)})
  Spacer(Modifier.height(12.dp))
  LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)) { items(notes.filter{n->n.parts.any{it.title.contains(q,true)||it.body.contains(q,true)}},key={it.id}) { n ->
   ElevatedCard(onClick={openNote=n.id},modifier=Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)) {Text(n.parts.firstOrNull()?.title?.ifBlank{"بدون عنوان"}?:"بدون عنوان",style=MaterialTheme.typography.titleMedium);Text("${n.parts.size} بخش",color=Color.Gray);if(n.parts.any{it.alarm>0})Text("⏰ دارای یادآور",color=blue) }; IconButton(onClick={deletingNote=n.id}){Icon(Icons.Default.DeleteOutline,"حذف")} } }
  } }
 }
 tab==1 -> { Text("مدیریت حساب‌ها",style=MaterialTheme.typography.titleLarge); Spacer(Modifier.height(12.dp))
  LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)) { items(accounts,key={it.id}){a->ElevatedCard(onClick={openAccount=a.id},modifier=Modifier.fillMaxWidth()) {Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(a.title,style=MaterialTheme.typography.titleMedium);Text("مانده: "+money(a.entries.sumOf{it.amount}));Text("${a.entries.size} تراکنش",color=Color.Gray)};IconButton(onClick={deletingAccount=a.id}){Icon(Icons.Default.DeleteOutline,"حذف")} }} } }
 }
 else -> {
  Text("تنظیمات",style=MaterialTheme.typography.titleLarge)
  Spacer(Modifier.height(20.dp))
  ElevatedCard {Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   Text("صدای یادآوری",style=MaterialTheme.typography.titleMedium)
   Text(if(sound.isBlank())"صدای پیش‌فرض اعلان‌ها" else "صدای انتخاب‌شده",color=Color.Gray)
   OutlinedButton(onClick={
    val intent=Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply{
     putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,RingtoneManager.TYPE_NOTIFICATION)
     putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT,true)
     putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT,false)
     putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,if(sound.isBlank())RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)else Uri.parse(sound))
    }
    ringtoneLauncher.launch(intent)
   }){Icon(Icons.Default.Notifications,null);Text(" انتخاب صدای یادآوری")}
  }}
  Spacer(Modifier.height(16.dp))
  ElevatedCard{Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   Text("انتقال و نگهداری اطلاعات",style=MaterialTheme.typography.titleMedium)
   Text("بکاپ شامل همه یادداشت‌ها، حساب‌ها و تنظیمات است.",color=Color.Gray)
   Button(onClick={backupLauncher.launch("AniNote-backup.json")},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.SaveAlt,null);Text(" تهیه بکاپ")}
   OutlinedButton(onClick={importLauncher.launch(arrayOf("application/json","text/plain","application/octet-stream"))},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Restore,null);Text(" بازیابی از فایل بکاپ")}
  }}
 }
 } } }
 if(deletingNote!=null) DeleteDialog({val id=deletingNote!!;notes.removeAll{it.id==id};repo.saveNotes(notes);deletingNote=null},{deletingNote=null})
 if(deletingAccount!=null) DeleteDialog({val id=deletingAccount!!;accounts.removeAll{it.id==id};repo.saveAccounts(accounts);deletingAccount=null},{deletingAccount=null})
 if(importedBackup!=null) AlertDialog(onDismissRequest={importedBackup=null},title={Text("تأیید بازیابی بکاپ")},text={Text("اطلاعات فعلی با اطلاعات فایل بکاپ جایگزین می‌شوند. قبل از ادامه از اطلاعات فعلی نسخه پشتیبان تهیه کنید.")},confirmButton={TextButton(onClick={
  try{BackupCodec.restore(repo,importedBackup!!);notes.clear();notes.addAll(repo.notes());accounts.clear();accounts.addAll(repo.accounts());sound=repo.p.getString("sound_uri","")?:"";NotificationSounds.channel(context,sound);importedBackup=null;android.widget.Toast.makeText(context,"بازیابی انجام شد",0).show()}catch(e:Exception){importedBackup=null;android.widget.Toast.makeText(context,"خطا در بازیابی",1).show()}
 }){Text("بازیابی")}},dismissButton={TextButton(onClick={importedBackup=null}){Text("انصراف")}})
 if(newAccount) AlertDialog(onDismissRequest={newAccount=false},title={Text("ایجاد حساب")},text={OutlinedTextField(accountName,{accountName=it},label={Text("عنوان حساب")})},confirmButton={TextButton(onClick={if(accountName.isNotBlank()){val a=Account(title=accountName.trim());accounts.add(a);repo.saveAccounts(accounts);openAccount=a.id;newAccount=false}}){Text("ایجاد")}},dismissButton={TextButton(onClick={newAccount=false}){Text("انصراف")}})
 } }
}
@Composable fun NoteEditor(n:Note,update:(Note)->Unit,delete:()->Unit,export:()->Unit) {
 val context=LocalContext.current
 var askDelete by remember { mutableStateOf(false) }
 Row(verticalAlignment=Alignment.CenterVertically){ Text("ذخیره خودکار فعال است",modifier=Modifier.weight(1f),color=blue);TextButton(onClick=export){Text("گزارش PDF")};TextButton(onClick={askDelete=true}){Text("حذف یادداشت")} }
 LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)) {
 items(n.parts,key={it.id}){ part ->
  var datePicker by remember { mutableStateOf(false) }
  ElevatedCard {Column(Modifier.padding(14.dp)) {
   Row(verticalAlignment=Alignment.CenterVertically){Text("بخش ${n.parts.indexOf(part)+1}",modifier=Modifier.weight(1f),color=blue);if(n.parts.size>1)IconButton(onClick={update(n.copy(parts=n.parts.filterNot{it.id==part.id}))}){Icon(Icons.Default.Close,"حذف بخش")}}
   Row(verticalAlignment=Alignment.CenterVertically){ OutlinedTextField(part.title,{v->update(n.copy(parts=n.parts.map{if(it.id==part.id)it.copy(title=v) else it}))},modifier=Modifier.weight(1f),label={Text("عنوان")})
    IconButton(onClick={datePicker=true}){Icon(if(part.alarm>0)Icons.Default.NotificationsActive else Icons.Default.AddAlarm,"یادآور",tint=blue)}
   }
   if(part.alarm>0) Row(verticalAlignment=Alignment.CenterVertically){Text("⏰ "+jalali(part.alarm)+" "+clock(part.alarm),modifier=Modifier.weight(1f));IconButton(onClick={update(n.copy(parts=n.parts.map{if(it.id==part.id)it.copy(alarm=0)else it}))}){Icon(Icons.Default.Close,"لغو")}}
   Spacer(Modifier.height(8.dp))
   OutlinedTextField(part.body,{v->update(n.copy(parts=n.parts.map{if(it.id==part.id)it.copy(body=v)else it}))},Modifier.fillMaxWidth().heightIn(min=150.dp),label={Text("متن و توضیحات")},minLines=4)
  }}
  if(datePicker) PersianPicker(if(part.alarm>0)part.alarm else System.currentTimeMillis(),{datePicker=false}){date->
   datePicker=false
   val cal=PersianCalendar.getInstance(android.icu.util.ULocale("fa_IR@calendar=persian")).apply{timeInMillis=date}
   TimePickerDialog(context,{_,h,m->cal.set(11,h);cal.set(12,m);cal.set(13,0);if(cal.timeInMillis>System.currentTimeMillis())update(n.copy(parts=n.parts.map{if(it.id==part.id)it.copy(alarm=cal.timeInMillis)else it}))},9,0,true).show()
  }
 }
 item { OutlinedButton(onClick={update(n.copy(parts=n.parts+Part()))},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.PostAdd,null);Text(" Page Break • بخش جدید")} }
 }
 if(askDelete) DeleteDialog({delete();askDelete=false},{askDelete=false})
}
@Composable fun AccountEditor(a:Account,update:(Account)->Unit,delete:()->Unit,export:()->Unit) {
 var add by remember { mutableStateOf(false) };var editing by remember { mutableStateOf<Entry?>(null) };var rename by remember { mutableStateOf(false) };var name by remember(a.id){ mutableStateOf(a.title) };var askDelete by remember { mutableStateOf(false) }
 Row(verticalAlignment=Alignment.CenterVertically){Text(a.title,style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f));IconButton(onClick=export){Icon(Icons.Default.PictureAsPdf,"گزارش PDF")};IconButton(onClick={rename=true}){Icon(Icons.Default.Edit,"ویرایش عنوان")};IconButton(onClick={askDelete=true}){Icon(Icons.Default.DeleteOutline,"حذف حساب")}}
 Card(colors=CardDefaults.cardColors(containerColor=blue)){Column(Modifier.fillMaxWidth().padding(20.dp)){Text("مانده حساب",color=Color.White);Text(money(a.entries.sumOf{it.amount}),color=Color.White,style=MaterialTheme.typography.titleLarge)}}
 Spacer(Modifier.height(12.dp))
 Button(onClick={editing=null;add=true},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Add,null);Text(" ثبت گردش جدید")}
 Spacer(Modifier.height(12.dp))
 LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(a.entries.sortedByDescending{it.date},key={it.id}){e->ElevatedCard{ Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(e.subject);Text(jalali(e.date),color=Color.Gray)};Text((if(e.amount>0)"+"else "")+money(e.amount),color=if(e.amount>=0)Color(0xFF16804A)else Color.Red);IconButton(onClick={editing=e;add=true}){Icon(Icons.Default.Edit,"ویرایش")};IconButton(onClick={update(a.copy(entries=a.entries.filterNot{it.id==e.id}))}){Icon(Icons.Default.DeleteOutline,"حذف")}} }} }
 if(add) EntryForm(editing,{add=false}){e->update(a.copy(entries=if(editing==null)a.entries+e else a.entries.map{if(it.id==e.id)e else it}));add=false}
 if(rename) AlertDialog(onDismissRequest={rename=false},title={Text("ویرایش عنوان حساب")},text={OutlinedTextField(name,{name=it})},confirmButton={TextButton(onClick={if(name.isNotBlank()){update(a.copy(title=name));rename=false}}){Text("ذخیره")}})
 if(askDelete) DeleteDialog(delete,{askDelete=false})
}
@Composable fun EntryForm(existing:Entry?,dismiss:()->Unit,save:(Entry)->Unit) {
 var subject by remember(existing?.id){mutableStateOf(existing?.subject?:"")};var amount by remember(existing?.id){mutableStateOf(existing?.amount?.let{kotlin.math.abs(it).toString()}?:"")};var plus by remember(existing?.id){mutableStateOf((existing?.amount?:0)>=0)};var date by remember(existing?.id){mutableLongStateOf(existing?.date?:System.currentTimeMillis())};var picker by remember{mutableStateOf(false)}
 AlertDialog(onDismissRequest=dismiss,title={Text("گردش حساب")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
  OutlinedTextField(subject,{subject=it},label={Text("موضوع")});OutlinedTextField(amount,{amount=it.filter{c->c.isDigit()}},label={Text("مبلغ (تومان)")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
  Row {FilterChip(plus,{plus=true},label={Text("+ واریز")});Spacer(Modifier.width(10.dp));FilterChip(!plus,{plus=false},label={Text("− برداشت")})}
  OutlinedButton(onClick={picker=true}){Text("تاریخ شمسی: "+jalali(date))}
 }},confirmButton={TextButton(onClick={val v=amount.toLongOrNull();if(subject.isNotBlank()&&v!=null&&v>0)save(Entry(existing?.id?:UUID.randomUUID().toString(),subject,if(plus)v else -v,date))}){Text("ذخیره")}},dismissButton={TextButton(onClick=dismiss){Text("انصراف")}})
 if(picker)PersianPicker(date,{picker=false}){date=it;picker=false}
}
internal fun calendarWeekCount(startOffset:Int,daysInMonth:Int):Int {
 require(startOffset in 0..6)
 require(daysInMonth in 28..31)
 return (startOffset+daysInMonth+6)/7
}
@Composable fun PersianPicker(initial:Long,dismiss:()->Unit,selected:(Long)->Unit) {
 val cal=remember(initial){PersianCalendar.getInstance(android.icu.util.ULocale("fa_IR@calendar=persian")).apply{timeInMillis=initial}}
 var year by remember(initial){mutableIntStateOf(cal.get(android.icu.util.Calendar.YEAR))}
 var month by remember(initial){mutableIntStateOf(cal.get(android.icu.util.Calendar.MONTH))}
 var day by remember(initial){mutableIntStateOf(cal.get(android.icu.util.Calendar.DAY_OF_MONTH))}
 val months=listOf("فروردین","اردیبهشت","خرداد","تیر","مرداد","شهریور","مهر","آبان","آذر","دی","بهمن","اسفند")
 val weekdays=listOf("ش","ی","د","س","چ","پ","ج")
 fun monthCal()=PersianCalendar.getInstance(android.icu.util.ULocale("fa_IR@calendar=persian")).apply{
  clear();set(android.icu.util.Calendar.YEAR,year);set(android.icu.util.Calendar.MONTH,month);set(android.icu.util.Calendar.DAY_OF_MONTH,1)
 }
 fun move(delta:Int){
  val c=monthCal().apply{add(android.icu.util.Calendar.MONTH,delta)}
  year=c.get(android.icu.util.Calendar.YEAR);month=c.get(android.icu.util.Calendar.MONTH);day=1
 }
 val first=remember(year,month){monthCal()}
 val count=remember(year,month){first.getActualMaximum(android.icu.util.Calendar.DAY_OF_MONTH)}
 // Android Calendar: Sunday=1 ... Saturday=7; Saturday is the first Persian weekday.
 val offset=remember(year,month){first.get(android.icu.util.Calendar.DAY_OF_WEEK)%7}
 val rows=calendarWeekCount(offset,count)
 AlertDialog(
  onDismissRequest=dismiss,
  shape=RoundedCornerShape(24.dp),
  title={Text("انتخاب تاریخ شمسی",style=MaterialTheme.typography.titleMedium)},
  text={Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
    IconButton(onClick={move(-1)}){Icon(Icons.Default.ChevronRight,"ماه قبل")}
    Text(months[month]+" "+year,Modifier.weight(1f),textAlign=androidx.compose.ui.text.style.TextAlign.Center,style=MaterialTheme.typography.titleMedium)
    IconButton(onClick={move(1)}){Icon(Icons.Default.ChevronLeft,"ماه بعد")}
   }
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
    TextButton(onClick={year--;day=1}){Text("سال قبل")}
    TextButton(onClick={year++;day=1}){Text("سال بعد")}
   }
   Row(Modifier.fillMaxWidth()){
    weekdays.forEach{label->
     Box(Modifier.weight(1f).height(32.dp),contentAlignment=Alignment.Center){
      Text(label,color=Color.Gray,style=MaterialTheme.typography.labelMedium,maxLines=1,softWrap=false)
     }
    }
   }
   repeat(rows){week->
    Row(Modifier.fillMaxWidth()){
     repeat(7){weekday->
      val num=week*7+weekday-offset+1
      Box(Modifier.weight(1f).height(42.dp).padding(1.dp),contentAlignment=Alignment.Center){
       if(num in 1..count){
        val chosen=day==num
        Box(
         Modifier.size(38.dp).background(if(chosen)blue else Color.Transparent,RoundedCornerShape(50))
          .clickable{day=num},
         contentAlignment=Alignment.Center
        ){
         Text(num.toString(),color=if(chosen)Color.White else MaterialTheme.colorScheme.onSurface,
          style=MaterialTheme.typography.bodyMedium,maxLines=1,softWrap=false,
          textAlign=androidx.compose.ui.text.style.TextAlign.Center)
        }
       }
      }
     }
    }
   }
  }},
  confirmButton={TextButton(onClick={
   val result=PersianCalendar.getInstance(android.icu.util.ULocale("fa_IR@calendar=persian")).apply{
    timeInMillis=initial
    set(android.icu.util.Calendar.DAY_OF_MONTH,1)
    set(android.icu.util.Calendar.YEAR,year)
    set(android.icu.util.Calendar.MONTH,month)
    set(android.icu.util.Calendar.DAY_OF_MONTH,day)
   }
   selected(result.timeInMillis)
  }){Text("تأیید")}},
  dismissButton={TextButton(onClick=dismiss){Text("انصراف")}}
 )
}
@Composable fun DeleteDialog(confirm:()->Unit,dismiss:()->Unit){AlertDialog(onDismissRequest=dismiss,title={Text("حذف اطلاعات")},text={Text("آیا از حذف مطمئن هستید؟")},confirmButton={TextButton(onClick=confirm){Text("حذف")}},dismissButton={TextButton(onClick=dismiss){Text("انصراف")}})}
