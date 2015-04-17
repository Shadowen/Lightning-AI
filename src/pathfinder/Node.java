package pathfinder;

public class Node {
	int x;
	int y;
	boolean walkable;
	public double costFromStart;
	public double predictedTotalCost;
	public Node parent;

	public Node(int ix, int iy) {
		x = ix;
		y = iy;
	}

	public String toString() {
		return "(" + x + ", " + y + ")";
	}
}
