package datastructure;

import bwapi.Unit;
import bwapi.UnitType;

public class GasResource extends Resource {

	public GasResource(Unit u) {
		super(u);
	}

	public boolean gasTaken() {
		return unit.getType() == UnitType.Terran_Refinery
				|| unit.getType() == UnitType.Protoss_Assimilator
				|| unit.getType() == UnitType.Zerg_Extractor;
	}
}
