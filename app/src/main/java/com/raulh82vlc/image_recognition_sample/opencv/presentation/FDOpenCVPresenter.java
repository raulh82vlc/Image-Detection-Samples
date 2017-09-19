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

package com.raulh82vlc.image_recognition_sample.opencv.presentation;

import android.content.Context;
import android.util.Log;
import android.view.SurfaceView;

import com.raulh82vlc.ar_imagerecognition_sample.R;
import com.raulh82vlc.image_recognition_sample.domain.InteractorExecutor;
import com.raulh82vlc.image_recognition_sample.domain.InteractorPoolExecutor;
import com.raulh82vlc.image_recognition_sample.domain.MainThread;
import com.raulh82vlc.image_recognition_sample.model.Face;
import com.raulh82vlc.image_recognition_sample.opencv.domain.EyesDetectionInteractor;
import com.raulh82vlc.image_recognition_sample.opencv.domain.EyesDetectionInteractorImpl;
import com.raulh82vlc.image_recognition_sample.opencv.domain.FDInteractor;
import com.raulh82vlc.image_recognition_sample.opencv.domain.FDInteractorImpl;
import com.raulh82vlc.image_recognition_sample.opencv.domain.FileHelper;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.IOException;

/**
 * Face Detection OpenCV Presenter decouples OpenCV logic from activity
 * @author Raul Hernandez Lopez.
 */
public class FDOpenCVPresenter implements
        FDInteractor.FaceCallback,
        EyesDetectionInteractor.EyesCallback,
        CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = FDOpenCVPresenter.class.getSimpleName();

    private static final int CAMERA_ID_FRONT = 1;
    public static final int MAX_WIDTH = 640;
    public static final int MAX_HEIGHT = 480;

    // Presentation
    private View view;
    // UIThread
    private final MainThread mainHandler;
    // Thread pool
    private final InteractorExecutor interactorExecutor;
    // Domain
    private FDInteractor fdInteractor;
    private EyesDetectionInteractor eyesDetectionInteractor;
    // Camera Lifecycle
    private boolean isStopped = false;
    private boolean isBlocked = false;
    // OpenCV
    // Camera Matrices
    private Mat matrixRgba;
    private Mat matrixGray;
    // Classifiers
    private CascadeClassifier detectorEye;
    private CascadeClassifier detectorFace;
    private boolean isMachineLearningInitialised = false;

    public FDOpenCVPresenter(MainThread handler, View view) {
        mainHandler = handler;
        this.view = view;
        interactorExecutor = new InteractorPoolExecutor();
    }

    public void detectEyes(Rect faceOpenCV) {
        if (!isStopped && eyesDetectionInteractor != null) {
            eyesDetectionInteractor.execute(matrixGray, matrixRgba, faceOpenCV, this);
        }
    }

    public interface View {
        void drawFace(Rect faceOpenCV, Mat face);

        void startEyesDetection(Rect faceOpenCV);
    }

    private void setMachineLearningMechanism() {
        if (!isMachineLearningInitialised) {
            fdInteractor = new FDInteractorImpl(detectorFace, mainHandler, interactorExecutor);
            eyesDetectionInteractor = new EyesDetectionInteractorImpl(detectorEye, mainHandler, interactorExecutor);
            isMachineLearningInitialised = true;
        }
    }

    @Override
    public void onFaceDetected(Rect faceOpenCV, Face face) {
        if (!isStopped && view != null) {
            view.drawFace(faceOpenCV, matrixRgba);
            view.startEyesDetection(faceOpenCV);
        }
    }

    @Override
    public void onEyesDetected() {
        if (!isStopped) {
            isBlocked = false;
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        matrixGray = new Mat();
        matrixRgba = new Mat();
        isStopped = false;
        eyesDetectionInteractor.setRunningStatus(true);
    }

    @Override
    public void onCameraViewStopped() {
        isStopped = true;
        eyesDetectionInteractor.setRunningStatus(false);
        matrixRgba.release();
        matrixGray.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();
        Mat gray = inputFrame.gray();
        setMatrices(rgba, gray);
        if (isMachineLearningInitialised && !isStopped) {
            fdInteractor.execute(matrixGray, this);
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

    private synchronized CascadeClassifier initializeJavaDetector(File cascadeFile) {
        CascadeClassifier detector = new CascadeClassifier(cascadeFile.getAbsolutePath());
        detector.load(cascadeFile.getAbsolutePath());
        if (detector.empty()) {
            Log.e(TAG, "Failed to load cascade classifier");
            detector = null;
        } else {
            Log.i(TAG, "Loaded cascade classifier from " + cascadeFile.getAbsolutePath());
        }
        return detector;
    }

    public BaseLoaderCallback getLoader(final Context context, final CameraBridgeViewBase openCvCameraView) {
        return new BaseLoaderCallback(context) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS:
                        Log.i(TAG, "OpenCV loaded successfully");
                        try {
                            /** load face classificator */
                            File cascadeFileFace = FileHelper.readCascadeFile(context,
                                    R.raw.haarcascade_frontalface_alt2,
                                    "cascade", "haarcascade_front.xml");
                            detectorFace = initializeJavaDetector(cascadeFileFace);
                            cascadeFileFace.delete();
                            /** load eye classificator */
                            File cascadeFileEye = FileHelper.readCascadeFile(context,
                                    R.raw.haarcascade_eye_tree_eyeglasses,
                                    "cascadeER", "haarcascade_eye.xml");
                            detectorEye = initializeJavaDetector(cascadeFileEye);
                            cascadeFileEye.delete();
                            setMachineLearningMechanism();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                        }

                        setCameraParameters(openCvCameraView);

                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }
            }
        };
    }

    private void setCameraParameters(CameraBridgeViewBase openCvCameraView) {
        openCvCameraView.setCameraIndex(CAMERA_ID_FRONT);
        openCvCameraView.enableFpsMeter();
        openCvCameraView.enableView();
    }

    public void setCamera(CameraBridgeViewBase openCvCameraView) {
        openCvCameraView.setVisibility(SurfaceView.VISIBLE);
        openCvCameraView.setMaxFrameSize(MAX_WIDTH, MAX_HEIGHT);
        openCvCameraView.setCvCameraViewListener(this);
    }

    public void cleanUp() {
        fdInteractor = null;
        eyesDetectionInteractor = null;
        matrixRgba = null;
        matrixGray = null;
        detectorEye = null;
        detectorFace = null;
        view = null;
    }
}
