package pathfinder;

public class Node {
	int x;
	int y;
	public double costFromStart;
	public double predictedTotalCost;
	public Node parent;
	public int clearance;

	public Node(int ix, int iy) {
		x = ix;
		y = iy;
		clearance = 0;
	}

	public String toString() {
		return "(" + x + ", " + y + ")";
	}
}
