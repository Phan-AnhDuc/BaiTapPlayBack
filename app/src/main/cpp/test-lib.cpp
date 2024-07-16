//
// Created by QuangNH on 02/11/2022.
//

#include <jni.h>
#include <string>

using namespace std;

extern "C" JNIEXPORT jstring JNICALL

Java_com_example_cameraplayback_utils_CryptoAES_getKeyTest(
        JNIEnv* env,
        jobject /* this */) {
    string key = "Ii7ky=PaM^:UrZXSk3]9:UZIKMsE";
    unsigned int len = key.length();

    for(int i = 0; i < len; i++) {
        key[i] = key[i] - 8;
    }

    return env->NewStringUTF(key.c_str());
}