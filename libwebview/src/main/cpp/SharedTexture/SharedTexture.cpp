/*
 * SharedTexture
 * @author 	: keith@robot9.me
 * @author  : tlabaltoh@gmail.com
 */

#include "SharedTexture.h"
#include "../LogUtil/JniLog.h"

#include <dlfcn.h>
#include <mutex>
#include <unistd.h>

namespace robot9 {
    typedef int (*Func_AHardwareBuffer_allocate)(const AHardwareBuffer_Desc *, AHardwareBuffer **);

    typedef void (*Func_AHardwareBuffer_release)(AHardwareBuffer *);

    typedef int (*Func_AHardwareBuffer_lock)(AHardwareBuffer *buffer, uint64_t usage, int32_t fence,
                                             const ARect *rect, void **outVirtualAddress);

    typedef int (*Func_AHardwareBuffer_unlock)(AHardwareBuffer *buffer, int32_t *fence);

    typedef void (*Func_AHardwareBuffer_describe)(const AHardwareBuffer *buffer,
                                                  AHardwareBuffer_Desc *outDesc);

    typedef void (*Func_AHardwareBuffer_acquire)(AHardwareBuffer *buffer);

    typedef AHardwareBuffer *(*Func_AHardwareBuffer_fromHardwareBuffer)(JNIEnv *env,
                                                                        jobject hardwareBufferObj);

    typedef jobject (*Func_AHardwareBuffer_toHardwareBuffer)(JNIEnv *env,
                                                             AHardwareBuffer *hardwareBuffer);

    class HWDriver {
    public:
        static Func_AHardwareBuffer_allocate AHardwareBuffer_allocate;
        static Func_AHardwareBuffer_release AHardwareBuffer_release;
        static Func_AHardwareBuffer_lock AHardwareBuffer_lock;
        static Func_AHardwareBuffer_unlock AHardwareBuffer_unlock;
        static Func_AHardwareBuffer_describe AHardwareBuffer_describe;
        static Func_AHardwareBuffer_acquire AHardwareBuffer_acquire;
        static Func_AHardwareBuffer_fromHardwareBuffer AHardwareBuffer_fromHardwareBuffer;
        static Func_AHardwareBuffer_toHardwareBuffer AHardwareBuffer_toHardwareBuffer;

        template<typename T>
        static void loadSymbol(T *&pfn, const char *symbol) {
            pfn = (T *) dlsym(RTLD_DEFAULT, symbol);
        }

        static bool initFunctions() noexcept {
            loadSymbol(AHardwareBuffer_allocate, "AHardwareBuffer_allocate");
            loadSymbol(AHardwareBuffer_release, "AHardwareBuffer_release");
            loadSymbol(AHardwareBuffer_lock, "AHardwareBuffer_lock");
            loadSymbol(AHardwareBuffer_unlock, "AHardwareBuffer_unlock");
            loadSymbol(AHardwareBuffer_describe, "AHardwareBuffer_describe");
            loadSymbol(AHardwareBuffer_acquire, "AHardwareBuffer_acquire");
            loadSymbol(AHardwareBuffer_fromHardwareBuffer, "AHardwareBuffer_fromHardwareBuffer");
            loadSymbol(AHardwareBuffer_toHardwareBuffer, "AHardwareBuffer_toHardwareBuffer");

            return AHardwareBuffer_allocate && AHardwareBuffer_release
                   && AHardwareBuffer_lock && AHardwareBuffer_unlock
                   && AHardwareBuffer_describe && AHardwareBuffer_acquire
                   && AHardwareBuffer_fromHardwareBuffer && AHardwareBuffer_toHardwareBuffer;
        }
    };

    Func_AHardwareBuffer_allocate HWDriver::AHardwareBuffer_allocate = nullptr;
    Func_AHardwareBuffer_release HWDriver::AHardwareBuffer_release = nullptr;
    Func_AHardwareBuffer_lock HWDriver::AHardwareBuffer_lock = nullptr;
    Func_AHardwareBuffer_unlock HWDriver::AHardwareBuffer_unlock = nullptr;
    Func_AHardwareBuffer_describe HWDriver::AHardwareBuffer_describe = nullptr;
    Func_AHardwareBuffer_acquire HWDriver::AHardwareBuffer_acquire = nullptr;
    Func_AHardwareBuffer_fromHardwareBuffer HWDriver::AHardwareBuffer_fromHardwareBuffer = nullptr;
    Func_AHardwareBuffer_toHardwareBuffer HWDriver::AHardwareBuffer_toHardwareBuffer = nullptr;

    bool SharedTexture::AVAILABLE = HWDriver::initFunctions();

    bool SharedTexture::available() {
        return AVAILABLE;
    }

