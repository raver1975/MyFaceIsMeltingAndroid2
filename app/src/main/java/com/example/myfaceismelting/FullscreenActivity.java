package com.example.myfaceismelting;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.jabistudio.androidjhlabs.filter.PosterizeFilter;
import com.jabistudio.androidjhlabs.filter.SwimFilter;
import com.jabistudio.androidjhlabs.filter.TransformFilter;
import com.jabistudio.androidjhlabs.filter.util.AndroidUtils;
import org.bytedeco.flycapture.FlyCapture2.CameraInfo;
import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.IplImage;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.List;

import static android.graphics.Bitmap.createScaledBitmap;
import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;


// ----------------------------------------------------------------------

public class FullscreenActivity extends Activity {


    private Preview mPreview;
    static int currentCameraId;
    static int orientation;

    public static void setCameraDisplayOrientation(int cameraId, android.hardware.Camera camera) {
        int result;
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (360-FullscreenActivity.orientation) % 360;
            if (FullscreenActivity.orientation==90){
                result = 0;
            }
            else if (FullscreenActivity.orientation==270){
                result = 90;
            }

//            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CameraManager manager = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                for (String id : manager.getCameraIdList()) {
                    CameraCharacteristics characteristics = null;
                    try {
                        characteristics = manager.getCameraCharacteristics(id);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                    orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                    System.out.println("camera sensor orientation is " + orientation);
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(FullscreenActivity.this, new String[]{android.Manifest.permission.CAMERA}, 50);
        }
        try {
            final FrameLayout layout = new FrameLayout(this);
            final FaceView faceView = new FaceView(this);
            mPreview = new Preview(this, faceView);
            faceView.preview = mPreview;

            Button buttonSnap = new Button(this);
            buttonSnap.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View view) {
                    System.out.println("snapped");
                }
            });

            ToggleButton toggleCam = new ToggleButton(this);
            toggleCam.setText("BACK");
            toggleCam.setTextOn("FRONT");
            toggleCam.setTextOff("BACK");
            toggleCam.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mPreview.mCamera.stopPreview();

//NB: if you don't release the current camera before switching, you app will crash
//                    mPreview.mCamera.release();
                    faceView.temp = null;
                    faceView.image = null;

                    Preview prev = new Preview(getApplicationContext(), faceView);
                    faceView.preview = prev;
//swap the id of the camera to be used
                    if (currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                        currentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
                    } else {
                        currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
                    }
                    prev.mCamera = prev.mCamera.open(currentCameraId);


                    setCameraDisplayOrientation(currentCameraId, prev.mCamera);
//                    setCameraDisplayOrientation(FullscreenActivity.orientation, currentCameraId, prev.mCamera);
                    try {

                        prev.mCamera.setPreviewDisplay(prev.mHolder);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    prev.mCamera.startPreview();
                    int index = layout.indexOfChild(mPreview);
                    layout.removeView(mPreview);
                    layout.addView(prev, index);
                    mPreview = prev;

                }
            });

            ToggleButton toggleOn = new ToggleButton(this);
            toggleOn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        faceView.enable = true;
                    } else {
                        faceView.enable = false;
                    }
                }
            });

//create a layout
            RelativeLayout relativeLayout1 = new RelativeLayout(this);
//             LinearLayout layoutlin2 = new LinearLayout(this);
//            relativeLayout1.setOrientation(LinearLayout.VERTICAL);
//            layoutlin2.setOrientation(LinearLayout.VERTICAL);
//            relativeLayout1.addView(layoutlin2);
//            toggle
            RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params1.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
            RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params2.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            relativeLayout1.addView(toggleOn);
            relativeLayout1.addView(toggleCam);
            toggleOn.setLayoutParams(params1);
            toggleCam.setLayoutParams(params2);

//            relativeLayout1.addView(buttonSnap);
            layout.addView(mPreview);
            layout.addView(faceView);
            layout.addView(relativeLayout1);
            setContentView(layout);
        } catch (IOException e) {
            e.printStackTrace();
            new AlertDialog.Builder(this).setMessage(e.getMessage()).create().show();
        }
    }
}

// ----------------------------------------------------------------------

class FaceView extends View implements Camera.PreviewCallback {

    public boolean enable;
    IplImage image;

