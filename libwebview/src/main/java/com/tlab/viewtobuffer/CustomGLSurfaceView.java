package com.tlab.viewtobuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

import android.util.AttributeSet;
import android.content.Context;
import android.opengl.EGL14;
import android.opengl.EGL15;
import android.opengl.EGLExt;
import android.opengl.GLSurfaceView;

// An attempt to share EGLContext and update textures from a Frame Buffer Object.

public class CustomGLSurfaceView extends GLSurfaceView {

    private static final String TAG = "libwebview";

    /**
     * @return
     */
    private int[] getContextAttributes() {
        // https://developer.android.com/reference/android/opengl/EGL14#EGL_CONTEXT_CLIENT_VERSION
        return new int[]{
                EGL14.EGL_CONTEXT_CLIENT_VERSION /* 0x3098 */, 3,
                EGL15.EGL_CONTEXT_MAJOR_VERSION, 3,
                EGL15.EGL_CONTEXT_MINOR_VERSION, 2,
                EGLExt.EGL_CONTEXT_MAJOR_VERSION_KHR, 3,
                EGLExt.EGL_CONTEXT_MINOR_VERSION_KHR, 2,
                EGL10.EGL_NONE};
    }

    private EGLContextFactory mEGLContextFactory = new EGLContextFactory() {
        /**
         *
         * @param egl
         * @param display
         * @param context
         */
        @Override
        public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
        }

        /**
         *
         * @param egl
         * @param display
         * @param config
         * @return
         */
        @Override
        public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig config) {
            // Here, I wanted to set Unity's EGLContext as a shared context so that Texture2D could be updated with a pointer.
            // This worked well on some devices and not on others.
            // EGL_BAD_MATCH (Oculus quest 2)

            EGLContext context = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, getContextAttributes());

            return context;
        }
    };

    /**
     * @param renderer the renderer to use to perform OpenGL drawing.
     */
    @Override
    public void setRenderer(Renderer renderer) {
        setEGLContextFactory(mEGLContextFactory);
        super.setRenderer(renderer);
    }

    /**
     * @param context
     */
    public CustomGLSurfaceView(Context context) {
        super(context);
    }

    /**
     * @param context
     * @param attrs
     */
    public CustomGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
}
