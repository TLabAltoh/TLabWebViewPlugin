#include "native.h"
#include "JNILog.h"

extern "C" {
using UnityRenderEvent = void (*)(int);

UnityRenderEvent DisposeFunc();

void Dispose(int);

UnityRenderEvent UpdateSharedTextureFunc();

void UpdateSharedTexture(int);

void ReleaseSharedTexture(int);

long GetPlatformTextureID(int);

void SetUnityTextureID(int, long);

bool ContentExists(int);

void SetSurface(int, int, int, int);

void RemoveSurface(int);

bool GetIsFragmentDisposed(int);

bool GetIsFragmentInitialized(int);

bool GetSharedBufferUpdateFlag(int);

void SetSharedBufferUpdateFlag(int, bool);
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

        if (status != JNI_OK) LOGE("JNI filed to detach env from this thread");
    }

    LOGE("JNI detach env from this thread finish");
}

static void CreateJNIPtrKey() {
    pthread_key_create(&g_jni_ptr, &ThreadDestructor);
}

static void FindField(jfieldID *target_field, jclass *target_class, char *name, char *sig) {
    *target_field = g_main_thread_env->GetFieldID(*target_class, name, sig);

    if (*target_field == nullptr) LOGE("JNI Field '%s' not found", name);
}

static void FindJClass(jclass *target, char *name) {
    *target = (jclass) g_main_thread_env->NewGlobalRef(g_main_thread_env->FindClass(name));

    if (*target == nullptr) LOGE("JNI Class '%s' not found", name);
}

static void FindJMethod(jmethodID *target_method, jclass *target_class, char *name, char *sig) {
    *target_method = g_main_thread_env->GetMethodID(*target_class, name, sig);

    if (*target_method == nullptr)
        LOGE("JNI Function '%s' not found", name);
}

static void Load() {
    FindJClass(&g_class_base_offscreen_fragment, (char *) "com/tlab/webkit/BaseOffscreenFragment");

    FindJMethod(&g_func_update_shared_texture, &g_class_base_offscreen_fragment,
                (char *) "UpdateSharedTexture", (char *) "()V");

    FindJMethod(&g_func_release_shared_texture, &g_class_base_offscreen_fragment,
                (char *) "ReleaseSharedTexture", (char *) "()V");

    FindJMethod(&g_func_get_platform_texture_id, &g_class_base_offscreen_fragment,
                (char *) "GetPlatformTextureID", (char *) "()J");

    FindJMethod(&g_func_set_unity_texture_id, &g_class_base_offscreen_fragment,
                (char *) "SetUnityTextureID", (char *) "(J)V");

    FindJMethod(&g_func_content_exists, &g_class_base_offscreen_fragment,
                (char *) "ContentExists", (char *) "()Z");

    FindJMethod(&g_func_set_surface, &g_class_base_offscreen_fragment,
                (char *) "SetSurface", (char *) "(Ljava/lang/Object;II)V");

    FindJMethod(&g_func_remove_surface, &g_class_base_offscreen_fragment,
                (char *) "RemoveSurface", (char *) "()V");

    FindJMethod(&g_func_dispose, &g_class_base_offscreen_fragment,
                (char *) "Dispose", (char *) "()V");

    FindField(&g_field_is_fragment_initialized, &g_class_base_offscreen_fragment,
              (char *) "mInitialized", (char *) "Z");

    FindField(&g_field_is_fragment_disposed, &g_class_base_offscreen_fragment,
              (char *) "mDisposed", (char *) "Z");

    FindField(&g_field_is_shared_buffer_exchanged, &g_class_base_offscreen_fragment,
              (char *) "mIsSharedBufferExchanged", (char *) "Z");
}

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    g_jvm = vm;

    g_jvm->AttachCurrentThread(&g_main_thread_env, nullptr);

    Load();

    return JNI_VERSION_1_6;
}

JNIEnv *AttachCurrentThreadIfNeeded() {
    JNIEnv *jni = GetEnv();

    if (jni) return jni;

    JNIEnv *env = nullptr;

    int status = g_jvm->AttachCurrentThread(&env, nullptr);

    if (status != JNI_OK) LOGE("JNIEnv Filed to attach current thread: %d", status);

    jni = reinterpret_cast<JNIEnv *>(env);

    CreateJNIPtrKey();

    pthread_setspecific(g_jni_ptr, jni);

    LOGD("JNIEnv attached to this thread");

    return jni;
}

bool GetSharedBufferUpdateFlag(int instance_ptr) {
    auto instance = (jobject) ((long) instance_ptr);

    return g_main_thread_env->GetBooleanField(instance,
                                              g_field_is_shared_buffer_exchanged);
}

void SetSharedBufferUpdateFlag(int instance_ptr, bool value) {
    auto instance = (jobject) ((long) instance_ptr);

    g_main_thread_env->SetBooleanField(instance, g_field_is_shared_buffer_exchanged,
                                       value);
}

bool GetIsFragmentInitialized(int instance_ptr) {
    auto instance = (jobject) ((long) instance_ptr);

    return g_main_thread_env->GetBooleanField(instance, g_field_is_fragment_initialized);
}

bool GetIsFragmentDisposed(int instance_ptr) {
    auto instance = (jobject) ((long) instance_ptr);

    return g_main_thread_env->GetBooleanField(instance, g_field_is_fragment_disposed);
}

void Dispose(int instance_ptr) {
    JNIEnv *env = AttachCurrentThreadIfNeeded();

    auto instance = (jobject) ((long) instance_ptr);

    env->CallVoidMethod(instance, g_func_dispose);
}

UnityRenderEvent DisposeFunc() {
    return Dispose;
}

void UpdateSharedTexture(int instance_ptr) {
    JNIEnv *env = AttachCurrentThreadIfNeeded();

    auto instance = (jobject) ((long) instance_ptr);

    env->CallVoidMethod(instance, g_func_update_shared_texture);
}

UnityRenderEvent UpdateSharedTextureFunc() {
    return UpdateSharedTexture;
}

void ReleaseSharedTexture(int instance_ptr) {
    auto instance = (jobject) ((long) instance_ptr);

    g_main_thread_env->CallVoidMethod(instance, g_func_release_shared_texture);
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

bool ContentExists(int instance_ptr) {
    auto instance = (jobject) ((long) instance_ptr);

    return g_main_thread_env->CallBooleanMethod(instance, g_func_content_exists);
}

void SetSurface(int instance_ptr, int surface_ptr, int width, int height) {
    auto instance = (jobject) ((long) instance_ptr);
    auto surface_obj = (jobject) ((long) surface_ptr);

    g_main_thread_env->CallVoidMethod(instance, g_func_set_surface, surface_obj, width, height);
}

void RemoveSurface(int instance_ptr) {
    auto instance = (jobject) ((long) instance_ptr);

    g_main_thread_env->CallVoidMethod(instance, g_func_remove_surface);
}