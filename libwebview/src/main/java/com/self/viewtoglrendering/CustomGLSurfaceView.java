package com.self.viewtoglrendering;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import android.util.AttributeSet;
import android.content.Context;
import android.opengl.EGL15;
import android.opengl.GLSurfaceView;

public class CustomGLSurfaceView extends GLSurfaceView {

    private EGLContext mContext = EGL10.EGL_NO_CONTEXT;

    private int[] getContextAttributes() { return new int[] { 0x3098 /* 0x3098 EGL_CONTEXT_CLIENT_VERSION */, EGL15.EGL_OPENGL_ES3_BIT /* EGL_OPENGL_ES3_BIT */, EGL10.EGL_NONE }; }

    private EGLContextFactory mEGLContextFactory = new EGLContextFactory() {

        @Override
        public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) { }

        @Override
        public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig config) {
            EGLContext context = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, getContextAttributes());
            return mContext;
        }
    };

    @Override
    public void setRenderer(Renderer renderer) {
        setEGLContextFactory(mEGLContextFactory);
        super.setRenderer(renderer);
    }

    public void setContext(EGLContext context){
        mContext = context;
    }

    public CustomGLSurfaceView(Context context) { super(context); }

    public CustomGLSurfaceView(Context context, AttributeSet attrs) { super(context, attrs); }
}
