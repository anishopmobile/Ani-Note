package com.anishop.aninote

import android.content.Context
import android.os.SystemClock
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Application-level UI lock; it does not encrypt the legacy on-disk notes. */
class SecureAccess(context:Context){
 private val p=context.getSharedPreferences("secure_access",Context.MODE_PRIVATE)
 var enabled:Boolean
  get()=p.getBoolean("enabled",false)
  set(value){p.edit().putBoolean("enabled",value).commit()}
 fun hasPin()=p.contains("pin_hash")
 fun hasPattern()=p.contains("pattern_hash")
 var biometric:Boolean
  get()=p.getBoolean("biometric",false)
  set(value){p.edit().putBoolean("biometric",value).commit()}
 var deviceCredential:Boolean
  get()=p.getBoolean("device_credential",false)
  set(value){p.edit().putBoolean("device_credential",value).commit()}
 fun configured()=hasPin()||hasPattern()||biometric||deviceCredential
 fun setPin(v:String){
  require(Regex("[0-9]{4,6}").matches(v))
  save("pin",v)
 }
 fun setPattern(v:String){
  require(Regex("[0-8]{4,9}").matches(v)&&v.toSet().size==v.length)
  save("pattern",v)
 }
 private fun save(key:String,v:String){
  val salt=ByteArray(16).also{SecureRandom().nextBytes(it)}
  val hash=derive(v,salt)
  p.edit().putString(key+"_salt",Base64.encodeToString(salt,Base64.NO_WRAP))
   .putString(key+"_hash",Base64.encodeToString(hash,Base64.NO_WRAP)).commit()
 }
 private fun derive(v:String,salt:ByteArray):ByteArray{
  val spec=PBEKeySpec(v.toCharArray(),salt,150000,256)
  return try{SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded}
  finally{spec.clearPassword()}
 }
 private var attempts=0
 private var blockedUntil=0L
 fun check(key:String,v:String):Boolean {
  if(SystemClock.elapsedRealtime()<blockedUntil)return false
  val stored=p.getString(key+"_hash",null)?:return false
  val salt=p.getString(key+"_salt",null)?:return false
  val ok=MessageDigest.isEqual(derive(v,Base64.decode(salt,Base64.DEFAULT)),Base64.decode(stored,Base64.DEFAULT))
  if(ok){attempts=0;blockedUntil=0}else{
   attempts++
   if(attempts>=5){blockedUntil=SystemClock.elapsedRealtime()+30000L;attempts=0}
  }
  return ok
 }
 fun clearPin(){p.edit().remove("pin_hash").remove("pin_salt").commit()}
 fun clearPattern(){p.edit().remove("pattern_hash").remove("pattern_salt").commit()}
}
