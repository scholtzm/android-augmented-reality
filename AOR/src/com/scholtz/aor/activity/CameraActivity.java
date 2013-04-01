package com.scholtz.aor.activity;

import com.scholtz.aor.view.CameraView;
import com.scholtz.aor.view.PoiView;

import android.os.Bundle;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

public class CameraActivity extends Activity {
	private FrameLayout frameLayout;
	private CameraView cameraView;
	private PoiView poiView;
	
	@SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        frameLayout = new FrameLayout(this);
        frameLayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        
        cameraView = new CameraView(this);
        frameLayout.addView(cameraView);
        
        poiView = new PoiView(this);
        frameLayout.addView(poiView);
        
        setContentView(frameLayout);
    }
	
	/**
	 * Disables back button
	 */
	public void onBackPressed() {
	}
	
	protected void onPause() {
		super.onPause();
	}
	
	protected void onResume() {
		super.onResume();
	}
}