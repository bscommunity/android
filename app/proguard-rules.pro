# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# SERIALIZATION

# Add these rules to your proguard-rules.pro file

# Keep Kotlin Serialization metadata
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep Serializable classes and their members
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep your serializable classes
-keep,includedescriptorclasses class com.meninocoiso.beatstarcommunity.presentation.navigation.** {
    *;
}
-keep,includedescriptorclasses class com.meninocoiso.beatstarcommunity.domain.enums.** {
    *;
}

# Keep generic signature of Call, Response classes and their extensions
-keepattributes Signature

# Keep serializers for your classes
-keepclassmembers class com.meninocoiso.beatstarcommunity.** {
    *** Companion;
}
-keepclasseswithmembers class com.meninocoiso.beatstarcommunity.** {
    kotlinx.serialization.KSerializer serializer(...);
}