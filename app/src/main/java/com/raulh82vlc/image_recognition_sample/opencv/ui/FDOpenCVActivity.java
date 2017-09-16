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

package com.raulh82vlc.image_recognition_sample.opencv.ui;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.raulh82vlc.ar_imagerecognition_sample.R;
import com.raulh82vlc.image_recognition_sample.opencv.presentation.FaceRecognitionOpenCVPresenter;
import com.raulh82vlc.image_recognition_sample.opencv.render.FaceDrawerOpenCV;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

/**
 * Activity meant to be the UI point to show information to the user through OpenCV library
 */
public class FDOpenCVActivity extends Activity implements FaceRecognitionOpenCVPresenter.View {

    private static final String TAG = FDOpenCVActivity.class.getSimpleName();
    // CONSTANTS
    private static final int PERMISSIONS_REQUEST_CAMERA = 666;

    private CameraBridgeViewBase openCvCameraView;

    private FaceRecognitionOpenCVPresenter presenter;

    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_face_detection_opencv);

        openCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_surface_view);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA);
        } else {
            initPresenter();
            presenter.setCamera(openCvCameraView);
        }
    }

    private void initPresenter() {
        if (presenter == null) {
            mainHandler = new Handler(Looper.myLooper());
            presenter = new FaceRecognitionOpenCVPresenter(mainHandler, this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        initPresenter();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, presenter.getLoader(getApplicationContext(), openCvCameraView));
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            presenter.getLoader(getApplicationContext(), openCvCameraView).onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
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
                initPresenter();
                presenter.setCamera(openCvCameraView);
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
            openCvCameraView = null;
        }
        mainHandler = null;
        presenter.onCameraViewStopped();
        presenter.cleanUp();
        presenter = null;
        super.onDestroy();
    }

    @Override
    public void drawFace(Rect faceOpenCV, Mat rgbaMat) {
        Log.i(TAG, "Face recognised and rendered");
        FaceDrawerOpenCV.drawFaceShapes(faceOpenCV, rgbaMat);
    }
}
