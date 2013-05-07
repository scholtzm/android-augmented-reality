package com.scholtz.aor.util;

/**
 * Utility class which includes static helper methods
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
	
	public static double angleDiff(double a, double b) {
		double d = a - b;
		if (d > 180)
			d -= 360;
		if (d < -180)
			d += 360;
		return d;
	}
	
	public static double toCartesianX(double lat, double lon) {
		lat = Math.toRadians(lat);
		lon = Math.toRadians(lon);
		return (double) (R * Math.cos(lat) * Math.cos(lon));
	}
	
	public static double toCartesianY(double lat, double lon) {
		lat = Math.toRadians(lat);
		lon = Math.toRadians(lon);
		return (double) (R * Math.cos(lat) * Math.sin(lon));
	}
}
