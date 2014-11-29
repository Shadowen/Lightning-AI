package javabot.datastructure;

import javabot.model.Unit;
import javabot.types.UnitType.UnitTypes;

public class GasResource extends Resource {

	public GasResource(Unit u) {
		super(u);
	}

	public boolean gasTaken() {
		return unit.getTypeID() == UnitTypes.Terran_Refinery.ordinal()
				|| unit.getTypeID() == UnitTypes.Protoss_Assimilator.ordinal()
				|| unit.getTypeID() == UnitTypes.Zerg_Extractor.ordinal();
	}
}