    int LAPLACIAN_FILTER_SIZE = 5;
    int MEDIAN_BLUR_FILTER_SIZE = 7;
    int repetitions = 7; // Repetitions for strong cartoon effect.
    int ksize = 1; // Filter size. Has a large effect on speed.
    double sigmaColor = 9; // Filter color strength.
    double sigmaSpace = 7; // Spatial strength. Affects speed.
    private IplImage gray;
    private IplImage edges;
    IplImage temp;
    SwimFilter sf = new SwimFilter();
    SwimFilter sf1 = new SwimFilter();
    PosterizeFilter glf = new PosterizeFilter();
    private float t1;
    private float t2;
    AndroidFrameConverter converter = new AndroidFrameConverter();
    OpenCVFrameConverter openconverter = new OpenCVFrameConverter.ToIplImage();
    AndroidFrameConverter androidFrameConverter = new AndroidFrameConverter();
    Paint paint = new Paint();
    Preview preview;
//    int zwidth = 640;
//    int zheight = 480;

    public FaceView(FullscreenActivity context) throws IOException {
        super(context);
        this.preview = preview;
        sf.setAmount(20f);
        sf.setTurbulence(1f);
        sf.setEdgeAction(TransformFilter.RGB_CLAMP);
        sf1.setEdgeAction(TransformFilter.RGB_CLAMP);
        sf1.setAmount(30f);
        sf1.setTurbulence(1f);
        sf1.setScale(300);
        sf1.setStretch(50);
        glf.setNumLevels(100);
    }

    public void onPreviewFrame(byte[] data, final Camera camera) {
        try {
            int hh = preview.mCamera.getParameters().getPreviewSize().width;
            int ww = preview.mCamera.getParameters().getPreviewSize().height;
            if (FullscreenActivity.currentCameraId == 0) {
                data = rotateYUV420Degree90(data, preview.mCamera.getParameters().getPreviewSize().width, preview.mCamera.getParameters().getPreviewSize().height);
            } else {
//                hh = preview.mCamera.getParameters().getPreviewSize().width;
//                ww = preview.mCamera.getParameters().getPreviewSize().height;
                android.hardware.Camera.CameraInfo info =
                        new android.hardware.Camera.CameraInfo();
                android.hardware.Camera.getCameraInfo(FullscreenActivity.currentCameraId, info);
//                System.out.println("surface rotation:" + FullscreenActivity.orientation + "\t" + info.orientation);
                switch ((FullscreenActivity.orientation) % 360) {
                    case 0:
//                        data = rotateYUV420Degree270(data, preview.mCamera.getParameters().getPreviewSize().width, preview.mCamera.getParameters().getPreviewSize().height);
//                        data = rotateYUV420Degree180(data, preview.mCamera.getParameters().getPreviewSize().width, preview.mCamera.getParameters().getPreviewSize().height);
                        ww = preview.mCamera.getParameters().getPreviewSize().width;
                        hh = preview.mCamera.getParameters().getPreviewSize().height;
                        break;
                    case 90:
//                        data = rotateYUV420Degree90(data, preview.mCamera.getParameters().getPreviewSize().width, preview.mCamera.getParameters().getPreviewSize().height);
                        ww = preview.mCamera.getParameters().getPreviewSize().width;
                        hh = preview.mCamera.getParameters().getPreviewSize().height;
                        break;
                    case 180:
                        data = rotateYUV420Degree180(data, preview.mCamera.getParameters().getPreviewSize().width, preview.mCamera.getParameters().getPreviewSize().height);
                        ww = preview.mCamera.getParameters().getPreviewSize().width;
                        hh = preview.mCamera.getParameters().getPreviewSize().height;
                        break;
                    case 270:
                        data = rotateYUV420Degree270(data, preview.mCamera.getParameters().getPreviewSize().width, preview.mCamera.getParameters().getPreviewSize().height);
                        hh = preview.mCamera.getParameters().getPreviewSize().width;
                        ww = preview.mCamera.getParameters().getPreviewSize().height;
                        break;
                }

            }

            processImage(data, ww, hh);
        } catch (ArrayIndexOutOfBoundsException e) {
            //e.printStackTrace();
            // The camera has probably just been released, ignore.
        }
        camera.addCallbackBuffer(data);

    }

