package fr.theosykas.parking.utils;

public class CoordUtils {
	private static final double EARTH_RADIUS_CONS =  6_371_000;

	private CoordUtils() {};

	// Equirectangular Distance Approximation
	public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
		Double lat1Rad = Math.toRadians(lat1);
		Double lat2Rad = Math.toRadians(lat2);
		Double lon1Rad = Math.toRadians(lon1);
		Double lon2Rad = Math.toRadians(lon2);
		
		Double x = (lon2Rad - lon1Rad) * Math.cos((lat1Rad + lat2Rad) / 2);
		Double y = lat2Rad - lat1Rad;
		Double distance = Math.sqrt(x * x + y * y) * EARTH_RADIUS_CONS;
		return distance;
	}
}

// C'est une approximation équirectangulaire. Je convertis les coordonnées en radians, je calcule l'écart Nord-Sud et l'écart Est-Ouest, je corrige ce dernier par le cosinus de la latitude parce que les méridiens se resserrent vers les pôles, puis j'applique Pythagore comme si la zone était plate. Je multiplie par le rayon terrestre pour passer des radians aux mètres. C'est moins précis que la haversine mais beaucoup plus rapide, et sur des distances urbaines l'erreur est négligeable.