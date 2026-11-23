# Project specific ProGuard / R8 release rules for Call for Growth
-keepattributes *Annotation*
-keep class com.example.data.** { *; }
-keepclassmembers class com.example.data.** { *; }
-dontwarn java.lang.instrument.**
