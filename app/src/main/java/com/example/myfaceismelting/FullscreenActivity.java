/*
 * Copyright (C) 2010,2011,2012 Samuel Audet
 *
 * FacePreview - A fusion of OpenCV's facedetect and Android's CameraPreview samples,
 *               with JavaCV + JavaCPP as the glue in between.
 *
 * This file was based on CameraPreview.java that came with the Samples for
 * Android SDK API 8, revision 1 and contained the following copyright notice:
 *
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * IMPORTANT - Make sure the AndroidManifest.xml file looks like this:
 *
 * <?xml version="1.0" encoding="utf-8"?>
 * <manifest xmlns:android="http://schemas.android.com/apk/res/android"
 *     package="com.googlecode.javacv.facepreview"
 *     android:versionCode="1"
 *     android:versionName="1.0" >
 *     <uses-sdk android:minSdkVersion="4" />
 *     <uses-permission android:name="android.permission.CAMERA" />
 *     <uses-feature android:name="android.hardware.camera" />
 *     <application android:label="@string/app_name">
 *         <activity
 *             android:name="FacePreview"
 *             android:label="@string/app_name"
 *             android:screenOrientation="landscape">
 *             <intent-filter>
 *                 <action android:name="android.intent.action.MAIN" />
 *                 <category android:name="android.intent.category.LAUNCHER" />
 *             </intent-filter>
 *         </activity>
 *     </application>
 * </manifest>
 */

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
import android.widget.FrameLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.IplImage;

import java.io.IOException;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;


// ----------------------------------------------------------------------

public class FullscreenActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Hide the window title.
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(FullscreenActivity.this, new String[]{android.Manifest.permission.CAMERA}, 50);
        }
        // Create our Preview view and set it as the content of our activity.
        try {
            FrameLayout layout = new FrameLayout(this);
            FaceView faceView = new FaceView(this);
            Preview mPreview = new Preview(this, faceView);
            layout.addView(mPreview);
            layout.addView(faceView);
            setContentView(layout);
        } catch (IOException e) {
            e.printStackTrace();
            new AlertDialog.Builder(this).setMessage(e.getMessage()).create().show();
        }
    }
}

// ----------------------------------------------------------------------

class FaceView extends View implements Camera.PreviewCallback {

    private IplImage image;
    private IplImage yuvImage;
    //	private Bitmap bitmap;
    int[] _temp = null;

    int EDGES_THRESHOLD = 70;
    int LAPLACIAN_FILTER_SIZE = 5;
    int MEDIAN_BLUR_FILTER_SIZE = 7;
    int repetitions = 7; // Repetitions for strong cartoon effect.
    int ksize = 1; // Filter size. Has a large effect on speed.
    double sigmaColor = 9; // Filter color strength.
    double sigmaSpace = 7; // Spatial strength. Affects speed.
    int NUM_COLORS = 16;
    private IplImage gray;
    private IplImage edges;
    private IplImage temp;
    //	private IplImage colorImage1;
    private IplImage gray1;
    private IplImage temp1;
    //    private IplImage temp1;
    AndroidFrameConverter converter = new AndroidFrameConverter();
    OpenCVFrameConverter openconverter = new OpenCVFrameConverter.ToIplImage();

    Paint paint = new Paint();

    public FaceView(FullscreenActivity context) throws IOException {
        super(context);

        // Preload the opencv_objdetect module to work around a known bug.
//		Loader.load(opencv_objdetect.class);
    }

    public void onPreviewFrame(final byte[] data, final Camera camera) {
        try {
            Size size = camera.getParameters().getPreviewSize();
            processImage(data, size.width, size.height);
            camera.addCallbackBuffer(data);
        } catch (RuntimeException e) {
            // The camera has probably just been released, ignore.
        }
    }

