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

package com.raulh82vlc.image_recognition_sample.opencv.domain;

import android.os.Handler;
import android.util.Log;

import com.raulh82vlc.image_recognition_sample.model.RecognisedFace;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.objdetect.CascadeClassifier;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Face Recognition implementation of {@link FaceRecognitionInteractor} contract
 * @author Raul Hernandez Lopez.
 */

public class FaceRecognitionInteractorImpl implements Interactor, FaceRecognitionInteractor {

    private static final float RELATIVE_FACE_SIZE = 0.2f;
    private static final String TAG = FaceRecognitionInteractor.class.getSimpleName();

    private final CascadeClassifier detectorFace;
    private final Handler mainThread;

    private MatOfRect faces;
    private final Executor executorImageRecognition;
    private Mat matrixGray;
    private int absoluteFaceSize = 0;
    private FaceCallback faceCallback;

    public FaceRecognitionInteractorImpl(CascadeClassifier detectorFace,
                                     Handler mainThread) {
        this.detectorFace = detectorFace;
        executorImageRecognition =  Executors.newSingleThreadExecutor();
        this.mainThread = mainThread;
    }

    @Override
    public void execute(Mat gray, FaceCallback callback) {
        this.matrixGray = gray;
        this.faceCallback = callback;
        executorImageRecognition.execute(this);
    }

    private void startDetectionAndRecognition() {
        if (absoluteFaceSize == 0) {
            int height = matrixGray.rows();
            if (Math.round(height * RELATIVE_FACE_SIZE) > 0) {
                absoluteFaceSize = Math.round(height * RELATIVE_FACE_SIZE);
            }
        }
        if (detectorFace != null) {
             if (faces == null) {
                 faces = new MatOfRect();
             }
            if (matrixGray.height() > 0) {
                detectorFace.detectMultiScale(matrixGray, faces, 1.1, 2, 2,
                        new Size(absoluteFaceSize, absoluteFaceSize), new Size());
            }
        }
    }

    @Override
    public void run() {
        startDetectionAndRecognition();
        Rect[] facesArray = null;
        if (faces != null) {
            synchronized (this) {
                if (faces != null) {
                    facesArray = faces.toArray();
                    Log.i(TAG, "Number of faces: " + facesArray.length);
                }
            }
            if (facesArray != null) {
                for (final Rect rect : facesArray) {
                    extractCharacteristics(rect);
                }
            }
        }
    }

    private void extractCharacteristics(Rect rect) {
        notifyFaceFound(rect, new RecognisedFace(rect.x,
                rect.y, rect.width, rect.height));
    }

    private void notifyFaceFound(final Rect faceOpenCV, final RecognisedFace recognisedFace) {
        mainThread.post(new Runnable() {
            @Override
            public void run() {
                faceCallback.onFaceRecognised(faceOpenCV, recognisedFace);
            }
        });
    }
}
