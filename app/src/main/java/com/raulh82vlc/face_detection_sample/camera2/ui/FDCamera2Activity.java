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

package com.raulh82vlc.face_detection_sample.camera2.ui;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import com.raulh82vlc.ar_face_detection_sample.R;
import com.raulh82vlc.face_detection_sample.camera2.render.FaceDrawer;
import com.raulh82vlc.face_detection_sample.domain.MainThreadImpl;
import com.raulh82vlc.face_detection_sample.model.Face;
import com.raulh82vlc.face_detection_sample.camera2.presentation.FDCamera2Presenter;
import com.raulh82vlc.face_detection_sample.ui.widgets.AutofitTextureView;

/**
 * UI detection through Camera 2 API
 */
public class FDCamera2Activity extends Activity implements FDCamera2Presenter.View {

    private static final String TAG = FDCamera2Activity.class.getSimpleName();
    // CONSTANTS
    public static final int PERMISSIONS_REQUEST_CAMERA = 666;

    // UI
    private AutofitTextureView textureView;
    private FDCamera2Presenter presenter;
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
        presenter = new FDCamera2Presenter(new MainThreadImpl());
        presenter.setView(this, this);
        textureView.setSurfaceTextureListener(presenter.getListener());
        faceDrawer = (FaceDrawer) findViewById(R.id.face_drawer);
        Point size = getSize();
        int width = size.x;
        int height = size.y;
        faceDrawer.setScreenSize(width, height);
    }

    @NonNull
    private Point getSize() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
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
    public void onFaceDetected(Face face) {
        if (presenter != null) {
            presenter.drawAR(face);
        }
    }

    @Override
    public void drawAR(Face face) {
        if (faceDrawer != null) {
            faceDrawer.drawFaceHear(face);
        }
    }
}
