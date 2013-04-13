package com.scholtz.aor.activity;

import com.scholtz.aor.util.GlobalApp;
import com.scholtz.aor.util.Util;
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
import android.util.Log;
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
	private GlobalApp gApp;
	
	private double lat = 0, lon = 0;
	private float[] accelerometerValues = new float[3];
	private float[] magneticFieldValues = new float[3];
	private float[] orientation = new float[3];
	private String locationSource = "---";
	
	@SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        gApp = ((GlobalApp)getApplicationContext());
        
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
	
	protected void onPause() {
		super.onPause();
		
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
		if(gApp.getCurrentLocation() == null) {
			gApp.setCurrentLocation(location);
			
			long t1 = System.currentTimeMillis();
			gApp.findRelevantStops();
			Log.d("aor.time", String.valueOf(System.currentTimeMillis()-t1));
			
		} else if(location.getProvider() == LocationManager.GPS_PROVIDER) {
			if(gApp.getCurrentLocation().getProvider() == LocationManager.NETWORK_PROVIDER) {
				gApp.setCurrentLocation(location);
				gApp.findRelevantStops();
			} else {
				if(location.distanceTo(gApp.getCurrentLocation()) >= gApp.getTreshhold()) {
					gApp.setCurrentLocation(location);
					gApp.findRelevantStops();
				}
			}
			
		} else if(location.getProvider() == LocationManager.NETWORK_PROVIDER) {
			if(gApp.getCurrentLocation().getProvider() == LocationManager.NETWORK_PROVIDER) {
				gApp.setCurrentLocation(location);
				gApp.findRelevantStops();
			}
		}
		
		locationSource = location.getProvider();
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
			
			textPaint.setARGB(255, 255, 43, 43);
			textPaint.setTextSize(30);
			setWillNotDraw(false);
		}

		@Override
		protected void onDraw(Canvas canvas) {
			canvas.drawText("Location source: " + locationSource, 10, 30, textPaint);
			canvas.drawText("Lat: " + lat, 10, 60, textPaint);
			canvas.drawText("Lon: " + lon, 10, 90, textPaint);
			canvas.drawText(String.format("Azimuth: %5.2f", Util.rad2deg(orientation[0])), 10, 120, textPaint);
			canvas.drawText(String.format("Pitch: %5.2f", Util.rad2deg(orientation[1])), 10, 150, textPaint);
			canvas.drawText(String.format("Roll: %5.2f", Util.rad2deg(orientation[2])), 10, 180, textPaint);
		}
	}
}