package fr.theosykas.parking.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class CoordUtilsTest {

	@Test
	void zeroDistanceForIdenticalPoints() {
		assertEquals(0.0, CoordUtils.calculateDistance(46.5836, 0.3348, 46.5836, 0.3348), 0.001);
	}

	@Test
	void distanceIsSymmetric() {
		double outbound = CoordUtils.calculateDistance(46.5836, 0.3348, 43.5510, 7.0177);
		double inbound = CoordUtils.calculateDistance(43.5510, 7.0177, 46.5836, 0.3348);

		assertEquals(outbound, inbound, 0.001);
	}
}
