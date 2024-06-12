#include "RenderAPI_Vulkan.h"
#include "JniLog.h"

#if SUPPORT_VULKAN

namespace tlab {

    static void
    LoadVulkanAPI(PFN_vkGetInstanceProcAddr getInstanceProcAddr, VkInstance instance) {
        DEVLOGD("[webview-vulkan-test] [LoadVulkanAPI] pass 0 (start)");
        if (!vkGetInstanceProcAddr && getInstanceProcAddr) {
            vkGetInstanceProcAddr = getInstanceProcAddr;
            DEVLOGD("[webview-vulkan-test] [LoadVulkanAPI] vkGetInstanceProcAddr");
        }

        if (!vkCreateInstance) {
            vkCreateInstance = (PFN_vkCreateInstance)vkGetInstanceProcAddr(VK_NULL_HANDLE, "vkCreateInstance");
            DEVLOGD("[webview-vulkan-test] [LoadVulkanAPI] vkCreateInstance");
        }

#define LOAD_VULKAN_FUNC(fn) \
        if (!fn) DEVLOGD("[webview-vulkan-test] befor load function is null %s", #fn); \
        if (fn) DEVLOGD("[webview-vulkan-test] befor load function is not null %s", #fn); \
        if (!fn) fn = (PFN_##fn)vkGetInstanceProcAddr(instance, #fn); \
        if (!fn) DEVLOGD("[webview-vulkan-test] after load function is null %s", #fn); \
        if (fn) DEVLOGD("[webview-vulkan-test] after load function is not null %s", #fn);

        UNITY_USED_VULKAN_API_FUNCTIONS(LOAD_VULKAN_FUNC);
#undef LOAD_VULKAN_FUNC
        DEVLOGD("[webview-vulkan-test] [LoadVulkanAPI] pass 1 (end)");
    }

    static VKAPI_ATTR VkResult VKAPI_CALL
    Hook_vkCreateInstance(const VkInstanceCreateInfo* pCreateInfo, const VkAllocationCallbacks* pAllocator, VkInstance* pInstance) {
        DEVLOGD("[webview-vulkan-test] [Hook_vkCreateInstance] pass 0 (start)");
        vkCreateInstance = (PFN_vkCreateInstance)vkGetInstanceProcAddr(VK_NULL_HANDLE, "vkCreateInstance");
        VkResult result = vkCreateInstance(pCreateInfo, pAllocator, pInstance);
        if (result == VK_SUCCESS) {
            LoadVulkanAPI(vkGetInstanceProcAddr, *pInstance);
        }
        DEVLOGD("[webview-vulkan-test] [Hook_vkCreateInstance] pass 1 (end)");

        return result;
    }

    static int
    FindMemoryTypeIndex(VkPhysicalDeviceMemoryProperties const & physicalDeviceMemoryProperties,
                        uint32_t memoryTypeBits, VkMemoryPropertyFlags memoryPropertyFlags) {

        // Search memory types to find first index with those properties
        for (uint32_t memoryTypeIndex = 0; memoryTypeIndex < VK_MAX_MEMORY_TYPES; ++memoryTypeIndex) {
            if ((memoryTypeBits & 1) == 1) {
                // Type is available, does it match user properties?
                if ((physicalDeviceMemoryProperties.memoryTypes[memoryTypeIndex].propertyFlags & memoryPropertyFlags) == memoryPropertyFlags) {
                    return memoryTypeIndex;
                }
            }
            memoryTypeBits >>= 1;
        }

        return 0;
    }

    static VKAPI_ATTR PFN_vkVoidFunction VKAPI_CALL
    Hook_vkGetInstanceProcAddr(VkInstance device, const char* funcName) {
        if (!funcName) {
            DEVLOGE("[webview-vulkan-test] [Hook_vkGetInstanceProcAddr] function name is null");
            return NULL;
        }

#define INTERCEPT(fn) if (strcmp(funcName, #fn) == 0) return (PFN_vkVoidFunction)&Hook_##fn
        INTERCEPT(vkCreateInstance);
#undef INTERCEPT

        return NULL;
    }

    static PFN_vkGetInstanceProcAddr UNITY_INTERFACE_API
    InterceptVulkanInitialization(PFN_vkGetInstanceProcAddr getInstanceProcAddr, void*) {
        DEVLOGD("[webview-vulkan-test] [InterceptVulkanInitialization] pass 0 (start)");
        vkGetInstanceProcAddr = getInstanceProcAddr;
        DEVLOGD("[webview-vulkan-test] [InterceptVulkanInitialization] pass 1 (end)");
        return Hook_vkGetInstanceProcAddr;
    }

    extern "C" void RenderAPI_Vulkan_OnPluginLoad(IUnityInterfaces* interfaces) {
        DEVLOGD("[webview-vulkan-test] [RenderAPI_Vulkan_OnPluginLoad] pass 0 (start)");
        if (IUnityGraphicsVulkanV2* vulkanInterfaceV2 = interfaces->Get<IUnityGraphicsVulkanV2>()) {
            DEVLOGD("[webview-vulkan-test] [RenderAPI_Vulkan_OnPluginLoad] pass 0 (v2)");
            if (!vulkanInterfaceV2->AddInterceptInitialization(InterceptVulkanInitialization, NULL, 0)) {
                DEVLOGE("[webview-vulkan-test] [RenderAPI_Vulkan_OnPluginLoad] filed to intercept initialization v2");
            }
        } else if (IUnityGraphicsVulkan* vulkanInterface = interfaces->Get<IUnityGraphicsVulkan>()) {
            DEVLOGD("[webview-vulkan-test] [RenderAPI_Vulkan_OnPluginLoad] pass 0 (v1)");
            if (!vulkanInterface->InterceptInitialization(InterceptVulkanInitialization, NULL)) {
                DEVLOGE("[webview-vulkan-test] [RenderAPI_Vulkan_OnPluginLoad] filed to intercept initialization v1");
            }
        }
        DEVLOGD("[webview-vulkan-test] [RenderAPI_Vulkan_OnPluginLoad] pass 1 (end)");
    }

    RenderAPI*
    CreateRenderAPI_Vulkan() {
        return new RenderAPI_Vulkan();
    }

    RenderAPI_Vulkan::RenderAPI_Vulkan()
            : m_UnityVulkan(NULL) {
        DEVLOGD("[webview-vulkan-test] [RenderAPI_Vulkan]");
    }

    void
    RenderAPI_Vulkan::ProcessDeviceEvent(UnityGfxDeviceEventType type, IUnityInterfaces* interfaces) {
        switch (type)
        {
            case kUnityGfxDeviceEventInitialize:
                m_UnityVulkan = interfaces->Get<IUnityGraphicsVulkan>();
                m_Instance = m_UnityVulkan->Instance();

                // Make sure Vulkan API functions are loaded
                LoadVulkanAPI(m_Instance.getInstanceProcAddr, m_Instance.instance);

                UnityVulkanPluginEventConfig config_1;
                config_1.graphicsQueueAccess = kUnityVulkanGraphicsQueueAccess_DontCare;
                config_1.renderPassPrecondition = kUnityVulkanRenderPass_EnsureInside;
                config_1.flags = kUnityVulkanEventConfigFlag_EnsurePreviousFrameSubmission | kUnityVulkanEventConfigFlag_ModifiesCommandBuffersState;
                m_UnityVulkan->ConfigureEvent(1, &config_1);

                break;
            case kUnityGfxDeviceEventShutdown:

                if (m_Instance.device != VK_NULL_HANDLE)
                {
                    GarbageCollect(true);
                }

                m_UnityVulkan = NULL;
                m_Instance = UnityVulkanInstance();

                DEVLOGD("[webview-vulkan-test] shutdown render api");

                break;
        }
    }

    long
    RenderAPI_Vulkan::RegistHWBufferConnectedTexture(uint32_t width, uint32_t height, AHardwareBuffer* hwBuffer) {
        m_mutex.lock();

        VulkanHWBImage* hwbImage = new VulkanHWBImage();
        CreateHWBufferConnectedVulkanImage(width, height, VK_FORMAT_B8G8R8A8_UNORM, VK_IMAGE_TILING_OPTIMAL, hwBuffer, hwbImage,
                                           VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT);

        // NOTE: In Unity, Vulkan's texture id is defined as a VImage pointer.
        // https://docs.unity3d.com/ScriptReference/Texture.GetNativeTexturePtr.html
        // https://docs.unity3d.com/ScriptReference/Texture2D.CreateExternalTexture.html

        // In Vulkan, GetNativeTexturePtr is treated as UnityVulkanImage ?
        // VkImage, VkImage* both caused the error vkUpdateDescriptorSetWithTemplate but
        // UnityVulkanImage* caused the error vkCreateImageView. Additionally,
        // com.unity.webrtc source returns UnityVulkanImage as GetNativeTexturePtr()
        // https://github.com/Unity-Technologies/com.unity.webrtc/blob/0314250c5e8f55bbeb8ee8d55958cd56f1199325/Plugin~/WebRTCPlugin/GraphicsDevice/Vulkan/VulkanTexture2D.h#L20

        // UpdateExternalTexture does not work in Unity 2021 Vulkan
        // https://issuetracker.unity3d.com/issues/android-vulkan-texture2d-dot-updateexternaltexture-does-not-respond-when-a-project-is-built-on-android-with-vulkan-graphics-api

        // The key of the hashmap used the pointer of VkImage, because VkImage has the possibility to update the value.

        long platformTexID = (long)(hwbImage->unityVulkanImage.image);

        m_VulkanImageMap.insert(std::make_pair(platformTexID, *hwbImage));

        DEVLOGD("[webview-vulkan-test] create new platform texture %ld", platformTexID);

        m_mutex.unlock();

        return platformTexID;
    }

    void
    RenderAPI_Vulkan::UnRegistHWBufferConnectedTexture(long platformTexID) {
        m_mutex.lock();

        if (m_VulkanImageMap.find(platformTexID) != m_VulkanImageMap.end()) {
            VulkanHWBImage image = m_VulkanImageMap[platformTexID];
            ImmediateDestroyVulkanHWBImage(image);
            m_VulkanImageMap.erase(platformTexID);

            DEVLOGD("[webview-vulkan-test] delete current platform texture");
        }

        m_mutex.unlock();
    }

    bool
    RenderAPI_Vulkan::CreateHWBufferConnectedVulkanImage(uint32_t width, uint32_t height,
                                                         VkFormat format, VkImageTiling tiling, AHardwareBuffer* hwBuffer, VulkanHWBImage* hwbImage, VkImageUsageFlags usage) {

        // Create an image to bind to our AHardwareBuffer
        // https://android.googlesource.com/platform/cts/+/master/tests/tests/graphics/jni/VulkanTestHelpers.cpp

        hwbImage->width = width;
        hwbImage->height = height;
        hwbImage->hwBuffer = hwBuffer;

        VkAndroidHardwareBufferFormatPropertiesANDROID hwbFormatInfo = {};
        hwbFormatInfo.sType = VK_STRUCTURE_TYPE_ANDROID_HARDWARE_BUFFER_FORMAT_PROPERTIES_ANDROID;
        hwbFormatInfo.pNext = nullptr;

        VkAndroidHardwareBufferPropertiesANDROID hwbProperties = {};
        hwbProperties.sType = VK_STRUCTURE_TYPE_ANDROID_HARDWARE_BUFFER_PROPERTIES_ANDROID;
        hwbProperties.pNext = &hwbFormatInfo;

        if (vkGetAndroidHardwareBufferPropertiesANDROID(m_Instance.device, hwBuffer, &hwbProperties) != VK_SUCCESS) {
            throw std::runtime_error("filed to get external properties!");
        }

        VkPhysicalDeviceMemoryProperties physicalDeviceProperties;
        vkGetPhysicalDeviceMemoryProperties(m_Instance.physicalDevice, &physicalDeviceProperties);

        VkImportAndroidHardwareBufferInfoANDROID androidHardwareBufferInfo = {};
        androidHardwareBufferInfo.sType = VK_STRUCTURE_TYPE_IMPORT_ANDROID_HARDWARE_BUFFER_INFO_ANDROID;
        androidHardwareBufferInfo.pNext = nullptr;
        androidHardwareBufferInfo.buffer = hwbImage->hwBuffer;

        VkExportMemoryAllocateInfoKHR exportInfo = {};
        exportInfo.sType = VK_STRUCTURE_TYPE_EXPORT_MEMORY_ALLOCATE_INFO_KHR;
        exportInfo.pNext = &androidHardwareBufferInfo;
        exportInfo.handleTypes = VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_FD_BIT_KHR;

        VkMemoryAllocateInfo allocateInfo = {};
        allocateInfo.sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
        allocateInfo.pNext = &exportInfo;
        allocateInfo.allocationSize = hwbProperties.allocationSize;
        allocateInfo.memoryTypeIndex = FindMemoryTypeIndex(physicalDeviceProperties, hwbProperties.memoryTypeBits, 0u);;

        if (vkAllocateMemory(m_Instance.device, &allocateInfo, nullptr, &hwbImage->unityVulkanImage.memory.memory) != VK_SUCCESS) {
            throw std::runtime_error("failed to allocate hardware buffer to device memory!");
        }

        DEVLOGD("[webview-vulkan-test] [CreateHWBufferConnectedVulkanImage] vkGetMemoryAndroidHardwareBufferANDROID success");

        // There is no problem in binding hardware buffers to
        // Vulkan memory and map/unmap memory. So maybe the
        // problem is in the vkImage build process.

        VkExternalFormatANDROID externalFormat = {};
        externalFormat.sType = VK_STRUCTURE_TYPE_EXTERNAL_FORMAT_ANDROID;
        externalFormat.pNext = nullptr;
        externalFormat.externalFormat = hwbFormatInfo.externalFormat;

        VkExternalMemoryImageCreateInfo externalCreateInfo = {};
        externalCreateInfo.sType = VK_STRUCTURE_TYPE_EXTERNAL_MEMORY_IMAGE_CREATE_INFO;
        externalCreateInfo.pNext = &externalFormat;
        externalCreateInfo.handleTypes = VK_EXTERNAL_MEMORY_HANDLE_TYPE_ANDROID_HARDWARE_BUFFER_BIT_ANDROID;

        VkImageCreateInfo imageInfo = {};
        imageInfo.sType = VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO;
        imageInfo.imageType = VK_IMAGE_TYPE_2D;
        imageInfo.pNext = &externalCreateInfo;
        imageInfo.extent.width = width;
        imageInfo.extent.height = height;
        imageInfo.extent.depth = 1;
        imageInfo.mipLevels = 1;
        imageInfo.arrayLayers = 1;
        imageInfo.format = format;
        imageInfo.tiling = tiling;
        imageInfo.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
        imageInfo.usage = usage;
        imageInfo.samples = VK_SAMPLE_COUNT_1_BIT;
        imageInfo.sharingMode = VK_SHARING_MODE_EXCLUSIVE;
        imageInfo.flags = 0;

        if (vkCreateImage(m_Instance.device, &imageInfo, nullptr, &hwbImage->unityVulkanImage.image) != VK_SUCCESS) {
            throw std::runtime_error("failed to create image!");
        }

        DEVLOGD("[webview-vulkan-test] [CreateHWBufferConnectedVulkanImage] vkCreateImage success");

        uint memoryOffset = 0;

        if (vkBindImageMemory(m_Instance.device, hwbImage->unityVulkanImage.image, hwbImage->unityVulkanImage.memory.memory, memoryOffset) != VK_SUCCESS) {
            throw std::runtime_error("failed to allocate hardware buffer to image memory!");
        }

        // Vulkan is different from OpenGL, UpdateExternalTexture creates a
        // new texture and the source texture buffer is not shared. So I
        // may have to update Unity's texture manually.

        // Texture2D created in Unity's native texture pointer have no
        // problem in UpdateExternalTexture, but VkImage created in the
        // native plugin cause a crash. (why??)

        DEVLOGD("[webview-vulkan-test] [CreateHWBufferConnectedVulkanImage] vkBindImageMemory success");

        VkMemoryRequirements memRequirements;
        vkGetImageMemoryRequirements(m_Instance.device, hwbImage->unityVulkanImage.image, &memRequirements);

        hwbImage->unityVulkanImage.memory.offset = memoryOffset;
        hwbImage->unityVulkanImage.memory.size = memRequirements.size;
        hwbImage->unityVulkanImage.memory.flags = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;
        hwbImage->unityVulkanImage.memory.memoryTypeIndex = allocateInfo.memoryTypeIndex;
        hwbImage->unityVulkanImage.layout = imageInfo.initialLayout;
        hwbImage->unityVulkanImage.usage = imageInfo.usage;
        hwbImage->unityVulkanImage.format = imageInfo.format;
        hwbImage->unityVulkanImage.extent = imageInfo.extent;
        hwbImage->unityVulkanImage.tiling = imageInfo.tiling;
        hwbImage->unityVulkanImage.type = imageInfo.imageType;
        hwbImage->unityVulkanImage.samples = imageInfo.samples;
        hwbImage->unityVulkanImage.layers = imageInfo.arrayLayers;
        hwbImage->unityVulkanImage.mipCount = imageInfo.mipLevels;

        return true;
    }

    void
    RenderAPI_Vulkan::ImmediateDestroyVulkanHWBImage(VulkanHWBImage &hwbImage) {
        if (hwbImage.unityVulkanImage.memory.memory != VK_NULL_HANDLE)
        {
            vkFreeMemory(m_Instance.device, hwbImage.unityVulkanImage.memory.memory, NULL);
            hwbImage.unityVulkanImage.memory.memory = VK_NULL_HANDLE;
        }

        if (hwbImage.unityVulkanImage.image != VK_NULL_HANDLE) {
            vkDestroyImage(m_Instance.device, hwbImage.unityVulkanImage.image, NULL);
            hwbImage.unityVulkanImage.image = VK_NULL_HANDLE;
        }

        if (hwbImage.hwBuffer != VK_NULL_HANDLE) {
            hwbImage.hwBuffer = NULL;
        }
    }

    void
    RenderAPI_Vulkan::GarbageCollect(bool force /* false */) {
        UnityVulkanRecordingState recordingState;
        if (force)
            recordingState.safeFrameNumber = ~0ull;
        else
        if (!m_UnityVulkan->CommandRecordingState(&recordingState, kUnityVulkanGraphicsQueueAccess_DontCare))
            return;

        std::map<intptr_t , VulkanHWBImage>::iterator it = m_VulkanImageMap.begin();

        while (it != m_VulkanImageMap.end())
        {
            ImmediateDestroyVulkanHWBImage(it->second);
            m_VulkanImageMap.erase(it++);
        }
    }

    void
    RenderAPI_Vulkan::DownloadHardwareBuffer(long platformTexID) {
        VulkanHWBImage hwbImage = m_VulkanImageMap[platformTexID];

        uint8_t *pData;
        vkMapMemory(m_Instance.device, hwbImage.unityVulkanImage.memory.memory, hwbImage.unityVulkanImage.memory.offset, hwbImage.unityVulkanImage.memory.size, 0, (void**)&pData);

        DEVLOGD("[webview-vulkan-test] memory mapping success %d", pData[(512 * 128 + 128) * 4]);

        // Why do I need to use vkUnmapMemory ?
        // https://www.reddit.com/r/vulkan/comments/6l2f0d/why_do_i_need_to_use_vkunmapmemory/
        vkUnmapMemory(m_Instance.device, hwbImage.unityVulkanImage.memory.memory);
    }

    void
    RenderAPI_Vulkan::UpdateUnityTexture(long unityPlatformTexID, long platformTexID) {

        UnityVulkanRecordingState recordingState;
        if (!m_UnityVulkan->CommandRecordingState(&recordingState, kUnityVulkanGraphicsQueueAccess_DontCare))
        {
            return;
        }

        VulkanHWBImage hwbImage = m_VulkanImageMap[platformTexID];

        VkImageCopy copyRegion {};
        copyRegion.srcSubresource = { VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1 };
        copyRegion.srcOffset = { 0, 0, 0 };
        copyRegion.dstSubresource = { VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1 };
        copyRegion.dstOffset = { 0, 0, 0 };
        copyRegion.extent = { hwbImage.width, hwbImage.height, 1 };
        vkCmdCopyImage(
                recordingState.commandBuffer,
                (VkImage)hwbImage.unityVulkanImage.image,
                VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                (VkImage)unityPlatformTexID,
                VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                1,
                &copyRegion);
    }

    long RenderAPI_Vulkan::GetPlatformNativeTexture(long unityTexID) {
        VkImageSubresource subResource { VK_IMAGE_ASPECT_COLOR_BIT, 0, 0 };
        UnityVulkanImage dstUnityImage;
        if (!m_UnityVulkan->AccessTexture(
                (void*)unityTexID,
                &subResource,
                VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                VK_PIPELINE_STAGE_TRANSFER_BIT,
                VK_ACCESS_TRANSFER_READ_BIT,
                kUnityVulkanResourceAccess_PipelineBarrier,
                &dstUnityImage)) {
            DEVLOGD("[webview-vulkan-test] filed to access texture");
            return 0;
        }

        DEVLOGD("[webview-vulkan-test] success to access texture");

        return (long)dstUnityImage.image;
    }
}

#endif // #if SUPPORT_VULKAN