    std::shared_ptr<SharedTexture> SharedTexture::Make(int width, int height, bool isVulkan) {
        if (!AVAILABLE) {
            LOGE("Make failed: not AVAILABLE");
            return nullptr;
        }
        AHardwareBuffer *buffer = nullptr;
        AHardwareBuffer_Desc desc = {
                static_cast<uint32_t>(width),
                static_cast<uint32_t>(height),
                1,
                AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM,
                AHARDWAREBUFFER_USAGE_CPU_READ_NEVER | AHARDWAREBUFFER_USAGE_CPU_WRITE_NEVER |
                AHARDWAREBUFFER_USAGE_GPU_SAMPLED_IMAGE | AHARDWAREBUFFER_USAGE_GPU_COLOR_OUTPUT,
                0,
                0,
                0};
        int errCode = HWDriver::AHardwareBuffer_allocate(&desc, &buffer);
        if (errCode != 0 || !buffer) {
            LOGE("Make failed: AHardwareBuffer_allocate error: %d", errCode);
            return nullptr;
        }
        return std::shared_ptr<SharedTexture>(
                new SharedTexture(buffer, static_cast<int>(desc.width),
                                  static_cast<int>(desc.height), isVulkan));
    }

    std::shared_ptr<SharedTexture>
    SharedTexture::MakeAdopted(AHardwareBuffer *buffer, bool isVulkan) {
        DEVLOGD("[sharedtex-jni] [MakeAdopted] pass 0 (start)");
        if (!AVAILABLE) {
            LOGE("MakeAdopted failed: not AVAILABLE");
            return nullptr;
        }
        if (!buffer) {
            LOGE("MakeAdopted failed: buffer null");
            return nullptr;
        }
        AHardwareBuffer_Desc desc;
        HWDriver::AHardwareBuffer_describe(buffer, &desc);
        HWDriver::AHardwareBuffer_acquire(buffer);
        DEVLOGD("[sharedtex-jni] [MakeAdopted] pass 1 (end)");
        return std::shared_ptr<SharedTexture>(
                new SharedTexture(buffer, static_cast<int>(desc.width),
                                  static_cast<int>(desc.height), isVulkan));
    }

    std::shared_ptr<SharedTexture>
    SharedTexture::MakeAdoptedJObject(JNIEnv *env, jobject buffer, bool isVulkan) {
        if (!AVAILABLE) {
            LOGE("MakeAdoptedJObject failed: not AVAILABLE");
            return nullptr;
        }
        if (!buffer) {
            LOGE("MakeAdoptedJObject failed: buffer null");
            return nullptr;
        }
        AHardwareBuffer *hwBuffer = HWDriver::AHardwareBuffer_fromHardwareBuffer(env, buffer);
        return MakeAdopted(hwBuffer, isVulkan);
    }

    SharedTexture::SharedTexture(AHardwareBuffer *buffer, int width, int height, bool isVulkan)
            : m_buffer(buffer), m_width(width), m_height(height), m_isVulkan(isVulkan) {
        LOGD("SharedTexture(%d, %d)", width, height);

        DEVLOGD("[sharedtex-jni] [SharedTexture] pass 0 (start)");

        if (!AVAILABLE) {
            LOGE("constructor failed: not AVAILABLE");
            return;
        }

        if (!m_buffer) {
            LOGE("constructor failed: m_buffer");
            return;
        }

        m_renderAPI = m_isVulkan ? m_vulkanAPI : m_glesAPI;

        if (m_isVulkan) {
            DEVLOGD("[sharedtex-jni] [SharedTexture] render api is vulkan");
        }

        if (!m_renderAPI) {
            DEVLOGE("[sharedtex-jni] [SharedTexture] render api is null");
        }

        m_platformTexID = m_renderAPI->RegistHWBufferConnectedTexture(m_width, m_height,
                                                                      m_buffer);

        if (m_platformTexID == 0) {
            LOGE("constructor failed: m_bindTextureId");
            return;
        }

        DEVLOGD("[sharedtex-jni] [SharedTexture] pass 1 (end)");
    }

    SharedTexture::~SharedTexture() {
        LOGD("~SharedTexture");

        if (m_platformTexID != 0) {
            m_renderAPI->UnRegistHWBufferConnectedTexture(m_platformTexID);
        }

        m_platformTexID = NULL;
        m_unityTexID = NULL;
        m_unityPlatformTexID = NULL;

        if (m_buffer) {
            HWDriver::AHardwareBuffer_release(m_buffer);
        }
    }

    jobject SharedTexture::getBufferJObject(JNIEnv *env) const {
        if (!AVAILABLE) {
            LOGE("getBufferJObject null: not AVAILABLE");
            return nullptr;
        }
        if (!m_buffer) {
            LOGE("getBufferJObject null: buffer_ null");
            return nullptr;
        }
        return HWDriver::AHardwareBuffer_toHardwareBuffer(env, m_buffer);
    }

    void SharedTexture::downloadBuffer() const {
        if (m_renderAPI && m_platformTexID != 0) {
            m_renderAPI->DownloadHardwareBuffer(m_platformTexID);
        }
    }

    int SharedTexture::getWidth() const {
        return m_width;
    }

    int SharedTexture::getHeight() const {
        return m_height;
    }

    long SharedTexture::getPlatformTexture() const {
        return m_platformTexID;
    }

    void SharedTexture::setUnityTexture(long unityTexID) {
        m_unityTexID = unityTexID;
        if (m_renderAPI && m_unityTexID) {
            m_unityPlatformTexID = m_renderAPI->GetPlatformNativeTexture(m_unityTexID);
        }
    }

    void SharedTexture::updateUnityTexture() {
        if (m_renderAPI && m_unityPlatformTexID && m_platformTexID) {
            m_renderAPI->UpdateUnityTexture(m_unityPlatformTexID, m_platformTexID);
        }
    }
}
