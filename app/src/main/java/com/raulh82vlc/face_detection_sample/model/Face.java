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

package com.raulh82vlc.face_detection_sample.model;

import android.support.annotation.NonNull;

import com.raulh82vlc.face_detection_sample.model.math.Point;
import com.raulh82vlc.face_detection_sample.model.math.Square;

/**
 * <p>Face shape model object full of attributes used on a face</p>
 * @author Raul Hernandez Lopez.
 */

public class Face {
    private Square faceShape;
    private Square eyeLeft;
    private Square eyeRight;
    private Point irisLeft;
    private Point irisRight;

    @NonNull
    private Square getSquare(double xStart, double yStart, int width, int height) {
        return new Square(new Point(xStart, yStart), width, height);
    }

    public Face() {

    }

    public Face(double xStart, double yStart, int width, int height) {
        this.faceShape = getSquare(xStart, yStart, width, height);
    }

    public void setEyeLeft(double xStart, double yStart, int width, int height) {
        this.eyeLeft = getSquare(xStart, yStart, width, height);
    }

    public void setEyeRight(double xStart, double yStart, int width, int height) {
        this.eyeRight = getSquare(xStart, yStart, width, height);
    }

    public void setIrisLeft(double x, double y) {
        this.irisLeft = new Point(x, y);
    }

    public void setIrisRight(double x, double y) {
        this.irisRight = new Point(x, y);
    }

    public Square getEyeLeft() {
        return eyeLeft;
    }

    public Square getEyeRight() {
        return eyeRight;
    }

    public Point getIrisLeft() {
        return irisLeft;
    }

    public Point getIrisRight() {
        return irisRight;
    }

    public Square getFaceShape() {
        return faceShape;
    }
}
