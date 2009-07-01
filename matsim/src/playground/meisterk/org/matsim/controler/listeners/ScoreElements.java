/* *********************************************************************** *
 * project: org.matsim.*
 * ScoreSummands.java
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

package playground.meisterk.org.matsim.controler.listeners;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.TreeMap;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.utils.io.IOUtils;

public class ScoreElements implements StartupListener, ScoringListener, ShutdownListener {

	public static final String[] SCORE_ELEMENT_NAMES = new String[]{"sum", "perf", "wait", "short", "facilityPenalty", "negative"};
	
	final private String filename;
	private BufferedWriter out;

	public ScoreElements(final String filename) {
		super();
		this.filename = filename;
	}

	public void notifyStartup(StartupEvent event) {

		try {
			
			this.out = IOUtils.getBufferedWriter(org.matsim.core.controler.Controler.getOutputFilename(this.filename));
			this.out.write("#iteration");
			for (String str : ScoreElements.SCORE_ELEMENT_NAMES) {
				this.out.write("\t" + str);
			}
			this.out.write(System.getProperty("line.separator"));

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void notifyScoring(ScoringEvent event) {
		playground.meisterk.org.matsim.scoring.ktiYear3.ActivityScoringFunction asf = null;
		TreeMap<String, Double> sumScoreParts = new TreeMap<String, Double>();
		for (String str : ScoreElements.SCORE_ELEMENT_NAMES) {
			sumScoreParts.put(str, 0.0);
		}
		double d;

		Controler c = event.getControler();
		EventsToScore eventsToScore = c.getPlansScoring().getPlanScorer();
		
		for (PersonImpl p : c.getPopulation().getPersons().values()) {
			
			ScoringFunction sf = eventsToScore.getScoringFunctionForAgent(p.getId());
			if (sf instanceof ScoringFunctionAccumulator) {
				if (((ScoringFunctionAccumulator) sf).getActivityScoringFunctions().get(0) instanceof playground.meisterk.org.matsim.scoring.ktiYear3.ActivityScoringFunction) {
					asf = (playground.meisterk.org.matsim.scoring.ktiYear3.ActivityScoringFunction) ((ScoringFunctionAccumulator) sf).getActivityScoringFunctions().get(0);
					d = sumScoreParts.get("sum") + sf.getScore();
					sumScoreParts.put("sum", d);
					d = sumScoreParts.get("perf") + asf.getPerformanceScore();
					sumScoreParts.put("perf", d);
					d = sumScoreParts.get("wait") + asf.getWaitingTimeScore();
					sumScoreParts.put("wait", d);
					d = sumScoreParts.get("short") + asf.getTooShortDurationScore();
					sumScoreParts.put("short", d);
					d = sumScoreParts.get("facilityPenalty") + asf.getFacilityPenaltiesScore();
					sumScoreParts.put("facilityPenalty", d);
					d = sumScoreParts.get("negative") + asf.getNegativeDurationScore();
					sumScoreParts.put("negative", d);
				}
			}
		}
		
		int popSize = c.getPopulation().getPersons().size();
		
		try {
			this.out.write(Integer.toString(event.getIteration()));
			for (String str : ScoreElements.SCORE_ELEMENT_NAMES) {
				out.write("\t" + Double.toString(sumScoreParts.get(str) / popSize));
			}
			this.out.write(System.getProperty("line.separator"));
			this.out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void notifyShutdown(ShutdownEvent event) {
		try {
			this.out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
