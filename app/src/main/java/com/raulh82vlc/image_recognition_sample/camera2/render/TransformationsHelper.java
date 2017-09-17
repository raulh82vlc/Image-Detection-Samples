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

package com.raulh82vlc.image_recognition_sample.camera2.render;

import com.raulh82vlc.image_recognition_sample.camera2.render.model.MeasuresUI;

/**
 * Transformation measures helper
 *
 * @author Raul Hernandez Lopez.
 */

final class TransformationsHelper {
    private TransformationsHelper() {}

    static MeasuresUI calcMeasures(int w, int h, int wBit, int hBit) {
        float scale;
        float dx, dy;
        if (wBit * h > w * hBit) {
            scale = ((float) h / (float) hBit) * 2;
            dx = (w - wBit * scale) * 0.5f;
            dy = 0f;
        } else {
            scale = ((float) w / (float) wBit) * 2;
            dy = (h - hBit * scale) * 0.5f;
            dx = 0f;
        }
        return new MeasuresUI(scale, dx, dy);
    }
}
