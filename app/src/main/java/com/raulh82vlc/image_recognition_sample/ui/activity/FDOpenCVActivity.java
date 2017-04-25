/*
 * Copyright (C) 2017 Raul Hernandez Lopez @raulh82vlc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.raulh82vlc.image_recognition_sample.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import com.raulh82vlc.ar_imagerecognition_sample.R;
import com.raulh82vlc.image_recognition_sample.domain.EyesRecognitionInteractor;
import com.raulh82vlc.image_recognition_sample.domain.EyesRecognitionInteractorImpl;
import com.raulh82vlc.image_recognition_sample.domain.FaceRecognitionInteractor;
import com.raulh82vlc.image_recognition_sample.domain.FaceRecognitionInteractorImpl;
import com.raulh82vlc.image_recognition_sample.model.RecognisedFace;
import com.raulh82vlc.image_recognition_sample.ui.widgets.FaceDrawerOpenCV;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Activity meant to be the UI point to show information to the user through OpenCV library
 */
public class FDOpenCVActivity extends Activity implements
        FaceRecognitionInteractor.FaceCallback,
        EyesRecognitionInteractor.EyesCallback,
        CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = FDOpenCVActivity.class.getSimpleName();
    // CONSTANTS
    private static final int PERMISSIONS_REQUEST_CAMERA = 666;
    private static final int CAMERA_ID_FRONT = 1;

    private CameraBridgeViewBase openCvCameraView;

    // Domain
    private Mat matrixRgba;
    private Mat matrixGray;


    private File cascadeFile;
    private CascadeClassifier detectorEye;
    private CascadeClassifier detectorFace;
    private FaceRecognitionInteractor faceRecognitionInteractor;
    private EyesRecognitionInteractorImpl eyesRecognitionInteractor;
    private boolean isMachineLearningInitialised = false;
    private boolean isStopped = false;

    private boolean isBlocked = false;

    private Handler mainHandler;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    try {
                        /** load face classificator */
                        File cascadeDirFace = readCascadeFile(R.raw.haarcascade_frontalface_alt2, "cascade", "haarcascade_front.xml");
                        initializeJavaDetectorForFace();
                        cascadeDirFace.delete();
                        /** load eye classificator */
                        File cascadeDirEye = readCascadeFile(R.raw.haarcascade_eye_tree_eyeglasses, "cascadeER", "haarcascade_eye.xml");
                        initializeJavaDetectorForEyes();
                        cascadeDirEye.delete();
                        setMachineLearningMechanism();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    setCameraParameters();

                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private void initializeJavaDetectorForEyes() {
        detectorEye = new CascadeClassifier(cascadeFile.getAbsolutePath());
        detectorEye.load(cascadeFile.getAbsolutePath());
        if (detectorEye.empty()) {
            Log.e(TAG, "Failed to load cascade classifier for eye");
            detectorEye = null;
        } else {
            Log.i(TAG, "Loaded cascade classifier from " + cascadeFile.getAbsolutePath());
        }
    }

    private void setCameraParameters() {
        openCvCameraView.setCameraIndex(CAMERA_ID_FRONT);
        openCvCameraView.enableFpsMeter();
        openCvCameraView.enableView();

    }

    private synchronized void initializeJavaDetectorForFace() {
        detectorFace = new CascadeClassifier(cascadeFile.getAbsolutePath());
        detectorFace.load(cascadeFile.getAbsolutePath());
        if (detectorFace.empty()) {
            Log.e(TAG, "Failed to load cascade classifier");
            detectorFace = null;
        } else {
            Log.i(TAG, "Loaded cascade classifier from " + cascadeFile.getAbsolutePath());
        }
    }

    private File readCascadeFile(int rawFile, String dir, String fileOutput) throws IOException {
        // load cascade file from application resources
        InputStream is = getResources().openRawResource(rawFile);
        File cascadeDir = getDir(dir, Context.MODE_PRIVATE);
        cascadeFile = new File(cascadeDir, fileOutput);
        FileOutputStream os = new FileOutputStream(cascadeFile);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
        is.close();
        os.close();
        return cascadeDir;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_face_detection_opencv);

        openCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_surface_view);
        setCamera();

        mainHandler = new Handler(Looper.myLooper());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA);
        }
    }

    private void setCamera() {
        openCvCameraView.setVisibility(SurfaceView.VISIBLE);
        openCvCameraView.setMaxFrameSize(640, 480);
        openCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, getString(R.string.no_permissions_allowed), Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }
        }
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CAMERA:
                setCamera();
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onPause() {
        if (openCvCameraView != null) {
            openCvCameraView.disableView();
        }
        super.onPause();
    }


    public void onDestroy() {
        if (openCvCameraView != null) {
            openCvCameraView.disableView();
        }

        mainHandler = null;
        faceRecognitionInteractor = null;
        onCameraViewStopped();
        matrixRgba = null;
        matrixGray = null;
        detectorEye = null;
        detectorFace = null;
        super.onDestroy();
    }

    public void onCameraViewStarted(int width, int height) {
        matrixGray = new Mat();
        matrixRgba = new Mat();
        isStopped = false;
    }

    public void onCameraViewStopped() {
        isStopped = true;
        matrixRgba.release();
        matrixGray.release();
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();
        Mat gray = inputFrame.gray();
        setMatrices(rgba, gray);
        if (isMachineLearningInitialised && !isStopped) {
            faceRecognitionInteractor.execute(matrixGray, this);
        }
        return matrixRgba;
    }

    private synchronized void setMatrices(Mat rgba, Mat gray) {
        if (!isBlocked) {
            matrixRgba = rgba;
            matrixGray = gray;
            isBlocked = true;
        }
    }

    private void setMachineLearningMechanism() {
        if (!isMachineLearningInitialised) {
            faceRecognitionInteractor = new FaceRecognitionInteractorImpl(detectorFace, mainHandler);
            eyesRecognitionInteractor = new EyesRecognitionInteractorImpl(detectorEye, mainHandler);
            isMachineLearningInitialised = true;
        }
    }

    @Override
    public void onEyesRecognised() {
        if (!isStopped) {
            Log.i(TAG, "Eyes recognised and rendered");
            isBlocked = false;
        }
    }

    @Override
    public void onFaceRecognised(Rect faceOpenCV, RecognisedFace face) {
        Log.i(TAG, "Face recognised and rendered");
        if (!isStopped) {
            FaceDrawerOpenCV.drawFaceShapes(faceOpenCV, matrixRgba);
            eyesRecognitionInteractor.execute(matrixGray, matrixRgba, faceOpenCV, this);
        }
    }
}
