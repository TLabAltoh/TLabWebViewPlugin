//
// Created by no name boy on 2024/08/07.
//

#include "glutil.h"
#include "JniLog.h"
#include <jni.h>
#include <GLES3/gl3.h>

extern "C" {
JNICALL void Java_com_tlab_viewtobuffer_ViewToPBORenderer_glUtilReadPixels(JNIEnv *env, jobject,
                                                                           jint x, jint y,
                                                                           jint width,
                                                                           jint height,
                                                                           jint pboId) {
    glBindBuffer(GL_PIXEL_PACK_BUFFER, pboId);
    glReadPixels(x, y, width, height, GL_RGBA, GL_UNSIGNED_BYTE, 0);
    glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
}

JNICALL jbyteArray
Java_com_tlab_viewtobuffer_ViewToPBORenderer_glUtilGetPboBuffer(JNIEnv *env, jobject thiz,
                                                                jint width, jint height,
                                                                jint pboId) {
    glBindBuffer(GL_PIXEL_PACK_BUFFER, pboId);
    void *srcBuff = glMapBufferRange(GL_PIXEL_PACK_BUFFER, 0, (width * height) << 2,
                                     GL_MAP_READ_BIT);
    jbyteArray buffer = ConvertDataProc(env, thiz, (jbyte *) srcBuff, width, height);

    if (srcBuff != NULL) {
        glUnmapBuffer(GL_PIXEL_PACK_BUFFER);
    } else {
        LOGE("GetPboBuffer failure");
    }
    glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);

    return buffer;
}
}

static jbyteArray
ConvertDataProc(JNIEnv *env, jobject thiz, jbyte *srcBuff, jint width, jint height) {
    int srcSize = width * height;
    uint8_t *dstBuff = NULL;
    int dstSize;
    dstSize = srcSize << 2;
    jbyteArray buffer = env->NewByteArray(dstSize);
    if (srcBuff != NULL) {
        env->SetByteArrayRegion(buffer, 0, dstSize,
                                (dstBuff != NULL ? (jbyte *) dstBuff : srcBuff));
    }
    delete[] dstBuff;

    return buffer;
}

static jbyteArray
ConvertData(JNIEnv *env, jobject thiz, jbyteArray data, jint width, jint height) {
    jbyte *srcBuff = env->GetByteArrayElements(data, NULL);
    return ConvertDataProc(env, thiz, srcBuff, width, height);
}