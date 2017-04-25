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

package com.raulh82vlc.image_recognition_sample.ui.widgets;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

/**
 * <p>Draw face shapes utils for OpenCV</p>
 *
 * @author Raul Hernandez Lopez.
 */
public final class FaceDrawerOpenCV {

    private FaceDrawerOpenCV() {

    }

    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);

    public static void drawFaceShapes(Rect face, Mat matrixRGBA) {
        Point start = face.tl();
        int h = (int) start.y + (face.height / 2);
        int w = (int) start.x + (face.width / 2);
        Imgproc.rectangle(matrixRGBA, start, face.br(),
                FACE_RECT_COLOR, 3);
        Point center = new Point(w, h);
        Imgproc.circle(matrixRGBA, center, 10, new Scalar(255, 0, 0, 255), 3);
    }

    public static void drawEyesRectangles(Rect rightEyeArea, Rect leftEyeArea, Mat matrixRgba) {
        drawIrisRectangle(leftEyeArea, matrixRgba);
        drawIrisRectangle(rightEyeArea, matrixRgba);
    }

    public static void drawIrisRectangle(Rect eyeTemplate, Mat matrixRgba) {
        Imgproc.rectangle(matrixRgba, eyeTemplate.tl(), eyeTemplate.br(),
                new Scalar(255, 0, 0, 255), 2);
    }

    public static void drawIrisCircle(Mat vyrez, Core.MinMaxLocResult mmG) {
        Imgproc.circle(vyrez, mmG.minLoc, 2, new Scalar(255, 255, 255, 255), 2);
    }

    public static void drawMatchedEye(Point matchLoc_tx, Point matchLoc_ty, Mat matrixRgba) {
        Imgproc.rectangle(matrixRgba, matchLoc_tx, matchLoc_ty, new Scalar(255, 255, 0,
                255));
    }
}