package com.example.primeiroprojeto;

import android.content.Context;
import android.util.AttributeSet;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.graphics.Color;

public class WaveformView extends View {

    private final Paint paint = new Paint();
    private short[] samples;

    public WaveformView(Context context) {
        super(context);
        configurar();
    }

    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);
        configurar();
    }

    public WaveformView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        configurar();
    }

    private void configurar() {
        paint.setStrokeWidth(3);
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
    }

    public void setSamples(short[] samples) {
        this.samples = samples;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (samples == null || samples.length == 0) {
            return;
        }

        float centro = getHeight() / 2.0f;
        paint.setStrokeWidth(1);
        paint.setColor(Color.GRAY);

        canvas.drawLine(
                0,
                centro,
                getWidth(),
                centro,
                paint
        );

        paint.setStrokeWidth(3);
        paint.setColor(Color.WHITE);
        float escalaX = (float) getWidth() / samples.length;
        float escalaY = (getHeight() / 2.0f) / 32767.0f;

        for (int i = 1; i < samples.length; i++) {

            float x1 = (i - 1) * escalaX;
            float y1 = centro - samples[i - 1] * escalaY;

            float x2 = i * escalaX;
            float y2 = centro - samples[i] * escalaY;

            canvas.drawLine(x1, y1, x2, y2, paint);
        }
    }
}