    private byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        // Rotate the Y luma
        int i = 0;
        for (int x = 0; x < imageWidth; x++) {
            for (int y = imageHeight - 1; y >= 0; y--) {
                yuv[i] = data[y * imageWidth + x];
                i++;
            }
        }
        // Rotate the U and V color components
        i = imageWidth * imageHeight * 3 / 2 - 1;
        for (int x = imageWidth - 1; x > 0; x = x - 2) {
            for (int y = 0; y < imageHeight / 2; y++) {
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
                i--;
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + (x - 1)];
                i--;
            }
        }
        return yuv;
    }

    private static byte[] rotateYUV420Degree180(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        int i = 0;
        int count = 0;
        for (i = imageWidth * imageHeight - 1; i >= 0; i--) {
            yuv[count] = data[i];
            count++;
        }
        i = imageWidth * imageHeight * 3 / 2 - 1;
        for (i = imageWidth * imageHeight * 3 / 2 - 1; i >= imageWidth
                * imageHeight; i -= 2) {
            yuv[count++] = data[i - 1];
            yuv[count++] = data[i];
        }
        return yuv;
    }

    public static byte[] rotateYUV420Degree270(byte[] data, int imageWidth,
                                               int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        int nWidth = 0, nHeight = 0;
        int wh = 0;
        int uvHeight = 0;
        if (imageWidth != nWidth || imageHeight != nHeight) {
            nWidth = imageWidth;
            nHeight = imageHeight;
            wh = imageWidth * imageHeight;
            uvHeight = imageHeight >> 1;// uvHeight = height / 2
        }
        // ??Y
        int k = 0;
        for (int i = 0; i < imageWidth; i++) {
            int nPos = 0;
            for (int j = 0; j < imageHeight; j++) {
                yuv[k] = data[nPos + i];
                k++;
                nPos += imageWidth;
            }
        }
        for (int i = 0; i < imageWidth; i += 2) {
            int nPos = wh;
            for (int j = 0; j < uvHeight; j++) {
                yuv[k] = data[nPos + i];
                yuv[k + 1] = data[nPos + i + 1];
                k += 2;
                nPos += imageWidth;
            }
        }
        return rotateYUV420Degree180(yuv, imageWidth, imageHeight);
    }

    protected void processImage(byte[] data, int width, int height) {
        if (!enable || width == 0 || height == 0) return;
        // First, downsample our image and convert it into a grayscale IplImage

        if (image == null || image.width() != width || image.height() != height) {
            image = IplImage.create(width, height, IPL_DEPTH_8U, 3);
            gray = IplImage.create(image.cvSize(), IPL_DEPTH_8U, 1);
            edges = IplImage.create(gray.cvSize(), gray.depth(), gray.nChannels());
            temp = IplImage.create(image.cvSize(), image.depth(), image.nChannels());
        }

        image = openconverter.convertToIplImage(converter.convert(data, width, height));

        ByteBuffer buffer = image.getByteBuffer();
        int[] img_vec = new int[image.width() * image.height()];
//
        for (int i = 0; i < image.width(); i++) {
            for (int j = 0; j < image.height(); j++) {
                img_vec[i * image.height() + j] = 0xFF << 24 | ((buffer.get() & 0xFF)) << 16 | ((buffer.get() & 0xFF) << 8) | ((buffer.get() & 0xFF));
            }
        }


        int[] swim1Ints2 = sf.filter(img_vec, image.width(), image.height());
        int[] swim1Ints3 = sf1.filter(swim1Ints2, image.width(), image.height());
        Bitmap bitmap1 = Bitmap.createBitmap(image.width(), image.height(), Bitmap.Config.ARGB_8888);
        bitmap1.copyPixelsFromBuffer(IntBuffer.wrap(swim1Ints3));
        IplImage image1 = openconverter.convertToIplImage(androidFrameConverter.convert(bitmap1));
        cvCvtColor(image1, image, CV_RGBA2BGR);
        cvCvtColor(image, gray, CV_BGR2GRAY);
        cvSmooth(gray, gray, CV_MEDIAN, MEDIAN_BLUR_FILTER_SIZE, 0, 0, 0);
        cvLaplace(gray, edges, LAPLACIAN_FILTER_SIZE);
        cvThreshold(edges, edges, 80, 255, CV_THRESH_BINARY_INV);
        temp = IplImage.create(image.cvSize(), image.depth(), image.nChannels());
        for (int i = 0; i < repetitions; i++) {
            cvSmooth(image, temp, CV_BILATERAL, ksize, 0, sigmaColor, sigmaSpace);
            cvSmooth(temp, image, CV_BILATERAL, ksize, 0, sigmaColor, sigmaSpace);
        }
        cvZero(temp);
        cvCopy(image, temp, edges);
        sf.setTime(t1 += .02f);
        sf1.setTime(t2 += .02f);
    }


    @Override
    protected void onDraw(Canvas canvas) {

        paint.setColor(Color.RED);
        paint.setTextSize(20);

//        String s = "FacePreview - This side up." + Math.random();
//        float textWidth = paint.measureText(s);
//        canvas.drawText(s, (getWidth() - textWidth) / 2, 20, paint);
        if (enable && temp != null && !temp.isNull() && temp.width() > 0 && temp.height() > 0) {
            paint.setColor(Color.WHITE);
            Bitmap bitmap = converter.convert(openconverter.convert(temp));
            /*//---------------------------
            int[] swim1Ints1 = AndroidUtils.bitmapToIntArray(bitmap);
            int[] swim1Ints2 = glf.filter(swim1Ints1, bitmap.getWidth(), bitmap.getHeight());
            bitmap.copyPixelsFromBuffer(IntBuffer.wrap(swim1Ints2));
            //---------------------------*/
            bitmap = createScaledBitmap(bitmap, getWidth(), getHeight(), true);
//            android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
//            android.hardware.Camera.getCameraInfo(FullscreenActivity.currentCameraId, info);
//            Matrix m = new Matrix();
//            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
////                canvas.scale(-1,-1);
////                canvas.drawBitmap(bitmap, 0, 0, paint);
//// Mirror is basically a rotation
//                m.setScale(-1, -1);
//// so you got to move your bitmap back to it's place. otherwise you will not see it
//                m.postTranslate(getWidth(), getHeight());
//            } else {
////                canvas.scale(1,1);
//            }
            Matrix flipHorizontalMatrix = new Matrix();
            if (FullscreenActivity.currentCameraId == 1&&FullscreenActivity.orientation!=270) {
                flipHorizontalMatrix.setScale(-1, 1);
                flipHorizontalMatrix.postTranslate(canvas.getWidth(), 0);
            }
            canvas.drawBitmap(bitmap, flipHorizontalMatrix, paint);

        }
        this.invalidate();
    }

}

