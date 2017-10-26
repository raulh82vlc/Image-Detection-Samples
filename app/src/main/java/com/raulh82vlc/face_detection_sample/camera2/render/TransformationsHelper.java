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

package com.raulh82vlc.face_detection_sample.camera2.render;

import com.raulh82vlc.face_detection_sample.camera2.render.model.MeasuresUI;

/**
 * Transformation measures helper
 *
 * @author Raul Hernandez Lopez.
 */

final class TransformationsHelper {
    private TransformationsHelper() {}

    static MeasuresUI calcMeasures(int w, int h, int viewWidth, int viewHeight, int x, int y, int widthGraphic, int heightGraphic) {
        float scaleGraphic;
        if (viewWidth * h > w * viewHeight) {
            scaleGraphic = ((float) h / (float) viewHeight) * 2;
        } else {
            scaleGraphic = ((float) w / (float) viewWidth) * 2;
        }

        int dxGraphic = (int) ((widthGraphic - (widthGraphic * scaleGraphic)));
        int dyGraphic = (int) ((heightGraphic - (heightGraphic * scaleGraphic)));
        int posX = (int) (x * scaleGraphic) - (dxGraphic / 2) + dxGraphic;
        int posY = (int) (y * scaleGraphic) - (dyGraphic / 2) + dyGraphic;
        return new MeasuresUI(scaleGraphic, posX, posY);
    }
}
