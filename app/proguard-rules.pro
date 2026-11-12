-dontwarn android.support.**
-dontwarn androidx.**
-keepattributes *Annotation*
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
