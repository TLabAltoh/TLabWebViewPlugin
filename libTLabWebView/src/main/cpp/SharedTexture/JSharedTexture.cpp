/*
 * SharedTexture
 * @author 	: keith@robot9.me
 * @author  : tlabaltoh@gmail.com
 */

#include "SharedTexture.h"
#include "JniLog.h"

namespace robot9 {

    class SharedTextureContext {
    public:
        std::shared_ptr<SharedTexture> buffer;
    };

}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_robot9_shared_SharedTexture_available(JNIEnv *env, jclass clazz) {
    return robot9::SharedTexture::available() ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_robot9_shared_SharedTexture_create(JNIEnv *env, jobject /* this */,
                                            jint width, jint height, jboolean isVulkan) {
    auto SharedTexture = robot9::SharedTexture::Make(width, height, isVulkan);
    if (SharedTexture) {
        auto *ctx = new robot9::SharedTextureContext();
        ctx->buffer = std::move(SharedTexture);
        return reinterpret_cast<jlong>(ctx);
    }
    LOGE("create SharedTexture failed");
    return 0;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_robot9_shared_SharedTexture_createFromBuffer(JNIEnv *env, jobject /* this */,
                                                      jobject buffer, jboolean isVulkan) {
    auto SharedTexture = robot9::SharedTexture::MakeAdoptedJObject(env, buffer, isVulkan);
    if (SharedTexture) {
        auto *ctx = new robot9::SharedTextureContext();
        ctx->buffer = std::move(SharedTexture);
        return reinterpret_cast<jlong>(ctx);
    }
    LOGE("create SharedTexture failed");
    return 0;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_robot9_shared_SharedTexture_getBuffer(JNIEnv *env, jobject /* this */,
                                               jlong ctx) {
    if (ctx == 0) {
        return nullptr;
    }

    auto *_ctx = reinterpret_cast<robot9::SharedTextureContext *>(ctx);
    if (!_ctx->buffer) {
        return nullptr;
    }
    return _ctx->buffer->getBufferJObject(env);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_robot9_shared_SharedTexture_downloadBuffer(JNIEnv *env, jobject /* this */,
                                                    jlong ctx) {
    if (ctx == 0) {
        return;
    }

    auto *_ctx = reinterpret_cast<robot9::SharedTextureContext *>(ctx);
    if (!_ctx->buffer) {
        return;
    }
    return _ctx->buffer->downloadBuffer();
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_robot9_shared_SharedTexture_getWidth(JNIEnv *env, jobject /* this */,
                                              jlong ctx) {
    if (ctx == 0) {
        return 0;
    }

    auto *_ctx = reinterpret_cast<robot9::SharedTextureContext *>(ctx);
    if (!_ctx->buffer) {
        return 0;
    }
    return _ctx->buffer->getWidth();
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_robot9_shared_SharedTexture_getHeight(JNIEnv *env, jobject /* this */,
                                               jlong ctx) {
    if (ctx == 0) {
        return 0;
    }

    auto *_ctx = reinterpret_cast<robot9::SharedTextureContext *>(ctx);
    if (!_ctx->buffer) {
        return 0;
    }
    return _ctx->buffer->getHeight();
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_robot9_shared_SharedTexture_getPlatformTexture(JNIEnv *env, jobject /* this */,
                                                        jlong ctx) {
    if (ctx == 0) {
        return 0;
    }

    auto *_ctx = reinterpret_cast<robot9::SharedTextureContext *>(ctx);
    if (!_ctx->buffer) {
        return 0;
    }
    return static_cast<jlong>(_ctx->buffer->getPlatformTexture());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_robot9_shared_SharedTexture_setUnityTexture(JNIEnv *env, jobject /* this */,
                                                     jlong ctx, jlong unityTexID) {
    if (ctx == 0) {
        return;
    }

    auto *_ctx = reinterpret_cast<robot9::SharedTextureContext *>(ctx);
    if (!_ctx->buffer) {
        return;
    }

    return _ctx->buffer->setUnityTexture(unityTexID);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_robot9_shared_SharedTexture_updateUnityTexture(JNIEnv *env, jobject /* this */,
                                                        jlong ctx) {
    if (ctx == 0) {
        return;
    }

    auto *_ctx = reinterpret_cast<robot9::SharedTextureContext *>(ctx);
    if (!_ctx->buffer) {
        return;
    }

    return _ctx->buffer->updateUnityTexture();
}

//extern "C"
//JNIEXPORT jint JNICALL
//Java_com_robot9_shared_SharedTexture_createEGLFence(JNIEnv *env, jclass clazz) {
//    return robot9::SharedTexture::createEGLFence();
//}
//
//extern "C"
//JNIEXPORT jboolean JNICALL
//Java_com_robot9_shared_SharedTexture_waitEGLFence(JNIEnv *env, jclass clazz,
//                                                  jint fenceFd) {
//    return robot9::SharedTexture::waitEGLFence(fenceFd) ? JNI_TRUE : JNI_FALSE;
//}

extern "C"
JNIEXPORT void JNICALL
Java_com_robot9_shared_SharedTexture_destroy(JNIEnv *env, jobject /* this */,
                                             jlong ctx) {
    if (ctx == 0) {
        return;
    }
    auto *_ctx = reinterpret_cast<robot9::SharedTextureContext *>(ctx);
    delete _ctx;
}