# Keep data classes
-keep class com.fontlens.data.** { *; }

# Critical: preserve generic type signatures Gson needs at runtime
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Gson internals
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Gson serializers/deserializers
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Apache PDFBox (fontbox) - reflection-heavy
-keep class org.apache.fontbox.** { *; }
-keep class org.apache.pdfbox.** { *; }
-dontwarn org.apache.fontbox.**
-dontwarn org.apache.pdfbox.**

# Navigation SafeArgs generated classes
-keep class com.fontlens.ui.**Args { *; }
-keep class com.fontlens.ui.**Directions { *; }

# Keep enum values (commonly broken by R8)
-keepclassmembers enum * { *; }