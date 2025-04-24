# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# preserve the line number information for debugging stack traces.
-printmapping out.map
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable,*Annotation*,javadoc
-keepattributes Signature,Exceptions,InnerClasses,EnclosingMethod

# Keep public interfaces
-keepparameternames
#-keep public class com.dsh.openai.home.** { public *; }

-keep class com.dsh.openai.home.** { *; }
-keepnames class com.dsh.openai.home.** { *; }
-keep class com.dsh.openai.home.internal.** { *; }

-keep class com.dsh.openai.home.model.message.** { *; }
-keepnames class com.dsh.openai.home.model.message.** { *; }

-keepparameternames
-keep interface com.dsh.openai.home.InferenceListener { *;}
-keepclasseswithmembers interface com.dsh.openai.home.InferenceListener {
    *;
}

-keepparameternames
-keep interface com.dsh.openai.home.InferenceEngine {*;}
-keepclasseswithmembers interface com.dsh.openai.home.InferenceEngine {
    *;
}