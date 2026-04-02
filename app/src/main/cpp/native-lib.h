#ifndef MOUSELOCK_NATIVE_LIB_H
#define MOUSELOCK_NATIVE_LIB_H

#include <jni.h>

extern "C" JNIEXPORT void JNICALL
Java_com_mouselock_app_MouseService_nativeStartLock(JNIEnv* env, jobject thiz, jstring shPath);

extern "C" JNIEXPORT void JNICALL
Java_com_mouselock_app_MouseService_nativeStopLock(JNIEnv* env, jobject thiz);

extern "C" JNIEXPORT void JNICALL
Java_com_mouselock_app_MouseService_nativeSetLocked(JNIEnv* env, jobject thiz, jboolean locked);

#endif //MOUSELOCK_NATIVE_LIB_H