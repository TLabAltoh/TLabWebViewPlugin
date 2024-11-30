/*
 * SharedTexture
 * @author 	: keith@robot9.me
 * @author  : tlabaltoh@gmail.com
 */

#pragma once

#include "RenderAPI.h"

#include <string>
#include <memory>
#include <map>
#include <vector>

using namespace tlab;

namespace robot9 {
    class SharedTexture {
    public:
        static bool available();

        static std::shared_ptr<SharedTexture>
        Make(int width, int height, bool isVulkan);

        static std::shared_ptr<SharedTexture>
        MakeAdopted(AHardwareBuffer *buffer, bool isVulkan);

        static std::shared_ptr<SharedTexture>
        MakeAdoptedJObject(JNIEnv *env, jobject buffer, bool isVulkan);

        virtual ~SharedTexture();

        jobject getBufferJObject(JNIEnv *env) const;

        void downloadBuffer() const;

        int getWidth() const;

        int getHeight() const;

        long getPlatformTexture() const;

        void setUnityTexture(long unityTexID);

        void updateUnityTexture();

    private:
        SharedTexture(AHardwareBuffer *buffer, int width, int height, bool isVulkan);

    private:
        static bool AVAILABLE;

        bool m_isVulkan = false;
        RenderAPI *m_renderAPI = nullptr;
        AHardwareBuffer *m_buffer = nullptr;
        int m_width = 0;
        int m_height = 0;
        long m_platformTexID = 0;
        long m_unityTexID = 0;
        long m_unityPlatformTexID = 0;
    };
}
