#include "JNIUtil.h"
#include "JNILog.h"

extern "C" {
using UnityRenderEvent = void (*)(int);

UnityRenderEvent UpdateSurfaceFunc();

void UpdateSurface(int);

long GetBindedPlatformTextureID(int);

void SetUnityTextureID(int, long);

void ReleaseSharedTexture(int);

bool GetSharedBufferUpdateFlag(int);

void SetHardwareBufferUpdateFlag(int, bool);
}

JavaVM *g_jvm;

JNIEnv *GetEnv() {
    void *env = NULL;
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

        if (!(status == JNI_OK)) {
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

    g_jvm->AttachCurrentThread(&g_main_thread_env, NULL);

    g_class_unity_connect = (jclass) g_main_thread_env->NewGlobalRef(
            g_main_thread_env->FindClass("com/tlab/libwebview/UnityConnect"));

    if (g_class_unity_connect == NULL) {
        LOGE("JNI Class 'UnityConnect' not found");
    }

    g_func_update_surface = g_main_thread_env->GetMethodID(g_class_unity_connect, "updateSurface",
                                                           "()V");

    if (g_func_update_surface == NULL) {
        LOGE("JNI Function 'updateSurface' not found");
    }

    g_func_get_binded_platform_texture_id = g_main_thread_env->GetMethodID(g_class_unity_connect,
                                                                           "getBindedPlatformTextureID",
                                                                           "()J");

    if (g_func_get_binded_platform_texture_id == NULL) {
        LOGE("JNI Function 'GetBindedPlatformTextureID' not found");
    }

    g_func_release_shared_texture = g_main_thread_env->GetMethodID(g_class_unity_connect,
                                                                   "releaseSharedTexture", "()V");

    if (g_func_release_shared_texture == NULL) {
        LOGE("JNI Function 'setUnityTextureID' not found");
    }

    g_func_set_unity_texture_id = g_main_thread_env->GetMethodID(g_class_unity_connect,
                                                                 "setUnityTextureID", "(J)V");

    if (g_func_set_unity_texture_id == NULL) {
        LOGE("JNI Function 'setUnityTextureID' not found");
    }

    g_field_is_shared_buffer_updated = g_main_thread_env->GetFieldID(g_class_unity_connect,
                                                                     "mSharedBufferUpdated", "Z");

    return JNI_VERSION_1_6;
}

JNIEnv *AttachCurrentThreadIfNeeded() {
    JNIEnv *jni = GetEnv();

    if (jni) {
        return jni;
    }

    JNIEnv *env = NULL;

    int status = g_jvm->AttachCurrentThread(&env, NULL);

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

    jobject instance = (jobject) ((long) instance_ptr);

    env->CallVoidMethod(instance, g_func_update_surface);
}

UnityRenderEvent UpdateSurfaceFunc() {
    return UpdateSurface;
}

long GetBindedPlatformTextureID(int instance_ptr) {
    jobject instance = (jobject) ((long) instance_ptr);

    return g_main_thread_env->CallLongMethod(instance, g_func_get_binded_platform_texture_id);
}

void SetUnityTextureID(int instance_ptr, long unity_texture_id) {
    jobject instance = (jobject) ((long) instance_ptr);

    return g_main_thread_env->CallVoidMethod(instance, g_func_set_unity_texture_id,
                                             (jlong) unity_texture_id);
}

void ReleaseSharedTexture(int instance_ptr) {
    jobject instance = (jobject) ((long) instance_ptr);

    g_main_thread_env->CallVoidMethod(instance, g_func_release_shared_texture);
}

bool GetSharedBufferUpdateFlag(int instance_ptr) {
    jobject instance = (jobject) ((long) instance_ptr);

    return g_main_thread_env->GetBooleanField(instance, g_field_is_shared_buffer_updated);
}

void SetHardwareBufferUpdateFlag(int instance_ptr, bool value) {
    jobject instance = (jobject) ((long) instance_ptr);

    g_main_thread_env->SetBooleanField(instance, g_field_is_shared_buffer_updated, value);
}