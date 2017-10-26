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

package com.raulh82vlc.face_detection_sample.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.raulh82vlc.ar_face_detection_sample.R;
import com.raulh82vlc.face_detection_sample.camera2.ui.FDCamera2Activity;
import com.raulh82vlc.face_detection_sample.opencv.ui.FDOpenCVActivity;

/**
 * Main menu activity
 * @author Raul Hernandez Lopez.
 */
public class MainActivity extends Activity implements View.OnClickListener {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        findViewById(R.id.opencv).setOnClickListener(this);
        findViewById(R.id.camera).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.opencv:
                startActivity(new Intent(this, FDOpenCVActivity.class));
                break;
            case R.id.camera:
                startActivity(new Intent(this, FDCamera2Activity.class));
                break;
        }
    }
}
