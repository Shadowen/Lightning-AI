package pathfinder;

public class WalkNodeData {
	public final int x;
	public final int y;
	public final int size;
	public int distanceFromStart;
	public double costFromStart;
	public double predictedTotalCost;
	public WalkNodeData parent;
	public int clearance;

	public WalkNodeData(int ix, int iy, int isize) {
		x = ix;
		y = iy;
		size = isize;
	}

	public String toString() {
		return "(" + x + ", " + y + " = )" + size;
	}
}
