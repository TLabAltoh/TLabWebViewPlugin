#include "libwebview.h"
#include "JNILog.h"

extern "C" {
using UnityRenderEvent = void (*)(int);

UnityRenderEvent UpdateSurfaceFunc();

void UpdateSurface(int);

long GetPlatformTextureID(int);

void SetUnityTextureID(int, long);

void ReleaseSharedTexture(int);

bool ContentExists(int);

void SetSurface(int, int);

bool GetSharedBufferUpdateFlag(int);

void SetHardwareBufferUpdateFlag(int, bool);
}

JavaVM *g_jvm;

JNIEnv *GetEnv() {
    void *env = nullptr;
    jint status = g_jvm->GetEnv(&env, JNI_VERSION_1_6);
    return reinterpret_cast<JNIEnv *>(env);
}

static void ThreadDestructor(void *prev_jni_ptr) {
    if (!GetEnv()) {
        LOGD("JNI env already detach from this thread");
        return;
    }

    if (GetEnv() == prev_jni_ptr) {
        jint status = g_jvm->DetachCurrentThread();

        if (status != JNI_OK) {
            LOGE("JNI filed to detach env from this thread");
        }
    }

    LOGE("JNI detach env from this thread finish");
}

static void CreateJNIPtrKey() {
    pthread_key_create(&g_jni_ptr, &ThreadDestructor);
}

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    g_jvm = vm;

    g_jvm->AttachCurrentThread(&g_main_thread_env, nullptr);

    g_class_unity_connect = (jclass) g_main_thread_env->NewGlobalRef(
            g_main_thread_env->FindClass("com/tlab/libwebview/UnityConnect"));

    if (g_class_unity_connect == nullptr) {
        LOGE("JNI Class 'UnityConnect' not found");
    }

    g_func_update_surface = g_main_thread_env->GetMethodID(g_class_unity_connect, "updateSurface",
                                                           "()V");

    if (g_func_update_surface == nullptr) {
        LOGE("JNI Function 'updateSurface' not found");
    }

    g_func_get_platform_texture_id = g_main_thread_env->GetMethodID(g_class_unity_connect,
                                                                    "getPlatformTextureID",
                                                                    "()J");

    if (g_func_get_platform_texture_id == nullptr) {
        LOGE("JNI Function 'GetPlatformTextureID' not found");
    }

    g_func_release_shared_texture = g_main_thread_env->GetMethodID(g_class_unity_connect,
                                                                   "releaseSharedTexture", "()V");

    if (g_func_release_shared_texture == nullptr) {
        LOGE("JNI Function 'setUnityTextureID' not found");
    }

    g_func_set_unity_texture_id = g_main_thread_env->GetMethodID(g_class_unity_connect,
                                                                 "setUnityTextureID", "(J)V");

    if (g_func_set_unity_texture_id == nullptr) {
        LOGE("JNI Function 'setUnityTextureID' not found");
    }

    g_func_content_exists = g_main_thread_env->GetMethodID(g_class_unity_connect,
                                                           "contentExists", "()Z");

    if (g_func_content_exists == nullptr) {
        LOGE("JNI Function 'contentExists' not found");
    }

    g_func_set_surface = g_main_thread_env->GetMethodID(g_class_unity_connect, "setSurface",
                                                        "(Ljava/lang/Object;)V");

    if (g_func_set_surface == nullptr) {
        LOGE("JNI Function 'setSurface' not found");
    }

    g_field_is_shared_buffer_exchanged = g_main_thread_env->GetFieldID(g_class_unity_connect,
                                                                       "m_isSharedBufferExchanged",
                                                                       "Z");

    return JNI_VERSION_1_6;
}

JNIEnv *AttachCurrentThreadIfNeeded() {
    JNIEnv *jni = GetEnv();

    if (jni) {
        return jni;
    }

    JNIEnv *env = nullptr;

    int status = g_jvm->AttachCurrentThread(&env, nullptr);

    if (status != JNI_OK) {
        LOGE("JNIEnv Filed to attach current thread: %d", status);
    }

    jni = reinterpret_cast<JNIEnv *>(env);

    CreateJNIPtrKey();

    pthread_setspecific(g_jni_ptr, jni);

    LOGD("JNIEnv attached to this thread");

    return jni;
}

void UpdateSurface(int instance_ptr) {
    JNIEnv *env = AttachCurrentThreadIfNeeded();

    auto instance = (jobject) ((long) instance_ptr);

    env->CallVoidMethod(instance, g_func_update_surface);
}

UnityRenderEvent UpdateSurfaceFunc() {
    return UpdateSurface;
}

long GetPlatformTextureID(int instance_ptr) {
    auto instance = (jobject) ((long) instance_ptr);

    return g_main_thread_env->CallLongMethod(instance, g_func_get_platform_texture_id);
}

void SetUnityTextureID(int instance_ptr, long unity_texture_id) {
    auto instance = (jobject) ((long) instance_ptr);

    return g_main_thread_env->CallVoidMethod(instance, g_func_set_unity_texture_id,
                                             (jlong) unity_texture_id);
}

void ReleaseSharedTexture(int instance_ptr) {
    auto instance = (jobject) ((long) instance_ptr);

    g_main_thread_env->CallVoidMethod(instance, g_func_release_shared_texture);
}

bool ContentExists(int instance_ptr) {
    auto instance = (jobject) ((long) instance_ptr);

    return g_main_thread_env->CallBooleanMethod(instance, g_func_content_exists);
}

void SetSurface(int instance_ptr, int surface_ptr) {
    auto instance = (jobject) ((long) instance_ptr);
    auto surface_obj = (jobject) ((long) surface_ptr);

    g_main_thread_env->CallVoidMethod(instance, g_func_set_surface, surface_obj);
}

bool GetSharedBufferUpdateFlag(int instance_ptr) {
    auto instance = (jobject) ((long) instance_ptr);

    return g_main_thread_env->GetBooleanField(instance, g_field_is_shared_buffer_exchanged);
}

void SetHardwareBufferUpdateFlag(int instance_ptr, bool value) {
    auto instance = (jobject) ((long) instance_ptr);

    g_main_thread_env->SetBooleanField(instance, g_field_is_shared_buffer_exchanged, value);
}