package micro;

import bwapi.Unit;

public class UnrecognizedUnitTypeException extends Exception {
	public Unit unit;

	public UnrecognizedUnitTypeException(Unit unit) {
		this.unit = unit;
	}

}
