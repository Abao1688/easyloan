# Keep the JS interface methods
-keepclassmembers class com.easyloan.app.LocationInterface {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep WebView
-keepclassmembers class * extends android.webkit.WebView {
    *** addJavascriptInterface(***);
}
