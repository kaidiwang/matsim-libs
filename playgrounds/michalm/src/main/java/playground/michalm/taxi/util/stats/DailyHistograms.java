/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.util.stats;

import java.io.PrintWriter;


public class DailyHistograms
{
    public final Histogram emptyDriveRatio = new Histogram(0.05, 20);
    public final Histogram stayRatio = new Histogram(0.05, 20);

    public static final String MAIN_HEADER = // 
    "Empty_Drive_Ratio" + HourlyHistograms.tabs(20) + //
            "Vehicle_Wait_Ratio" + HourlyHistograms.tabs(20); //


    public void printSubHeaders(PrintWriter pw)
    {
        pw.print(emptyDriveRatio.binsToString());
        pw.print(stayRatio.binsToString());
        pw.println();
    }


    public void printStats(PrintWriter pw)
    {
        pw.print(emptyDriveRatio.countsToString());
        pw.print(stayRatio.countsToString());
        pw.println();
    }
}
