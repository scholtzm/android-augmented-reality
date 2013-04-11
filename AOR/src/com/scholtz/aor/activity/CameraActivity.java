package com.scholtz.aor.activity;

import com.scholtz.aor.view.CameraView;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

public class CameraActivity extends Activity implements LocationListener, SensorEventListener {
	private FrameLayout frameLayout;
	private CameraView cameraView;
	private PoiView poiView;
	private LocationManager locationManager;
	private SensorManager mSensorManager;
	
	private double lat = 0, lon = 0;
	private float[] accelerometerValues = new float[3];
	private float[] magneticFieldValues = new float[3];
	private float[] orientation = new float[3];
	
	@SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, this);
		
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		
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
		
		/*if(cameraView != null) {
			cameraView.onPause();
			cameraView = null;
		}*/
		
		locationManager.removeUpdates(this);
		mSensorManager.unregisterListener(this);
	}
	
	protected void onResume() {
		super.onResume();
		
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, this);
		
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	// Location Listener Methods - START
	public void onLocationChanged(Location location) {
		lat = location.getLatitude();
		lon = location.getLongitude();
		
		poiView.invalidate();
	}

	public void onProviderDisabled(String provider) {
	}

	public void onProviderEnabled(String provider) {
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
	// Location Listener Methods - END 
	
	// Sensor Event Listener Methods - START
	public void onAccuracyChanged(Sensor arg0, int arg1) {
	}

	public void onSensorChanged(SensorEvent event) {
		if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			accelerometerValues = event.values;
		}
		if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			magneticFieldValues = event.values;
		}
		
		float[] R = new float[9];
		float[] I = new float[9];
		SensorManager.getRotationMatrix(R, I, accelerometerValues, magneticFieldValues);
		
		SensorManager.getOrientation(R, orientation);
		
		poiView.invalidate();
	}
	// Sensor Event Listener Methods - END
	
	
	private class PoiView extends SurfaceView {
		private Paint textPaint = new Paint();

		public PoiView(Context context) {
			super(context);
			
			textPaint.setARGB(255, 255, 255, 255);
			textPaint.setTextSize(30);
			setWillNotDraw(false);
		}

		@Override
		protected void onDraw(Canvas canvas) {
			canvas.drawText("Lat: " + lat, 10, 50, textPaint);
			canvas.drawText("Lon: " + lon, 10, 80, textPaint);
			canvas.drawText("Azimuth: " + orientation[0], 10, 110, textPaint);
			canvas.drawText("Pitch: " + orientation[1], 10, 140, textPaint);
			canvas.drawText("Roll: " + orientation[2], 10, 170, textPaint);
		}
	}
}