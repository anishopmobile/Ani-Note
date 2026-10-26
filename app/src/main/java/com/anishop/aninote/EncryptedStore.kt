package com.anishop.aninote

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** AES-256-GCM using an Android Keystore key that never appears in backup JSON. */
class EncryptedStore(context:Context){
 private val prefs=context.getSharedPreferences("data",Context.MODE_PRIVATE)
 private val alias="ani-note-local-data-v1"
 private fun key():SecretKey{
  val ks=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
  (ks.getKey(alias,null) as? SecretKey)?.let{return it}
  return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply{
   init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
    .setKeySize(256).build())
  }.generateKey()
 }
 fun read(name:String):String{
  val stored=prefs.getString(name,"[]") ?: "[]"
  if(!stored.startsWith("gcm:")){
   // Migrate pre-encryption installs without losing their existing records.
   write(name,stored)
   return stored
  }
  val bytes=Base64.decode(stored.removePrefix("gcm:"),Base64.NO_WRAP)
  require(bytes.size>=29)
  val cipher=Cipher.getInstance("AES/GCM/NoPadding")
  cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,bytes.copyOfRange(0,12)))
  return cipher.doFinal(bytes.copyOfRange(12,bytes.size)).toString(Charsets.UTF_8)
 }
 fun write(name:String,plain:String){
  val cipher=Cipher.getInstance("AES/GCM/NoPadding")
  cipher.init(Cipher.ENCRYPT_MODE,key())
  val bytes=cipher.iv+cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
  check(prefs.edit().putString(name,"gcm:"+Base64.encodeToString(bytes,Base64.NO_WRAP)).commit())
 }
}
