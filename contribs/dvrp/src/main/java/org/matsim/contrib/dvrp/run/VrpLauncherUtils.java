/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.run;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;

import org.matsim.analysis.LegHistogram;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.util.time.TimeDiscretizer;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.dvrp.vrpagent.*;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.*;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.util.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.*;


public class VrpLauncherUtils
{
    public static final int MAX_TIME = 36 * 60 * 60;


    public enum TravelTimeSource
    {
        FREE_FLOW_SPEED("FF", TimeDiscretizer.ALL_DAY), // no eventsFileName
        EVENTS_24_H("24H", TimeDiscretizer.ALL_DAY), // based on eventsFileName, averaged over a whole day
        EVENTS_15_MIN("15M", TimeDiscretizer.DEFAULT); // based on eventsFileName, 15-minute time interval

        public final String shortcut;
        public final TimeDiscretizer timeDiscretizer;


        private TravelTimeSource(String shortcut, TimeDiscretizer timeDiscretizer)
        {
            this.shortcut = shortcut;
            this.timeDiscretizer = timeDiscretizer;
        }
    }


    public enum TravelDisutilitySource
    {
        STRAIGHT_LINE, // however, Dijkstra's algo will use DISTANCE cost
        DISTANCE, // travel distance
        TIME; // travel time
    }


    public static Scenario initScenario(String netFileName, String plansFileName)
    {
        Scenario scenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());
        new MatsimNetworkReader(scenario).readFile(netFileName);
        new MatsimPopulationReader(scenario).readFile(plansFileName);
        return scenario;
    }


    public static void convertLegModes(List<String> passengerIds, String mode, Scenario scenario)
    {
        Map<Id, ? extends Person> persons = scenario.getPopulation().getPersons();

        for (String id : passengerIds) {
            Person person = persons.get(scenario.createId(id));

            for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
                if (pe instanceof Leg) {
                    ((Leg)pe).setMode(mode);
                }
            }
        }
    }


    public static void removeNonPassengers(String mode, Scenario scenario)
    {
        Set<Id> nonPassengerIds = new HashSet<Id>();
        Map<Id, ? extends Person> persons = scenario.getPopulation().getPersons();

        for (Entry<Id, ? extends Person> e : persons.entrySet()) {
            Person person = e.getValue();

            boolean isPassenger = false;
            for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
                if (pe instanceof Leg) {
                    if ( ((Leg)pe).getMode().equals(mode)) {
                        isPassenger = true;
                        break;
                    }
                }
            }

            if (!isPassenger) {
                nonPassengerIds.add(e.getKey());
            }
        }

        for (Id id : nonPassengerIds) {
            persons.remove(id);
        }
    }


    public static TravelTime initTravelTime(Scenario scenario, TravelTimeSource ttimeSource,
            String eventsFileName)
    {
        switch (ttimeSource) {
            case FREE_FLOW_SPEED:
                return new FreeSpeedTravelTime();

            case EVENTS_15_MIN:
            case EVENTS_24_H:
                scenario.getConfig().travelTimeCalculator()
                        .setTraveltimeBinSize(ttimeSource.timeDiscretizer.getTimeInterval());
                return TravelTimeCalculators.createTravelTimeFromEvents(eventsFileName, scenario);

            default:
                throw new IllegalArgumentException();
        }
    }


    public static TravelDisutility initTravelDisutility(TravelDisutilitySource tdisSource,
            TravelTime travelTime)
    {
        switch (tdisSource) {
            case STRAIGHT_LINE:
            case DISTANCE:
                return new DistanceAsTravelDisutility();

            case TIME:
                return new TimeAsTravelDisutility(travelTime);

            default:
                throw new IllegalArgumentException();
        }
    }


    public static VrpData initVrpData(MatsimVrpContext context, String vehiclesFileName)
    {
        VrpData vrpData = new VrpDataImpl();
        new VehicleReader(context.getScenario(), vrpData).readFile(vehiclesFileName);
        return vrpData;
    }


    public static PassengerEngine initPassengerEngine(String mode,
            PassengerRequestCreator requestCreator, VrpOptimizer optimizer,
            MatsimVrpContext context, QSim qSim)
    {
        PassengerEngine passengerEngine = new PassengerEngine(mode, requestCreator, optimizer,
                context);
        qSim.addMobsimEngine(passengerEngine);
        qSim.addDepartureHandler(passengerEngine);
        return passengerEngine;
    }


    public static void initAgentSources(QSim qSim, MatsimVrpContext context,
            VrpOptimizer optimizer, DynActionCreator actionCreator)
    {
        qSim.addAgentSource(new VrpAgentSource(actionCreator, context, optimizer, qSim));
        qSim.addAgentSource(new PopulationAgentSource(context.getScenario().getPopulation(),
                new DefaultAgentFactory(qSim), qSim));
    }


    public static void writeHistograms(LegHistogram legHistogram, String histogramOutDirName)
    {
        new File(histogramOutDirName).mkdir();
        legHistogram.write(histogramOutDirName + "legHistogram_all.txt");
        legHistogram.writeGraphic(histogramOutDirName + "legHistogram_all.png");
        for (String legMode : legHistogram.getLegModes()) {
            legHistogram.writeGraphic(histogramOutDirName + "legHistogram_" + legMode + ".png",
                    legMode);
        }
    }
}
