package eaglesWings.gamestructure;

import java.util.Hashtable;
import java.util.Map.Entry;
import eaglesWings.botstate.BotState;
import eaglesWings.botstate.FirstFrameState;
import eaglesWings.datastructure.Base;
import eaglesWings.datastructure.BaseManager;
import eaglesWings.datastructure.BuildManager;
import eaglesWings.datastructure.BuildingPlan;
import eaglesWings.micromanager.MicroManager;
import javabot.BWAPIEventListener;
import javabot.model.*;
import javabot.types.UnitType.UnitTypes;

public class JavaBot implements BWAPIEventListener {

	private GameHandler game;
	private Hashtable<Integer, Unit> unitsUnderConstruction;

	private BotState botState;
	private BaseManager baseManager;
	private BuildManager buildManager;
	private MicroManager microManager;

	public static void main(String[] args) {
		new JavaBot();
	}

	public JavaBot() {
		game = new GameHandler(this);
		game.start();
	}

	public void connected() {
		game.loadTypeData();
	}

	// Method called at the beginning of the game.
	public void gameStarted() {
		// Run BWTA
		game.loadMapData(true);
		// Allow manual control of units during the game
		game.enableUserInput();
		// Draw the commands each unit is executing
		game.drawTargets(true);

		// Initialize
		unitsUnderConstruction = new Hashtable<Integer, Unit>();

		// Start all the modules
		baseManager = new BaseManager(game);
		buildManager = new BuildManager(game, baseManager);
		botState = new FirstFrameState(game, baseManager, buildManager);
		microManager = new MicroManager(game);

		// Register debuggers
		baseManager.registerDebugFunctions(game);
		buildManager.registerDebugFunctions(game);
		microManager.registerDebugFunctions(game);
		game.registerDebugFunction(new DebugModule() {
			@Override
			public void draw(DebugEngine engine) {
				engine.drawText(5, 5, "Bot state: "
						+ botState.getClass().toString(), true);
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
		game.registerDebugFunction(new DebugModule() {
			@Override
			public void draw(DebugEngine engine) {
				String uucString = "";
				for (Entry<Integer, Unit> u : unitsUnderConstruction.entrySet()) {
					uucString += game.getUnitType(u.getValue().getTypeID())
							.getName() + ", ";
				}
				engine.drawText(5, 60, "unitsUnderConstruction: " + uucString,
						true);
				engine.drawText(500, 15, "Supply: "
						+ game.getSelf().getSupplyUsed() + "/"
						+ game.getSelf().getSupplyTotal(), true);
			}
		});
	}

	// Method called on every logical frame
	public void gameUpdate() {
		// Check if any units have completed
		for (Entry<Integer, Unit> u : unitsUnderConstruction.entrySet()) {
			if (u.getValue().isCompleted()) {
				game.sendText("Unit completed: "
						+ game.getUnitType(u.getValue().getTypeID()).getName());
				unitsUnderConstruction.remove(u.getKey());
				unitComplete(u.getKey());
			}
		}

		// Allow the bot to act
		try {
			botState = botState.act();

			microManager.micro();

			// Auto economy
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

			// Auto supplies
			// Add supply depots if necessary
			if (game.getSelf().getSupplyUsed() > game.getSelf()
					.getSupplyTotal() - 2 * 2) {
				// Check that it's not already in the queue
				if (!buildManager.isInQueue(UnitTypes.Terran_Supply_Depot)) {
					buildManager.addToQueue(UnitTypes.Terran_Supply_Depot);
				}
			}

			// Auto build
			for (BuildingPlan toBuild : buildManager.buildingQueue) {
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
			// Auto train
			for (UnitTypes toTrain : buildManager.unitQueue)
				if (toTrain != null) {
					int trainFrom = game.getUnitType(toTrain.ordinal())
							.getWhatBuildID();

					for (Unit u : game.getMyUnits()) {
						if (u.getTypeID() == trainFrom
								&& u.getTrainingQueueSize() == 0) {
							game.train(u.getID(), toTrain.ordinal());
							break;
						}
					}
				}
			// Draw debug information on screen
			game.drawDebug();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Some additional event-related methods.
	public void gameEnded() {
	}

	public void matchEnded(boolean winner) {
	}

	public void nukeDetect(int x, int y) {
	}

	public void nukeDetect() {
	}

	public void playerLeft(int id) {
	}

	public void unitCreate(int unitID) {
		try {
			unitsUnderConstruction.put(unitID, game.getUnit(unitID));
			microManager.unitCreate(unitID);
			botState = botState.unitCreate(unitID);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// This event is triggered when a unit is finished building.
	public void unitComplete(int unitID) {
		try {
			Unit u = game.getUnit(unitID);
			int typeID = u.getTypeID();

			buildManager.doneBuilding(u);

			if (typeID == UnitTypes.Terran_SCV.ordinal()) {
				// Add new workers to nearest base
				Base base = baseManager.getClosestBase(u.getX(), u.getY());
				base.addWorker(unitID, u);
			} else if (typeID == UnitTypes.Terran_Command_Center.ordinal()) {
				// Add command centers to nearest base
				Base base = baseManager.getClosestBase(u.getX(), u.getY());
				base.commandCenter = u;
			}

			botState = botState.unitComplete(unitID);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void unitDestroy(int unitID) {
		try {
			// If a unit is canceled from a build queue or building is cancelled
			// under construction
			unitsUnderConstruction.remove(unitID);
			if (unitsUnderConstruction.containsKey(unitID)) {
				game.sendText("Unit under construction destroyed!");
			}

			// Remove workers from the baseManager
			baseManager.removeWorker(unitID);
			// Deletes units from microManager
			microManager.unitDestroy(unitID);

			// Allow the bot state to act
			botState = botState.unitDestroy(unitID);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void unitDiscover(int unitID) {
		try {
			botState = botState.unitDiscover(unitID);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void unitEvade(int unitID) {
	}

	public void unitHide(int unitID) {
	}

	public void unitMorph(int unitID) {
		Unit unit = game.getUnit(unitID);
		unitsUnderConstruction.put(unitID, unit);
	}

	public void unitShow(int unitID) {
	}

	public void keyPressed(int keyCode) {
	}
}
