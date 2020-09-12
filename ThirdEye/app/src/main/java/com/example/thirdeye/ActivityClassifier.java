package com.example.thirdeye;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.SystemClock;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.SyncFailedException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class ActivityClassifier {
    private static final String TAG = "ActivityClassifier";

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    SimpleDateFormat sdf_day = new SimpleDateFormat("dd-MM");

    ///////// tensorflow lite interpreter -------- variables
    ///**model file stored in assets. */
    private static final String MobileNetV2_MODEL_PATH = "mobilenet_with_preprocessing.tflite";
    private static final String GRU_MODEL_PATH = "gru.tflite";
    /**label file stored in Assets. */
    private static final String LABEL_PATH = "labels.txt";

    /** Number of results to show in the UI. */
    private static final int RESULTS_TO_SHOW = 1;
    /** Labels corresponding to the output of the vision model. */
    private List<String> labelList;
    /** An array to hold inference results, to be feed into Tensorflow Lite as outputs. */
    private float[][] labelProbArray = new float[1][13];
    private PriorityQueue<Map.Entry<String, Float>> sortedLabels =
            new PriorityQueue<>(
                    RESULTS_TO_SHOW,
                    new Comparator<Map.Entry<String, Float>>() {
                        @Override
                        public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
                            return (o1.getValue()).compareTo(o2.getValue());
                        }
                    });

    ////// java interpreter for tflite models
    private Interpreter mobile_NetV2_tflite;
    private Interpreter gru_tflite;
    /** Dimensions of mobileNetv2 model inputs. These are used to convert image Bitmap file to input */
    private static final int DIM_BATCH_SIZE = 1;
    private static final int DIM_PIXEL_SIZE = 3;
    static final int DIM_IMG_SIZE_X = 224;
    static final int DIM_IMG_SIZE_Y = 224;

    /* Preallocated buffers for storing image data in. */
    private int[] intValues = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];
    /** A ByteBuffer to hold image data, to be feed into Tensorflow Lite as inputs. */
    /// input image data for the mobile_NetV2_tflite
    private static ByteBuffer imgData = null;

    /** An array to hold mobile_NetV2_tflite inference results, to be feed into gru_tflite as outputs. */
    //////the output array for the mobile_NetV2_tflite
    private float[][][][] imgFeatureSetArray = new float[DIM_BATCH_SIZE][7][7][1280];
    private float[] reshapeArray = new float[62720];
    //////the input array for the gru_tflite
    private float[][][] imgFeatureSetArray20 = new float[DIM_BATCH_SIZE][20][62720];
    ///////// gru_tflite output is taken into the labelProbArray
    /////////  and saved to file from that array using the PriorityQueue<Map.Entry<String, Float>> sortedLabels

    ///////// tensorflow lite interpreter -------- variables ENDED


    ////// reading image files & converting to bitmap
    private static Bitmap bitmap = null;
    private static String fileName=null;
    private static File file = null;


    private static volatile boolean runProcess= false;




    public ActivityClassifier (Activity activity , boolean runProcess)  {
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        this.runProcess = runProcess;
        //////create the tflite object and initialize input output arrays
        try {
            mobile_NetV2_tflite = new Interpreter(loadMobileNetV2ModellFile(activity));
            labelList = loadLabelList(activity);
            gru_tflite = new Interpreter(loadGRUModellFile(activity));

            imgData = ByteBuffer.allocateDirect(
                    4 * DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE);
            imgData.order(ByteOrder.nativeOrder());
//            Log.d(TAG, "Created a Tensorflow Lite Image Classifier."+imgData);
        } catch (IOException e) {
            Log.d(TAG, "TfLite Image Classifier crashed." + e);
            e.printStackTrace();
        }



        File folder = new File(activity.getExternalFilesDir(null) + "/imageData");
//        File activitiesFile = new File(activity.getExternalFilesDir(null) + "/day_" + mUser.getUid() + "_activities.txt");
        File activitiesFile = new File(activity.getExternalFilesDir(null) + "/day_" + sdf_day.format(new Date()) + "_activities.txt");
        if(!folder.exists()){
            folder.mkdirs();
        }
        createFilesOrDirectory(activitiesFile);



        while (folder.listFiles().length > 20 && this.runProcess) {
            File[] files = folder.listFiles();
            Log.d(TAG, "Number of images found: " + files.length);
            Arrays.sort(files, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    return Long.compare(f1.lastModified(), f2.lastModified());
                }
            });

            int count=0 , num_of_files = files.length;
            while ( count+20<num_of_files  && this.runProcess ){
                for (int i = count, imgFeatureSetCount = 0 ; i < (count+20) ; i++, imgFeatureSetCount++) {
//                    Log.d(TAG, "FileName:" + files[i].getName());
                    fileName = activity.getExternalFilesDir(null).toString() + "/imageData/" + files[i].getName();
                    file = new File(fileName);
                    bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
//                    Log.d(TAG, "FileName:" + file.getAbsolutePath());
                    /////////creating a scaled bitmap from the image file
                    bitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
                    ////////////////////////////////////////////////////////////////////
                    //////// get images features using mobilenetv2 tflite model/////////
                    getImageFeature(bitmap);
                    ////////////////////////////////////////////////////////////////////
                    if ( i < (count+4) ) {
                        if (files[i].delete()) {
//                        Log.d(TAG, "ActivityClassifier: Deleted the file: " + file.getName());
                        } else {
                            System.out.println("Failed to delete the file.");
                        }
                    }
//                Log.d(TAG,  Arrays.deepToString(imgFeatureSetArray));
                    /*reshape the imgFeatureSetArray*/
                    int reshape_ind = 0;
                    for (int p = 0; p < 7; p++) {
                        for (int j = 0; j < 7; j++) {
                            for (int k = 0; k < 1280; k++) {
                                reshapeArray[reshape_ind] = imgFeatureSetArray[0][p][j][k];
                                reshape_ind++;
                            }
                        }
                    }
                    imgFeatureSetArray20[0][imgFeatureSetCount] = reshapeArray;
//                    Log.d(TAG,  Arrays.deepToString(imgFeatureSetArray20));
//                    Log.d(TAG, String.valueOf(imgFeatureSetArray20.length));
//                    Log.d(TAG, String.valueOf(imgFeatureSetArray20[0].length));
//                    Log.d(TAG, String.valueOf(imgFeatureSetArray20[0][0].length));
//                    Log.d(TAG, String.valueOf(imgFeatureSetArray20[0][0][0].length));
                }
                ////////////////////////////////////////////////////////////////////
                //////// classify activity from 20 images/frames  using gru tflite model
                String theActivity = classifyActivity();
                ////////////////////////////////////////////////////////////////////
                /////// taking the time from the name of the last image among the 20 images
                String activityTime = files[count+20].getName().substring(10,15) ;
//                Log.d(TAG, "classifyActivity, result >>: "+ theActivity+activityTime+"\n");
                File activitiesfile = new File(activity.getExternalFilesDir(null), activitiesFile.getName());
//                Log.d(TAG, "ActivityClassifier: activity file : "+file.getAbsolutePath());
                writeToFile(theActivity+activityTime +"\n", activitiesfile);

                count = count+4;
            }

        }

    }

    //////// append the given activity to File
    private void writeToFile(String str, File activitiesfile) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(activitiesfile,true);
            OutputStreamWriter outputStreamWriter=new OutputStreamWriter(fos);
            outputStreamWriter.write(str);
            outputStreamWriter.flush();
            fos.getFD().sync();
            outputStreamWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (SyncFailedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /////// create file or directory.
    private void createFilesOrDirectory(File f) {
        if (f.isDirectory() && !f.exists()) {
            f.mkdirs();
            Log.e(TAG, " folder created: " + f.getAbsolutePath());
        }
        else if (!f.exists()) {
            try {
                f.createNewFile();
                Log.e(TAG, " file created: "+ f.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, " file NOT created: " + f.getAbsolutePath());
            }
        }
    }


    /** Closes tflite to release resources. */
    public void close() {
        runProcess = false;

        mobile_NetV2_tflite.close();
        mobile_NetV2_tflite = null;
        gru_tflite.close();
        gru_tflite=null;
    }

    //////////////////////////////////////////
    /** Reads label list from Assets. */
    private List<String> loadLabelList(Activity activity) throws IOException {
        List<String> labelList = new ArrayList<String>();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(activity.getAssets().open(LABEL_PATH)));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }

        reader.close();
        return labelList;
    }


    //////////////////////////////////////////
    /////////tflite model loader//////////////
    private MappedByteBuffer loadMobileNetV2ModellFile(Activity activity) throws IOException {
        //////////loading the tflite model from assets folder
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MobileNetV2_MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredlength = fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredlength);
    }

    private MappedByteBuffer loadGRUModellFile(Activity activity) throws IOException {
        //////////loading the tflite model from assets folder
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(GRU_MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredlength = fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredlength);
    }

    //////////////////////////////////////////
    /** Classifies an Image from the storage. */
    String getImageFeature(Bitmap bitmap) {
        if (mobile_NetV2_tflite == null) {
            Log.e(TAG, "Image classifier has not been initialized; Skipped.");
            return "Uninitialized Classifier.";
        }
        convertBitmapToByteBuffer(bitmap);
        long startTime = SystemClock.uptimeMillis();
        //Here's where the magic happens!!!
        mobile_NetV2_tflite.run(imgData, imgFeatureSetArray);
        long endTime = SystemClock.uptimeMillis();
//        Log.d(TAG, "Timecost to run mobilenet_v2 model inference: " + Long.toString(endTime - startTime));
        // print the time results
        String timetextToShow = Long.toString(endTime - startTime) + "ms";
        return timetextToShow;
    }

    //////////// activity classification
    String classifyActivity() {
        if (gru_tflite == null) {
            Log.e(TAG, "activity classifier has not been initialized; Skipped.");
            return "Uninitialized activity Classifier.";
        }
        long startTime = SystemClock.uptimeMillis();
        //Here's where the magic happens!!!
        gru_tflite.run(imgFeatureSetArray20, labelProbArray);
//        Log.d(TAG, "classifyActivity: after gru_model run");
        long endTime = SystemClock.uptimeMillis();
//        Log.d(TAG, "Timecost to run model inference: " + Long.toString(endTime - startTime));
//        Log.d(TAG,  Arrays.deepToString(imgFeatureSetArray));
        // print the results
        String textToShow = printTopKLabels();
        // print the time results
        String timetextToShow = Long.toString(endTime - startTime) + "ms";
//        Log.d(TAG, "classifyActivity: "+ textToShow);
        return textToShow;
    }

    //////////////////////////////////////////
    /** Writes Image data into a {@code ByteBuffer}. */
    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (imgData == null) {
            Log.d(TAG, " \"convertBitmapToByteBuffer() >>> imgData variable is null\" ");
            return;
        }
        imgData.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        // Convert the image to floating point.
        int pixel = 0;
        long startTime = SystemClock.uptimeMillis();
        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                final int val = intValues[pixel++];

                imgData.putFloat((((val >> 16) & 0xFF)));
                imgData.putFloat((((val >> 8) & 0xFF)));
                imgData.putFloat((((val) & 0xFF)));
            }
        }
        long endTime = SystemClock.uptimeMillis();
//        Log.d(TAG, "Timecost to put values into ByteBuffer: " + Long.toString(endTime - startTime));
    }

    /** Prints top-K labels, to be shown in UI as the results. */
    private String printTopKLabels() {
        for (int i = 0; i < labelList.size(); ++i) {
            sortedLabels.add(
                    new AbstractMap.SimpleEntry<>(labelList.get(i), labelProbArray[0][i]));
            if (sortedLabels.size() > RESULTS_TO_SHOW) {
                sortedLabels.poll();
            }
        }
        String textToShow = "";
        final int size = sortedLabels.size();
        for (int i = 0; i < size; ++i) {
            Map.Entry<String, Float> label = sortedLabels.poll();
//            textToShow = String.format("%s,%4.2f,",label.getKey(),label.getValue())+ textToShow;
            textToShow = String.format("%s,",label.getKey())+ textToShow;
        }
        return textToShow;
    }

}
