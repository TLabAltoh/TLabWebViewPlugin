#include "RenderAPI.h"
#include "JniLog.h"

namespace tlab {

    RenderAPI *CreateRenderAPI(UnityGfxRenderer apiType) {
        DEVLOGD("[sharedtex-jni] [CreateRenderAPI] pass 0 (start)");

#	if SUPPORT_OPENGL_UNIFIED
        extern RenderAPI *CreateRenderAPI_OpenGLCoreES(UnityGfxRenderer apiType);
        m_glesAPI = CreateRenderAPI_OpenGLCoreES(apiType);
#	endif // if SUPPORT_OPENGL_UNIFIED

#	if SUPPORT_VULKAN
        if (apiType == kUnityGfxRendererVulkan) {
            extern RenderAPI *CreateRenderAPI_Vulkan();
            m_vulkanAPI = CreateRenderAPI_Vulkan();
        }
#	endif // if SUPPORT_VULKAN

        DEVLOGD("[sharedtex-jni] [CreateRenderAPI] pass 1 (end)");

        // Unknown or unsupported graphics API
        return NULL;
    }
}