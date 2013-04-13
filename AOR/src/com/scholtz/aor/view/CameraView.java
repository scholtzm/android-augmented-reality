package com.scholtz.aor.view;

import java.io.IOException;

import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraView extends SurfaceView implements SurfaceHolder.Callback {
	private Camera camera;
	private SurfaceHolder surfaceHolder;

	@SuppressWarnings("deprecation")
	public CameraView(Context context, Camera camera) {
		super(context);
		
		this.camera = camera;
		surfaceHolder = getHolder();
		surfaceHolder.addCallback(this);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	// Surface Holder Callback Methods - START
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		camera.startPreview();
	}

	public void surfaceCreated(SurfaceHolder holder) {
		Camera.Parameters cameraParameters = camera.getParameters();
        camera.setParameters(cameraParameters);
        
        try {
			camera.setPreviewDisplay(surfaceHolder);
		} catch (IOException e) {
			e.printStackTrace();
			camera.release();
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
	}
	// Surface Holder Callback Methods - END
	
	public void onPause() {
		camera.stopPreview();
        camera.release();
        camera = null;
    }
}
