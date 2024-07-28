#include "RenderAPI_OpenGLCoreES.h"
#include "JniLog.h"

#if SUPPORT_OPENGL_UNIFIED

namespace tlab {

    namespace glext {

        PFNEGLGETNATIVECLIENTBUFFERANDROIDPROC eglGetNativeClientBufferANDROID = nullptr;
        PFNGLEGLIMAGETARGETTEXTURE2DOESPROC glEGLImageTargetTexture2DOES = nullptr;
        PFNEGLCREATEIMAGEKHRPROC eglCreateImageKHR = nullptr;
        PFNEGLDESTROYIMAGEKHRPROC eglDestroyImageKHR = nullptr;
        PFNEGLCREATESYNCKHRPROC eglCreateSyncKHR = nullptr;
        PFNEGLDESTROYSYNCKHRPROC eglDestroySyncKHR = nullptr;
        PFNEGLWAITSYNCKHRPROC eglWaitSyncKHR = nullptr;
        PFNEGLDUPNATIVEFENCEFDANDROIDPROC eglDupNativeFenceFDANDROID = nullptr;

    }

    std::once_flag glProcOnceFlag;

    static bool initGLExtProc() noexcept {
        std::call_once(glProcOnceFlag, []() {
            glext::eglGetNativeClientBufferANDROID = (PFNEGLGETNATIVECLIENTBUFFERANDROIDPROC) eglGetProcAddress(
                    "eglGetNativeClientBufferANDROID");
            glext::glEGLImageTargetTexture2DOES = (PFNGLEGLIMAGETARGETTEXTURE2DOESPROC) eglGetProcAddress(
                    "glEGLImageTargetTexture2DOES");
            glext::eglCreateImageKHR = (PFNEGLCREATEIMAGEKHRPROC) eglGetProcAddress(
                    "eglCreateImageKHR");
            glext::eglDestroyImageKHR = (PFNEGLDESTROYIMAGEKHRPROC) eglGetProcAddress(
                    "eglDestroyImageKHR");
            glext::eglCreateSyncKHR = (PFNEGLCREATESYNCKHRPROC) eglGetProcAddress(
                    "eglCreateSyncKHR");
            glext::eglDestroySyncKHR = (PFNEGLDESTROYSYNCKHRPROC) eglGetProcAddress(
                    "eglDestroySyncKHR");
            glext::eglWaitSyncKHR = (PFNEGLWAITSYNCKHRPROC) eglGetProcAddress("eglWaitSyncKHR");
            glext::eglDupNativeFenceFDANDROID = (PFNEGLDUPNATIVEFENCEFDANDROIDPROC) eglGetProcAddress(
                    "eglDupNativeFenceFDANDROID");
        });
        return glext::eglGetNativeClientBufferANDROID
               && glext::glEGLImageTargetTexture2DOES
               && glext::eglCreateImageKHR
               && glext::eglDestroyImageKHR
               && glext::eglCreateSyncKHR
               && glext::eglDestroySyncKHR
               && glext::eglWaitSyncKHR
               && glext::eglDupNativeFenceFDANDROID;
    }

    RenderAPI *
    CreateRenderAPI_OpenGLCoreES(UnityGfxRenderer apiType) {
        return new RenderAPI_OpenGLCoreES(apiType);
    }

    RenderAPI_OpenGLCoreES::RenderAPI_OpenGLCoreES(UnityGfxRenderer apiType)
            : m_APIType(apiType) {
        DEVLOGD("[sharedtex-jni] [RenderAPI_OpenGLCoreES]");
    }

    void
    RenderAPI_OpenGLCoreES::ProcessDeviceEvent(UnityGfxDeviceEventType type,
                                               IUnityInterfaces *interfaces) {
        switch (type) {
            case kUnityGfxDeviceEventInitialize:

                break;
            case kUnityGfxDeviceEventShutdown:

                GarbageCollect(true);

                DEVLOGD("[sharedtex-jni] shutdown render api");

                break;
            case kUnityGfxDeviceEventBeforeReset:
                break;
            case kUnityGfxDeviceEventAfterReset:
                break;
        }
    }

