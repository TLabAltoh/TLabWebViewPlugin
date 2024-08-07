#pragma once

#define SUPPORT_VULKAN 1
#define SUPPORT_OPENGL_UNIFIED 1

#include "IUnityInterface.h"
#include "IUnityGraphics.h"

#include <stddef.h>
#include <stdint.h>
#include <jni.h>
#include <android/hardware_buffer.h>

struct IUnityInterfaces;

namespace tlab {
    // Super-simple "graphics abstraction". This is nothing like how a proper platform abstraction layer would look like;
    // all this does is a base interface for whatever our plugin sample needs. Which is only "draw some triangles"
    // and "modify a texture" at this point.
    //
    // There are implementations of this base class for D3D9, D3D11, OpenGL etc.; see individual RenderAPI_* files.
    class RenderAPI {
    public:
        virtual ~RenderAPI() = default;

        // Process general event like initialization, shutdown, device loss/reset etc.
        virtual void
        ProcessDeviceEvent(UnityGfxDeviceEventType type, IUnityInterfaces *interfaces) = 0;

        virtual long RegistHWBufferConnectedTexture(uint32_t width, uint32_t height,
                                                    AHardwareBuffer *hwBuffer) = 0;

        virtual void UnRegistHWBufferConnectedTexture(long platformTexID) = 0;

        virtual void DownloadHardwareBuffer(long platformTexID) = 0;

        virtual long GetPlatformNativeTexture(long unityTexID) = 0;

        virtual void UpdateUnityTexture(long unityPlatformTexID, long platformTexID) = 0;
    };

    // Create a graphics API implementation instance for the given API type.
    RenderAPI *CreateRenderAPI(UnityGfxRenderer apiType);

#if SUPPORT_OPENGL_UNIFIED
    inline RenderAPI *m_glesAPI;
#endif

#if SUPPORT_VULKAN
    inline RenderAPI *m_vulkanAPI;
#endif
}