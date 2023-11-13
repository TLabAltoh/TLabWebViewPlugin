# TLabWebViewPlugin
Source code of java plugin used in TLabWebView

## Operating Environment
Android Studio Version:
 Android Studio Giraffe | 2022.3.1 Patch 2
 Build #AI-223.8836.35.2231.10811636, built on September 15, 2023
 Runtime version: 17.0.6+0-b2043.56-10027231 amd64
 VM: OpenJDK 64-Bit Server VM by JetBrains s.r.o.
 Windows 10 10.0
 GC: G1 Young Generation, G1 Old Generation
 Memory: 1280M
 Cores: 24
 Registry:
 external.system.auto.import.disabled=true
 ide.text.editor.with.preview.show.floating.toolbar=false

Non-Bundled Plugins:
com.google.idea.bazel.aswb (2023.10.10-aswb.0.1-api-version-223)

OS: Windows 10  

## Getting Started
### Build
Build and use with .aar

## Current Issue
### Did not find frame
- The following error occurs when using lockhardwarecanvas
```
2023/11/13 15:40:53.051 13492 13511 Error FrameEvents updateAcquireFence: Did not find frame.
```
Corresponding part of the code
```java
// ViewToGLRenderer.java
public Canvas onDrawViewBegin() {
    mSurfaceCanvas = null;
    if (mSurface != null) {
        try {
            //mSurfaceCanvas = mSurface.lockCanvas(null);
            // https://learn.microsoft.com/en-us/dotnet/api/android.views.surface.lockhardwarecanvas?view=xamarin-android-sdk-13
            mSurfaceCanvas = mSurface.lockHardwareCanvas();
        }catch (Exception e){
            Log.e(TAG, "error while rendering view to gl: " + e);
        }
    }
    return mSurfaceCanvas;
}
```
- [Issues that may be relevant](https://github.com/flutter/flutter/issues/104268)

## Link
- [UnityAsset using this plugin](https://github.com/TLabAltoh/TLabWebView)  
- [Reference repositorie](https://bitbucket.org/HoshiyamaTakaaki/pixelreadstest/src/master/)
