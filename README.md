# TLabWebViewPlugin
Source code of java plugin used in [```TLabWebView```](https://github.com/TLabAltoh/TLabWebView) (3D web browser / 3D WebView Unity plugin)

## Operating Environment
```
Android Studio Version:
Android Studio Koala | 2024.1.1
Build #AI-241.15989.150.2411.11948838, built on June 11, 2024
Runtime version: 17.0.10+0--11609105 amd64
VM: OpenJDK 64-Bit Server VM by JetBrains s.r.o.
Windows 10.0
GC: G1 Young Generation, G1 Old Generation
Memory: 4096M
Cores: 24
Registry:
  debugger.new.tool.window.layout=true
  ide.experimental.ui=true
Non-Bundled Plugins:
  OpenGL-Plugin (1.1.5)
  GLSL (1.24)

OS: Windows 10  
```

> [!WARNING]
> The latest version of Android Studio (`2024.3.1.14 Meerkat`) requires an update to `Gradle 8.*`, and plug-ins built with the latest version of Android Studio may not be able to be embedded in the current version of Unity (This project uses `Gradle 7.4`). Therefore, it is recommended to build with the Android Studio version specified in the Operating Environment (`2024.1.1 Koala`).

## Current Issue
### Did not find frame
- The following error occurs when using [```lockHardwareCanvas```](https://developer.android.com/reference/android/view/SurfaceHolder#lockHardwareCanvas()) in unity 2021
```
2023/11/13 15:40:53.051 13492 13511 Error FrameEvents updateAcquireFence: Did not find frame.
```
Corresponding part of the code
```java
// ViewToGLRenderer.java
public Canvas onDrawViewBegin() {
    m_surfaceCanvas = null;
    if (m_surface != null) {
        try {
            //mSurfaceCanvas = mSurface.lockCanvas(null);
            // https://learn.microsoft.com/en-us/dotnet/api/android.views.surface.lockhardwarecanvas?view=xamarin-android-sdk-13
            m_surfaceCanvas = mSurface.lockHardwareCanvas();
        }catch (Exception e){
            Log.e(TAG, "error while rendering view to gl: " + e);
        }
    }
    return m_surfaceCanvas;
}
```

## Link
- [OpenGL Texture to HardwareBuffer](https://github.com/keith2018/SharedTexture)
- [WebView to ByteBuffer](https://bitbucket.org/HoshiyamaTakaaki/pixelreadstest/src/master/)