    protected void processImage(byte[] data, int width, int height) {
        // First, downsample our image and convert it into a grayscale IplImage

		if (image == null || image.width() != width || image.height() != height) {
			image = IplImage.create(width, height, IPL_DEPTH_8U, 3);
////			colorImage1 = IplImage.create(width, height, IPL_DEPTH_8U, 4);
//			bitmap = Bitmap.createBitmap(colorImage.width(), colorImage.height(), Bitmap.Config.ARGB_8888);
//			_temp = new int[(width * height)];
			gray1 = IplImage.create(image.cvSize(), IPL_DEPTH_8U, 1);
			gray = IplImage.create(image.cvSize(), IPL_DEPTH_8U, 1);
			edges = IplImage.create(gray.cvSize(), gray.depth(), gray.nChannels());
			temp = IplImage.create(image.cvSize(), image.depth(), image.nChannels());
			temp1 = IplImage.create(image.cvSize(), image.depth(), image.nChannels());
		}

//		decodeYUV420SP(_temp, data, width, height);

//        int f = 1;// SUBSAMPLING_FACTOR;

        // First, downsample our image and convert it into a grayscale IplImage
//		yuvImage = IplImage.create(width, height, IPL_DEPTH_8U, 3);

//		int dataStride = width;
//		int imageStride = colorImage.widthStep();
//		ByteBuffer imageBuffer = colorImage.getByteBuffer();
//		imageBuffer.put(data);
////		for (int y = 0; y < height; y++) {
////			int dataLine = y *dataStride;
////			int imageLine = y * imageStride;
////			for (int x = 0; x < width; x++) {
////				imageBuffer.put(imageLine + x, data[dataLine + x]);
////				imageBuffer.put(imageLine + x, data[dataLine + 3*x]);
////				imageBuffer.put(imageLine + x,(data[dataLine + x] & 0xFF) | ((data[dataLine + x] & 0xFF) << 8) | ((data[dataLine + x] & 0xFF) << 16));
////				imageBuffer.put(imageLine + 3*x+1, data[dataLine + x]);
////				imageBuffer.put(imageLine + 3*x+2, data[dataLine + x]);
////				_temp[dataLine+x/3]=(data[dataLine + x] & 0xFF) | ((data[dataLine + x+1] & 0xFF) << 8) | ((data[dataLine + x+2] & 0xFF) << 16);
////			}
////		}
//        if (yuvImage==null){
//        	yuvImage = new Frame(width, height, Frame.DEPTH_UBYTE, 2);
//		}
//        ((ByteBuffer) yuvImage.image[0].position(0)).put(data);
        image = openconverter.convertToIplImage(converter.convert(data, width, height));
//        image = render(render(colorImage, sf), sf1);
        cvCvtColor(image, gray, CV_BGR2GRAY);
        cvSmooth(gray, gray, CV_MEDIAN, MEDIAN_BLUR_FILTER_SIZE, 0, 0, 0);
        cvLaplace(gray, edges, LAPLACIAN_FILTER_SIZE);
        cvThreshold(edges, edges, 80, 255, CV_THRESH_BINARY_INV);
//			 cvErode(edges, edges, null,2);
//			 cvDilate(edges, edges, null,1);
        // create contours around white regions
//			CvSeq contour = new CvSeq();
//			CvMemStorage storage = CvMemStorage.create();
//			cvSmooth(edges, edges, CV_MEDIAN, MEDIAN_BLUR_FILTER_SIZE, 0, 0, 0);
//			cvNot(edges,edges);

//			cvFindContours(edges, storage, contour,
//					Loader.sizeof(CvContour.class), CV_RETR_TREE,
//					com.googlecode.javacv.cpp.opencv_imgproc.CV_CHAIN_APPROX_SIMPLE);// CV_CHAIN_APPROX_SIMPLE);
//			// loop through all detected contours
//			cvZero(edges);
//			for (; contour != null && !contour.isNull(); contour = contour.h_next()) {
////				CvSeq approx = cvApproxPoly(contour,
////						Loader.sizeof(CvContour.class), storage, CV_POLY_APPROX_DP,
////						cvContourPerimeter(contour) * 0.001, 0);
////				CvRect rec = cvBoundingRect(contour, 0);
////					if (rec.height() > recthighgap && rec.width() > rectwidthgap) {
//				int area= Math.abs((int) cvContourArea(contour, CV_WHOLE_SEQ, -1));
//				int perimeter=(int) cvArcLength(contour, CV_WHOLE_SEQ, -1)+1;
//
//				if (perimeter>100&&area>100&& area/perimeter==0){
////					System.out.println(area+"\t"+perimeter+"\t"+(area/perimeter));
////						CvMemStorage storage1 = CvMemStorage.create();
////						CvSeq convexContour = cvConvexHull2(contour, storage1,
////								CV_CLOCKWISE, 1);
//						cvDrawContours(edges, contour, CvScalar.WHITE,
//								CvScalar.WHITE, 127,1, 8);
//					}
//			}
//			cvNot(edges,edges);

        for (int i = 0; i < repetitions; i++) {
            cvSmooth(image, temp, CV_BILATERAL, ksize, 0, sigmaColor, sigmaSpace);
            cvSmooth(temp, image, CV_BILATERAL, ksize, 0, sigmaColor, sigmaSpace);
        }
        temp = IplImage.create(image.cvSize(), image.depth(), image.nChannels());
        cvZero(temp);



        cvCopy(image, temp, edges);
//        sf.setTime(t1 += .02f);
//        sf1.setTime(t2 += .02f);

//		colorImage.getIntBuffer().put(_temp);
//		colorImage.getByteBuffer().put(_temp);

//		IplImage yuvimage = IplImage.create(width, height * 3 / 2, IPL_DEPTH_8U, 2);
//		IplImage rgbimage = IplImage.create(width, height, IPL_DEPTH_8U, 3);
//		cvCvtColor(yuvimage, rgbimage, CV_YUV2BGR_NV21);
//
//		IplImage r = IplImage.create(colorImage.width(), colorImage.height(), colorImage.depth(), CV_8UC1);
//		IplImage g = IplImage.create(colorImage.width(), colorImage.height(), colorImage.depth(), CV_8UC1);
//		IplImage b = IplImage.create(colorImage.width(), colorImage.height(), colorImage.depth(), CV_8UC1);
//		cvSplit(colorImage, r, g, b, null);
//		cvMerge(r, g, b, null, colorImage);

//        cvCvtColor(image, gray1, CV_BGR2GRAY);
//        cvConvertScale(gray1, gray, 255, 0);
//        cvSmooth(gray, gray, CV_MEDIAN, MEDIAN_BLUR_FILTER_SIZE, 0, 0, 0);
//////
//        cvLaplace(gray, edges, LAPLACIAN_FILTER_SIZE);
//        cvThreshold(edges, edges, 80, 255, CV_THRESH_BINARY_INV);
//        for (int i = 0; i < repetitions; i++) {
//            cvSmooth(image, temp, CV_BILATERAL, ksize, 0, sigmaColor, sigmaSpace);
//            cvSmooth(temp, image, CV_BILATERAL, ksize, 0, sigmaColor, sigmaSpace);
//        }
//        cvZero(temp);
//        cvCopy(image, temp, edges);
//        cvCvtColor(temp, temp1, CV_BGR2BGRA);
        postInvalidate();
    }
    public IplImage render(IplImage image) {
//        BufferedImage bi = new BufferedImage(image.width(), image.height(), BufferedImage.TYPE_INT_ARGB);
//        BufferedImage bi2 = new BufferedImage(image.width(), image.height(), BufferedImage.TYPE_INT_ARGB);
//        bi.getGraphics().drawImage(converterjava2d.getBufferedImage(converter.convert(image)), 0, 0, null);
//        rf.filter(bi, bi2);
//        BufferedImage bi1 = new BufferedImage(image.width(), image.height(), BufferedImage.TYPE_3BYTE_BGR);
//        bi1.getGraphics().drawImage(bi2, 0, 0, null);
//        image =converter.convertToIplImage(converterjava2d.convert(bi1));
        return image;
    }
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
        if (image != null) {
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(5);

            int ww = temp1.width();
            int hh = temp1.height();
            int x0 = 0;//-ww/4;
            int y0 = 0;//-hh/4;
//			ww+=ww/2;
//			hh+=hh/2;

//			x0+=10;
//			y0+=10;
//			x0-=320;
//			ww-=320;
            y0 += hh / 2;
            hh += hh / 2;
//			ww-=10;
//			hh-=10;
            canvas.drawBitmap(converter.convert(openconverter.convert(temp)), x0, y0, paint);
            paint.setColor(Color.RED);
            canvas.drawLine(x0, y0, ww, y0, paint);
            paint.setColor(Color.GREEN);
            canvas.drawLine(x0, hh, ww, hh, paint);
            paint.setColor(Color.BLUE);
            canvas.drawLine(x0, y0, x0, hh, paint);
            paint.setColor(Color.YELLOW);
            canvas.drawLine(ww, y0, ww, hh, paint);
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
        mCamera = Camera.open();
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
        Size optimalSize = getOptimalPreviewSize(sizes, w, h);
        parameters.setPreviewSize(optimalSize.width, optimalSize.height);

        mCamera.setParameters(parameters);
        if (previewCallback != null) {
            mCamera.setPreviewCallbackWithBuffer(previewCallback);
            Size size = parameters.getPreviewSize();
            byte[] data = new byte[size.width * size.height * ImageFormat.getBitsPerPixel(parameters.getPreviewFormat()) / 8];
            mCamera.addCallbackBuffer(data);
        }
        mCamera.startPreview();
    }

}