package com.scholtz.aor.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.SurfaceView;

public class PoiView extends SurfaceView {
	private Paint textPaint = new Paint();

	public PoiView(Context context) {
		super(context);
		// Create out paint to use for drawing
		textPaint.setARGB(255, 200, 0, 0);
		textPaint.setTextSize(60);
		// This call is necessary, or else the
		// draw method will not be called.
		setWillNotDraw(false);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// A Simple Text Render to test the display
		canvas.drawText("Hello World!", 50, 50, textPaint);
	}
}