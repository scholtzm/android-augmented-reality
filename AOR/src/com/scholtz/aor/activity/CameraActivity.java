package com.scholtz.aor.activity;

import java.util.List;

import com.scholtz.aor.R;
import com.scholtz.aor.util.GlobalApp;
import com.scholtz.aor.util.Poi;
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
import android.graphics.Rect;
import android.graphics.drawable.NinePatchDrawable;
import android.hardware.Camera;
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

/**
 * CameraActivity class
 * This class combines CameraView and DrawView and implements location and sensor listeners (accelerometer, magnetic field)
 * @author Mike
 *
 */
public class CameraActivity extends Activity implements LocationListener, SensorEventListener {
	private FrameLayout frameLayout;
	private CameraView cameraView;
	private PoiView poiView;
	private LocationManager locationManager;
	private SensorManager mSensorManager;
	private GlobalApp gApp;
	
	private List<Poi> visible = null;
	private List<Poi> relevant = null;
	private double lat = 0, lon = 0;
	private float[] accelerometerValues = new float[3];
	private float[] magneticFieldValues = new float[3];
	private float[] orientation = new float[3];
	private String locationSource = "---";
	
	/**
	 * Setup required services in onCreate
	 * Set window parameters
	 */
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
    }
	
	/**
	 * Set Layout as a combination of 2 views on top of each other
	 * Open camera and pass it as parameter to CameraView
	 */
	@SuppressWarnings("deprecation")
	private void LayoutSetup() {
		Camera camera = Camera.open();
		
	    frameLayout = new FrameLayout(this);
	    frameLayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
	    
	    cameraView = new CameraView(this, camera);
	    frameLayout.addView(cameraView);
	    
	    poiView = new PoiView(this);
	    frameLayout.addView(poiView);
	    
	    setContentView(frameLayout);
	}
	
	/**
	 * Take care of camera and unregister listeners
	 */
	protected void onPause() {
		super.onPause();
		
		if(cameraView != null) {
			cameraView.onPause();
			cameraView = null;
		}
		
		locationManager.removeUpdates(this);
		mSensorManager.unregisterListener(this);
	}
	
	/**
	 * Call layout setup
	 * Request location and sensor updates
	 */
	@SuppressWarnings("deprecation")
	protected void onResume() {
		super.onResume();
		
		LayoutSetup();
		
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, this);
		
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	// Location Listener Methods - START
	/**
	 * This method takes care of location events
	 * Store GPS coordinates if possible, otherwise use network
	 */
	public void onLocationChanged(Location location) {
		if(gApp.getCurrentLocation() == null) {
			gApp.setCurrentLocation(location);
			
			long t1 = System.currentTimeMillis();
			gApp.findRelevantStops();
			Log.d("aor.time.relevant", String.valueOf(System.currentTimeMillis()-t1));
			
			locationSource = location.getProvider();
			lat = location.getLatitude();
			lon = location.getLongitude();
			
		} else if(location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
			if(gApp.getCurrentLocation().getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
				gApp.setCurrentLocation(location);
				gApp.findRelevantStops();
				
				locationSource = location.getProvider();
				lat = location.getLatitude();
				lon = location.getLongitude();
			} else {
				if(location.distanceTo(gApp.getCurrentLocation()) >= gApp.getTreshhold()) {
					gApp.setCurrentLocation(location);
					gApp.findRelevantStops();
					
					locationSource = location.getProvider();
					lat = location.getLatitude();
					lon = location.getLongitude();
				}
			}
			
		} else if(location.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
			if(gApp.getCurrentLocation().getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
				gApp.setCurrentLocation(location);
				gApp.findRelevantStops();
				
				locationSource = location.getProvider();
				lat = location.getLatitude();
				lon = location.getLongitude();
			}
		}
		
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

	/**
	 * This method takes care of location events
	 * Recalculate azimuth and visible stops
	 */
	public void onSensorChanged(SensorEvent event) {
		if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			System.arraycopy(event.values, 0, accelerometerValues, 0, event.values.length);
		}
		if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			System.arraycopy(event.values, 0, magneticFieldValues, 0, event.values.length);
		}
		
		float[] R = new float[9];
		
		SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldValues);
		// needs to be remapped for landscape mode
		SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_Z, SensorManager.AXIS_MINUS_X, R);
		SensorManager.getOrientation(R, orientation);
		
		// find visible stops according to azimuth
		gApp.findVisibleStops(Util.rad2deg(orientation[0]));
		visible = gApp.getVisible();
		
		poiView.invalidate();
	}
	// Sensor Event Listener Methods - END
	
	/**
	 * Inner class for DrawView
	 * Currently used for debugging (mostly)
	 * @author Mike
	 *
	 */
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
			canvas.drawText("Latitude: " + lat, 10, 60, textPaint);
			canvas.drawText("Longitude: " + lon, 10, 90, textPaint);
			canvas.drawText(String.format("Azimuth: %5.2f", Util.normalize(Util.rad2deg(orientation[0]))), 10, 120, textPaint);
			canvas.drawText(String.format("Pitch: %5.2f", Util.rad2deg(orientation[1])), 10, 150, textPaint);
			canvas.drawText(String.format("Roll: %5.2f", Util.rad2deg(orientation[2])), 10, 180, textPaint);
			
			if(visible != null) {
				int startY = 210;
				int max = 7;
				
				if(max > visible.size()) {
					max = visible.size();
				}
				
				for(int i = 0; i < max; i++) {
					Poi p = visible.get(i);
					canvas.drawText(p.getName() + " - " + p.getDescription(), 10, startY, textPaint);
					startY += 30;
				}
			}
			
			drawHud(canvas);
			drawStops(canvas);
		}
		
		private void drawStops(Canvas canvas) {
			NinePatchDrawable npd = (NinePatchDrawable)getResources().getDrawable(R.drawable.test);

			// Set its bound where you need
			Rect npdBounds = new Rect(700, 300, 716, 316);
			npd.setBounds(npdBounds);

			// Finally draw on the canvas
			npd.draw(canvas);
		}
		
		/**
		 * Draw HUD
		 * This method is only for debugging.
		 * @param canvas Canvas na ktory kreslime.
		 */
		private void drawHud(Canvas canvas) {
			relevant = gApp.getRelevant();
			Location cloc = gApp.getCurrentLocation();
			
			Paint paintBg = new Paint();
			paintBg.setARGB(255, 255, 255, 255);
			Paint paintGreen = new Paint();
			paintGreen.setARGB(255, 0, 255, 0);
			paintGreen.setStrokeWidth(3);
			Paint paintRed = new Paint();
			paintRed.setARGB(255, 255, 0, 0);
			paintRed.setStrokeWidth(3);
			Paint paintBlue = new Paint();
			paintBlue.setARGB(255, 0, 0, 255);
			paintBlue.setStrokeWidth(2);
			
			// center - kind of
			float fromLeft = canvas.getWidth() - 100;
			float fromTop = 100;
			
			// draw white bg
			canvas.drawRect(canvas.getWidth() - 200, 0, canvas.getWidth(), 200, paintBg);
			
			// draw direction			
			float angleRad = (float) ((Math.PI/2) - orientation[0]);
			float userX = (float) Math.cos(angleRad);
			float userY = (float) Math.sin(angleRad);
			canvas.drawLine(fromLeft, fromTop, userX * 70 + fromLeft, userY * 70 + fromTop, paintBlue);
			
			// draw stops
			if(relevant != null && cloc != null) {
				for(Poi p : relevant) {
					float pX = (float) ((p.getLon() - cloc.getLongitude()) / gApp.getD() * 100);
					float pY = (float) ((p.getLat() - cloc.getLatitude()) / gApp.getD() * 100);
					
					if(p.visible == true) {
						canvas.drawPoint(pX + fromLeft, pY + fromTop, paintGreen);
					} else {
						canvas.drawPoint(pX + fromLeft, pY + fromTop, paintRed);
					}
				}
			}
		}
	}
}