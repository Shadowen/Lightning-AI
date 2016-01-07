package pathfinder;

import java.util.Collection;

public abstract class WalkNode implements Iterable<WalkNodeData> {
	protected WalkNode() {
	}

	public abstract Collection<WalkNodeData> getCellsByRow(int row);

	public abstract Collection<WalkNodeData> getCellsByColumn(int column);

	public abstract WalkNodeData getCell(int x, int y);

	public abstract void setClearance(int c);

	public abstract int getClearance();
}
