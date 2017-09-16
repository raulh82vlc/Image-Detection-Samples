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

package com.raulh82vlc.image_recognition_sample.camera2.ui;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.raulh82vlc.ar_imagerecognition_sample.R;
import com.raulh82vlc.image_recognition_sample.camera2.render.FaceDrawer;
import com.raulh82vlc.image_recognition_sample.model.RecognisedFace;
import com.raulh82vlc.image_recognition_sample.camera2.presentation.FaceRecognitionCamera2Presenter;
import com.raulh82vlc.image_recognition_sample.ui.widgets.AutofitTextureView;

/**
 * Activity meant to be the UI point to show information to the user through Camera 2 API
 */
public class FDCamera2Activity extends Activity implements FaceRecognitionCamera2Presenter.CameraFaceCallback {

    private static final String TAG = FDCamera2Activity.class.getSimpleName();
    // CONSTANTS
    public static final int PERMISSIONS_REQUEST_CAMERA = 666;

    // UI
    private AutofitTextureView textureView;
    private FaceRecognitionCamera2Presenter presenter;
    private FaceDrawer faceDrawer;

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        presenter.startBackgroundThread();
        presenter.onResume();
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        presenter.closeCamera();
        presenter.stopBackgroundThread();
        super.onPause();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_face_detection);

        textureView = (AutofitTextureView) findViewById(R.id.texture);
        presenter = new FaceRecognitionCamera2Presenter(new Handler(Looper.myLooper()));
        presenter.setView(this, this);
        textureView.setSurfaceTextureListener(presenter.getListener());
        faceDrawer = (FaceDrawer) findViewById(R.id.face_drawer);
        faceDrawer.settings();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, getString(R.string.no_permissions_allowed), Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }
        }
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CAMERA:
                //HERE any camera action after permissions allowed
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onDestroy() {
        presenter.setView(null, null);
        textureView = null;
        faceDrawer = null;
        presenter.cleanUp();
        presenter = null;
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    public AutofitTextureView getTextureView() {
        return textureView;
    }

    @Override
    public void onFaceRecognised(RecognisedFace face) {
        if (faceDrawer != null) {
            faceDrawer.drawFaceHear(face);
        }
    }
}
