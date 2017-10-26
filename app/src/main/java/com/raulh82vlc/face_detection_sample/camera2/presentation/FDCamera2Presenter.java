/*
 * Copyright (C) 2017 Raul Hernandez Lopez @raulh82vlc
 * This presenter class uses Camera2 component methods from the Sample API
 * of The Android Open Source Project and has been extended to make this Presenter
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

package com.raulh82vlc.face_detection_sample.camera2.presentation;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;

import com.raulh82vlc.face_detection_sample.domain.MainThread;
import com.raulh82vlc.face_detection_sample.model.Face;
import com.raulh82vlc.face_detection_sample.camera2.ui.FDCamera2Activity;
import com.raulh82vlc.face_detection_sample.ui.widgets.AutofitTextureView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Face Detection Camera 2 Presenter decouples Camera 2 logic from activity
 * @author Raul Hernandez Lopez.
 */

public class FDCamera2Presenter {
    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final String TAG = FDCamera2Presenter.class.getSimpleName();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private CameraCaptureSession cameraCaptureSessions;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraDevice cameraDevice;

    private MainThread mainThread;
    private HandlerThread backgroundThread;
    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore cameraOpenCloseLock = new Semaphore(1);
    private Size previewSize;

    private Size imageDimension;
    private FDCamera2Activity activityView;
    private View presenterView;

    public FDCamera2Presenter(MainThread mainThread) {
        this.mainThread = mainThread;
    }

    /**
     * Camera Face View (Callback) used when Camera 2 API returns a {@link Face} from Presenter
     * to the UI (activity)
     */
    public interface View {

        void onFaceDetected(Face face);

        void drawAR(Face face);
    }

    public void drawAR(Face face) {
        presenterView.drawAR(face);
    }

    public AutofitTextureView.SurfaceTextureListener getListener() {
        return textureListener;
    }

