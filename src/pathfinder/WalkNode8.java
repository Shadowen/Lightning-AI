package pathfinder;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

public class WalkNode8 extends WalkNode {
	private WalkNodeData data;

	public WalkNode8(WalkNodeData idata) {
		super();
		data = idata;
	}

	@Override
	public Collection<WalkNodeData> getCellsByRow(int row) {
		return Arrays.asList(data);
	}

	@Override
	public Collection<WalkNodeData> getCellsByColumn(int column) {
		return Arrays.asList(data);
	}

	@Override
	public WalkNodeData getCell(int x, int y) {
		return data;
	}

	@Override
	public void setClearance(int c) {
		data.clearance = c;
	}

	@Override
	public int getClearance() {
		return data.clearance;
	}

	@Override
	public Iterator<WalkNodeData> iterator() {
		return Arrays.asList(data).stream().iterator();
	}
}
