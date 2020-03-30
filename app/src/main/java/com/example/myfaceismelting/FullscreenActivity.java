package com.example.myfaceismelting;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.jabistudio.androidjhlabs.filter.PosterizeFilter;
import com.jabistudio.androidjhlabs.filter.SwimFilter;
import com.jabistudio.androidjhlabs.filter.TransformFilter;
import com.jabistudio.androidjhlabs.filter.util.AndroidUtils;
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

    public static void setCameraDisplayOrientation(int rotation,int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
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
            toggleCam.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mPreview.mCamera.stopPreview();

//NB: if you don't release the current camera before switching, you app will crash
                    mPreview.mCamera.release();
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


                    setCameraDisplayOrientation(FullscreenActivity.this.getWindowManager().getDefaultDisplay().getRotation(), currentCameraId, prev.mCamera);
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
            LinearLayout layoutlin1 = new LinearLayout(this);
            LinearLayout layoutlin2 = new LinearLayout(this);
            layoutlin1.setOrientation(LinearLayout.VERTICAL);
            layoutlin2.setOrientation(LinearLayout.HORIZONTAL);
            layoutlin1.addView(layoutlin2);
            layoutlin2.addView(toggleOn);
            layoutlin2.addView(toggleCam);
            layoutlin2.addView(buttonSnap);
            layout.addView(mPreview);
            layout.addView(faceView);
            layout.addView(layoutlin1);
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
        sf.setEdgeAction(TransformFilter.CLAMP);
        sf1.setEdgeAction(TransformFilter.CLAMP);
        sf1.setAmount(30f);
        sf1.setTurbulence(1f);
        sf1.setScale(300);
        sf1.setStretch(50);
        glf.setNumLevels(100);
    }

    public void onPreviewFrame(final byte[] data, final Camera camera) {
        try {
//            Size size = camera.getParameters().getPreviewSize();

            processImage(rotateYUV420Degree90(data, preview.mCamera.getParameters().getPreviewSize().width, preview.mCamera.getParameters().getPreviewSize().height), preview.mCamera.getParameters().getPreviewSize().height, preview.mCamera.getParameters().getPreviewSize().width);
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
        cvCvtColor(image1, image, CV_RGBA2RGB);
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

    public int[] toArray(IntBuffer b) {
        if (b.hasArray()) {
            if (b.arrayOffset() == 0)
                return b.array();

            return Arrays.copyOfRange(b.array(), b.arrayOffset(), b.array().length);
        }

        b.rewind();
        int[] foo = new int[b.remaining()];
        b.get(foo);

        return foo;
    }

    public static ByteBuffer deepCopy(ByteBuffer orig) {
        int pos = orig.position(), lim = orig.limit();
        try {
            orig.position(0).limit(orig.capacity()); // set range to entire buffer
            ByteBuffer toReturn = deepCopyVisible(orig); // deep copy range
            toReturn.position(pos).limit(lim); // set range to original
            return toReturn;
        } finally // do in finally in case something goes wrong we don't bork the orig
        {
            orig.position(pos).limit(lim); // restore original
        }
    }

    public static ByteBuffer deepCopyVisible(ByteBuffer orig) {
        int pos = orig.position();
        try {
            ByteBuffer toReturn;
            // try to maintain implementation to keep performance
            if (orig.isDirect())
                toReturn = ByteBuffer.allocateDirect(orig.remaining());
            else
                toReturn = ByteBuffer.allocate(orig.remaining());

            toReturn.put(orig);
            toReturn.order(orig.order());

            return (ByteBuffer) toReturn.position(0);
        } finally {
            orig.position(pos);
        }
    }

    byte[] integersToBytes(int[] values) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        for (int i = 0; i < values.length; ++i) {
            try {
                dos.writeInt(values[i]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return baos.toByteArray();
    }
//    public IplImage render(IplImage image, int[] pixels) {
////        BufferedImage bi = new BufferedImage(image.width(), image.height(), BufferedImage.TYPE_INT_ARGB);
////        BufferedImage bi2 = new BufferedImage(image.width(), image.height(), BufferedImage.TYPE_INT_ARGB);
////        bi.getGraphics().drawImage(converterjava2d.getBufferedImage(converter.convert(image)), 0, 0, null);
////        rf.filter(bi, bi2);
////        BufferedImage bi1 = new BufferedImage(image.width(), image.height(), BufferedImage.TYPE_3BYTE_BGR);
////        bi1.getGraphics().drawImage(bi2, 0, 0, null);
////        image =converter.convertToIplImage(converterjava2d.convert(bi1));
//        return image;
//    }

    protected void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {
        int frameSize = width * height;
        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0)
                    y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }
                int y1192 = 1192 * y;

                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0)
                    r = 0;
                else if (r > 262143)
                    r = 262143;
                if (g < 0)
                    g = 0;
                else if (g > 262143)
                    g = 262143;
                if (b < 0)
                    b = 0;
                else if (b > 262143)
                    b = 262143;

                rgb[yp] = 0xff000000 | ((b << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((r >> 10) & 0xff);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {

        paint.setColor(Color.RED);
        paint.setTextSize(20);

        String s = "FacePreview - This side up." + Math.random();
        float textWidth = paint.measureText(s);
        canvas.drawText(s, (getWidth() - textWidth) / 2, (float) (20 + Math.random() * 20), paint);

/*
			 IplImage _1image = IplImage.create(colorImage.width(), colorImage.height(), IPL_DEPTH_8U, 4);
			 IplImage _2image = IplImage.create(colorImage.width(), colorImage.height(), colorImage.depth(), 4);
			 cvCvtColor(edges, _1image, CV_GRAY2RGBA);
			IplImage r = IplImage.create(colorImage.width(), colorImage.height(), colorImage.depth(), CV_8UC1);
			IplImage g = IplImage.create(colorImage.width(), colorImage.height(), colorImage.depth(), CV_8UC1);
			IplImage b = IplImage.create(colorImage.width(), colorImage.height(), colorImage.depth(), CV_8UC1);
			IplImage a = IplImage.create(colorImage.width(), colorImage.height(), colorImage.depth(), CV_8UC1);
			cvZero(a);
			cvNot(a,a);
			cvSplit(temp,r,g,b,null);
			cvMerge(a, r, g,b, _2image);
			ByteBuffer bb=_2image.getByteBuffer();
			bb.rewind();
*/
//			ByteBuffer bb = colorImage.getByteBuffer();
//			bitmap.copyPixelsToBuffer(bb);
//			bitmap=converter.convert(openconverter.convert(colorImage));
        if (enable && temp != null && !temp.isNull() && temp.width() > 0 && temp.height() > 0) {
            paint.setColor(Color.WHITE);
            Bitmap bitmap = converter.convert(openconverter.convert(temp));
            int[] swim1Ints1 = AndroidUtils.bitmapToIntArray(bitmap);
            int[] swim1Ints2 = glf.filter(swim1Ints1, bitmap.getWidth(), bitmap.getHeight());
            bitmap.copyPixelsFromBuffer(IntBuffer.wrap(swim1Ints2));
            bitmap = createScaledBitmap(bitmap, getWidth(), getHeight(), true);
            canvas.drawBitmap(bitmap, 0, 0, paint);
        }
        this.invalidate();
    }


    public static Bitmap IplImageToBitmap(IplImage src) {
        Bitmap bm = null;
        int width = src.width();
        int height = src.height();
//		// Unfortunately cvCvtColor will not let us convert in place, so we need to create a new IplImage with matching dimensions.
        IplImage frame2 = IplImage.create(width, height, IPL_DEPTH_8U, 4);
        cvCvtColor(src, frame2, CV_BGR2RGBA);
        // Now we make an Android Bitmap with matching size ... Nb. at this point we functionally have 3 buffers == image size. Watch your memory usage!
        bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bm.copyPixelsFromBuffer(frame2.getByteBuffer());
        //src.release();
//		frame2.release();
        return bm;
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

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
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
        Size optimalSize = getOptimalPreviewSize(sizes, 100, 300);
        parameters.setPreviewSize(optimalSize.width, optimalSize.height);

//        Camera.CameraInfo info = new Camera.CameraInfo();
//        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
        int rotation = 0;//((Activity) getContext()).getWindowManager().getDefaultDisplay().getRotation();
//        int degrees = 0;
//        switch (rotation) {
//            case Surface.ROTATION_0:
//                degrees = 0;
//                break; //Natural orientation
//            case Surface.ROTATION_90:
//                degrees = 90;
//                break; //Landscape left
//            case Surface.ROTATION_180:
//                degrees = 180;
//                break;//Upside down
//            case Surface.ROTATION_270:
//                degrees = 270;
//                break;//Landscape right
//        }
//        int rotate = (info.orientation - degrees + 360) % 360;

//STEP #2: Set the 'rotation' parameter
//        Camera.Parameters params = mCamera.getParameters();
//        parameters.setRotation(rotate);
//
        mCamera.setParameters(parameters);
//        mCamera.setDisplayOrientation(rotate);
        FullscreenActivity.setCameraDisplayOrientation(rotation,FullscreenActivity.currentCameraId, mCamera);
        if (previewCallback != null) {
            mCamera.setPreviewCallbackWithBuffer(previewCallback);
            Size size = parameters.getPreviewSize();
            byte[] data = new byte[size.width * size.height * ImageFormat.getBitsPerPixel(parameters.getPreviewFormat()) / 8];
            mCamera.addCallbackBuffer(data);
        }
        mCamera.startPreview();
    }

}