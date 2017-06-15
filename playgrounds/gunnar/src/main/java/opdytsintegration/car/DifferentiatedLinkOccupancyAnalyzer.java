package opdytsintegration.car;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleAbortsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

import floetteroed.utilities.math.Vector;
import opdytsintegration.MATSimCountingStateAnalyzer;
import opdytsintegration.utils.TimeDiscretization;

/**
 * Keeps track of link occupancies per time bin and network mode.
 * 
 * TODO: The mode String comparisons could probably be sped up.
 * 
 * @author Gunnar Flötteröd
 *
 */
public class DifferentiatedLinkOccupancyAnalyzer implements LinkLeaveEventHandler, LinkEnterEventHandler,
		VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, VehicleAbortsEventHandler {

	// -------------------- MEMBERS --------------------

	// TODO NEW
	private final TimeDiscretization timeDiscretization;

	// one occupancy analyzer per considered mode
	private final Map<String, MATSimCountingStateAnalyzer<Link>> mode2stateAnalyzer;

	// where occupancies are to be tracked
	private final Set<Id<Link>> relevantLinks;

	// fast occupancy analyzer lookup for each currently traveling vehicle
	private Map<Id<Vehicle>, MATSimCountingStateAnalyzer<Link>> vehicleId2stateAnalyzer = null;

	// -------------------- CONSTRUCTION --------------------

	public DifferentiatedLinkOccupancyAnalyzer(final TimeDiscretization timeDiscretization,
			final Set<String> relevantModes, final Set<Id<Link>> relevantLinks) {
		this.timeDiscretization = timeDiscretization;
		this.mode2stateAnalyzer = new LinkedHashMap<>();
		for (String mode : relevantModes) {
			this.mode2stateAnalyzer.put(mode, new MATSimCountingStateAnalyzer<Link>(timeDiscretization));
		}
		this.relevantLinks = relevantLinks;
	}

	// -------------------- INTERNALS --------------------

	private boolean relevantLink(final Id<Link> link) {
		return ((this.relevantLinks == null) || this.relevantLinks.contains(link));
	}

	// TODO: Encapsulate and provide access to the full real-valued state vector
	// per mode.
	public MATSimCountingStateAnalyzer<Link> getNetworkModeAnalyzer(final String mode) {
		return this.mode2stateAnalyzer.get(mode);
	}

	// ---------- IMPLEMENTATION OF *EventHandler INTERFACES ----------

	// This replaces EventHandler.reset(int), which appears to be called before
	// the "before mobsim" hook.
	public void beforeIteration() {
		for (MATSimCountingStateAnalyzer<?> stateAnalyzer : this.mode2stateAnalyzer.values()) {
			stateAnalyzer.beforeIteration();
		}
		this.vehicleId2stateAnalyzer = new LinkedHashMap<>();
	}

	@Override
	public void reset(final int iteration) {
		// see the explanation of beforeIteration()
	}

	@Override
	public void handleEvent(final VehicleEntersTrafficEvent event) {
		final MATSimCountingStateAnalyzer<Link> stateAnalyzer = this.mode2stateAnalyzer.get(event.getNetworkMode());
		if (stateAnalyzer != null) { // relevantMode
			this.vehicleId2stateAnalyzer.put(event.getVehicleId(), stateAnalyzer);
			if (this.relevantLink(event.getLinkId())) {
				stateAnalyzer.registerIncrease(event.getLinkId(), (int) event.getTime());
			}
		}
	}

	@Override
	public void handleEvent(final VehicleLeavesTrafficEvent event) {
		final MATSimCountingStateAnalyzer<Link> stateAnalyzer = this.vehicleId2stateAnalyzer.get(event.getVehicleId());
		if (stateAnalyzer != null) { // relevant mode
			if (this.relevantLink(event.getLinkId())) {
				stateAnalyzer.registerDecrease(event.getLinkId(), (int) event.getTime());
			}
			this.vehicleId2stateAnalyzer.remove(event.getVehicleId());
		}
	}

	@Override
	public void handleEvent(final LinkEnterEvent event) {
		final MATSimCountingStateAnalyzer<Link> stateAnalyzer = this.vehicleId2stateAnalyzer.get(event.getVehicleId());
		if (stateAnalyzer != null) { // relevant mode
			if (this.relevantLink(event.getLinkId())) {
				stateAnalyzer.registerIncrease(event.getLinkId(), (int) event.getTime());
			}
		}
	}

	@Override
	public void handleEvent(final LinkLeaveEvent event) {
		final MATSimCountingStateAnalyzer<Link> stateAnalyzer = this.vehicleId2stateAnalyzer.get(event.getVehicleId());
		if (stateAnalyzer != null) { // relevant mode
			if (this.relevantLink(event.getLinkId())) {
				stateAnalyzer.registerDecrease(event.getLinkId(), (int) event.getTime());
			}
		}
	}

	@Override
	public void handleEvent(final VehicleAbortsEvent event) {
		final MATSimCountingStateAnalyzer<Link> stateAnalyzer = this.vehicleId2stateAnalyzer.get(event.getVehicleId());
		if (stateAnalyzer != null) { // relevant mode
			if (this.relevantLink(event.getLinkId())) {
				stateAnalyzer.registerDecrease(event.getLinkId(), (int) event.getTime());
			}
			// TODO: Based on the assumption "abort = abort trip".
			this.vehicleId2stateAnalyzer.remove(event.getVehicleId());
		}
	}

	// TODO NEW

	public Vector newStateVectorRepresentation() {
		final Vector result = new Vector(
				this.mode2stateAnalyzer.size() * this.relevantLinks.size() * this.timeDiscretization.getBinCnt());
		int i = 0;
		for (String mode : this.mode2stateAnalyzer.keySet()) {
			final MATSimCountingStateAnalyzer<Link> analyzer = this.mode2stateAnalyzer.get(mode);
			for (Id<Link> linkId : this.relevantLinks) {
				for (int bin = 0; bin < this.timeDiscretization.getBinCnt(); bin++) {
					result.set(i++, analyzer.getCount(linkId, bin));
				}
			}
		}
		return result;
	}
}
