package playground.mmoyo.PTCase1;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.mmoyo.PTRouter.PTLine;
import playground.mmoyo.PTRouter.PTNode;

/**
 * Represent a network layer with independent route with special (transfer) links at intersections
 * *
 * @param cityNet complete network from which the PT network is extracted
 */

public class PTNetworkLayer extends NetworkLayer {
	//--> Get these values out from the city network
	// These are temporary values for the 5x5 scenario
	int maxNodekey = 24;
	int maxLinkKey = 79;

	private final NetworkLayer cityNet;


	// This map stores the children nodes of each father node, necessary to
	// create the transfer between them
	public Map<Id, ArrayList<String>> childrenList = new TreeMap<Id, ArrayList<String>>();

	public PTNetworkLayer(final NetworkLayer cityNet) {
		super();
		this.cityNet = cityNet;
	}

	public void createPTNetwork(final List<PTLine> ptLineList) {
		// Read the route of every PTline and adds the corresponding links and
		// nodes
		for (PTLine ptLine : ptLineList) {
			boolean firstLink = true;
			String idFromNode = "";
			for (String strId : ptLine.getRoute()) {
				Link l = this.cityNet.getLink(strId);
				idFromNode = addToSubwayPTN(l, idFromNode, firstLink, ptLine.getId());
				firstLink = false;
			}
		}
		createTransferlinks();
	}

	public String addToSubwayPTN(final Link l, String idFromNode, final boolean firstLink,final Id IdPTLine) {
		// Create the "Metro underground paths" related to the city network

		// Create FromNode
		if (firstLink) {
			this.maxNodekey++;
			idFromNode = String.valueOf(this.maxNodekey);
			addPTNode(idFromNode, l.getFromNode(), IdPTLine);
		}

		// Create ToNode
		this.maxNodekey++;
		String idToNode = String.valueOf(this.maxNodekey);
		addPTNode(idToNode, l.getToNode(), IdPTLine);// ToNode

		// Create the Link
		this.maxLinkKey++;
		this.createLink(this.maxLinkKey, idFromNode, idToNode, "Standard");

		return idToNode;
	}// AddToSub

	private void addWalkingNode(final Id id) {
		//System.out.print(idImpl.toString() + " " + this.nodes.containsKey(idImpl) + " " );

		if (this.getNodes().containsKey(id)) {
			throw new IllegalArgumentException(this + "[id=" + id + " already exists]");
		}

		Node n = this.cityNet.getNode(id);
		PTNode node = new PTNode(id,	n.getCoord(), n.getType());
		node.setIdFather(id);        //All ptnodes must have a father, including fathers (themselves)
		node.setIdPTLine(new IdImpl("Walk"));
		this.getNodes().put(id, node);
		n= null;
	}

	private void addPTNode(final String strId, final Node original, final Id IdPTLine) {
		// Creates a underground clone of a node with a different ID
		Id id = new IdImpl(strId);
		Id idFather = new IdImpl(original.getId().toString());

		PTNode ptNode = new PTNode(id, original.getCoord(), original.getType(), idFather, IdPTLine);
		Id i = new IdImpl(strId);
		if (this.getNodes().containsKey(i)) {
			throw new IllegalArgumentException(this + "[id=" + id + " already exists]");
		}
		this.getNodes().put(i, ptNode);

		// updates this list of childrenNodes
		if (!this.childrenList.containsKey(idFather)) {
			ArrayList<String> ch = new ArrayList<String>();
			this.childrenList.put(idFather, ch);
		}
		this.childrenList.get(idFather).add(strId);

		id = null;
		idFather = null;
		ptNode = null;
		i = null;
	}

	// To print nodes and his respective children
	public static void showMap(final Map<Object, Object> map) {
		for(Map.Entry <Object, Object> entry: map.entrySet() ){
			Object key = entry.getKey(); 
			Object value = entry.getValue();
			System.out.println(key.toString() + " = " + value.toString());
		}
	}

	private void createTransferlinks() {
		for (List<String> chList : childrenList.values()){
			List<String> chList1 = chList;
			List<String> chList2 = chList;

			if (chList1.size() > 1) {
				for (String n1 : chList1) {
					for (String n2 : chList2) {
						if (n1 != n2) {
							this.maxLinkKey++;
							this.createLink(this.maxLinkKey, n1, n2,"Transfer");
						}//if n1
					}//for iter2
				}// for iter1
			}// if chlist
		}// while
	}// CreateTransfer


	public List<String> createWalkingLinks(final Id idFromNode, final Id idToNode ){
		//Adds temporary the origin and destination node and create new temporary links between
		//between them  and its respective children to the routing process

		addWalkingNode(idFromNode);
		addWalkingNode(idToNode);

		int i = 0;
		List<String> WalkingLinkList = new ArrayList<String>();

		//Starting links
		List<String> uChildren = this.childrenList.get(idFromNode);
		for (String strChild : uChildren) {
			this.createLink(--i, idFromNode.toString(), strChild, "Walking");
			WalkingLinkList.add(String.valueOf(i));
		}

		//Endings links
		uChildren = this.childrenList.get(idToNode);
		for (String uChild : uChildren) {
			this.createLink(--i, uChild, idToNode.toString(), "Walking");
			WalkingLinkList.add(String.valueOf(i));
		}
		return WalkingLinkList;
	}

	public void removeWalkinkLinks(final List<String> WalkingLinkList){
		//Removes temporal links at the end of the ruting process
		for (String strWalkLink :  WalkingLinkList) {
			this.removeLink(this.getLink(strWalkLink));
		}
	}

	public void removeWalkingNodes(final Id node1, final Id node2){
		//Removes temporal links at the end of the routing process
		this.removeNode(this.getNode(node1));
		this.removeNode(this.getNode(node2));
	}

	// Creates only irrelevant values to create a PTLink. Actually the cost is calculated on other parameters
	private void createLink(final int intId, final String from, final String to, final String ptType ){

		Id id = new IdImpl(String.valueOf(intId)); 
		Node fromNode = this.getNode(from);
		Node toNode = this.getNode(to);
		double length = CoordUtils.calcDistance(fromNode.getCoord(), toNode.getCoord());
		double freespeed=   1;
		double capacity =  1;
		double numLanes=  1;
		String origId =  "0";
		this.createLink(id, fromNode, toNode, length, freespeed, capacity, numLanes, origId, ptType);
	}

		/*
	private void createLink(final int id, final String from, final String to, final String ptType ){
		String length = "1";
		String freespeed= "1";
		String capacity = "1";
		String permlanes = "1";
		String origid = "0";
		this.createLink(String.valueOf(id), from, to, length, freespeed, capacity, permlanes, origid, ptType);
>>>>>>> .r5540
	}
*/


	public void printLinks() {
		//Displays a quick visualization of links with from- and to- nodes
		for (org.matsim.core.api.network.Link l : this.getLinks().values()) {
			System.out.print("\n(" );
			System.out.print(l.getFromNode().getId().toString());
			System.out.print( ")----" );
			System.out.print( l.getId().toString() );
			System.out.print( "--->(");
			System.out.print( l.getToNode().getId().toString() );
			System.out.print( ")   " + l.getType() );
			System.out.print( "      (" );
			System.out.print( ((PTNode) l.getFromNode()).getIdFather().toString());
			System.out.print( ")----" );
			System.out.print( l.getId().toString() );
			System.out.print( "--->(" );
			System.out.print( ((PTNode) l.getToNode()).getIdFather().toString() );
			System.out.print( ")");
		}
	}
}
