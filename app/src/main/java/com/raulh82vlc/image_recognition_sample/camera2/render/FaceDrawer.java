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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.raulh82vlc.ar_imagerecognition_sample.R;
import com.raulh82vlc.image_recognition_sample.camera2.render.model.MeasuresUI;
import com.raulh82vlc.image_recognition_sample.model.Face;
import com.raulh82vlc.image_recognition_sample.model.math.Square;

import java.util.HashMap;

import static android.graphics.BitmapFactory.decodeResource;

/**
 * <p>Face Drawer is responsible of having all UI elements ready for painting
 * as soon as there is a recognised face to paint anything</p>
 *
 * @author Raul Hernandez Lopez.
 */

public class FaceDrawer extends View {
    public static final String KEY_BITMAP = "Goku_Sayan";
    private HashMap<String, Bitmap> mStore = new HashMap <>();
    private Paint paint;
    private Face face;
    private Paint paintMarker;
    private Rect rect;
    private Matrix transformation;

    public FaceDrawer(Context context) {
        super(context);
        setAttributes();
    }

    public FaceDrawer(Context context, AttributeSet attrs) {
        super(context, attrs);
        setAttributes();
    }

    private void setAttributes() {
        paint = new Paint();
        paintMarker = new Paint();
        paintMarker.setColor(Color.RED);
        paintMarker.setStrokeWidth(10);
        paintMarker.setStyle(Paint.Style.STROKE);
        // to create a discontinuous marker
        paintMarker.setPathEffect(new DashPathEffect(new float[] {10,20}, 0));
        paint.setFilterBitmap(true);
        mStore.put(KEY_BITMAP, decodeResource(getResources(),
                R.drawable.goku_supersayan));
        rect = new Rect();
        transformation = new Matrix();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (face != null) {
            Square faceShape = face.getFaceShape();
            int w =  faceShape.getWidth();
            int h =  faceShape.getHeight();
            drawFaceMarker(canvas, faceShape, w, h);
            drawBitmapHairAR(canvas, w, h);
        }
    }

    private void drawBitmapHairAR(Canvas canvas, int w, int h) {
        // Bitmap AR Super Sayan
        Bitmap bmp = mStore.get(KEY_BITMAP);
        if (bmp != null) {
            int wBit = bmp.getWidth();
            int hBit = bmp.getHeight();
            setTransformation(w, h, wBit, hBit);
            canvas.drawBitmap(bmp,
                    transformation, paint);
        }
    }

    private void drawFaceMarker(Canvas canvas, Square faceShape, int w, int h) {
        // Face AR mark
        setMarker(faceShape, w, h);
        canvas.drawRect(rect, paintMarker);
    }

    private void setTransformation(int w, int h, int wBit, int hBit) {
        MeasuresUI measures = TransformationsHelper.calcMeasures(w, h, wBit, hBit);
        transformation.setScale(measures.getScale(), measures.getScale());
        transformation.postTranslate(measures.getDx(), measures.getDy());
    }

    private void setMarker(Square faceShape, int w, int h) {
        int x = (int) faceShape.getStart().getxAxis();
        int y = (int) faceShape.getStart().getyAxis();
        int left = x;
        int right = x + w;
        int top = y;
        int bottom = y + h;
        // Marker
        rect.set(left, top, right, bottom);
    }

    public void drawFaceHear(Face face) {
        this.face = face;
        invalidate();
    }

    public void settings() {
        setAttributes();
    }
}
