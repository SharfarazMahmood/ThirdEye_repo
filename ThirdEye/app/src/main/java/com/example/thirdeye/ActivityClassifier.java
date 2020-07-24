package com.example.thirdeye;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class ActivityClassifier {
    private static final String TAG = "ActivityClassifier";

    ///////// tensorflow lite interpreter -------- variables
    /** Name of the model file stored in Assets. */
    private static final String MODEL_PATH = "mobilenet_with_preprocessing.tflite";
    /** Name of the label file stored in Assets. */
    //private static final String LABEL_PATH = "labels.txt";

    private Interpreter mobile_NetV2_tflite;

    /** Dimensions of inputs. */
    private static final int DIM_BATCH_SIZE = 1;
    private static final int DIM_PIXEL_SIZE = 3;
    static final int DIM_IMG_SIZE_X = 224;
    static final int DIM_IMG_SIZE_Y = 224;

    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128.0f;
    /* Preallocated buffers for storing image data in. */
    private int[] intValues = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];

    /** A ByteBuffer to hold image data, to be feed into Tensorflow Lite as inputs. */
    /// input image data for the mobile_NetV2_tflite
    private static ByteBuffer imgData = null;

    /** An array to hold inference results, to be feed into Tensorflow Lite as outputs. */
    //////the output array for the mobile_NetV2_tflite
    private float[][][][] imgFeatureSetArray = new float[DIM_BATCH_SIZE][7][7][1280];

    private static Bitmap bitmap = null;
    private static String fileName=null;
    private static File file = null;
    ///////// tensorflow lite interpreter -------- variables ENDED



    public ActivityClassifier (Activity activity)  {
        //////create the tflite object and initialize input output arrays
        try {
            mobile_NetV2_tflite = new Interpreter(loadMobileNetV2ModellFile(activity));
            imgData =
                    ByteBuffer.allocateDirect(
                            4 * DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE);
            imgData.order(ByteOrder.nativeOrder());
            Log.d(TAG, "Created a Tensorflow Lite Image Classifier.");

        } catch (IOException e) {
            Log.d(TAG, "TfLite Image Classifier crashed." + e);
            e.printStackTrace();
        }


        String path = Environment.getExternalStorageDirectory().toString()+"/Pictures/ThirdEye/";
        Log.d(TAG, "Path: " + path);
        File directory = new File(path);
        File[] files = directory.listFiles();

        if(files.length>20){
            for (int i = 0; i < 5 /* files.length*/; i++)
            {
                Log.d(TAG, "FileName:" + files[i].getName());
                fileName = Environment.getExternalStorageDirectory().toString()+"/Pictures/ThirdEye/"+files[i].getName();
                file = new File(fileName);
                bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                /////////creating a scaled bitmap from the image file
                bitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
                getImageFeature(bitmap);

                fileName=null;
                file=null;
                bitmap =null;
            }
        }
        Log.d(TAG, "Size: "+ files.length);
    }

    /** Closes tflite to release resources. */
    public void close() {
        mobile_NetV2_tflite.close();
        mobile_NetV2_tflite = null;
    }

    //////////////////////////////////////////
    /////////tflite model loader//////////////
    private MappedByteBuffer loadMobileNetV2ModellFile(Activity activity) throws IOException {
        //////////loading the tflite model from assets folder
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredlength = fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredlength);
    }

    /** Classifies an Image from the storage. */
    String getImageFeature(Bitmap bitmap) {
        if (mobile_NetV2_tflite == null) {
            Log.e(TAG, "Image classifier has not been initialized; Skipped.");
            return "Uninitialized Classifier.";
        }
        convertBitmapToByteBuffer(bitmap);
        //Here's where the magic happens!!!
        long startTime = SystemClock.uptimeMillis();
        mobile_NetV2_tflite.run(imgData, imgFeatureSetArray);
        long endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "Timecost to run model inference: " + Long.toString(endTime - startTime));
        Log.d(TAG,  Arrays.deepToString(imgFeatureSetArray));
        // print the time results
        String timetextToShow = Long.toString(endTime - startTime) + "ms";
        return timetextToShow;
    }


    /** Writes Image data into a {@code ByteBuffer}. */
    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (imgData == null) {
            Log.d(TAG, " \"convertBitmapToByteBuffer() >>> imgData variable is null\" ");
            return;
        }
        imgData.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

//        Log.d(TAG, " \"convertBitmapToByteBuffer() >>> bitmap to byte float ");
        // Convert the image to floating point.
        int pixel = 0;
        long startTime = SystemClock.uptimeMillis();
        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                final int val = intValues[pixel++];
                imgData.putFloat((((val >> 16) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                imgData.putFloat((((val >> 8) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                imgData.putFloat((((val) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
            }
        }
        long endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "Timecost to put values into ByteBuffer: " + Long.toString(endTime - startTime));
    }
}