    private final AutofitTextureView.SurfaceTextureListener textureListener =  new AutofitTextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera(width, height);
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
            configureTransform(width, height);
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return true;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };


    /**
     * Compares two {@code Size}s based on their areas.
     */
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraOpenCloseLock.release();
            Log.i(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraOpenCloseLock.release();
            camera.close();
            cameraDevice = null;
        }
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraOpenCloseLock.release();
            camera.close();
            cameraDevice = null;
        }
    };

    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        private void detectFaces(CaptureResult captureResult) {
            Integer mode = captureResult.get(CaptureResult.STATISTICS_FACE_DETECT_MODE);

            if (isViewAvailable() && mode != null) {
                android.hardware.camera2.params.Face[] faces = captureResult.get(CaptureResult.STATISTICS_FACES);
                if (faces != null) {
                    Log.i(TAG, "faces : " + faces.length + " , mode : " + mode);
                    for (android.hardware.camera2.params.Face face : faces) {
                        Rect faceBounds = face.getBounds();
                        // Once processed, the result is sent back to the View
                        presenterView.onFaceDetected(mapCameraFaceToCanvas(faceBounds, face.getLeftEyePosition(),
                                face.getRightEyePosition()));
                    }
                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                        @NonNull CaptureResult captureResult) {
            detectFaces(captureResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult captureResult) {
            detectFaces(captureResult);
        }
    };

    public void setView(FDCamera2Activity camera2View, View callback) {
        this.activityView = camera2View;
        this.presenterView = callback;
    }

    private void openCamera(int width, int height) {
        if (isViewAvailable()) {
            if (ActivityCompat.checkSelfPermission(activityView, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(activityView, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activityView, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        FDCamera2Activity.PERMISSIONS_REQUEST_CAMERA);
                return;
            }
            setUpCameraOutputs(width, height);
            configureTransform(previewSize.getHeight(), previewSize.getWidth());
            // Add permission for camera and let user grant the permission
            CameraManager manager = (CameraManager) activityView.getSystemService(Context.CAMERA_SERVICE);

            try {
                if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException("Time out waiting to lock camera opening.");
                }
                String[] cameras = manager.getCameraIdList();
                String cameraId = cameras[1];
//                CameraCharacteristics characteristics
//                        = manager.getCameraCharacteristics(cameraId);
                // Check detectModes
//                int[] faceDetectModes = characteristics.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES);
                manager.openCamera(cameraId, stateCallback, mainThread.get());
            } catch (CameraAccessException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
            }
        }
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    private void setUpCameraOutputs(int width, int height) {
        if (isViewAvailable()) {
            CameraManager manager = (CameraManager) activityView.getSystemService(Context.CAMERA_SERVICE);
            try {
                for (String cameraId : manager.getCameraIdList()) {
                    CameraCharacteristics characteristics
                            = manager.getCameraCharacteristics(cameraId);

                    // We don't use a front facing camera in this sample.
                    Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        continue;
                    }

                    StreamConfigurationMap map = characteristics.get(
                            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    if (map == null) {
                        continue;
                    }

                    imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];

                    // For still image captures, we use the largest available size.
                    Size largest = Collections.max(
                            Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                            new CompareSizesByArea());

                    Point displaySize = new Point();
                    activityView.getWindowManager().getDefaultDisplay().getSize(displaySize);
                    int rotatedPreviewWidth = width;
                    int rotatedPreviewHeight = height;
                    int maxPreviewWidth = displaySize.x;
                    int maxPreviewHeight = displaySize.y;

                    if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                        maxPreviewWidth = MAX_PREVIEW_WIDTH;
                    }

                    if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                        maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                    }

                    // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                    // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                    // garbage capture data.
                    previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                            rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                            maxPreviewHeight, largest);

                    // We fit the aspect ratio of TextureView to the size of preview we picked.
                    int orientation = activityView.getResources().getConfiguration().orientation;
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        activityView.getTextureView().setAspectRatio(
                                previewSize.getWidth(), previewSize.getHeight());
                    } else {
                        activityView.getTextureView().setAspectRatio(
                                previewSize.getHeight(), previewSize.getWidth());
                    }
                    return;
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                // Currently an NPE is thrown when the Camera2API is used but not supported on the
                // device this code runs.
            }
        }
    }

    private void updatePreview() {
        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                CameraMetadata.STATISTICS_FACE_DETECT_MODE_FULL);

        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(),
                    captureCallback, mainThread.get());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void closeCamera() {
        try {
            cameraOpenCloseLock.acquire();
            if (null != cameraCaptureSessions) {
                cameraCaptureSessions.close();
                cameraCaptureSessions = null;
            }
            if (null != cameraDevice) {
                cameraDevice.close();
                cameraDevice = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            cameraOpenCloseLock.release();
        }
    }

    public void startBackgroundThread() {
        backgroundThread = new HandlerThread("Camera Background");
        backgroundThread.start();
    }

    public void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            mainThread = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Maps {@link Rect} face bounds from Camera 2 API towards the model used throughout the samples
     * then passes the RecognisedFace object back
     * @param faceBounds
     * @param leftEyePosition
     * @param rightEyePosition
     */
    private Face mapCameraFaceToCanvas(Rect faceBounds, Point leftEyePosition, Point rightEyePosition) {
        if (isViewAvailable()) {
            int w = faceBounds.width();
            int h = faceBounds.height();
            Face face = new Face(
                    faceBounds.centerX() - w,
                    faceBounds.centerY(),
                    w,
                    h);
            if (leftEyePosition != null) {
                face.setIrisLeft(leftEyePosition.x, leftEyePosition.y);
            }
            return face;
        }
        return new Face();
    }

    public void cleanUp() {
        mainThread = null;
        if (null != cameraCaptureSessions) {
            cameraCaptureSessions.close();
            cameraCaptureSessions = null;
        }
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != captureCallback) {
            captureCallback = null;
        }
    }

    public boolean isViewAvailable() {
        return activityView != null && presenterView != null;
    }

    public void onResume() {
        if (isViewAvailable()) {
            AutofitTextureView textureView = activityView.getTextureView();
            if (textureView.isAvailable()) {
                openCamera(textureView.getWidth(), textureView.getHeight());
            } else {
                textureView.setSurfaceTextureListener(getListener());
            }
        }
    }

    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    private void createCameraPreview() {
        if (isViewAvailable()) {
            try {
                SurfaceTexture texture = activityView.getTextureView().getSurfaceTexture();
                assert texture != null;

                texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
                Surface surface = new Surface(texture);
                captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                captureRequestBuilder.addTarget(surface);
                cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                        //The camera is already closed
                        if (null == cameraDevice) {
                            return;
                        }
                        // When the session is ready, we start displaying the preview.
                        cameraCaptureSessions = cameraCaptureSession;
                        updatePreview();
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                        //                Toast.makeText(this, "Configuration change", Toast.LENGTH_SHORT).show();
                    }
                }, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        if (isViewAvailable()) {
            if (null == activityView.getTextureView()) {
                return;
            }
            int rotation = activityView.getWindowManager().getDefaultDisplay().getRotation();
            Matrix matrix = new Matrix();
            RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
            RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
            float centerX = viewRect.centerX();
            float centerY = viewRect.centerY();
            if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
                bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
                float scale = Math.max(
                        (float) viewHeight / previewSize.getHeight(),
                        (float) viewWidth / previewSize.getWidth());
                matrix.postScale(scale, scale, centerX, centerY);
                matrix.postRotate(90 * (rotation - 2), centerX, centerY);
            } else if (Surface.ROTATION_180 == rotation) {
                matrix.postRotate(180, centerX, centerY);
            }
            activityView.getTextureView().setTransform(matrix);
        }
    }
}
