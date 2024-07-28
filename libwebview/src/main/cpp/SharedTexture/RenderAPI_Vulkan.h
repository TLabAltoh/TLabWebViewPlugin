#pragma onece

#include "RenderAPI.h"

#include <string.h>
#include <map>
#include <mutex>
#include <vector>
#include <math.h>

#if SUPPORT_VULKAN

// This plugin does not link to the Vulkan loader, easier to support multiple APIs and systems that don't have Vulkan support
#define VK_ANDROID_external_memory_android_hardware_buffer
#define VK_ANDROID_EXTERNAL_MEMORY_ANDROID_HARDWARE_BUFFER_SPEC_VERSION 3
#define VK_NO_PROTOTYPES

#include <vulkan/vulkan.h>
#include <vulkan/vulkan_core.h>
#include <vulkan/vulkan_android.h>
#include "IUnityGraphicsVulkan.h"

#define UNITY_USED_VULKAN_API_FUNCTIONS(apply) \
    apply(vkCreateInstance); \
    apply(vkCmdBeginRenderPass); \
    apply(vkCreateBuffer); \
    apply(vkGetPhysicalDeviceMemoryProperties); \
    apply(vkGetBufferMemoryRequirements); \
    apply(vkMapMemory); \
    apply(vkBindBufferMemory); \
    apply(vkAllocateMemory); \
    apply(vkDestroyBuffer); \
    apply(vkFreeMemory); \
    apply(vkUnmapMemory); \
    apply(vkQueueWaitIdle); \
    apply(vkDeviceWaitIdle); \
    apply(vkCmdCopyBufferToImage); \
    apply(vkFlushMappedMemoryRanges); \
    apply(vkCreatePipelineLayout); \
    apply(vkCreateShaderModule); \
    apply(vkCreateImage); \
    apply(vkCmdCopyImage); \
    apply(vkBindImageMemory); \
    apply(vkGetImageMemoryRequirements); \
    apply(vkDestroyShaderModule); \
    apply(vkDestroyImage); \
    apply(vkCreateGraphicsPipelines); \
    apply(vkCmdBindPipeline); \
    apply(vkCmdDraw); \
    apply(vkCmdPushConstants); \
    apply(vkCmdBindVertexBuffers); \
    apply(vkDestroyPipeline); \
    apply(vkDestroyPipelineLayout); \
    apply(vkGetAndroidHardwareBufferPropertiesANDROID); \
    apply(vkGetMemoryAndroidHardwareBufferANDROID);

#define VULKAN_DEFINE_API_FUNCPTR(func) static PFN_##func func
VULKAN_DEFINE_API_FUNCPTR(vkGetInstanceProcAddr);
UNITY_USED_VULKAN_API_FUNCTIONS(VULKAN_DEFINE_API_FUNCPTR);
#undef VULKAN_DEFINE_API_FUNCPTR

namespace tlab {

    struct VulkanHWBImage {
        uint32_t width;
        uint32_t height;
        UnityVulkanImage unityVulkanImage;
        AHardwareBuffer *hwBuffer = VK_NULL_HANDLE;
        VkImportAndroidHardwareBufferInfoANDROID *vkHWBInfo = VK_NULL_HANDLE;
    };

    class RenderAPI_Vulkan : public RenderAPI {
    public:
        RenderAPI_Vulkan();

        virtual ~RenderAPI_Vulkan() {}

        virtual void ProcessDeviceEvent(UnityGfxDeviceEventType type, IUnityInterfaces *interfaces);

        virtual long
        RegistHWBufferConnectedTexture(uint32_t width, uint32_t height, AHardwareBuffer *hwBuffer);

        virtual void UnRegistHWBufferConnectedTexture(long platformTexID);

        virtual void DownloadHardwareBuffer(long platformTexID);

        virtual long GetPlatformNativeTexture(long unityTexID);

        virtual void UpdateUnityTexture(long unityPlatformTexID, long platformTexID);

    private:
        bool CreateHWBufferConnectedVulkanImage(uint32_t width, uint32_t height,
                                                VkFormat format, VkImageTiling tiling,
                                                AHardwareBuffer *hwBuffer, VulkanHWBImage *hwbImage,
                                                VkImageUsageFlags usage) const;

        void ImmediateDestroyVulkanHWBImage(VulkanHWBImage &hwbImage) const;

    private:
        IUnityGraphicsVulkan *m_UnityVulkan;
        UnityVulkanInstance m_Instance;
        std::map<std::pair<intptr_t, std::__thread_id>, VulkanHWBImage> m_VulkanImageMap;
        std::mutex m_mutex;

        void GarbageCollect(bool force);

        void DeviceEventInitialize(IUnityInterfaces *interfaces);
    };
}

#endif