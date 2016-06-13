#include <jni.h>
#include <queue>
#include <android/bitmap.h>
#include <opencv2/imgproc.hpp>

#ifdef __cplusplus
extern "C" {
#endif



void processFrame(cv::Mat& mRgba){
    cv::Mat mGray, mGraySmall;
    cv::cvtColor(mRgba, mGray, CV_RGBA2GRAY);
    cv::Canny(mGray, mGray, 70, 200);

    cv::Mat mGray3Ch;
    cv::cvtColor(mGray, mRgba, CV_GRAY2RGBA);
}


float fps = 0;
std::queue<int64> time_queue;

void onFrame(cv::Mat& mRgba){

    int64 now = cv::getTickCount();
    int64 then;

    time_queue.push(now);

    // Process frame
    if(mRgba.cols != 0) {

        //processFrame(mRgba);

        char buffer[256];
        sprintf(buffer, "Display performance: %dx%d @ %.3f", mRgba.cols, mRgba.rows, fps);
        cv::putText(mRgba, std::string(buffer), cv::Point(8,64),
                    cv::FONT_HERSHEY_COMPLEX_SMALL, 1, cv::Scalar(0,255,255,255));
    }

    if (time_queue.size() >= 2)
        then = time_queue.front();
    else
        then = 0;

    if (time_queue.size() >= 25)
        time_queue.pop();

    fps = time_queue.size() * (float)cv::getTickFrequency() / (now-then);
}




JNIEXPORT void JNICALL
Java_ph_edu_dlsu_nativecamera_NativeCameraPreview_FrameProcessing(JNIEnv *env,
                                                                            jobject instance,
                                                                            jint width, jint height,
                                                                            jbyteArray NV21FrameData_,
                                                                            jintArray pixels_) {

    jbyte *NV21FrameData = (jbyte*)env->GetByteArrayElements(NV21FrameData_, NULL);
    jint *pixels = (jint*)env->GetIntArrayElements(pixels_, NULL);

    // byte to Mat
    cv::Mat mYuv(height + height/2, width, CV_8UC1, (uchar*)NV21FrameData);
    cv::Mat mRgba(height, width, CV_8UC4, (uchar *)pixels);
    cv::cvtColor(mYuv, mRgba, CV_YUV2BGRA_NV21);

    // onFrame(mRgba);

    env->ReleaseByteArrayElements(NV21FrameData_, NV21FrameData, 0);
    env->ReleaseIntArrayElements(pixels_, pixels, 0);
}
#ifdef __cplusplus
    }
#endif
