package com.anishop.aninote

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/** Group only the whole part visually; the editable value and decimal point stay intact. */
internal object AmountVisualTransformation:VisualTransformation {
 override fun filter(text:AnnotatedString):TransformedText {
  val raw=text.text
  if(!Regex("[0-9۰-۹٠-٩]*(?:[.٫][0-9۰-۹٠-٩]*)?").matches(raw))
   return TransformedText(text,OffsetMapping.Identity)
  val wholeEnd=raw.indexOfFirst{it=='.'||it=='٫'}.let{if(it<0)raw.length else it}
  val display=StringBuilder()
  val originalOffsets=IntArray(raw.length+1)
  val displayOffsets=mutableListOf(0)
  raw.forEachIndexed { i,c ->
   if(i>0 && i<wholeEnd && (wholeEnd-i)%3==0) {
    display.append(',');displayOffsets.add(i)
   }
   originalOffsets[i]=display.length
   display.append(c);displayOffsets.add(i+1)
  }
  originalOffsets[raw.length]=display.length
  val mapping=object:OffsetMapping {
   override fun originalToTransformed(offset:Int)=originalOffsets[offset.coerceIn(0,raw.length)]
   override fun transformedToOriginal(offset:Int)=displayOffsets[offset.coerceIn(0,display.length)]
  }
  return TransformedText(AnnotatedString(display.toString()),mapping)
 }
}
