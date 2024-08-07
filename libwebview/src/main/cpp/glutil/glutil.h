#pragma once

//
// Created by no name boy on 2024/08/07.
//

#ifndef WEBVIEW_GLUTIL_H
#define WEBVIEW_GLUTIL_H

#include <jni.h>

static jbyteArray
ConvertDataProc(JNIEnv *env, jobject thiz, jbyte *srcBuff, jint width, jint height);

static jbyteArray
ConvertData(JNIEnv *env, jobject thiz, jbyteArray data, jint width, jint height);

#endif //WEBVIEW_GLUTIL_H
