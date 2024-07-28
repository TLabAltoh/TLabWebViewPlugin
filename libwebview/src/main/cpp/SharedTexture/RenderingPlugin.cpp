#include "RenderAPI.h"
#include "JniLog.h"
#include <assert.h>
#include <math.h>
#include <vector>

using namespace tlab;

static void UNITY_INTERFACE_API OnGraphicsDeviceEvent(UnityGfxDeviceEventType eventType);

static IUnityInterfaces *s_UnityInterfaces = NULL;
static IUnityGraphics *s_Graphics = NULL;

extern "C" {
using UnityRenderEvent = void (*)(int);

UnityRenderEvent DummyRenderEventFunc();

void DummyRenderEvent(int);
}

UnityRenderEvent DummyRenderEventFunc() {
    return DummyRenderEvent;
}

void DummyRenderEvent(int instance_ptr) {

}

extern "C" void UNITY_INTERFACE_EXPORT UNITY_INTERFACE_API
UnityPluginLoad(IUnityInterfaces *unityInterfaces) {
    DEVLOGD("[sharedtex-jni] [UnityPluginLoad] pass 0 (start)");

    s_UnityInterfaces = unityInterfaces;
    s_Graphics = s_UnityInterfaces->Get<IUnityGraphics>();
    s_Graphics->RegisterDeviceEventCallback(OnGraphicsDeviceEvent);

#if SUPPORT_VULKAN
    DEVLOGD("[sharedtex-jni] [UnityPluginLoad] current renderer %d", s_Graphics->GetRenderer());
    if (s_Graphics->GetRenderer() == kUnityGfxRendererNull) {
        DEVLOGD("[sharedtex-jni] [UnityPluginLoad] [RenderAPI_Vulkan_OnPluginLoad] pass 0 (star)");
        extern void RenderAPI_Vulkan_OnPluginLoad(IUnityInterfaces *);
        RenderAPI_Vulkan_OnPluginLoad(unityInterfaces);
        DEVLOGD("[sharedtex-jni] [UnityPluginLoad] [RenderAPI_Vulkan_OnPluginLoad] pass 1 (end)");
    }
#endif // SUPPORT_VULKAN

    // Run OnGraphicsDeviceEvent(initialize) manually on plugin load
    OnGraphicsDeviceEvent(kUnityGfxDeviceEventInitialize);

    DEVLOGD("[sharedtex-jni] [UnityPluginLoad] pass 1 (end)");
}

extern "C" void UNITY_INTERFACE_EXPORT UNITY_INTERFACE_API UnityPluginUnload() {
    s_Graphics->UnregisterDeviceEventCallback(OnGraphicsDeviceEvent);
}

static RenderAPI *s_CurrentAPI = NULL;
static UnityGfxRenderer s_DeviceType = kUnityGfxRendererNull;

static void UNITY_INTERFACE_API OnGraphicsDeviceEvent(UnityGfxDeviceEventType eventType) {
    DEVLOGD("[sharedtex-jni] [OnGraphicsDeviceEvent] pass 0 (start)");

    // Create graphics API implementation upon initialization
    if (eventType == kUnityGfxDeviceEventInitialize) {
        DEVLOGD("[sharedtex-jni] [OnGraphicsDeviceEvent] kUnityGfxDeviceEventInitialize");
        assert(s_CurrentAPI == NULL);
        s_DeviceType = s_Graphics->GetRenderer();
        s_CurrentAPI = CreateRenderAPI(s_DeviceType);
    }

    // Let the implementation process the device related events
    if (m_glesAPI) {
        m_glesAPI->ProcessDeviceEvent(eventType, s_UnityInterfaces);
    }

    if (m_vulkanAPI) {
        m_vulkanAPI->ProcessDeviceEvent(eventType, s_UnityInterfaces);
    }

    // Cleanup graphics API implementation upon shutdown
    if (eventType == kUnityGfxDeviceEventShutdown) {
        delete s_CurrentAPI;
        s_CurrentAPI = NULL;
        s_DeviceType = kUnityGfxRendererNull;
    }

    DEVLOGD("[sharedtex-jni] [OnGraphicsDeviceEvent] pass 1 (end)");
}