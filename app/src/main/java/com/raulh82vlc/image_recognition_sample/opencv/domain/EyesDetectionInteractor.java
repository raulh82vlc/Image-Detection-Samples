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

import com.raulh82vlc.image_recognition_sample.model.Face;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

/**
 * Eyes Detection interactor contract for eyes recognition use case
 * @author Raul Hernandez Lopez
 */

public interface EyesDetectionInteractor {
    void execute(Mat matrixGray, Mat matrixRGBA, Rect face, EyesCallback callback);

    void setRunningStatus(boolean isRunning);

    /**
     * <p>Eyes Callback used when OpenCV returns a satisfactory eyes recognition
     * as a future improvement, could be passed the structure {@link Face} with the eyes inside
     * by decoupling the code futher</p>
     *
     * @author Raul Hernandez Lopez.
     */
    interface EyesCallback {
        void onEyesDetected();
    }
}
