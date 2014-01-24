/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.michalm.taxi;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.core.mobsim.framework.MobsimAgent;

import playground.michalm.taxi.model.TaxiRequest;


public class TaxiRequestCreator
    implements PassengerRequestCreator
{
    public static final String MODE = "taxi";


    @Override
    public TaxiRequest createRequest(Id id, MobsimAgent passenger, Link fromLink, Link toLink,
            double t0, double t1, double now)
    {
        return new TaxiRequest(id, passenger, fromLink, toLink, t0, now);
    }
}
