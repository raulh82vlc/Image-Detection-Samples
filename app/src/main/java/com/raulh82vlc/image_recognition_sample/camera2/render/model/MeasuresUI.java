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

package com.raulh82vlc.image_recognition_sample.camera2.render.model;

/**
 * Measures for UI rendering
 * @author Raul Hernandez Lopez.
 */

public class MeasuresUI {

    private final float scale;
    private final float dx;
    private final float dy;

    public MeasuresUI(float scale, float dx, float dy) {
        this.scale = scale;
        this.dx = dx;
        this.dy = dy;
    }

    public float getScale() {
        return scale;
    }

    public float getDx() {
        return dx;
    }

    public float getDy() {
        return dy;
    }
}
