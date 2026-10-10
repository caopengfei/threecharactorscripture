# 三字经

# kotlinx.serialization：R8 full mode 下 Json 反射查找 Companion.serializer() 需要保留
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.rakor.sanzijing.data.content.** {
    *** Companion;
}
-keepclasseswithmembers class com.rakor.sanzijing.data.content.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.rakor.sanzijing.data.content.**$$serializer { *; }
