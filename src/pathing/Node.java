package pathing;

public class Node {
	int wx;
	int wy;
	public int distanceFromStart;
	public double costFromStart;
	public double predictedTotalCost;
	public Node parent;
	public int clearance;

	public Node(int ix, int iy) {
		wx = ix;
		wy = iy;
		clearance = 0;
	}

	public String toString() {
		return "(" + wx + ", " + wy + ")";
	}
}
