package fr.theosykas.parking.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class CoordUtilsTests {

	@Test
	void calculateNullDistanceDuplicatePoint() {
		assertEquals(0.0, CoordUtils.calculateDistance(46.5836, 0.3348, 46.5836, 0.3348), 0.001);
	}

	@Test
	void SymOfLatAndLon() {
		double aller = CoordUtils.calculateDistance(46.5836, 0.3348, 43.5510, 7.0177);
		double retour = CoordUtils.calculateDistance(43.5510, 7.0177, 46.5836, 0.3348);

		assertEquals(aller, retour, 0.001);
	}
}
