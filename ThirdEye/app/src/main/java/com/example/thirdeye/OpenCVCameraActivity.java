package com.example.thirdeye;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class OpenCVCameraActivity extends AppCompatActivity implements  CameraBridgeViewBase.CvCameraViewListener2{
    private static String TAG = "OpenCVCameraActivity";

    ////// activity classifire variable
    private final Object lock = new Object();
    //    private boolean runClassifier = false;
    private boolean runClassifier = false;
    private ActivityClassifier classifier = null;
    //private static boolean classifierCreated = false;

    /** An additional thread for running tasks that shouldn't block the UI. */
    private HandlerThread backgroundThread;
    /** A {@link Handler} for running tasks in the background. */
    private Handler backgroundHandler;
    private static final String HANDLE_THREAD_NAME = "OpenCVActivityClassifyBackground";


    ///////// openCV java camera frame capture, save ---- variables
    private int fileNum = 0;
    private static File folder = new File(Environment.getExternalStorageDirectory() + "/Pictures/ThirdEye/imageData/");
    private static File nomediaFile = new File(Environment.getExternalStorageDirectory()+"/Pictures/ThirdEye/.nomedia");
    private static String filename;
    private static File imgfile;
    SimpleDateFormat sdf  = new java.text.SimpleDateFormat("MM-dd_HH-mm-ss");;
    private static Boolean fileWritten = null;
    private static JavaCameraView openCVCamView;
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

    //////////////////////////////////////////////////////////////
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

        ///////// image/data folders created when/if not found
        if(!folder.exists()){
            folder.mkdirs();
        }
        if(!nomediaFile.exists()){
            try {
                nomediaFile.createNewFile();
                Log.e(TAG, "opencvCamPage: nomedia file created: ");
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "opencvCamPage: nomedia file NOT created: "+nomediaFile.getName());
            }
        }

        openCVCamView  = (JavaCameraView) findViewById(R.id.openCVCamViewID);
        openCVCamView.setVisibility(View.VISIBLE);
        openCVCamView.setCvCameraViewListener( this);


        startBackgroundThread();
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


        fileNum++;
        filename = "img_"+sdf.format(new Date()) +"_"+fileNum+"_.jpeg";
        imgfile = new File(folder, filename);
        filename = imgfile.toString();

        ///////// comment/uncomment next line to save/not save image-----------########
        Imgcodecs.imwrite(filename, mRGBAT);
        try {
            // thread to sleep
            Thread.sleep(200);
        } catch (Exception e) {
            System.out.println(e);
        }

        return mRGBAT;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
    ////////////opencv camera view ENDED///////////////////////


    //////////////////////////////////////////
    /////////background thread for activity classification
    ////Starts a background thread and its {@link Handler}.
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread(HANDLE_THREAD_NAME);
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        synchronized (lock) {
            runClassifier = true;
        }
        backgroundHandler.post(runBackgroundClassifire);
    }

    //Stops the background thread and its {@link Handler}. /////
    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
            synchronized (lock) {
                runClassifier = false;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    ////classify images from directory in the background. ///////
    private Runnable runBackgroundClassifire =
            new Runnable() {
                @Override
                public void run() {
                    synchronized (lock) {
                        if (runClassifier && classifier == null) {
                            classifier = new ActivityClassifier(OpenCVCameraActivity.this);
                            Log.e(TAG, "OpenCV: Background Activity classifier started.");
                        }else{
                            //Log.e(TAG, "OpenCV: Background Activity classifier running.");
                        }
                    }
                    backgroundHandler.post(runBackgroundClassifire);
                }
            };
    /////////background thread for activity classification ENDED



    //////////////////////////////////////////
    ///////// resume or pause app/////////////

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();

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

    @Override
    protected void onPause() {
        if(openCVCamView != null){
            openCVCamView.disableView();
        }

        stopBackgroundThread();
        super.onPause();
    }

    @Override
    protected void onDestroy() {

        runClassifier =  false;
        if (classifier != null){
            classifier.close();
            classifier=null;
        }

        if(openCVCamView != null){
            openCVCamView.disableView();
        }
        super.onDestroy();
    }

}