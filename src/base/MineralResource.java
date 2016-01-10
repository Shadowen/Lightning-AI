package base;

import bwapi.Unit;

public class MineralResource extends Resource {

	public MineralResource(Unit u) {
		super(u);
		if (!u.getType().isMineralField()) {
			throw new NullPointerException();
		}
	}

}
