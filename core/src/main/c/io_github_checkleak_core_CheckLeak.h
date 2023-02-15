/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class io_github_checkleak_core_CheckLeak */

#ifndef _Included_io_github_checkleak_core_CheckLeak
#define _Included_io_github_checkleak_core_CheckLeak
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     io_github_checkleak_core_CheckLeak
 * Method:    blank
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_io_github_checkleak_core_CheckLeak_blank
  (JNIEnv *, jobject);

/*
 * Class:     io_github_checkleak_core_CheckLeak
 * Method:    forceGC
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_io_github_checkleak_core_CheckLeak_forceGC
  (JNIEnv *, jobject);

/*
 * Class:     io_github_checkleak_core_CheckLeak
 * Method:    releaseTags
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_io_github_checkleak_core_CheckLeak_releaseTags
  (JNIEnv *, jobject);

/*
 * Class:     io_github_checkleak_core_CheckLeak
 * Method:    notifyInventory
 * Signature: (ZLjava/lang/String;Ljava/lang/String;Lio/github/checkleak/core/JVMTICallBack;)V
 */
JNIEXPORT void JNICALL Java_io_github_checkleak_core_CheckLeak_notifyInventory
  (JNIEnv *, jobject, jboolean, jstring, jstring, jobject);

/*
 * Class:     io_github_checkleak_core_CheckLeak
 * Method:    getReferenceHolders
 * Signature: ([Ljava/lang/Object;)[Ljava/lang/Object;
 */
JNIEXPORT jobjectArray JNICALL Java_io_github_checkleak_core_CheckLeak_getReferenceHolders
  (JNIEnv *, jobject, jobjectArray);

/*
 * Class:     io_github_checkleak_core_CheckLeak
 * Method:    getLoadedClasses
 * Signature: ()[Ljava/lang/Class;
 */
JNIEXPORT jobjectArray JNICALL Java_io_github_checkleak_core_CheckLeak_getLoadedClasses
  (JNIEnv *, jobject);

/*
 * Class:     io_github_checkleak_core_CheckLeak
 * Method:    getAllObjects
 * Signature: (Ljava/lang/Class;)[Ljava/lang/Object;
 */
JNIEXPORT jobjectArray JNICALL Java_io_github_checkleak_core_CheckLeak_getAllObjects
  (JNIEnv *, jobject, jclass);

/*
 * Class:     io_github_checkleak_core_CheckLeak
 * Method:    getTagOnObject
 * Signature: (Ljava/lang/Object;)J
 */
JNIEXPORT jlong JNICALL Java_io_github_checkleak_core_CheckLeak_getTagOnObject
  (JNIEnv *, jobject, jobject);

/*
 * Class:     io_github_checkleak_core_CheckLeak
 * Method:    getObjectOnTag
 * Signature: (J)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_io_github_checkleak_core_CheckLeak_getObjectOnTag
  (JNIEnv *, jobject, jlong);

/*
 * Class:     io_github_checkleak_core_CheckLeak
 * Method:    getMethodName
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_io_github_checkleak_core_CheckLeak_getMethodName
  (JNIEnv *, jobject, jlong);

/*
 * Class:     io_github_checkleak_core_CheckLeak
 * Method:    getMethodSignature
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_io_github_checkleak_core_CheckLeak_getMethodSignature
  (JNIEnv *, jobject, jlong);

/*
 * Class:     io_github_checkleak_core_CheckLeak
 * Method:    getMethodClass
 * Signature: (J)Ljava/lang/Class;
 */
JNIEXPORT jclass JNICALL Java_io_github_checkleak_core_CheckLeak_getMethodClass
  (JNIEnv *, jobject, jlong);

#ifdef __cplusplus
}
#endif
#endif
