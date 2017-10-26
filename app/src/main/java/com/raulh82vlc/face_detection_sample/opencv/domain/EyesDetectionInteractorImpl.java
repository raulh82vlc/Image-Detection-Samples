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

package com.raulh82vlc.face_detection_sample.opencv.domain;

import android.support.annotation.NonNull;

import com.raulh82vlc.face_detection_sample.domain.InteractorExecutor;
import com.raulh82vlc.face_detection_sample.domain.MainThread;
import com.raulh82vlc.face_detection_sample.opencv.render.FaceDrawerOpenCV;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

/**
 * Eyes Detection Interactor implementation of the {@link EyesDetectionInteractor} contract
 * @author Raul Hernandez Lopez.
 */
public class EyesDetectionInteractorImpl implements Interactor, EyesDetectionInteractor {
    // Constants
    private static final int LEARN_FRAMES_LIMIT = 50;
    private static final int LEARN_FRAMES_MATCH_EYE = 25;
    private static final int EYE_MIN_SIZE = 30;
    private static final int IRIS_MIN_SIZE = 24;
    // Frames
    private int learnFrames = 0;
    private int learnFramesCounter = 0;
    // Cascade classifier
    private final CascadeClassifier detectorEye;
    // Templates
    private Mat templateRight;
    private Mat templateLeft;
    // Previously detected face
    private Rect face;
    // Image Matrices
    private Mat matrixGray, matrixRGBA;
    // Interactor mechanism
    private final InteractorExecutor executor;
    private final MainThread mainThread;
    private EyesCallback eyesCallback;
    private boolean isRunning = false;

    public EyesDetectionInteractorImpl(CascadeClassifier detectorEye,
                                       MainThread mainThread, InteractorExecutor executor) {
        this.detectorEye = detectorEye;
        this.executor =  executor;
        this.mainThread = mainThread;
    }

    @Override
    public void run() {
        extractEyes();
    }

    @Override
    public void execute(Mat matrixGray, Mat matrixRGBA, Rect face, EyesCallback callback) {
        this.matrixGray = matrixGray;
        this.matrixRGBA = matrixRGBA;
        this.face = face;
        this.eyesCallback = callback;
        isRunning = true;
        executor.execute(this);
    }

    @Override
    public void setRunningStatus(boolean isRunning) {
        this.isRunning = isRunning;
    }

    private void extractEyes() {
        if (isRunning) {
            // computing eye areas as well as splitting it
            Rect rightEyeArea = getEyeArea(face.x + face.width / 16,
                    (int) (face.y + (face.height / 4.5)),
                    (face.width - 2 * face.width / 16) / 2,
                    (int) (face.height / 3.0));
            Rect leftEyeArea = getEyeArea(face.x + face.width / 16
                    + (face.width - 2 * face.width / 16) / 2,
                    (int) (face.y + (face.height / 4.5)),
                    (face.width - 2 * face.width / 16) / 2,
                    (int) (face.height / 3.0));

            FaceDrawerOpenCV.drawEyesRectangles(rightEyeArea, leftEyeArea, matrixRGBA);

            String methodForEyes;
            if (learnFrames < LEARN_FRAMES_LIMIT) {
                templateRight = buildTemplate(rightEyeArea, IRIS_MIN_SIZE, matrixGray, matrixRGBA,
                        detectorEye);
                templateLeft = buildTemplate(leftEyeArea, IRIS_MIN_SIZE, matrixGray, matrixRGBA,
                        detectorEye);
                learnFrames++;
                methodForEyes = "building Template with Detect multiscale, frame: " + learnFrames;
            } else {
                // Learning finished, use the new templates for template matching
                matchEye(rightEyeArea, templateRight, matrixGray, matrixRGBA);
                matchEye(leftEyeArea, templateLeft, matrixGray, matrixRGBA);
//                resetChronometerOfFrames();
                methodForEyes = "match eye with Template, frame: " + learnFrames;
            }
            notifyEyesFound(methodForEyes);
        }
    }

    @NonNull
    private static Rect getEyeArea(int x, int y, int width, int height) {
        return new Rect(x,
                y,
                width,
                height);
    }

