package com.scholtz.aor.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import android.app.Application;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

/**
 * Globally accessible variables and methods
 * This class holds data such as stops or current location
 * @author Mike
 *
 */
public class GlobalApp extends Application {
	private final double D = 0.01;
	private final double TRESHOLD = 500.0;
	private final int FOV = 60;
	private final int FOVHALF = FOV / 2;

	private List<Poi> stops = null;
	private List<Poi> relevant = new ArrayList<Poi>();
	private List<Poi> visible = new ArrayList<Poi>();
	private Location currentLocation = null;
	
	public int getFov() {
		return FOV;
	}

	public double getTreshhold() {
		return TRESHOLD;
	}
	
	public double getD() {
		return D;
	}
	
	public List<Poi> getStops() {
		return stops;
	}

	public void setStops(List<Poi> stops) {
		this.stops = stops;
	}
	
	public List<Poi> getRelevant() {
		return relevant;
	}
	
	public List<Poi> getVisible() {
		return visible;
	}

	public Location getCurrentLocation() {
		return currentLocation;
	}

	public void setCurrentLocation(Location currentLocation) {
		this.currentLocation = currentLocation;
	}

	/**
	 * Finds relevant bus stops around the user according to current location
	 */
	public void findRelevantStops() {
		long start = System.nanoTime();

		relevant.clear();
		
		for (Poi p : stops) {
			if (p.getLat() >= (currentLocation.getLatitude() - D) &&
				p.getLat() <= (currentLocation.getLatitude() + D) &&
				p.getLon() >= (currentLocation.getLongitude() - D) &&
				p.getLon() <= (currentLocation.getLongitude() + D)) {
				relevant.add(p);
			}
		}

		double time = (System.nanoTime() - start) / 1000000.0;
		Log.d("AOR", "Found " + relevant.size() + " stops in " + time + "ms");
	}
	
	private long counter = 0;

	/**
	 * Finds visible stops according to users location and azimuth
	 * @param azimuth Device azimuth
	 */
	public void findVisibleStops(double azimuth) {
		if(currentLocation == null) {
			Log.d("AOR.noloc", "No location...");
			return;
		}
		
		counter++;
		long start = System.nanoTime();

		for (Poi p : relevant)
			p.visible = false;
		visible.clear();
		
		double currentX = currentLocation.getLongitude(); //Util.toCartesianX(currentLocation.getLatitude(), currentLocation.getLongitude());
		double currentY = currentLocation.getLatitude();  //Util.toCartesianY(currentLocation.getLatitude(), currentLocation.getLongitude());

		// Log.d("AOR", "currentX: " + currentX + " currentY: " + currentY + " azimuth:" + azimuth);
		
		if (counter % 100 == 0)
			Log.d("AOR.azimuth", "oldAzimuth: " + azimuth);
		azimuth = 90 - azimuth;
		if (counter % 100 == 0)
			Log.d("AOR.azimuth", "newAzimuth: " + azimuth);

		// Log.d("AOR", "newAzimuth:" + azimuth);

		for(Poi p : relevant) {
			double poiX = p.getLon(); //Util.toCartesianX(p.getLat(), p.getLon());
			double poiY = p.getLat(); //Util.toCartesianY(p.getLat(), p.getLon());
			
			double angle = Math.atan2(poiY - currentY, poiX - currentX);
			double degAngle = Util.rad2deg(angle);
			double poiAngle = Util.normalize(degAngle);
			
			if (counter % 100 == 0) {
				Log.d("AOR", p.getName() + " lat:" + p.getLat() + " lon:" + p.getLon() + " angle:" + poiAngle);
			}

			if(Math.abs(Util.angleDiff(azimuth, poiAngle)) <= FOVHALF) {
				visible.add(p);
				p.visible = true;
			}
		}

		double time = (System.nanoTime() - start) / 1000000.0;
		if (counter % 100 == 0)
			Log.d("AOR", "Added " + visible.size() + " stops in " + time + "ms");
	}

	/**
	 * Sorts stops by distance according to current location
	 */
	public void sortByDistance() {
		Collections.sort(stops, new Comparator<Poi>() {
			public int compare(Poi a, Poi b) {
				Location locA = new Location(LocationManager.PASSIVE_PROVIDER);
				locA.setLatitude(a.getLat());
				locA.setLongitude(a.getLon());

				Location locB = new Location(LocationManager.PASSIVE_PROVIDER);
				locB.setLatitude(b.getLat());
				locB.setLongitude(b.getLon());

				float distA = locA.distanceTo(currentLocation);
				float distB = locB.distanceTo(currentLocation);

				return Float.compare(distA, distB);
			}
		});
	}
}
