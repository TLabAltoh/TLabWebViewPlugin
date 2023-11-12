package com.self.viewtoglrendering;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

import android.util.AttributeSet;
import android.content.Context;
import android.opengl.EGL14;
import android.opengl.EGL15;
import android.opengl.GLSurfaceView;

public class CustomGLSurfaceView extends GLSurfaceView {

    private EGLContext mSharedEGLContext = EGL10.EGL_NO_CONTEXT;

    // https://developer.android.com/reference/android/opengl/EGL14#EGL_CONTEXT_CLIENT_VERSION

    private int[] getContextAttributes() { return new int[] {
            EGL14.EGL_CONTEXT_CLIENT_VERSION /* 0x3098 EGL_CONTEXT_CLIENT_VERSION */,
            3 /* EGL15.EGL_OPENGL_ES3_BIT */,
            EGL10.EGL_NONE };
    }

    private EGLContextFactory mEGLContextFactory = new EGLContextFactory() {

        @Override
        public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) { }

        @Override
        public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig config) {
            EGLContext context = egl.eglCreateContext(display, config, mSharedEGLContext, getContextAttributes());
            return context;
        }
    };

    @Override
    public void setRenderer(Renderer renderer) {
        setEGLContextFactory(mEGLContextFactory);
        super.setRenderer(renderer);
    }

    public void setSharedContext(EGLContext sharedEGLContext){ mSharedEGLContext = sharedEGLContext; }

    public CustomGLSurfaceView(Context context) { super(context); }

    public CustomGLSurfaceView(Context context, AttributeSet attrs) { super(context, attrs); }
}
