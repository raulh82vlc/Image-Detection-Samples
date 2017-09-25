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

import com.raulh82vlc.image_recognition_sample.domain.InteractorExecutor;
import com.raulh82vlc.image_recognition_sample.domain.MainThread;
import com.raulh82vlc.image_recognition_sample.opencv.render.FaceDrawerOpenCV;

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
    private static final int LEARN_FRAMES_LIMIT = 50;
    private static final int LEARN_FRAMES_MATCH_EYE = 25;
    private int learnFrames = 0;
    private final CascadeClassifier detectorEye;
    private int learnFramesCounter = 0;
    private Mat templateRight;
    private Mat templateLeft;
    private Rect face;
    private Mat matrixGray, matrixRGBA;
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
            Rect rightEyeArea = new Rect(face.x + face.width / 16,
                    (int) (face.y + (face.height / 4.5)),
                    (face.width - 2 * face.width / 16) / 2, (int) (face.height / 3.0));
            Rect leftEyeArea = new Rect(face.x + face.width / 16
                    + (face.width - 2 * face.width / 16) / 2,
                    (int) (face.y + (face.height / 4.5)),
                    (face.width - 2 * face.width / 16) / 2, (int) (face.height / 3.0));
            FaceDrawerOpenCV.drawEyesRectangles(rightEyeArea, leftEyeArea, matrixRGBA);
            String methodForEyes;
            if (learnFrames < LEARN_FRAMES_LIMIT) {
                templateRight = buildTemplate(rightEyeArea, 24, matrixGray, matrixRGBA, detectorEye);
                templateLeft = buildTemplate(leftEyeArea, 24, matrixGray, matrixRGBA, detectorEye);
                learnFrames++;
                methodForEyes = "building Template with Detect multiscale, frame: " + learnFrames;
            } else {
                // Learning finished, use the new templates for template matching
                matchEye(rightEyeArea, templateRight);
                matchEye(leftEyeArea, templateLeft);
//                resetChronometerOfFrames();
                methodForEyes = "match eye with Template, frame: " + learnFrames;
            }
            notifyEyesFound(methodForEyes);
        }
    }

    private synchronized void resetChronometerOfFrames() {
        if (learnFramesCounter > LEARN_FRAMES_MATCH_EYE) {
            learnFrames = 0;
            learnFramesCounter = 0;
        }
        learnFramesCounter++;
    }

    /**
     * Matches concrete point of the eye
     * @param area
     * @param template
     */
    private void matchEye(Rect area, Mat template) {
        Point matchLoc;
        try {
            Mat mROI = matrixGray.submat(area);
            int result_cols = mROI.cols() - template.cols() + 1;
            int result_rows = mROI.rows() - template.rows() + 1;
            // Check for bad template size
            if (template.cols() == 0 || template.rows() == 0) {
                return;
            }
            Mat mResult = new Mat(result_cols, result_rows, CvType.CV_8U);

            Imgproc.matchTemplate(mROI, template, mResult,
                    Imgproc.TM_SQDIFF_NORMED);
            Core.MinMaxLocResult mmres = Core.minMaxLoc(mResult);
            // there is difference in matching methods - best match is max/min value
            matchLoc = mmres.minLoc;
            Point matchLoc_tx = new Point(matchLoc.x + area.x, matchLoc.y + area.y);
            Point matchLoc_ty = new Point(matchLoc.x + template.cols() + area.x,
                    matchLoc.y + template.rows() + area.y);

            FaceDrawerOpenCV.drawMatchedEye(matchLoc_tx, matchLoc_ty, matrixRGBA);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Mat buildTemplate(Rect area, int size, Mat grayMat,
                                     Mat rgbaMat,
                                     CascadeClassifier detectorEye) {
        Mat template = new Mat();
        Mat mROI = grayMat.submat(area);
        MatOfRect eyes = new MatOfRect();
        Point iris = new Point();
        Rect eyeTemplate;
        detectorEye.detectMultiScale(mROI, eyes, 1.15, 2,
                Objdetect.CASCADE_FIND_BIGGEST_OBJECT
                        | Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30),
                new Size());

        Rect[] eyesArray = eyes.toArray();
        if (eyesArray.length > 0) {
            Rect e = eyesArray[0];
            e.x = area.x + e.x;
            e.y = area.y + e.y;
            Rect eyeRectangle = new Rect((int) e.tl().x,
                    (int) (e.tl().y + e.height * 0.4), e.width,
                    (int) (e.height * 0.6));
            mROI = grayMat.submat(eyeRectangle);
            Mat vyrez = rgbaMat.submat(eyeRectangle);


            Core.MinMaxLocResult mmG = Core.minMaxLoc(mROI);

            FaceDrawerOpenCV.drawIrisCircle(vyrez, mmG);
            iris.x = mmG.minLoc.x + eyeRectangle.x;
            iris.y = mmG.minLoc.y + eyeRectangle.y;
            eyeTemplate = new Rect((int) iris.x - size / 2, (int) iris.y
                    - size / 2, size, size);
            FaceDrawerOpenCV.drawIrisRectangle(eyeTemplate, rgbaMat);
            template = (grayMat.submat(eyeTemplate)).clone();
            return template;
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
