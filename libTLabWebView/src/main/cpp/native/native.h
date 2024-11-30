#pragma once

#include "IUnityInterface.h"
#include "IUnityGraphics.h"
#include <jni.h>
#include <pthread.h>

namespace {
    jclass g_class_base_offscreen_fragment = nullptr;

    jfieldID g_field_is_shared_buffer_exchanged = nullptr;
    jfieldID g_field_is_fragment_initialized = nullptr;
    jfieldID g_field_is_fragment_disposed = nullptr;

    jmethodID g_func_get_platform_texture_id = nullptr;
    jmethodID g_func_set_unity_texture_id = nullptr;
    jmethodID g_func_update_shared_texture = nullptr;
    jmethodID g_func_release_shared_texture = nullptr;
    jmethodID g_func_content_exists = nullptr;
    jmethodID g_func_set_surface = nullptr;
    jmethodID g_func_remove_surface = nullptr;
    jmethodID g_func_dispose = nullptr;

    // https://stackoverflow.com/questions/7096350/can-you-cache-jnienv
    JNIEnv *g_main_thread_env = nullptr;
    pthread_key_t g_jni_ptr;
}
