// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("bscm");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("bscm")
//      }
//    }

#include <jni.h>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_meninocoiso_bscm_util_KeystoreUtils_getApiSecret(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF("07c842564361cc54bb78637d71504e7a86ebbb2384aee0b72903d81a523a17ca");
}