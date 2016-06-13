package ph.edu.dlsu.nativecamera;

import android.app.Activity;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.util.List;

public class MainActivity extends Activity {

    private int PreviewSizeWidthWanted = 1280;
    private int PreviewSizeHeightWanted = 720;

    private int PreviewSizeWidth, PreviewSizeHeight;

    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native_processing");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int[] wPreview;
        int[] hPreview;

        Camera mCamera = Camera.open();

        // get available preview sizes
        List<Camera.Size> lPreview = mCamera.getParameters().getSupportedPreviewSizes();
        wPreview = new int[lPreview.size()];
        hPreview = new int[lPreview.size()];
        for (int i = 0; i < lPreview.size(); i++) {
            wPreview[i] = lPreview.get(i).width;
            hPreview[i] = lPreview.get(i).height;
        }

        // get optimal preview size
        setOptimalPreviewResolution(wPreview, hPreview);

        // set full screen and remove title
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        // get dimensions of window
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int widthWindow = size.x;
        int heightWindow = size.y;

        // adjust size of views so that the full image is on the screen and centered
        int widthView;
        int heightView;
        if (PreviewSizeHeight * widthWindow < PreviewSizeWidth * heightWindow) {
            widthView = widthWindow;
            heightView = (int) (widthView * (float) PreviewSizeHeight / (float) PreviewSizeWidth);
        } else {
            heightView = heightWindow;
            widthView = (int) (heightView * (float) PreviewSizeWidth / (float) PreviewSizeHeight);
        }

        // create camera preview
        ImageView mImageView = new ImageView(this);
        SurfaceView camView = new SurfaceView(this);
        SurfaceHolder camHolder = camView.getHolder();

        NativeCameraPreview camPreview = new NativeCameraPreview(PreviewSizeWidth, PreviewSizeHeight, mImageView, mCamera);

        camHolder.addCallback(camPreview);

        // add views to layout
        RelativeLayout mainLayout = (RelativeLayout) findViewById(R.id.frame);

        RelativeLayout.LayoutParams layoutParamsCam = new RelativeLayout.LayoutParams(1, 1);
        RelativeLayout.LayoutParams layoutParamsImg = new RelativeLayout.LayoutParams(widthView, heightView);
        layoutParamsImg.addRule(RelativeLayout.CENTER_IN_PARENT);
        mainLayout.addView(camView, 0, layoutParamsCam);
        mainLayout.addView(mImageView, 1, layoutParamsImg);
    }

    public void setOptimalPreviewResolution(int[] wPreview, int[] hPreview) {
        // we find the best available preview resolution given the inputs in PreviewSizeWidthWanted and PreviewSizeHeightWanted

        PreviewSizeWidth = 0;
        PreviewSizeHeight = 0;

        for (int i = 0; i < wPreview.length; i++) {
            if ((wPreview[i] <= PreviewSizeWidthWanted) && (hPreview[i] <= PreviewSizeHeightWanted)) {
                if (PreviewSizeHeight * PreviewSizeWidth < wPreview[i] * hPreview[i]) {
                    PreviewSizeWidth = wPreview[i];
                    PreviewSizeHeight = hPreview[i];
                }
            }
        }
    }
}