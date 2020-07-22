package com.example.thirdeye;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class OpenCVCameraActivity extends AppCompatActivity implements  CameraBridgeViewBase.CvCameraViewListener2{
    private static String TAG = "OpenCVCameraActivity";

    ///////// tensorflow lite interpreter -------- variables
//    private EditText inputNumber;
//    private TextView outputNumer;
    private Button inferButton;
    private ImageView imageView;

    private Interpreter tflite;

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
    private ByteBuffer imgData = null;

    /** An array to hold inference results, to be feed into Tensorflow Lite as outputs. */
    private float[][][][] imgFeatureSetArray = new float[DIM_BATCH_SIZE][7][7][1280];

    private static Bitmap bitmap = null;
    ///////// tensorflow lite interpreter -------- variables ENDED


    ///////// openCV java camera frame capture---- variables
    private int fileNum = 0;
    Boolean bool = null;
    SimpleDateFormat sdf  = new java.text.SimpleDateFormat("MM-dd_HH-mm-ss");;
    JavaCameraView openCVCamView;
    Mat mRGBA , mRGBAT;
    BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback() {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case BaseLoaderCallback.SUCCESS:
                {
                    openCVCamView.enableView();
                    break;
                }
                default:
                {
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };
    ///////// openCV java camera frame capture---- variables ENDED



    ///////Checking?Asking for camera permission------------------
    private static final String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    private static final int PERMISSIONS_COUNT = 3; //////////////should be equal to num of permissions
    private static final int REQUEST_PERMISSIONS = 39;  //////this is a request code

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean arePermisionsDenied(){
        for (int i=0 ; i < PERMISSIONS_COUNT ; i++){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if( checkSelfPermission(PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED ){
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if( requestCode == REQUEST_PERMISSIONS && grantResults.length > 0){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if( arePermisionsDenied() ){
                    Intent intent_camToHomePage  = new Intent(getApplicationContext(), HomePageActivity.class);
                    //to resume the main activity use the following line -----------
                    intent_camToHomePage.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent_camToHomePage);
                    finish();
                }else{
                    onResume();
                }
            }
        }
    }
    ///////Checking?Asking for camera permission ENDED------------------


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.open_cv_camera_activity);

        openCVCamView  = (JavaCameraView) findViewById(R.id.openCVCamViewID);
        openCVCamView.setVisibility(View.VISIBLE);
        openCVCamView.setCvCameraViewListener( this);

        //////////////////////////////////////////
        ////////taking input for the model/////////
//        inputNumber = (EditText) findViewById(R.id.inputNumberID);
//        outputNumer = (TextView) findViewById(R.id.outputNumberID);
        inferButton = (Button) findViewById(R.id.inferButtonID);
        imageView = (ImageView) findViewById(R.id.imageViewId);


        //////create the tflite object
        try {
            tflite = new Interpreter(loadModelFile());
            imgData =
                    ByteBuffer.allocateDirect(
                            4 * DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE);
            imgData.order(ByteOrder.nativeOrder());
            Log.d(TAG, "Created a Tensorflow Lite Image Classifier.");
        } catch (Exception e) {
            e.printStackTrace();
        }
        //////do inference
        inferButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*
                //////////////////////////////////////////
                ////////pass input for the model/////////
                float prediction = doInference(inputNumber.getText().toString());
                outputNumer.setText(Float.toString(prediction));
                */
                String fileName = "/storage/emulated/0/Pictures/ThirdEye/img_1.jpeg";
                File file = new File(fileName);
                bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                /////////creating a scaled bitmap from the image file
                bitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
                imageView.setImageBitmap(bitmap);
                getImageFeature(bitmap );
            }
        });
    }
    /** Closes tflite to release resources. */
    public void close() {
        tflite.close();
        tflite = null;
    }

    //////////////////////////////////////////
    /////////tflite model loader//////////////
    private MappedByteBuffer loadModelFile() throws IOException{
        //////////loading the tflite model from assets folder
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("mobilenet_with_preprocessing.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredlength = fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredlength);
    }

    /*
    /////////////////////////////////////////////////////////////////
    //////// method to do inference using the tflite model///////////
    private float doInference(String inputString) {
        ///////input shape id [1]
        float[] inputVal = new float[1];
        inputVal[0] = Float.valueOf(inputString);

        //////output shape is [1][1]
        float[][] outputVal = new float[1][1];
        ///Run inference passing the input shape and getting the output shape
        tflite.run(inputVal , outputVal);

        float inferredValue = outputVal[0][0];

        return inferredValue;
    }
    */

    /** Classifies a frame from the preview stream. */
    String getImageFeature(Bitmap bitmap) {
        if (tflite == null) {
            Log.e(TAG, "Image classifier has not been initialized; Skipped.");
            return "Uninitialized Classifier.";
        }
        //Log.e(TAG, "getImageFeature() >>>>>>> Image bitmap to byte conversion startig");
        convertBitmapToByteBuffer(bitmap);
        //Here's where the magic happens!!!
        long startTime = SystemClock.uptimeMillis();
        Log.e(TAG, "getImageFeature() >>>>>>> input image bytebuffer in the tflite model");
        tflite.run(imgData, imgFeatureSetArray);
        long endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "Timecost to run model inference: " + Long.toString(endTime - startTime));
        Log.d(TAG,  Arrays.deepToString(imgFeatureSetArray));

        Log.d(TAG, "print done " );
        // print the results
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

        Log.d(TAG, " \"convertBitmapToByteBuffer() >>> bitmap to byte float ");
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



    //////////////////////////////////////////
    ////////////opencv camera view ///////////
    @Override
    public void onCameraViewStarted(int width, int height) {
        mRGBA = new Mat(height,width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRGBA.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRGBA = inputFrame.gray();
        mRGBAT = mRGBA.t();
        Core.flip(mRGBA.t() , mRGBAT ,1);
        Imgproc.resize(mRGBAT , mRGBAT , mRGBA.size() );

//        File path = new File(getFilesDir()+"/Pictures/opencv/");
        File path = new File(Environment.getExternalStorageDirectory() + "/Pictures/ThirdEye/");
        path.mkdirs();

        fileNum++;
        String filename = "img_"+sdf.format(new Date()) +"_"+fileNum+"_.jpeg";
        File file = new File(path, filename);
        filename = file.toString();


            ///////// comment/uncomment next line to save/not save image-----------########
        Imgcodecs.imwrite(filename, mRGBAT);


//        bool = Imgcodecs.imwrite(filename, mRGBAT);
//        if( bool == true){
//            Log.e("Imwrite" , "saved in "+filename );
//        }else{
//            Log.e("Imwrite_failed" , "File write FAILED");
//        }

        return mRGBAT;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
    ////////////opencv camera view ENDED///////////////////////

    //////////////////////////////////////////
    ///////// resume or pause app/////////////
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(openCVCamView != null){
            openCVCamView.disableView();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(openCVCamView != null){
            openCVCamView.disableView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (OpenCVLoader.initDebug()) {
//            Log.e(TAG, "opencv ok");
            baseLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        } else {
//            Log.e(TAG, "opencv NOT ok");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseLoaderCallback);
        }

        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M   &&   arePermisionsDenied() ){
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
            return;
        }

    }
}