#pragma once

#include "RenderAPI.h"

// OpenGL Core profile (desktop) or OpenGL ES (mobile) implementation of RenderAPI.
// Supports several flavors: Core, ES2, ES3

#include <string.h>
#include <memory>
#include <map>
#include <mutex>
#include <vector>
#include <math.h>
#include <unistd.h>

#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <GLES/gl.h>
#include <GLES/glext.h>

#if SUPPORT_OPENGL_UNIFIED

#define UNITY_ANDROID 1

#include <assert.h>

#if UNITY_ANDROID

#	include <GLES2/gl2.h>

#elif UNITY_LINUX
#	define GL_GLEXT_PROTOTYPES
#	include <GL/gl.h>
#elif UNITY_EMBEDDED_LINUX
#	include <GLES2/gl2.h>
#if SUPPORT_OPENGL_CORE
#	define GL_GLEXT_PROTOTYPES
#	include <GL/gl.h>
#endif
#else
#	error Unknown platform
#endif

namespace tlab {

    struct GLESHWBImage {
        GLuint image = 0;
        AHardwareBuffer *hwBuffer = nullptr;
        EGLImageKHR eglImage = EGL_NO_IMAGE_KHR;
    };

    class RenderAPI_OpenGLCoreES : public RenderAPI {
    public:
        RenderAPI_OpenGLCoreES(UnityGfxRenderer apiType);

        virtual ~RenderAPI_OpenGLCoreES() {}

        virtual void ProcessDeviceEvent(UnityGfxDeviceEventType type, IUnityInterfaces *interfaces);

        virtual long
        RegistHWBufferConnectedTexture(uint32_t width, uint32_t height, AHardwareBuffer *hwBuffer);

        virtual void UnRegistHWBufferConnectedTexture(long platformTexID);

        virtual void DownloadHardwareBuffer(long platformTexID);

        virtual long GetPlatformNativeTexture(long unityTexID);

        virtual void UpdateUnityTexture(long unityPlatformTexID, long platformTexID);

    private:
        static bool AVAILABLE;

        static const char *getEGLError();

        static int CreateEGLFence();

        static bool WaitEGLFence(int fenceFd);

        static bool CreateHWBufferConnectedGLESImage(uint32_t width, uint32_t height,
                                                     AHardwareBuffer *hwBuffer,
                                                     GLESHWBImage *hwbImage);

        static void ImmediateDestroyGLESHWBImage(GLESHWBImage &hwbImage);

    private:
        UnityGfxRenderer m_APIType;
        std::map<std::pair<unsigned long long, std::__thread_id>, GLESHWBImage> m_GLESImageMap;
        std::mutex m_mutex;

        void GarbageCollect(bool force);
    };
}

#endif