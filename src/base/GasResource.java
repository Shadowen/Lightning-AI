package base;

import bwapi.Unit;
import bwapi.UnitType;

/**
 * A {@link UnitType.Resource_Vespene_Geyser}, {@link UnitType.Terran_Refinery},
 * {@link UnitType.Protoss_Assimilator}, or {@link UnitType.Zerg_Extractor}.
 * 
 * @author wesley
 *
 */
public class GasResource extends Resource {

	public GasResource(Unit u) {
		super(u);
		if (!u.getType().isResourceContainer() || u.getType().isMineralField()) {
			System.err.println("Non-gas unit used for GasResource!");
		}
	}

	public boolean gasTaken() {
		return unit.getType().isRefinery();
	}
}
