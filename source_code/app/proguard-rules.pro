# ClubSMS App - ProGuard Configuration
# 
# ProGuard rules for release builds to optimize and obfuscate the code
# while maintaining functionality for SMS broadcasting application.
# 
# This configuration:
# - Removes unused code to reduce APK size
# - Obfuscates class and method names for security
# - Preserves essential Android and library components
# - Maintains crash reporting capability
# 
# @author John (Electrical Engineer)
# @version 1.0
# @since 2024-12-24

# ==========================================
# Android Framework and AndroidX Libraries
# ==========================================

# Keep Android framework classes
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class com.android.vending.licensing.ILicensingService

# AndroidX and Support Library
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# Material Design Components
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# ==========================================
# App-Specific Classes to Preserve
# ==========================================

# Keep main application classes
-keep class com.clubsms.MainActivity { *; }
-keep class com.clubsms.ClubContactManager { *; }
-keep class com.clubsms.MessageHistoryManager { *; }

# Preserve data model classes for JSON serialization
-keep class com.clubsms.ClubMember { *; }
-keep class com.clubsms.MessageRecord { *; }
-keep class com.clubsms.MessageHistoryManager$SendingStatistics { *; }
-keep class com.clubsms.ClubContactManager$ContactStats { *; }

# Keep enums used in data models
-keepclassmembers enum com.clubsms.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Preserve SMS-related broadcast receivers
-keep class com.clubsms.SmsStatusReceiver { *; }
-keep class com.clubsms.NotificationClickReceiver { *; }

# Keep service classes for background operations
-keep class com.clubsms.SmsBroadcastService { *; }

# ==========================================
# JSON Serialization (Gson)
# ==========================================

# Gson uses generic type information stored in a class file when working with fields
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Application classes that will be serialized/deserialized over Gson
-keep class com.clubsms.ClubMember { <fields>; }
-keep class com.clubsms.MessageRecord { <fields>; }
-keep class com.clubsms.MessageHistoryManager$SendingStatistics { <fields>; }
-keep class com.clubsms.ClubContactManager$ContactStats { <fields>; }

# ==========================================
# Reflection and Dynamic Access
# ==========================================

# Keep classes accessed via reflection
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep native method names
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep constructors for classes with custom constructors
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep custom View classes and their constructors
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
    *** get*();
}

# ==========================================
# Third-Party Libraries
# ==========================================

# OkHttp (if used in future versions)
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Retrofit (if used in future versions)
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Glide image loading library
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# Timber logging library
-keep class timber.log.** { *; }
-dontwarn timber.log.**

# CSV processing library
-keep class com.opencsv.** { *; }
-dontwarn com.opencsv.**

# ==========================================
# SMS and Telephony Framework
# ==========================================

# Keep SMS manager and telephony classes
-keep class android.telephony.SmsManager { *; }
-keep class android.telephony.SmsMessage { *; }
-keep class android.telephony.TelephonyManager { *; }

# Keep permission-related classes
-keep class android.permission.** { *; }

# ==========================================
# Debugging and Crash Reporting
# ==========================================

# Keep source file names and line numbers for crash reports
-keepattributes SourceFile,LineNumberTable

# Keep custom exceptions for better crash reporting
-keep public class * extends java.lang.Exception

# If using crash reporting, keep these
-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**

# ==========================================
# Optimization Settings
# ==========================================

# Enable more aggressive optimizations
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Remove logging calls in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Remove debug-only code
-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

# ==========================================
# Resource Shrinking Configuration
# ==========================================

# Keep resources referenced from XML
-keep class **.R
-keep class **.R$* {
    <fields>;
}

# Keep resources for runtime access
-keepclassmembers class * {
    @android.support.annotation.Keep *;
}

# ==========================================
# Final Settings
# ==========================================

# Don't warn about missing classes that are not used
-dontwarn android.support.**
-dontwarn org.apache.http.**
-dontwarn android.net.http.AndroidHttpClient
-dontwarn com.google.android.gms.**
-dontwarn java.lang.invoke.**

# Print mapping for crash report deobfuscation
-printmapping mapping.txt

# Print usage for optimization verification
-printusage usage.txt

# Print seeds for keeping verification  
-printseeds seeds.txt