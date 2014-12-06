package eaglesWings.pathfinder;

import java.util.ArrayList;
import java.util.List;

public class NodeSet {
	private List<Node> nodes;

	public NodeSet() {
		nodes = new ArrayList<Node>();
	}

	public int size() {
		return nodes.size();
	}

	public void add(Node n) {
		nodes.add(n);
	}

	public Node getNext() {
		Node lowestCostNode = null;
		for (Node n : nodes) {
			if (lowestCostNode == null
					|| n.predictedTotalCost < lowestCostNode.predictedTotalCost) {
				lowestCostNode = n;
			}
		}
		return lowestCostNode;
	}

	public boolean contains(Node n) {
		return nodes.contains(n);
	}

	public void remove(Node n) {
		nodes.remove(n);
	}

	public String toString() {
		String result = "[";
		for (Node n : nodes) {
			result += n + ", ";
		}
		// Chop off the extra comma and space and close the square bracket
		result = result.substring(0, result.length() - 2) + "]";
		return result;
	}
}
