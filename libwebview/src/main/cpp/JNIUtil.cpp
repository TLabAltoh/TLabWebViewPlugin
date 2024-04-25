#include "Unity/IUnityInterface.h"
#include "Unity/IUnityGraphics.h"
#include "JniLog.h"
#include <jni.h>
#include <pthread.h>

extern "C" {
using UnityRenderEvent = void(*)(int);

UnityRenderEvent UpdateSurfaceFunc();

int GetTexturePtr(int);

void UpdateSurface(int);

int add(int x, int y) {
    return x + y;
}
}

JavaVM* g_jvm;

static jclass g_class_unity_connect = NULL;

static jmethodID g_func_update_surface = NULL;

static jmethodID g_func_get_texture_ptr = NULL;

static pthread_key_t g_jni_ptr;

JNIEnv* GetEnv() {
    void* env = NULL;
    jint status = g_jvm->GetEnv(&env, JNI_VERSION_1_6);
    return reinterpret_cast<JNIEnv*>(env);
}

static void ThreadDestructor(void* prev_jni_ptr) {
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

jint JNI_OnLoad(JavaVM *vm, void* reserved) {
    g_jvm = vm;

    JNIEnv* env = 0;

    g_jvm->AttachCurrentThread(&env, NULL);

    g_class_unity_connect = (jclass)env->NewGlobalRef(env->FindClass("com/tlab/libwebview/UnityConnect"));

    if (g_class_unity_connect == NULL) {
        LOGE("JNI Class 'UnityConnect' not found");
    }

    g_func_update_surface = env->GetMethodID(g_class_unity_connect, "updateSharedTexture", "()V");

    if (g_func_update_surface == NULL) {
        LOGE("JNI Function 'updateSharedTexture' not found");
    }

    g_func_get_texture_ptr = env->GetMethodID(g_class_unity_connect, "getTexturePtr", "()I");

    if (g_func_get_texture_ptr == NULL) {
        LOGE("JNI Function 'getTexturePtr' not found");
    }

    return JNI_VERSION_1_6;
}

JNIEnv* AttachCurrentThreadIfNeeded() {
    JNIEnv* jni = GetEnv();

    if (jni) {
        return jni;
    }

    JNIEnv* env = NULL;

    int status = g_jvm->AttachCurrentThread(&env, NULL);

    if (status != JNI_OK ) {
        LOGE("JNIEnv Filed to attach current thread: %d", status);
    }

    jni = reinterpret_cast<JNIEnv*>(env);

    CreateJNIPtrKey();

    pthread_setspecific(g_jni_ptr, jni);

    LOGD("JNIEnv attached to this thread");

    return jni;
}

void UpdateSurface(int instance_ptr) {
    JNIEnv* env = AttachCurrentThreadIfNeeded();

    jobject instance = (jobject)((long)instance_ptr);

    env->CallVoidMethod(instance, g_func_update_surface);
}

UnityRenderEvent UpdateSurfaceFunc()
{
    return UpdateSurface;
}

int GetTexturePtr(int instance_ptr) {

    JNIEnv* env = GetEnv();

    jobject instance = (jobject)((long)instance_ptr);

    return env->CallIntMethod(instance, g_func_get_texture_ptr);
}