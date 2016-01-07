package pathfinder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

public class WalkNode1 extends WalkNode {
	private WalkNodeData[][] data;

	public WalkNode1(WalkNodeData[][] idata) {
		super();
		data = idata;
	}

	@Override
	public Collection<WalkNodeData> getCellsByRow(int row) {
		Collection<WalkNodeData> list = new ArrayList<>();
		for (int x = 0; x < 8; x++) {
			list.add(data[x][row]);
		}
		return list;
	}

	@Override
	public Collection<WalkNodeData> getCellsByColumn(int column) {
		return Arrays.asList(data[column]);
	}

	@Override
	public WalkNodeData getCell(int x, int y) {
		return data[x][y];
	}

	@Override
	public void setClearance(int c) {
		for (int x = 0; x < data.length; x++) {
			for (int y = 0; y < data[x].length; y++) {
				data[x][y].clearance = c;
			}
		}
	}

	@Override
	public int getClearance() {
		int min = Integer.MAX_VALUE;
		for (int x = 0; x < data.length; x++) {
			for (int y = 0; y < data[x].length; y++) {
				if (data[x][y].clearance > min) {
					min = data[x][y].clearance;
				}
			}
		}
		return min;
	}

	@Override
	public Iterator<WalkNodeData> iterator() {
		return Arrays.asList(data).stream().flatMap(a -> Arrays.asList(a).stream()).iterator();
	}
}
