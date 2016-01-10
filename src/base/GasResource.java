package base;

import bwapi.Unit;

public class GasResource extends Resource {

	public GasResource(Unit u) {
		super(u);
		if (!u.getType().isResourceContainer() || u.getType().isMineralField()) {
			throw new NullPointerException();
		}
	}

	public boolean gasTaken() {
		return unit.getType().isRefinery();
	}
}
