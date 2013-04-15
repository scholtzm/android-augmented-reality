package com.scholtz.aor.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import android.app.Application;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

public class GlobalApp extends Application {
	private final double D = 0.01;
	private final double treshhold = 500;
	private final int fov = 60;
	private final int fovHalf = fov / 2;

	private List<Poi> stops = null;
	private List<Poi> relevant = null;
	private List<Poi> visible = null;
	private Location currentLocation = null;
	
	public int getFov() {
		return fov;
	}

	public double getTreshhold() {
		return treshhold;
	}
	
	public List<Poi> getStops() {
		return stops;
	}

	public void setStops(List<Poi> stops) {
		this.stops = stops;
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
		relevant = new ArrayList<Poi>();
		
		for (Poi p : stops) {
			if (p.getLat() >= (currentLocation.getLatitude() - D) &&
				p.getLat() <= (currentLocation.getLatitude() + D) &&
				p.getLon() >= (currentLocation.getLongitude() - D) &&
				p.getLon() <= (currentLocation.getLongitude() + D)) {
				relevant.add(p);
			}
		}
	}
	
	/**
	 * Finds visible stops according to users location and azimuth
	 * @param azimuth Device azimuth
	 */
	public void findVisibleStops(float azimuth) {
		if(currentLocation == null) {
			return;
		}
		
		visible = new ArrayList<Poi>();
		
		double currentX = Util.toCartesianX(currentLocation.getLatitude(), currentLocation.getLongitude());
		double currentY = Util.toCartesianY(currentLocation.getLatitude(), currentLocation.getLongitude());
		
		for(Poi p : relevant) {
			double poiX = Util.toCartesianX(p.getLat(), p.getLon());
			double poiY = Util.toCartesianY(p.getLat(), p.getLon());
			
			int poiAngle = (int) Util.normalize(Util.rad2deg(Math.atan2(poiY - currentY, poiX - currentX)));
			Log.d("aor.visible.poiangle", p.getName() + " LAT: " + p.getLat() + " LON: " + p.getLon() + " " + String.valueOf(poiAngle));
			
			int azimuthAngle = (int) Util.fixAzimuth(azimuth);
			
			if(Math.abs(poiAngle-azimuthAngle) <= fovHalf) {
				visible.add(p);
				Log.d("aor.visible.added", p.getName());
			}
		}
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