// ----------------------------------------------------------------------

class Preview extends SurfaceView implements SurfaceHolder.Callback {
    SurfaceHolder mHolder;
    Camera mCamera;
    Camera.PreviewCallback previewCallback;

    Preview(Context context, Camera.PreviewCallback previewCallback) {
        super(context);
        this.previewCallback = previewCallback;
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = Camera.open(FullscreenActivity.currentCameraId);
        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException exception) {
            mCamera.release();
            mCamera = null;
            // TODO: add more exception handling logic here
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        // Because the CameraDevice object is not a shared resource, it's very
        // important to release it when the activity is paused.
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w / h;
        if (sizes == null)
            return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - h) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - h);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - h) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - h);
                }
            }
        }
        return optimalSize;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        Camera.Parameters parameters = mCamera.getParameters();
        List<Size> sizes = parameters.getSupportedPreviewSizes();
        Size optimalSize = getOptimalPreviewSize(sizes, 640, 480);
//        Size optimalSize = getOptimalPreviewSize(sizes, 0,0);
        parameters.setPreviewSize(optimalSize.width, optimalSize.height);
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        int rotation = wm.getDefaultDisplay().getRotation();
        rotation = FullscreenActivity.orientation;
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(FullscreenActivity.currentCameraId, info);
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (rotation) % 360;
//            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - rotation + 360) % 360;
        }
        parameters.setRotation(result);
        mCamera.setParameters(parameters);
        FullscreenActivity.setCameraDisplayOrientation(FullscreenActivity.currentCameraId, mCamera);
        if (previewCallback != null) {
            mCamera.setPreviewCallbackWithBuffer(previewCallback);
            Size size = parameters.getPreviewSize();
            byte[] data = new byte[size.width * size.height * ImageFormat.getBitsPerPixel(parameters.getPreviewFormat()) / 8];
            mCamera.addCallbackBuffer(data);
        }
        mCamera.startPreview();
        mCamera.autoFocus(null);
    }

}