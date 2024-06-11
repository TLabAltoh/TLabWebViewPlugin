#pragma once

#include "IUnityInterface.h"
#include "IUnityGraphics.h"
#include <jni.h>
#include <pthread.h>

namespace {
    static jclass g_class_unity_connect = NULL;

    static jmethodID g_func_update_surface = NULL;

    static jmethodID g_func_get_binded_platform_texture_id = NULL;

    static jmethodID g_func_set_unity_texture_id = NULL;

    static jfieldID g_field_is_shared_buffer_updated = NULL;

    // https://stackoverflow.com/questions/7096350/can-you-cache-jnienv
    static JNIEnv* g_main_thread_env = NULL;

    static pthread_key_t g_jni_ptr;
}
