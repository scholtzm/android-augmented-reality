package com.scholtz.aor.util;

public class Util {
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
}
