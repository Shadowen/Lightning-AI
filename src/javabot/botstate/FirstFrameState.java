package javabot.botstate;

import java.util.ArrayList;
import java.util.List;

import javabot.datastructure.Base;
import javabot.datastructure.GameHandler;
import javabot.model.BaseLocation;
import javabot.model.Unit;
import javabot.types.UnitType.UnitTypes;

public class FirstFrameState extends BotState {

	public FirstFrameState(GameHandler game) {
		super(game);
	}

	@Override
	public BotState act() {
		// Set up the main
		bases = new ArrayList<Base>();
		Base mainBase = null;

		// Sort through the units and find my SCVs and CC
		List<Unit> units = game.getMyUnits();
		List<Unit> workers = new ArrayList<Unit>();
		Unit commandCenter = null;
		for (Unit u : units) {
			if (u.getTypeID() == UnitTypes.Terran_SCV.ordinal()) {
				workers.add(u);
				game.sendText("SCV found");
			} else if (u.getTypeID() == UnitTypes.Terran_Command_Center
					.ordinal()) {
				commandCenter = u;
				game.sendText("Command center found.");
			}
		}
		if (commandCenter == null) {
			throw new NullPointerException(); // TODO custom exceptions
		}

		// Find the location we're at
		List<BaseLocation> baseLocations = game.getMap().getBaseLocations();
		for (BaseLocation location : baseLocations) {
			if (location.getX() == commandCenter.getX()
					&& location.getY() == commandCenter.getY()) {
				mainBase = new Base(game, location);
				mainBase.workers = workers;
				mainBase.commandCenter = commandCenter;
				game.sendText("Location found.");
				break;
			}
		}
		if (mainBase == null) {
			throw new NullPointerException(); // TODO custom exceptions
		}

		bases.add(mainBase);

		game.sendText("First frame initialization complete!");
		return new OpeningBuildState(this);
	}
}
