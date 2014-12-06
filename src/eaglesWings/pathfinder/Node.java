package eaglesWings.pathfinder;

public class Node {
	int x;
	int y;
	boolean walkable;
	public double costFromStart;
	public double predictedTotalCost;
	public Node parent;

	public Node(int ix, int iy, boolean iwalkable) {
		x = ix;
		y = iy;
		walkable = iwalkable;
	}

	public String toString() {
		return "(" + x + ", " + y + ")";
	}
}