    long
    RenderAPI_OpenGLCoreES::RegistHWBufferConnectedTexture(uint32_t width, uint32_t height,
                                                           AHardwareBuffer *hwBuffer) {
        m_mutex.lock();

        auto *hwbImage = new GLESHWBImage();

        CreateHWBufferConnectedGLESImage(width, height, hwBuffer, hwbImage);

        long platformTexID = (long) (hwbImage->image);

        m_GLESImageMap.insert(
                std::make_pair(std::make_pair(platformTexID, std::this_thread::get_id()),
                               *hwbImage));

        DEVLOGD("[sharedtex-jni] regist platform texture %ld", platformTexID);

        m_mutex.unlock();

        return platformTexID;
    }

    void
    RenderAPI_OpenGLCoreES::UnRegistHWBufferConnectedTexture(long platformTexID) {
        m_mutex.lock();

        if (m_GLESImageMap.find(std::make_pair(platformTexID, std::this_thread::get_id())) !=
            m_GLESImageMap.end()) {
            GLESHWBImage image = m_GLESImageMap[std::make_pair(platformTexID,
                                                               std::this_thread::get_id())];
            ImmediateDestroyGLESHWBImage(image);
            m_GLESImageMap.erase(std::make_pair(platformTexID, std::this_thread::get_id()));

            DEVLOGD("[sharedtex-jni] delete current platform texture");
        }

        m_mutex.unlock();
    }

    bool
            RenderAPI_OpenGLCoreES::AVAILABLE = initGLExtProc();

    const char *
    RenderAPI_OpenGLCoreES::getEGLError() {
        switch (eglGetError()) {
            case EGL_SUCCESS:
                return "EGL_SUCCESS";
            case EGL_NOT_INITIALIZED:
                return "EGL_NOT_INITIALIZED";
            case EGL_BAD_ACCESS:
                return "EGL_BAD_ACCESS";
            case EGL_BAD_ALLOC:
                return "EGL_BAD_ALLOC";
            case EGL_BAD_ATTRIBUTE:
                return "EGL_BAD_ATTRIBUTE";
            case EGL_BAD_CONTEXT:
                return "EGL_BAD_CONTEXT";
            case EGL_BAD_CONFIG:
                return "EGL_BAD_CONFIG";
            case EGL_BAD_CURRENT_SURFACE:
                return "EGL_BAD_CURRENT_SURFACE";
            case EGL_BAD_DISPLAY:
                return "EGL_BAD_DISPLAY";
            case EGL_BAD_SURFACE:
                return "EGL_BAD_SURFACE";
            case EGL_BAD_MATCH:
                return "EGL_BAD_MATCH";
            case EGL_BAD_PARAMETER:
                return "EGL_BAD_PARAMETER";
            case EGL_BAD_NATIVE_PIXMAP:
                return "EGL_BAD_NATIVE_PIXMAP";
            case EGL_BAD_NATIVE_WINDOW:
                return "EGL_BAD_NATIVE_WINDOW";
            case EGL_CONTEXT_LOST:
                return "EGL_CONTEXT_LOST";
            default:
                return "Unknown error";
        }
    }

    int
    RenderAPI_OpenGLCoreES::CreateEGLFence() {
        if (!AVAILABLE) {
            LOGE("createEGLFence null: not AVAILABLE");
            return EGL_NO_NATIVE_FENCE_FD_ANDROID;
        }

        EGLDisplay display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
        EGLSyncKHR eglSync = glext::eglCreateSyncKHR(display, EGL_SYNC_NATIVE_FENCE_ANDROID,
                                                     nullptr);
        if (eglSync == EGL_NO_SYNC_KHR) {
            LOGE("createEGLFence null: eglCreateSyncKHR null: %s", getEGLError());
            return EGL_NO_NATIVE_FENCE_FD_ANDROID;
        }

        // need flush before wait
        glFlush();

        int fenceFd = glext::eglDupNativeFenceFDANDROID(display, eglSync);
        glext::eglDestroySyncKHR(display, eglSync);

        if (fenceFd == EGL_NO_NATIVE_FENCE_FD_ANDROID) {
            LOGE("createEGLFence null: eglDupNativeFenceFDANDROID error: %s", getEGLError());
        }

        return fenceFd;
    }

