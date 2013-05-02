package com.scholtz.aor.activity;

import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.scholtz.aor.R;
import com.scholtz.aor.util.GlobalApp;
import com.scholtz.aor.util.Poi;
import com.scholtz.aor.util.Util;
import com.scholtz.aor.view.CameraView;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.NinePatchDrawable;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.text.TextPaint;
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
	private LocationManager mLocationManager;
	private SensorManager mSensorManager;
	private GlobalApp gApp;
	
	private List<Poi> visible = null;
	private List<Poi> relevant = null;
	private double lat = 0, lon = 0;
	private float[] accelerometerValues = new float[3];
	private float[] magneticFieldValues = new float[3];
	private float[] orientation = new float[3];
	private String locationSource = "---";
	
	private Bitmap bitmap;
	
	/**
	 * Setup required services in onCreate
	 * Set window parameters
	 */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        gApp = ((GlobalApp)getApplicationContext());
        
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, this);
		
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
		
		mLocationManager.removeUpdates(this);
		mSensorManager.unregisterListener(this);
	}
	
	/**
	 * Call layout setup
	 * Request location and sensor updates
	 */
	protected void onResume() {
		super.onResume();
		
		LayoutSetup();
		
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
		mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, this);
		
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	private void updateLocation(Location location, boolean updateRelevant) {
		gApp.setCurrentLocation(location);
		
		long t1 = System.currentTimeMillis();
		if(updateRelevant) gApp.findRelevantStops();
		Log.d("aor.time.relevant", String.valueOf(System.currentTimeMillis()-t1));
		
		locationSource = location.getProvider();
		lat = location.getLatitude();
		lon = location.getLongitude();
		
		String url = String.format("http://maps.googleapis.com/maps/api/staticmap?center=%s,%s&size=200x200&sensor=false&maptype=roadmap&zoom=15",
		Double.toString(lat).replace(',', '.'), Double.toString(lon).replace(',', '.'));

		Log.d("AORLOC", "Loading: " + url);
		new DownloadImageTask().execute(url);
	}
	
	private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
		protected Bitmap doInBackground(String... urls) {
			String urldisplay = urls[0];
			Bitmap bitmap = null;
			try {
				InputStream in = new java.net.URL(urldisplay).openStream();
				bitmap = BitmapFactory.decodeStream(in);
				in.close();
			} catch (Exception e) {
				Log.e("Error", "Failed to download " + urldisplay, e);
			}
			return bitmap;
		}
		
		protected void onPostExecute(Bitmap result) {
			bitmap = result;
		}
}
	// Location Listener Methods - START
	/**
	 * This method takes care of location events
	 * Store GPS coordinates if possible, otherwise use network
	 */
	public void onLocationChanged(Location location) {
		if(gApp.getCurrentLocation() == null) {
			updateLocation(location, true);	
			
		} else if(location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
			if(gApp.getCurrentLocation().getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
				updateLocation(location, true);
			} else {
				if(location.distanceTo(gApp.getCalcLocation()) >= gApp.getTRESHOLD()) {
					updateLocation(location, true);
				} else {
					updateLocation(location, false);
				}
			}
			
		} else if(location.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
			if(gApp.getCurrentLocation().getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
				updateLocation(location, true);
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
			drawDebug(canvas);
			drawHud(canvas);
			drawStops(canvas);
		}
		
		/**
		 * Method to draw debug info onto canvas
		 * @param canvas
		 */
		private void drawDebug(Canvas canvas) {
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
		}
		
		/**
		 * Method to draw stops on canvas.
		 * @param canvas Canvas we draw on.
		 */
		private void drawStops(Canvas canvas) {
			if(visible == null || visible.size() == 0)
				return;
			
			// sort by distance; in reverse
			Collections.sort(visible, new Comparator<Poi>() {
				public int compare(Poi a, Poi b) {
					if(a.getDistance() > b.getDistance()) return -1;
					if(a.getDistance() < b.getDistance()) return 1;
					return 0;
				}
			});
			
			// load NPD
			NinePatchDrawable npd = (NinePatchDrawable)getResources().getDrawable(R.drawable.test);
			
			// setup text paint
			int textSize = 30;
			TextPaint textPaint = new TextPaint();
			textPaint.setARGB(255, 0, 0, 0);
			textPaint.setTextSize(textSize);
			
			// calculate data related to width
			int cw = canvas.getWidth();
			double widthFOV = (double) cw / (double) gApp.getFOV();
			
			// calculate variables related to height
			int ch = canvas.getHeight();
			Location currentLocation = gApp.getCurrentLocation();
			float[] distanceBetween = new float[3];
			Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), currentLocation.getLatitude() + gApp.getD(), currentLocation.getLongitude() + gApp.getD(), distanceBetween);
			double heightDistance = (double) ch / (double) distanceBetween[0];

			for(Poi p : visible) {
				double angleDiff = p.getAngleDiff() + (double) gApp.getFOVHALF();
				
				int left = (int) (widthFOV * angleDiff);
				int top = ch - (int) (heightDistance * p.getDistance());
				int right = left + 10;
				int bottom = top + textSize + textSize + 10;
				
				Rect npdBounds = new Rect(left, top, right, bottom);
				textPaint.getTextBounds(p.getName(), 0, p.getName().length(), npdBounds);
				//Log.d("aor.draw", "name:" + p.getName() + " left:" + npdBounds.left + " top:" + npdBounds.top + " right:" + npdBounds.right + " bottom:" + npdBounds.bottom);
				npdBounds.left += left + 2;
				npdBounds.top += top + 22;
				npdBounds.right += right;
				npdBounds.bottom += bottom;
				
				// adjust to center
				int shiftLeft = npdBounds.width() / 2;
				npdBounds.left -= shiftLeft;
				npdBounds.right -= shiftLeft;
				
				npd.setBounds(npdBounds);
				npd.draw(canvas);
				
				canvas.drawText(p.getName(), left + 5 - shiftLeft, top + textSize + 5, textPaint);
				canvas.drawText(String.valueOf(p.getDistance()) + "m", left + 5 - shiftLeft, top + textSize + 5 + textSize, textPaint);
			}
		}
		
		/**
		 * Draw HUD
		 * This method is only for debugging.
		 * @param canvas Canvas we draw on.
		 */
		private void drawHud(Canvas canvas) {
			relevant = gApp.getRelevant();
			Location cloc = gApp.getCalcLocation();
			
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
			if(bitmap == null) {
				canvas.drawRect(canvas.getWidth() - 200, 0, canvas.getWidth(), 200, paintBg);
			} else {
				canvas.drawBitmap(bitmap, canvas.getWidth() - 200, 0, paintBg);
			}
			
			// draw direction			
			float angleRad = (float) ((Math.PI/2.0) - orientation[0]);
			float userX = (float) Math.cos(angleRad);
			float userY = (float) -Math.sin(angleRad);
			canvas.drawLine(fromLeft, fromTop, userX * 70 + fromLeft, userY * 70 + fromTop, paintBlue);
			
			// draw stops
			if(relevant != null && cloc != null) {
				for(Poi p : relevant) {
					float pX = (float) ((p.getLon() - cloc.getLongitude()) / gApp.getD() * 100);
					float pY = (float) -((p.getLat() - cloc.getLatitude()) / gApp.getD() * 100);
					
					if(p.isVisible() == true) {
						canvas.drawPoint(pX + fromLeft, pY + fromTop, paintGreen);
					} else {
						canvas.drawPoint(pX + fromLeft, pY + fromTop, paintRed);
					}
				}
			}
		}
	}
}