package com.anishop.aninote

import java.math.BigDecimal

/** Dot/Arabic decimal separator is always decimal; comma/Arabic grouping is thousands. */
internal fun parseMoney(input:String):BigDecimal? {
 if(input.length>80)return null
 val normalized=input.trim().map { c -> when(c) {
  in '۰'..'۹' -> '0'+(c-'۰')
  in '٠'..'٩' -> '0'+(c-'٠')
  '٫' -> '.'
  '٬' -> ','
  else -> c
 } }.joinToString("")
 if(!Regex("(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+)(?:\\.[0-9]{0,18})?|\\.[0-9]{1,18}").matches(normalized))return null
 val plain=normalized.replace(",","")
 if(plain.substringBefore('.').trimStart('0').length>19)return null
 return plain.toBigDecimalOrNull()?.stripTrailingZeros()
}

/** Read old JSON integer numbers and new exact decimal strings, without Long/Double conversion. */
internal fun storedMoney(value:String):BigDecimal {
 require(value.length<=80 && Regex("-?[0-9]{1,19}(?:\\.[0-9]{1,18})?").matches(value))
 return value.toBigDecimal().stripTrailingZeros()
}
internal fun editableMoney(value:BigDecimal):String = value.abs().stripTrailingZeros().toPlainString()
fun money(value:BigDecimal):String {
 val plain=value.abs().stripTrailingZeros().toPlainString()
 val whole=plain.substringBefore('.').reversed().chunked(3).joinToString(",").reversed()
 val fraction=if('.' in plain)"."+plain.substringAfter('.') else ""
 return (if(value.signum()<0)"-" else "")+whole+fraction+" تومان"
}
