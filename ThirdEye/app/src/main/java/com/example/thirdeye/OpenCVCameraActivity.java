package com.example.thirdeye;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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
import java.text.SimpleDateFormat;
import java.util.Date;

public class OpenCVCameraActivity extends AppCompatActivity implements  CameraBridgeViewBase.CvCameraViewListener2{
    private static String TAG = "OpenCVCameraActivity";

    ///////// tensorflow lite interpreter -------- variables

    ///////// openCV java camera frame capture, save ---- variables
    private int fileNum = 0;

    SimpleDateFormat sdf  = new SimpleDateFormat("dd-MM_HH:mm");
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
    ///////Checking?Asking for permission------------------
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
    ///////Checking/Asking for permission ENDED------------------


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.open_cv_camera_activity);

        ///////// image/data folders created when/if not found
        File folder = new File(this.getExternalFilesDir(null) + "/imageData/");
        if(!folder.exists()){
            folder.mkdirs();
        }

        openCVCamView  = (JavaCameraView) findViewById(R.id.openCVCamViewID);
        openCVCamView.setVisibility(View.VISIBLE);
        openCVCamView.setCvCameraViewListener(this);
//        openCVCamView.setMaxFrameSize( 512,  512);

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

        File folder = new File(this.getExternalFilesDir(null) + "/imageData/");
        if(!folder.exists()){
            folder.mkdirs();
        }

        fileNum++;
        String st = String.format("%06d",fileNum); /////a 6 digit long file number to keep the sequence correctly
        String filename = "img_"+ sdf.format(new Date()) +"_"+st+"_.jpeg";
        File imgfile = new File(folder, filename);
        filename = imgfile.toString();

        ///////// comment/uncomment next line to save/not save image-----------########
        Imgcodecs.imwrite(filename, mRGBAT);

        try {
            Thread.sleep(250);
        } catch (Exception e) {
            System.out.println(e);
        }

        return mRGBAT;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) { }
    ////////////opencv camera view ENDED///////////////////////


    //////////////////////////////////////////
    ///////// resume or pause app/////////////

    @Override
    protected void onResume() {
        super.onResume();

        if (OpenCVLoader.initDebug()) {
            baseLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        } else {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseLoaderCallback);
        }

        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M   &&   arePermisionsDenied() ){
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
        }
    }

    @Override
    protected void onPause() {
        if(openCVCamView != null){
            openCVCamView.disableView();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if(openCVCamView != null){
            openCVCamView.disableView();
        }
        super.onDestroy();
    }


}