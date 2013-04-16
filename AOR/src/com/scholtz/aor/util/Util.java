package com.scholtz.aor.util;

/**
 * Utility class which inclused static helper methods
 * @author Mike
 *
 */
public class Util {
	private static final int R = 6371;
	
	public static double rad2deg(double rad) {
		return rad * 180 / Math.PI;
	}
	
	public static double normalize(double angle) {
		if(angle >= 0) {
			return angle;
		} else {
			return angle + 360;
		}
	}
	
	public static double toCartesianX(double lat, double lon) {
		return (double) (R * Math.cos(lat) * Math.cos(lon));
	}
	
	public static double toCartesianY(double lat, double lon) {
		return (double) (R * Math.cos(lat) * Math.sin(lon));
	}
	
	public static float fixAzimuth(float azimuth) {
		float newAzimuth = azimuth + 270;
		if(azimuth > 360) {
			newAzimuth -= 360;
		}
		
		return newAzimuth;
	}
}
