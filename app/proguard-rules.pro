# JARVIS ProGuard rules
# Keep Room, Hilt, Gson, SQLCipher and Compose entry points.
-keep class com.jarvis.** { *; }
-keep class androidx.room.** { *; }
-keep class net.sqlcipher.** { *; }
-keep class com.google.gson.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes *Annotation*
-dontwarn org.slf4j.impl.StaticLoggerBinder