    bool
    RenderAPI_OpenGLCoreES::WaitEGLFence(int fenceFd) {
        if (!AVAILABLE) {
            LOGE("waitEGLFence failed: not AVAILABLE");
            return false;
        }

        EGLDisplay display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
        EGLint attribs[] = {EGL_SYNC_NATIVE_FENCE_FD_ANDROID, fenceFd, EGL_NONE};
        EGLSyncKHR eglSync = glext::eglCreateSyncKHR(display, EGL_SYNC_NATIVE_FENCE_ANDROID,
                                                     attribs);
        if (eglSync == EGL_NO_SYNC_KHR) {
            LOGE("waitEGLFence failed: eglCreateSyncKHR null: %s", getEGLError());
            close(fenceFd);
            return false;
        }

        EGLint success = glext::eglWaitSyncKHR(display, eglSync, 0);
        glext::eglDestroySyncKHR(display, eglSync);

        if (success == EGL_FALSE) {
            LOGE("waitEGLFence failed: eglWaitSyncKHR fail: %s", getEGLError());
            return false;
        }

        return true;
    }

    bool
    RenderAPI_OpenGLCoreES::CreateHWBufferConnectedGLESImage(uint32_t width, uint32_t height,
                                                             AHardwareBuffer *hwBuffer,
                                                             GLESHWBImage *hwbImage) {

        if (!AVAILABLE) {
            LOGE("CreateHWBufferConnectedGLESImage null: not AVAILABLE");
            return false;
        }

        hwbImage->hwBuffer = hwBuffer;

        GLuint HWBTexID[1];
        glGenTextures(1, HWBTexID);
        glBindTexture(GL_TEXTURE_2D, HWBTexID[0]);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        hwbImage->image = HWBTexID[0];

        EGLClientBuffer clientBuffer = glext::eglGetNativeClientBufferANDROID(hwBuffer);
        if (!clientBuffer) {
            LOGE("bindTexture failed: clientBuffer null");
            return false;
        }

        EGLDisplay display = eglGetDisplay(EGL_DEFAULT_DISPLAY);

        EGLint eglImageAttributes[] = {EGL_IMAGE_PRESERVED_KHR, EGL_TRUE, EGL_NONE};
        hwbImage->eglImage = glext::eglCreateImageKHR(display, EGL_NO_CONTEXT,
                                                      EGL_NATIVE_BUFFER_ANDROID,
                                                      clientBuffer, eglImageAttributes);
        if (hwbImage->eglImage == EGL_NO_IMAGE_KHR) {
            LOGE("bindTexture failed: eglCreateImageKHR null: %s", getEGLError());
            return false;
        }

        glBindTexture(GL_TEXTURE_2D, HWBTexID[0]);

        DEVLOGD("[sharedtex-jni] texture create %d, thread %d", HWBTexID[0],
                std::this_thread::get_id());

        glext::glEGLImageTargetTexture2DOES(GL_TEXTURE_2D, (GLeglImageOES) hwbImage->eglImage);

        return true;
    }

    void
    RenderAPI_OpenGLCoreES::ImmediateDestroyGLESHWBImage(GLESHWBImage &hwbImage) {
        if (hwbImage.image != 0) {
            GLuint HWBTexID[1];
            HWBTexID[0] = hwbImage.image;
            glDeleteTextures(1, HWBTexID);

            DEVLOGD("[sharedtex-jni] texture delete %d, thread %d", HWBTexID[0],
                    std::this_thread::get_id());
        }

        if (hwbImage.hwBuffer != nullptr) {
            hwbImage.hwBuffer = nullptr;
        }

        EGLDisplay display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
        if (hwbImage.eglImage != EGL_NO_IMAGE_KHR) {
            glext::eglDestroyImageKHR(display, hwbImage.eglImage);
            hwbImage.eglImage = EGL_NO_IMAGE_KHR;
        }
    }

    void
    RenderAPI_OpenGLCoreES::GarbageCollect(bool force /*= false*/) {
        auto it = m_GLESImageMap.begin();

        while (it != m_GLESImageMap.end()) {
            ImmediateDestroyGLESHWBImage(it->second);
            m_GLESImageMap.erase(it++);
        }
    }

    void
    RenderAPI_OpenGLCoreES::DownloadHardwareBuffer(long platformTexID) {
        // Currently OpenGLES does not implement this function because there
        // may be only one way to map the graphics device buffer to the CPU,
        // which is to use glReadPixels, and it is too heavy to process.
    }

    void
    RenderAPI_OpenGLCoreES::UpdateUnityTexture(long unityPlatformTexID, long platformTexID) {

    }

    long
    RenderAPI_OpenGLCoreES::GetPlatformNativeTexture(long unityTexID) {
        return unityTexID;
    }
}

#endif // #if SUPPORT_OPENGL_UNIFIED