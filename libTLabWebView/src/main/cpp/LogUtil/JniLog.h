/*
 * SharedTexture
 * @author 	: keith@robot9.me
 *
 */

#pragma once

#include <jni.h>
#include <android/log.h>

#define LOG_TAG "SharedTexture-JNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,  LOG_TAG,  __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,   LOG_TAG,  __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,   LOG_TAG,  __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,  LOG_TAG,  __VA_ARGS__)
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL,  LOG_TAG,  __VA_ARGS__)

#define DEVLOGD(...) //__android_log_print(ANDROID_LOG_DEBUG,  LOG_TAG,  __VA_ARGS__)
#define DEVLOGI(...) //__android_log_print(ANDROID_LOG_INFO,   LOG_TAG,  __VA_ARGS__)
#define DEVLOGW(...) //__android_log_print(ANDROID_LOG_WARN,   LOG_TAG,  __VA_ARGS__)
#define DEVLOGE(...) //__android_log_print(ANDROID_LOG_ERROR,  LOG_TAG,  __VA_ARGS__)
#define DEVLOGF(...) //__android_log_print(ANDROID_LOG_FATAL,  LOG_TAG,  __VA_ARGS__)
