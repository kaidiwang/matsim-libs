/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationLegDistanceDistributionTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.meisterk.org.matsim.population.algorithms;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.NodeNetworkRoute;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;

import playground.meisterk.org.matsim.population.algorithms.PopulationLegDistanceDistribution.CrosstabFormat;

public class PopulationLegDistanceDistributionTest extends MatsimTestCase {

	public static final double[] distanceClasses = new double[]{
		0, 
		100, 200, 500, 
		1000, 2000, 5000, 
		10000, 20000, 50000, 
		100000, 200000, 500000,
		1000000};

	public void testGenerationDistribution() {
		
		NetworkLayer testNetwork = new NetworkLayer();
		NodeImpl node1 = testNetwork.createNode(new IdImpl("1"), new CoordImpl(0.0, 0.0));
		NodeImpl node2 = testNetwork.createNode(new IdImpl("2"), new CoordImpl(500.0, 500.0));
		NodeImpl node3 = testNetwork.createNode(new IdImpl("3"), new CoordImpl(1000.0, 1000.0));
		LinkImpl startLink = testNetwork.createLink(new IdImpl("101"), node1, node2, 500.0, 27.7778, 2000.0, 1.0);
		LinkImpl endLink = testNetwork.createLink(new IdImpl("102"), node2, node3, 1000.0, 27.7778, 2000.0, 1.0);
		
		PersonImpl testPerson = new PersonImpl(new IdImpl("1000"));
		PlanImpl testPlan = testPerson.createPlan(true);
		
		ActivityImpl act = testPlan.createActivity("startActivity", startLink);
		
		Leg leg = testPlan.createLeg(TransportMode.car);
		
		NodeNetworkRoute route = new NodeNetworkRoute(startLink, endLink);
		route.setDistance(1200.0);
		
		leg.setRoute(route);
		
		act = testPlan.createActivity("endActivity", endLink);
		
		PopulationImpl pop = new PopulationImpl();
		pop.setIsStreaming(true);
		
		PopulationLegDistanceDistribution testee = new PopulationLegDistanceDistribution();
		pop.addAlgorithm(testee);
		
		pop.addPerson(testPerson);
		
		assertEquals(1, testee.getNumberOfModes());
		assertEquals(1, testee.getNumberOfLegs(TransportMode.car, distanceClasses[4], distanceClasses[5]));
		assertEquals(1, testee.getNumberOfLegs(TransportMode.car, distanceClasses[5], distanceClasses[4]));
		assertEquals(1, testee.getNumberOfLegs(TransportMode.car, distanceClasses[4], distanceClasses[5]));

		leg.setMode(TransportMode.pt);
		route.setDistance(13456.7);

		pop.addPerson(testPerson);
		
		assertEquals(2, testee.getNumberOfModes());
		assertEquals(1, testee.getNumberOfLegs(TransportMode.car, distanceClasses[4], distanceClasses[5]));
		assertEquals(1, testee.getNumberOfLegs(TransportMode.pt, distanceClasses[7], distanceClasses[8]));
		
		leg.setMode(TransportMode.car);
		route.setDistance(0.0);
		
		pop.addPerson(testPerson);
		
		assertEquals(2, testee.getNumberOfModes());
		assertEquals(1, testee.getNumberOfLegs(TransportMode.car, -1000.0, distanceClasses[0]));
		assertEquals(1, testee.getNumberOfLegs(TransportMode.car, distanceClasses[5], distanceClasses[4]));
		assertEquals(1, testee.getNumberOfLegs(TransportMode.pt, distanceClasses[7], distanceClasses[8]));

		assertEquals(3, testee.getNumberOfLegs());
		assertEquals(2, testee.getNumberOfLegs(TransportMode.car));
		assertEquals(1, testee.getNumberOfLegs(TransportMode.pt));
		assertEquals(1, testee.getNumberOfLegs(-1000.0, distanceClasses[0]));
		assertEquals(1, testee.getNumberOfLegs(distanceClasses[5], distanceClasses[4]));
		assertEquals(1, testee.getNumberOfLegs(distanceClasses[7], distanceClasses[8]));
		
		for (boolean isCumulative : new boolean[]{false, true}) {
			for (CrosstabFormat crosstabFormat : CrosstabFormat.values()) {
				testee.printCrosstab(crosstabFormat, isCumulative, distanceClasses);
			}
		}
		
	}
	
}
