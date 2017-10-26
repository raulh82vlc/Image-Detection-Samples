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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.raulh82vlc.ar_face_detection_sample.R;
import com.raulh82vlc.face_detection_sample.camera2.render.model.MeasuresUI;
import com.raulh82vlc.face_detection_sample.model.Face;
import com.raulh82vlc.face_detection_sample.model.math.Square;

import java.util.HashMap;
import java.util.Map;

import static android.graphics.BitmapFactory.decodeResource;

/**
 * <p>Face Drawer is responsible of having all UI elements ready for painting
 * as soon as there is a detected face to paint anything</p>
 *
 * @author Raul Hernandez Lopez.
 */

public class FaceDrawer extends View {
    private static final String KEY_BITMAP_HEAD = "Goku_Sayan";
    private Map<String, Drawable> drawableStore = new HashMap <>();
    private Face face;
    private Paint paintMarker;
    private Rect rect;
    private Matrix transformation;
    private int screenWidth, screenHeight;
    private int widthMeasure, heightMeasure;
    private int viewWidth, viewHeight;

    public FaceDrawer(Context context) {
        super(context);
        init();
    }

    public FaceDrawer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paintMarker = new Paint();
        paintMarker.setColor(Color.RED);
        paintMarker.setStrokeWidth(10);
        paintMarker.setStyle(Paint.Style.STROKE);
        // to create a discontinuous marker
        paintMarker.setPathEffect(new DashPathEffect(new float[] {10,20}, 0));

        Drawable drawable = new BitmapDrawable(getResources(), decodeResource(getResources(),
                R.drawable.goku_supersayan));
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawableStore.put(KEY_BITMAP_HEAD, drawable);
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
            // Bitmap AR Super Sayan
            drawBitmapAR(canvas, drawableStore.get(KEY_BITMAP_HEAD), w, h,
                    (int) faceShape.getStart().getxAxis() - (w / 2),
                    (int) faceShape.getStart().getyAxis() - (int)(h * 1.5));
        }
    }

    /**
     * Draw the bitmap / drawable on screen
     * @param canvas canvas to be applied to the drawable
     * @param drawable drawable with bitmap
     * @param w width of face
     * @param h height of face
     * @param x face x coordinate starting point
     * @param y face y coordinate starting point point
     */
    private void drawBitmapAR(Canvas canvas, Drawable drawable, int w, int h, int x, int y) {
        if (drawable != null) {
            int widthGraphic = drawable.getIntrinsicWidth();
            int heightGraphic = drawable.getIntrinsicHeight();
            setTransformation(w, h, x, y, widthGraphic, heightGraphic);
            canvas.setMatrix(transformation);
            drawable.draw(canvas);
        }
    }

    private void setTransformation(int w, int h, int x, int y, int widthGraphic, int heightGraphic) {
        MeasuresUI measures = TransformationsHelper.calcMeasures(w, h, viewWidth, viewHeight, x, y,
                widthGraphic,
                heightGraphic);
        transformation.setScale(measures.getScale(), measures.getScale());
        transformation.postTranslate(measures.getDx(), measures.getDy());
        transformation.postConcat(transformation);
    }

    private void drawFaceMarker(Canvas canvas, Square faceShape, int w, int h) {
        // Face AR mark
        setMarker(faceShape, w, h);
        canvas.drawRect(rect, paintMarker);
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

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        calcViewSize();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        this.widthMeasure = widthMeasureSpec;
        this.heightMeasure = heightMeasureSpec;
        calcViewSize();
    }

    /**
     * Calculation of view size
     */
    private void calcViewSize() {
        int width;
        int height;
        width = Math.max(screenWidth, getSuggestedMinimumWidth());
        height = Math.max(screenHeight, getSuggestedMinimumHeight());
        if (MeasureSpec.getMode(heightMeasure)
                == MeasureSpec.AT_MOST) {
            if (screenWidth == width) {
                height = screenHeight;
            } else {
                height = width * screenHeight / screenWidth;
            }
            viewWidth = resolveSize(width, widthMeasure);
            if (screenWidth != 0) {
                viewHeight = viewWidth * height / screenWidth;
            }
            viewHeight = resolveSize(viewHeight, heightMeasure);
        } else {
            viewWidth = resolveSize(width, widthMeasure);
            viewHeight = resolveSize(height, heightMeasure);
        }

        setMeasuredDimension(viewWidth, viewHeight);
    }

    public void setScreenSize(int width, int height) {
        screenWidth = width;
        screenHeight = height;
        calcViewSize();
    }
}
