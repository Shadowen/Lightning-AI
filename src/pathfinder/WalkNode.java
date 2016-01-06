package pathfinder;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Square tiles of 8x8 or 1x1 for the pathfinder to operate on. Positions are in
 * pixels unless otherwise stated.
 */
public class WalkNode extends Rectangle {
	public int distanceFromStart;
	public double costFromStart;
	public double predictedTotalCost;
	public WalkNode parent;
	public int clearance;

	public WalkNode(int ix, int iy, int iw) {
		x = ix;
		y = iy;
		width = iw;
		height = iw;
		clearance = 0;
	}

	public String toString() {
		return "(" + x + ", " + y + ")";
	}
}
