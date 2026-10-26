package com.anishop.aninote

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions

@Composable fun PatternPad(value:String,onChange:(String)->Unit) {
 Column {
  repeat(3){r->
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){
    repeat(3){c->
     val n=(r*3+c).toString()
     OutlinedButton(onClick={if(!value.contains(n))onChange(value+n)},modifier=Modifier.size(64.dp),contentPadding=PaddingValues(0.dp)){
      Text(if(value.contains(n))"●" else "○")
     }
    }
   }
  }
  TextButton(onClick={onChange("")}){Text("پاک‌کردن الگو")}
 }
}
@Composable fun CredentialSetup(method:String,access:SecureAccess,onClose:()->Unit,onSaved:()->Unit) {
 var step by remember(method){mutableIntStateOf(0)}
 var first by remember(method){mutableStateOf("")}
 var value by remember(method){mutableStateOf("")}
 val pin=method=="pin"
 val good=if(pin)Regex("[0-9]{4,6}").matches(value) else Regex("[0-8]{4,9}").matches(value)&&value.toSet().size==value.length
 AlertDialog(onDismissRequest=onClose,title={Text(if(step==0)"تعریف "+(if(pin)"رمز عبور" else "الگو") else "تأیید مجدد")},
 text={Column{
  if(pin)OutlinedTextField(value,{value=it.filter{c->c.isDigit()}.take(6)},label={Text("رمز ۴ تا ۶ رقمی")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword),singleLine=true)
  else {Text("حداقل ۴ نقطه متفاوت را به ترتیب لمس کنید");PatternPad(value){value=it}}
 }},
 confirmButton={TextButton(enabled=good,onClick={
  if(step==0){first=value;value="";step=1}
  else if(value==first){if(pin)access.setPin(value) else access.setPattern(value);onSaved()}
  else {step=0;first="";value=""}
 }){Text(if(step==0)"ادامه" else "ذخیره")}},
 dismissButton={TextButton(onClick=onClose){Text("انصراف")}})
}
@Composable fun LockedScreen(access:SecureAccess,onOpen:()->Unit,onSystem:(Boolean)->Unit) {
 var mode by remember{mutableStateOf(if(access.hasPin())"pin" else "pattern")}
 var value by remember{mutableStateOf("")}
 var error by remember{mutableStateOf("")}
 MaterialTheme{
  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){
   Text("Ani Note",style=MaterialTheme.typography.headlineMedium)
   Text("برای دسترسی به اطلاعات، هویت خود را تأیید کنید")
   Spacer(Modifier.height(18.dp))
   if(access.hasPin())TextButton(onClick={mode="pin";value=""}){Text("ورود با رمز")}
   if(access.hasPattern())TextButton(onClick={mode="pattern";value=""}){Text("ورود با الگوی اختصاصی")}
   if(mode=="pin"&&access.hasPin())OutlinedTextField(value,{value=it.filter{c->c.isDigit()}.take(6)},label={Text("رمز عبور")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword),singleLine=true)
   if(mode=="pattern"&&access.hasPattern())PatternPad(value){value=it}
   if(access.hasPin()||access.hasPattern())Button(onClick={
    if(access.check(mode,value))onOpen() else {error="رمز یا الگو صحیح نیست";value=""}
   }){Text("ورود")}
   if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
   if(access.biometric)OutlinedButton(onClick={onSystem(false)}){Text("ورود با اثر انگشت")}
   if(access.deviceCredential)OutlinedButton(onClick={onSystem(true)}){Text("ورود با قفل گوشی")}
  }
 }
}
@Composable fun SecureSettings(access:SecureAccess,onLock:()->Unit) {
 var active by remember{mutableStateOf(access.enabled)}
 var pin by remember{mutableStateOf(access.hasPin())}
 var pattern by remember{mutableStateOf(access.hasPattern())}
 var bio by remember{mutableStateOf(access.biometric)}
 var device by remember{mutableStateOf(access.deviceCredential)}
 var setting by remember{mutableStateOf("")}
 val context=androidx.compose.ui.platform.LocalContext.current
 ElevatedCard {
  Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Text("ورود امن به برنامه",style=MaterialTheme.typography.titleMedium)
   Row(verticalAlignment=Alignment.CenterVertically){
    Text("فعال‌سازی ورود امن",Modifier.weight(1f))
    Switch(checked=active,onCheckedChange={v->
     if(!v){access.enabled=false;active=false}
     else if(access.configured()){access.enabled=true;active=true}
     else setting="pin"
    })
   }
   OutlinedButton(onClick={setting="pin"},modifier=Modifier.fillMaxWidth()){Text(if(pin)"تغییر رمز ۴ تا ۶ رقمی" else "تعریف رمز ۴ تا ۶ رقمی")}
   OutlinedButton(onClick={setting="pattern"},modifier=Modifier.fillMaxWidth()){Text(if(pattern)"تغییر الگوی اختصاصی" else "تعریف الگوی اختصاصی")}
   Row(verticalAlignment=Alignment.CenterVertically){
    Text("اثر انگشت گوشی",Modifier.weight(1f))
    Switch(checked=bio,onCheckedChange={v->
     if(!v){bio=false;access.biometric=false}
     else if(androidx.biometric.BiometricManager.from(context).canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)==androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS){bio=true;access.biometric=true}
     else android.widget.Toast.makeText(context,"اثر انگشت امن روی گوشی تنظیم نشده",1).show()
    })
   }
   Row(verticalAlignment=Alignment.CenterVertically){
    Text("الگو یا رمز پیش‌فرض گوشی",Modifier.weight(1f))
    Switch(checked=device,onCheckedChange={v->
     if(!v){device=false;access.deviceCredential=false}
     else if(androidx.biometric.BiometricManager.from(context).canAuthenticate(androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)==androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS){device=true;access.deviceCredential=true}
     else android.widget.Toast.makeText(context,"قفل پیش‌فرض گوشی فعال نیست",1).show()
    })
   }
   if(active)OutlinedButton(onClick=onLock){Text("قفل‌کردن برنامه")}
   Text("برای فعال‌سازی حداقل یک روش ورود لازم است. برای ورود هر روش فعال کافی است.")
  }
 }
 if(setting.isNotBlank())CredentialSetup(setting,access,onClose={setting=""},onSaved={
  pin=access.hasPin();pattern=access.hasPattern()
  access.enabled=true;active=true;setting=""
 })
}