    private synchronized void resetChronometerOfFrames() {
        if (learnFramesCounter > LEARN_FRAMES_MATCH_EYE) {
            learnFrames = 0;
            learnFramesCounter = 0;
        }
        learnFramesCounter++;
    }

    /**
     * Matches concrete point of the eye by using template with TM_SQDIFF_NORMED
     */
    private static void matchEye(Rect area, Mat builtTemplate, Mat matrixGray, Mat matrixRGBA) {
        Point matchLoc;
        try {
            // when there is not builtTemplate we skip it
            if (builtTemplate.cols() == 0 || builtTemplate.rows() == 0) {
                return;
            }
            Mat submatGray = matrixGray.submat(area);
            int cols = submatGray.cols() - builtTemplate.cols() + 1;
            int rows = submatGray.rows() - builtTemplate.rows() + 1;
            Mat outputTemplateMat = new Mat(cols, rows, CvType.CV_8U);

            Imgproc.matchTemplate(submatGray, builtTemplate, outputTemplateMat,
                    Imgproc.TM_SQDIFF_NORMED);
            Core.MinMaxLocResult minMaxLocResult = Core.minMaxLoc(outputTemplateMat);
            // when is difference in matching methods, the best match is max / min value
            matchLoc = minMaxLocResult.minLoc;
            Point matchLocTx = new Point(matchLoc.x + area.x, matchLoc.y + area.y);
            Point matchLocTy = new Point(matchLoc.x + builtTemplate.cols() + area.x,
                    matchLoc.y + builtTemplate.rows() + area.y);

            FaceDrawerOpenCV.drawMatchedEye(matchLocTx, matchLocTy, matrixRGBA);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * <p>Build a template from a specific eye area previously substracted
     * uses detectMultiScale for this area, then uses minMaxLoc method to
     * detect iris from the detected eye</p>
     *
     * @param area Preformatted Area
     * @param size minimum iris size
     * @param grayMat image in gray
     * @param rgbaMat image in color
     * @param detectorEye Haar Cascade classifier
     * @return built template
     */
    @NonNull
    private static Mat buildTemplate(Rect area, final int size,
                                     @NonNull Mat grayMat,
                                     @NonNull Mat rgbaMat,
                                     CascadeClassifier detectorEye) {
        Mat template = new Mat();
        Mat graySubMatEye = grayMat.submat(area);
        MatOfRect eyes = new MatOfRect();
        Rect eyeTemplate;
        detectorEye.detectMultiScale(graySubMatEye, eyes, 1.15, 2,
                Objdetect.CASCADE_FIND_BIGGEST_OBJECT
                        | Objdetect.CASCADE_SCALE_IMAGE, new Size(EYE_MIN_SIZE, EYE_MIN_SIZE),
                new Size());

        Rect[] eyesArray = eyes.toArray();
        if (eyesArray.length > 0) {
            Rect e = eyesArray[0];
            e.x = area.x + e.x;
            e.y = area.y + e.y;
            Rect eyeRectangle = getEyeArea((int) e.tl().x,
                    (int) (e.tl().y + e.height * 0.4),
                    e.width,
                    (int) (e.height * 0.6));
            graySubMatEye = grayMat.submat(eyeRectangle);
            Mat rgbaMatEye = rgbaMat.submat(eyeRectangle);


            Core.MinMaxLocResult minMaxLoc = Core.minMaxLoc(graySubMatEye);

            FaceDrawerOpenCV.drawIrisCircle(rgbaMatEye, minMaxLoc);
            Point iris = new Point();
            iris.x = minMaxLoc.minLoc.x + eyeRectangle.x;
            iris.y = minMaxLoc.minLoc.y + eyeRectangle.y;
            eyeTemplate = getEyeArea((int) iris.x - size / 2,
                    (int) iris.y
                            - size / 2, size, size);
            FaceDrawerOpenCV.drawEyeRectangle(eyeTemplate, rgbaMat);
            template = (grayMat.submat(eyeTemplate)).clone();
        }
        return template;
    }

    private void notifyEyesFound(final String methodForEyes) {
        mainThread.post(new Runnable() {
            @Override
            public void run() {
                eyesCallback.onEyesDetected(methodForEyes);
            }
        });
    }
}
