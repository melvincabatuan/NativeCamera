package ph.edu.dlsu.nativecamera;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.ImageView;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Created by cobalt on 6/13/16.
 */

public final class NativeCameraPreview implements SurfaceHolder.Callback, Camera.PreviewCallback
{
    private Camera mCamera = null;
    private ImageView mImageView = null;
    private Bitmap mBitmap = null;
    private int[] pixels = null;
    private byte[] frameData = null;
    private int imageFormat;
    private int PreviewSizeWidth;
    private int PreviewSizeHeight;
    private boolean bProcessing = false;

    private Queue<Long> mQueueTime = new LinkedList<Long>();

    Handler mHandler = new Handler(Looper.getMainLooper());

    public NativeCameraPreview(int PreviewlayoutWidth, int PreviewlayoutHeight,
                         ImageView imageView, Camera camera)
    {
        PreviewSizeWidth = PreviewlayoutWidth;
        PreviewSizeHeight = PreviewlayoutHeight;
        mImageView = imageView;
        mCamera = camera;

        mBitmap = Bitmap.createBitmap(PreviewSizeWidth, PreviewSizeHeight, Bitmap.Config.ARGB_8888);
        pixels = new int[PreviewSizeWidth * PreviewSizeHeight];
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera)
    {
        // At preview mode, the frame data will push to here.
        if (imageFormat == ImageFormat.NV21)
        {
            //We only accept the NV21(YUV420) format.
            if (!bProcessing){
                bProcessing = true;

                frameData = data;

                mHandler.post(DoImageProcessing);
            }
        }
    }

    public void onPause()
    {
        mCamera.stopPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3)
    {
        Camera.Parameters parameters;

        parameters = mCamera.getParameters();

        // Set the camera preview size
        parameters.setPreviewSize(PreviewSizeWidth, PreviewSizeHeight);

        // Set FPS to the max
        List<int[]> fpsRanges = parameters.getSupportedPreviewFpsRange();
        parameters.setPreviewFpsRange(fpsRanges.get(fpsRanges.size()-1)[0], fpsRanges.get(fpsRanges.size()-1)[1]);

        // Set focus mode
        String focusMode = parameters.getFocusMode ();
        if ( focusMode != null && parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) )
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

        mImageView.setImageBitmap(mBitmap);

        mCamera.setParameters(parameters);

        mCamera.startPreview();
    }

    @Override
    public void surfaceCreated(SurfaceHolder arg0)
    {
        if(mCamera == null)
            mCamera = Camera.open(0);
        try
        {
            imageFormat = mCamera.getParameters().getPreviewFormat();

            // If did not set the SurfaceHolder, the preview area will be black.
            mCamera.setPreviewDisplay(arg0);
            mCamera.setPreviewCallbackWithBuffer(this);

            // Pre-allocate 3 buffers for the camera frames
            for(int i=0; i<3; i++){
                byte [] cameraBuffer = new byte[PreviewSizeWidth * PreviewSizeHeight * ImageFormat.getBitsPerPixel(imageFormat) / 8];
                mCamera.addCallbackBuffer(cameraBuffer);
            }
        }
        catch (IOException e)
        {
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0)
    {
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    //
    // Native JNI
    //
    private native void FrameProcessing(int width, int height,
                                        byte[] NV21FrameData, int [] pixels);

    private Runnable DoImageProcessing = new Runnable()
    {
        public void run()
        {
            Log.i("native_processing", "DoFrameProcessing()");

            FrameProcessing(PreviewSizeWidth, PreviewSizeHeight, frameData, pixels);

            mCamera.addCallbackBuffer(frameData);

            mBitmap.setPixels(pixels, 0, PreviewSizeWidth, 0, 0, PreviewSizeWidth, PreviewSizeHeight);
            mImageView.setImageBitmap(mBitmap);
            bProcessing = false;
        }
    };
}