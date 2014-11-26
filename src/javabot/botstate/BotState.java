package javabot.botstate;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javabot.datastructure.Base;
import javabot.datastructure.BaseManager;
import javabot.datastructure.BuildManager;
import javabot.datastructure.BuildingPlan;
import javabot.gamestructure.DebugEngine;
import javabot.gamestructure.DebugModule;
import javabot.gamestructure.Debuggable;
import javabot.gamestructure.GameHandler;
import javabot.model.Unit;
import javabot.types.UnitType;
import javabot.types.UnitType.UnitTypes;
import javabot.util.BWColor;

public abstract class BotState implements Debuggable {
	public GameHandler game;
	public BaseManager baseManager;
	public BuildManager buildManager;

	// Constructor used for creating a new BotState
	protected BotState(GameHandler igame) {
		game = igame;
		baseManager = new BaseManager(game.getSelf().getID(), game);
		buildManager = new BuildManager(game, baseManager);
	}

	// Constructor for moving from one state to another
	protected BotState(BotState oldState) {
		game = oldState.game;
		baseManager = oldState.baseManager;
		buildManager = oldState.buildManager;
	}

	public abstract BotState act();

	public BotState unitCreate(int unitID) {
		return this;
	}

	public BotState unitComplete(int unitID) {
		Unit u = game.getUnit(unitID);
		buildManager.doneBuilding(u);
		return this;
	}

	public BotState unitDestroy(int unitID) {
		return this;
	}

	public BotState unitDiscover(int unitID) {
		return this;
	}

	protected void autoEconomy() {
		for (Base b : baseManager.getMyBases()) {
			b.gatherResources();

			// Train SCVS if necessary
			// This can't go in the build queue since it is specific to a
			// command center!
			if (b.getWorkerCount() < b.getMineralCount() * 2) {
				if (game.getSelf().getMinerals() >= 50
						&& b.commandCenter.getTrainingQueueSize() == 0) {
					game.train(b.commandCenter.getID(),
							UnitTypes.Terran_SCV.ordinal());
				}
			}
		}
	}

	protected void autoSupplies() {
		// Add supply depots if necessary
		if (game.getSelf().getSupplyUsed() > game.getSelf().getSupplyTotal() - 4) {
			// Check that it's not already in the queue
			if (!buildManager.buildQueueContains(UnitTypes.Terran_Supply_Depot)) {
				buildManager.addBuilding(UnitTypes.Terran_Supply_Depot);
			}
		}
	}

	protected void autoBuild() {
		BuildingPlan toBuild = buildManager.getToBuild();
		// Attempt to build the next building
		if (toBuild != null) {
			// If we have the minerals and gas
			if (game.getSelf().getMinerals() > game.getUnitType(
					toBuild.getTypeID()).getMineralPrice()
					&& game.getSelf().getGas() >= game.getUnitType(
							toBuild.getTypeID()).getGasPrice()) {
				// If it has a builder, tell them to hurry up!
				if (toBuild.hasBuilder()) {
					toBuild.builder.build(toBuild);
				} else {
					// If it isn't being built yet
					baseManager.getBuilder().build(toBuild);
				}
			}
		}
	}

	protected void autoTrain() {
		UnitTypes toTrain = buildManager.getToTrain();
		if (toTrain != null) {
			int trainFrom = game.getUnitType(toTrain.ordinal())
					.getWhatBuildID();

			for (Unit u : game.getMyUnits()) {
				if (u.getTypeID() == trainFrom && u.getTrainingQueueSize() == 0) {
					game.train(u.getID(), toTrain.ordinal());
					buildManager.removeUnitFromQueue(toTrain);
					break;
				}
			}
		}
	}

	@Override
	public void registerDebugFunctions(GameHandler g) {
		// Which botstate am I in?
		g.registerDebugFunction(new DebugModule() {
			@Override
			public void draw(DebugEngine engine) {
				game.drawText(5, 5, this.getClass().toString(), true);

				// for (int i = 0; i < game.getMap().getWidth(); i++) {
				// for (int e = 0; e < game.getMap().getHeight(); e++) {
				// if (game.isBuildable(i, e, true)) {
				// engine.drawBox(i * 32 + 16 - 5, e * 32 + 16 - 5,
				// i * 32 + 16 + 5, e * 32 + 16 + 5,
				// BWColor.GREEN, true, false);
				// } else {
				// engine.drawBox(i * 32 + 16 - 5, e * 32 + 16 - 5,
				// i * 32 + 16 + 5, e * 32 + 16 + 5,
				// BWColor.RED, true, false);
				// }
				// }
				// }
			}
		});

		buildManager.registerDebugFunctions(g);
		baseManager.registerDebugFunctions(g);
	}
}
