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

# ONNX Runtime's native code creates and calls its Java classes by name (OnnxTensor,
# OrtSession, OrtException, ...), so R8 must not rename or remove any of them
-keep class ai.onnxruntime.** { *; }

# ML Kit finds its component registrars by class name and creates them by reflection;
# R8 full mode otherwise drops their no-argument constructors
-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }
