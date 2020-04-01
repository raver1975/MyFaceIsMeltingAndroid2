package com.example.myfaceismelting;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.*;
import android.widget.*;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.jabistudio.androidjhlabs.filter.PosterizeFilter;
import com.jabistudio.androidjhlabs.filter.SwimFilter;
import com.jabistudio.androidjhlabs.filter.TransformFilter;
import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point2f;


import java.io.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.graphics.Bitmap.createScaledBitmap;
import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imdecode;
import static org.bytedeco.opencv.global.opencv_imgproc.*;


// ----------------------------------------------------------------------

public class FullscreenActivity extends Activity {


    public static int cameraRotation;
    public static ProgressBar pbar;
    private Preview mPreview;
    static int currentCameraId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(FullscreenActivity.this, new String[]{android.Manifest.permission.CAMERA}, 50);
        }
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(FullscreenActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 50);
        }
        try {
            final FrameLayout layout = new FrameLayout(this);
            final FaceView faceView = new FaceView(this);
            mPreview = new Preview(this, faceView);
            mPreview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });
            faceView.preview = mPreview;


            faceView.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View view) {
                    pbar.setVisibility(View.VISIBLE);
                    new Thread(new Runnable() {
                        //                                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {

                            mPreview.mCamera.takePicture(new Camera.ShutterCallback() {
                                                             @Override
                                                             public void onShutter() {
                                                             }
                                                         }
                                    , new Camera.PictureCallback() {
                                        @Override
                                        public void onPictureTaken(byte[] bytes, Camera camera) {
                                        }
                                    }, new Camera.PictureCallback() {
                                        @Override
                                        public void onPictureTaken(byte[] bytes, Camera camera) {
                                            System.out.println("image coming in");
                                            boolean enableMemory = faceView.enable;
                                            faceView.enable = true;
                                            Mat ma = new Mat(bytes);
                                            faceView.processImage1(new IplImage(imdecode(ma, -1)), camera.getParameters().getPictureSize().width, camera.getParameters().getPictureSize().height);
                                            faceView.enable = enableMemory;
//                                            System.out.println("2" + faceView.outputImage);
                                            System.out.println("snapped");

                                            mPreview.mCamera.startPreview();
                                            new Handler().post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    pbar.setVisibility(View.INVISIBLE);
                                                }
                                            });

                                        }
                                    });


                        }
                    }).start();

                }
            });

            ToggleButton toggleCam = new ToggleButton(this);
//            toggleCam.setText("FRONT");
//            toggleCam.setTextOn("BACK");
//            toggleCam.setTextOff("FRONT");
            toggleCam.setText("FRONT");
//            toggleCam.setTextOn("FRONT");
//            toggleCam.setTextOff("BACK");
            toggleCam.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mPreview.mCamera.stopPreview();

//NB: if you don't release the current camera before switching, you app will crash
//                    mPreview.mCamera.release();
                    faceView.outputImage = null;
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

//                    prev.mCamera.setDisplayOrientation(cameraRotation);


//                    System.out.println("setting camera rotation:"+FullscreenActivity.cameraRotation);
//                    prev.mCamera.setDisplayOrientation(FullscreenActivity.cameraRotation);
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
                    //prev.surfaceChanged();
                }
            });

            ToggleButton toggleOn = new ToggleButton(this);
            toggleOn.setText("MELT");
//            toggleOn.setTextOn("MELT");
//            toggleOn.setTextOff("OFF");
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
            params1.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
            RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params2.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
            params2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
            RelativeLayout.LayoutParams params3 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params3.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.CENTER_IN_PARENT);
            pbar = new ProgressBar(this);
            pbar.setMax(10);
            pbar.setVisibility(View.INVISIBLE);
            pbar.setLayoutParams(params3);
            relativeLayout1.addView(toggleOn);
            relativeLayout1.addView(toggleCam);
            relativeLayout1.addView(pbar);
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
    boolean pictureLockTemp;
    IplImage image;

    private static int LAPLACIAN_FILTER_SIZE = 5;
    static int MEDIAN_BLUR_FILTER_SIZE = 7;
    static int repetitions = 7; // Repetitions for strong cartoon effect.
    static int ksize = 1; // Filter size. Has a large effect on speed.
    static double sigmaColor = 9; // Filter color strength.
    static double sigmaSpace = 7; // Spatial strength. Affects speed.
    private IplImage gray;
    private IplImage edges;
    IplImage outputImage;
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
        sf.setAmount(8f);
        sf.setTurbulence(1f);
        sf.setEdgeAction(TransformFilter.RGB_CLAMP);
//        sf.setScale(10);
//        sf.setStretch(10);
        sf1.setEdgeAction(TransformFilter.RGB_CLAMP);
        sf1.setAmount(30f);
        sf1.setTurbulence(1f);
        sf1.setScale(300);
        sf1.setStretch(50);
        glf.setNumLevels(100);
    }

    public void onPreviewFrame(byte[] data, final Camera camera) {
        try {
            int ww = camera.getParameters().getPreviewSize().width;
            int hh = camera.getParameters().getPreviewSize().height;
//            boolean landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
//            boolean landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
            processImage(data, ww, hh);
        } catch (RuntimeException e) {
            System.out.println("----------------------------------");
            e.printStackTrace();
            //e.printStackTrace();
            // The camera has probably just been released, ignore.
        }
        camera.addCallbackBuffer(data);

    }

    private static byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight) {
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

    synchronized void processImage(byte[] data, int width, int height) {
        if (!enable || width == 0 || height == 0) return;
        int rotate = FullscreenActivity.cameraRotation;
        int hh = width;
        int ww = height;
        switch ((360 + rotate) % 360) {
            case 0:
                ww = width;
                hh = height;
                break;
            case 90:
                hh = width;
                ww = height;
                data = rotateYUV420Degree90(data, width, height);
                break;
            case 180:
                ww = width;
                hh = height;
                data = rotateYUV420Degree180(data, width, height);

                break;
            case 270:
                hh = width;
                ww = height;
                data = rotateYUV420Degree270(data, width, height);
                break;
        }
        width = ww;
        height = hh;
        if (image == null || image.width() != width || image.height() != height) {
            gray = IplImage.create(width, height, IPL_DEPTH_8U, 1);
            edges = IplImage.create(gray.cvSize(), gray.depth(), gray.nChannels());
            outputImage = IplImage.create(width, height, IPL_DEPTH_8U, 3);
        }
        image = openconverter.convertToIplImage(converter.convert(data, width, height));
        ByteBuffer buffer = image.getByteBuffer();
        int[] img_vec = new int[image.width() * image.height()];
        for (
                int i = 0; i < image.width(); i++) {
            for (int j = 0; j < image.height(); j++) {
                img_vec[i * image.height() + j] = 0xFF << 24 | ((buffer.get() & 0xFF)) << 16 | ((buffer.get() & 0xFF) << 8) | ((buffer.get() & 0xFF));
            }
        }
        int[] swim1Ints2 = sf.filter(img_vec, image.width(), image.height());
        int[] swim1Ints3 = sf1.filter(swim1Ints2, image.width(), image.height());
        Bitmap bitmap1 = Bitmap.createBitmap(image.width(), image.height(), Bitmap.Config.ARGB_8888);
        bitmap1.copyPixelsFromBuffer(IntBuffer.wrap(swim1Ints3));
        cvCvtColor(openconverter.convertToIplImage(androidFrameConverter.convert(bitmap1)), image, CV_RGBA2BGR);
        cvCvtColor(image, gray, CV_BGR2GRAY);
        cvSmooth(gray, gray, CV_MEDIAN, MEDIAN_BLUR_FILTER_SIZE, 0, 0, 0);
        cvLaplace(gray, edges, LAPLACIAN_FILTER_SIZE);
        cvThreshold(edges, edges, 80, 255, CV_THRESH_BINARY_INV);
        outputImage = IplImage.create(image.cvSize(), image.depth(), image.nChannels());
        for (
                int i = 0;
                i < repetitions; i++) {
            cvSmooth(image, outputImage, CV_BILATERAL, ksize, 0, sigmaColor, sigmaSpace);
            cvSmooth(outputImage, image, CV_BILATERAL, ksize, 0, sigmaColor, sigmaSpace);
        }
        cvZero(outputImage);
        cvCopy(image, outputImage, edges);
        sf.setTime(t1 += .02f);
        sf1.setTime(t2 += .02f);
    }

    @SuppressLint("WrongThread")
    synchronized void processImage1(IplImage image, int height, int width) {
        int rotate = FullscreenActivity.cameraRotation;
        int hh = width;
        int ww = height;
        switch ((360 + rotate) % 360) {
            case 0:
                ww = width;
                hh = height;
                break;
            case 90:
                hh = width;
                ww = height;
                Mat rotation_matrix = getRotationMatrix2D(new Point2f(ww / 2, hh / 2), 90, 1.0);
                Mat ma = new Mat(image);
                Mat ma1 = new Mat();
                warpAffine(ma, ma1, rotation_matrix, new org.bytedeco.opencv.opencv_core.Size(image.width(), image.height()));
                image = new IplImage(ma1);
                break;
            case 180:
                ww = width;
                hh = height;
                rotation_matrix = getRotationMatrix2D(new Point2f(ww / 2, hh / 2), 180, 1.0);
                ma = new Mat(image);
                ma1 = new Mat();
                warpAffine(ma, ma1, rotation_matrix, new org.bytedeco.opencv.opencv_core.Size(image.width(), image.height()));
                image = new IplImage(ma1);
                break;
            case 270:
                hh = width;
                ww = height;
                rotation_matrix = getRotationMatrix2D(new Point2f(ww / 2, hh / 2), 270, 1.0);
                ma = new Mat(image);
                ma1 = new Mat();
                warpAffine(ma, ma1, rotation_matrix, new org.bytedeco.opencv.opencv_core.Size(image.width(), image.height()));
                image = new IplImage(ma1);
                break;
        }
        width = ww;
        height = hh;
        IplImage gray = IplImage.create(image.cvSize(), image.depth(), 1);
        IplImage edges = IplImage.create(gray.cvSize(), gray.depth(), gray.nChannels());
        IplImage outputImage = IplImage.create(image.cvSize(), image.depth(), 3);
        IplImage temp = IplImage.create(image.cvSize(), image.depth(), 3);

        ByteBuffer buffer = image.getByteBuffer();
        int[] img_vec = new int[image.width() * image.height()];
        for (
                int i = 0; i < image.width(); i++) {
            for (int j = 0; j < image.height(); j++) {
                img_vec[i * image.height() + j] = 0xFF << 24 | ((buffer.get() & 0xFF)) << 16 | ((buffer.get() & 0xFF) << 8) | ((buffer.get() & 0xFF));
            }
        }
        int[] swim1Ints2 = sf.filter(img_vec, image.width(), image.height());
        int[] swim1Ints3 = sf1.filter(swim1Ints2, image.width(), image.height());
        Bitmap bitmap1 = Bitmap.createBitmap(image.width(), image.height(), Bitmap.Config.ARGB_8888);
        bitmap1.copyPixelsFromBuffer(IntBuffer.wrap(swim1Ints3));
        cvCvtColor(openconverter.convertToIplImage(androidFrameConverter.convert(bitmap1)), image, CV_RGBA2BGR);
        cvCvtColor(image, gray, CV_BGR2GRAY);
        cvSmooth(gray, gray, CV_MEDIAN, MEDIAN_BLUR_FILTER_SIZE, 0, 0, 0);
        cvLaplace(gray, edges, LAPLACIAN_FILTER_SIZE);
        cvThreshold(edges, edges, 80, 255, CV_THRESH_BINARY_INV);
        outputImage = IplImage.create(image.cvSize(), image.depth(), image.nChannels());
        cvCopy(image,temp);
        for (
                int i = 0;
                i < repetitions; i++) {
            cvSmooth(temp, outputImage, CV_BILATERAL, ksize, 0, sigmaColor, sigmaSpace);
            cvSmooth(outputImage, temp, CV_BILATERAL, ksize, 0, sigmaColor, sigmaSpace);
        }
        cvZero(outputImage);
        cvCopy(temp, outputImage, edges);
        sf.setTime(t1 += .02f);
        sf1.setTime(t2 += .02f);
        ByteBuffer buffer1 = outputImage.getByteBuffer();
        img_vec = new int[outputImage.width() * outputImage.height()];
        for (
                int i = 0; i < outputImage.width(); i++) {
            for (int j = 0; j < outputImage.height(); j++) {
                img_vec[i * outputImage.height() + j] = 0xFF << 24 | ((buffer1.get() & 0xFF)) << 16 | ((buffer1.get() & 0xFF) << 8) | ((buffer1.get() & 0xFF));
            }
        }

        glf.filter(img_vec, outputImage.width(), outputImage.height());
        bitmap1 = Bitmap.createBitmap(outputImage.width(), outputImage.height(), Bitmap.Config.ARGB_8888);
        bitmap1.copyPixelsFromBuffer(IntBuffer.wrap(img_vec));
        System.out.println("sharing");
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("image/jpeg");
        Bitmap icon = converter.convert(openconverter.convert(outputImage));
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "title");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        Uri uri = getContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        OutputStream outstream;
        try {
            outstream = getContext().getContentResolver().openOutputStream(uri);
            icon.compress(Bitmap.CompressFormat.JPEG, 100, outstream);
            outstream.close();
        } catch (Exception e) {
            System.err.println(e.toString());
        }

        share.putExtra(Intent.EXTRA_STREAM, uri);
        Intent chooserIntent = Intent.createChooser(share, "Open With");
        chooserIntent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        getContext().getApplicationContext().startActivity(chooserIntent);
    }


    @Override
    protected void onDraw(Canvas canvas) {

        paint.setColor(Color.RED);
        paint.setTextSize(20);

//        String s = "FacePreview - This side up." + Math.random();
//        float textWidth = paint.measureText(s);
//        canvas.drawText(s, (getWidth() - textWidth) / 2, 20, paint);
        if (enable && outputImage != null && !outputImage.isNull() && outputImage.width() > 0 && outputImage.height() > 0) {
            paint.setColor(Color.WHITE);
            Bitmap bitmap = converter.convert(openconverter.convert(outputImage));
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
//            Matrix flipHorizontalMatrix = new Matrix();
//            if (FullscreenActivity.currentCameraId == 1) {
//                flipHorizontalMatrix.setScale(-1, 1);
//                flipHorizontalMatrix.postTranslate(canvas.getWidth(), 0);
//            }
            try {
                final Matrix bitmapMatrix = new Matrix();

                switch (preview.exifOrientation) {
                    case 1:
                        break;  // top left
                    case 2:
                        bitmapMatrix.postScale(-1, 1);
                        break;  // top right
                    case 3:
                        bitmapMatrix.postRotate(180);
                        break;  // bottom right
                    case 4:
                        bitmapMatrix.postRotate(180);
                        bitmapMatrix.postScale(-1, 1);
                        break;  // bottom left
                    case 5:
                        bitmapMatrix.postRotate(90);
                        bitmapMatrix.postScale(-1, 1);
                        break;  // left top
                    case 6:
                        bitmapMatrix.postRotate(90);
                        break;  // right top
                    case 7:
                        bitmapMatrix.postRotate(270);
                        bitmapMatrix.postScale(-1, 1);
                        break;  // right bottom
                    case 8:
                        bitmapMatrix.postRotate(270);
                        break;  // left bottom
                    default:
                        break;  // Unknown
                }

                if (FullscreenActivity.currentCameraId == 1) {
//                    bitmapMatrix.preRotate(270);
                    bitmapMatrix.postScale(-1, 1);
                    bitmapMatrix.postTranslate(canvas.getWidth(), 0);
                } else {
//                    bitmapMatrix.preRotate(90);
                }

                // Create new bitmap.
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), bitmapMatrix, false);
                bitmap = createScaledBitmap(bitmap, getWidth(), getHeight(), true);
                canvas.drawBitmap(bitmap, 0, 0, paint);
//                canvas.drawBitmap(transformedBitmap, flipHorizontalMatrix, paint);
            } catch (Exception e) {
                // TODO: handle exception
            }

        }
        this.invalidate();
    }


}

// ----------------------------------------------------------------------

class Preview extends SurfaceView implements SurfaceHolder.Callback {
    SurfaceHolder mHolder;
    Camera mCamera;
    Camera.PreviewCallback previewCallback;
    int exifOrientation;
    Integer sensorOrientation;
    int deviceOrientation;

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
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
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

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    public void surfaceChanged() {
        surfaceChanged(getHolder(), 0, 320, 240);
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        Camera.Parameters parameters = mCamera.getParameters();
        List<Size> sizes = parameters.getSupportedPreviewSizes();
        Size optimalSize = getOptimalPreviewSize(sizes, 320, 240);
//        Size optimalSize = getOptimalPreviewSize(sizes, 0,0);
        parameters.setPreviewSize(optimalSize.width, optimalSize.height);
//        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
//        int rotation = wm.getDefaultDisplay().getRotation();
//        rotation = FullscreenActivity.orientation;
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(FullscreenActivity.currentCameraId, info);
//        boolean landscape = this.getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        CameraManager manager = (CameraManager) this.getContext().getSystemService(Context.CAMERA_SERVICE);
        System.out.println("---------------------------");
        final int deviceRotation = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        deviceOrientation = ORIENTATIONS.get(deviceRotation);
//            Point displaySize = new Point();
//            activity.getWindowManager().getDefaultDisplay().getSize(displaySize);

        // Find the rotation of the device relative to the camera sensor's orientation.
//            int totalRotation = sensorToDeviceRotation(mCharacteristics, deviceRotation);


        // Get device orientation in degrees
//            int deviceOrientation = ORIENTATIONS.get(deviceOrientation);

        try {
            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics characteristics;
                characteristics = manager.getCameraCharacteristics(id);
                if (Integer.parseInt(id) == FullscreenActivity.currentCameraId) {
                    sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                    System.out.println("------------------" + id + "\t" + sensorOrientation);
                    if (manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                        deviceOrientation -= deviceOrientation;
                        sensorOrientation -= 90;
                    }

//                    System.out.println((sensorOrientation + deviceOrientation + 360) % 360);
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        mCamera.setParameters(parameters);
        if (previewCallback != null) {
            mCamera.setPreviewCallbackWithBuffer(previewCallback);
            Size size = parameters.getPreviewSize();
            byte[] data = new byte[size.width * size.height * ImageFormat.getBitsPerPixel(parameters.getPreviewFormat()) / 8];
            mCamera.addCallbackBuffer(data);
        }
        mCamera.startPreview();
        try {
            mCamera.autoFocus(null);
        } catch (Exception e) {
        }
        FullscreenActivity.cameraRotation = (360 + sensorOrientation - deviceOrientation) % 360;
        System.out.println("sensor:" + sensorOrientation + " device:" + deviceOrientation + " Setting camera rotation to:" + FullscreenActivity.cameraRotation);
        mCamera.setDisplayOrientation(FullscreenActivity.cameraRotation);
        /*mCamera.takePicture(new Camera.ShutterCallback() {
                                @Override
                                public void onShutter() {

                                }

                            }
                , new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] bytes, Camera camera) {


                    }
                }, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] imageData, Camera camera) {
//                                BitmapFactory.Options opt=new BitmapFactory.Options();
//                                Bitmap bm=BitmapFactory.decodeByteArray(data, 0, data.length,opt);
                        try {
                            // Extract metadata.
                            Metadata metadata = ImageMetadataReader.readMetadata(new BufferedInputStream(new ByteArrayInputStream(imageData)), imageData.length);

                            // Log each directory.
                            for (Directory directory : metadata.getDirectories()) {
                                Log.d("LOG", "Directory: " + directory.getName());

                                // Log all errors.
                                for (String error : directory.getErrors()) {
                                    Log.d("LOG", "> error: " + error);
                                }

                                // Log all tags.
                                for (Tag tag : directory.getTags()) {
                                    Log.d("LOG", "> tag: " + tag.getTagName() + " = " + tag.getDescription());
                                }
                            }

                            final ExifIFD0Directory exifIFD0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
                            if (exifIFD0Directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                                exifOrientation = exifIFD0Directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
                                System.out.println("-----------------orientation:" + exifOrientation);
                            }
                            FullscreenActivity.cameraRotation = 0;
                            switch (exifOrientation) {
                                case 1:
                                    break;  // top left
                                case 2:
                                    break;  // top right
                                case 3:
                                    FullscreenActivity.cameraRotation = 180;//         bitmapMatrix.postRotate(180);                                               break;  // bottom right
                                case 4:
                                    FullscreenActivity.cameraRotation = 180;//         bitmapMatrix.postRotate(180);           bitmapMatrix.postScale(-1, 1);      break;  // bottom left
                                case 5:
                                    FullscreenActivity.cameraRotation = 90;//         bitmapMatrix.postRotate(90);            bitmapMatrix.postScale(-1, 1);      break;  // left top
                                case 6:
                                    FullscreenActivity.cameraRotation = 90;//         bitmapMatrix.postRotate(90);                                                break;  // right top
                                case 7:
                                    FullscreenActivity.cameraRotation = 270;//         bitmapMatrix.postRotate(270);           bitmapMatrix.postScale(-1, 1);      break;  // right bottom
                                case 8:
                                    FullscreenActivity.cameraRotation = 270;//         bitmapMatrix.postRotate(270);                                               break;  // left bottom
                                default:
                                    break;  // Unknown
                            }

                            *//* Work on exifOrientation *//*
                        } catch (Exception e) {
                            // TODO: handle exception
                        }
                        mCamera.startPreview();
                    }
                });
*/
    }
}