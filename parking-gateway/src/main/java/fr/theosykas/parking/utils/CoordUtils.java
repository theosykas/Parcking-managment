package fr.theosykas.parking.utils;

public class CoordUtils {
	private static final double EARTH_RADIUS_CONS =  6_371_000;

	// Equirectangular Distance Approximation,
	// erreur < 0,1 % sur quelques km, bien moins coûteux.
	public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
		Double lat1Rad = Math.toRadians(lat1);
		Double lat2Rad = Math.toRadians(lat2);
		Double lon1Rad = Math.toRadians(lon1);
		Double lon2Rad = Math.toRadians(lon2);
		
		Double x = (lon2Rad - lon1Rad) * Math.cos((lat1Rad + lat2Rad) / 2);
		Double y = lat2Rad - lat1Rad;
		double distance = Math.sqrt(x * x + y * y) * EARTH_RADIUS_CONS;
		return distance;
	}
}
