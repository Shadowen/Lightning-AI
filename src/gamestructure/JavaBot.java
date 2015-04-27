package gamestructure;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.Predicate;

import micromanager.MicroManager;
import pathfinder.PathingManager;
import datastructure.Base;
import datastructure.BaseManager;
import datastructure.BaseStatus;
import datastructure.BuildManager;
import datastructure.MineralResource;
import botstate.BotState;
import botstate.FirstFrameState;
import bwapi.DefaultBWListener;
import bwapi.Mirror;
import bwapi.Player;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;

public class JavaBot extends DefaultBWListener {
	private Mirror mirror = new Mirror();
	private GameHandler game;
	private DebugEngine debugEngine;
	// Only contains my units under construction
	private List<Unit> unitsUnderConstruction;

	private BotState botState;
	private BaseManager baseManager;
	private BuildManager buildManager;
	private MicroManager microManager;
	private PathingManager pathingManager;

	// The radius the bot looks around a potential base location to determine if
	// it is occupied
	private static final double BASE_RADIUS = 300;

	public static void main(String[] args) {
		new JavaBot();
	}

	public JavaBot() {
		mirror.getModule().setEventListener(this);
		mirror.startGame();
	}

	/**
	 * Method called at the beginning of the game.
	 */
	@Override
	public void onStart() {
		game = new GameHandler(mirror.getGame());
		debugEngine = new DebugEngine(mirror.getGame());
		try {
			game.setTextSize(1);
			// allow me to manually control units during the game
			game.enableFlag(1);
			// Use BWTA to analyze map
			// This may take a few minutes if the map is processed first time!
			System.out.println("Analyzing map...");
			BWTA.readMap();
			BWTA.analyze();
			System.out.println("Map data ready");

			// Initialize
			unitsUnderConstruction = new ArrayList<Unit>();

			// Start all the modules
			baseManager = new BaseManager(game, debugEngine);
			buildManager = new BuildManager(game, baseManager, debugEngine);
			pathingManager = new PathingManager(game, baseManager, debugEngine);
			microManager = new MicroManager(game, baseManager, pathingManager,
					debugEngine);
			botState = new FirstFrameState(game, baseManager, buildManager,
					microManager, pathingManager);
			registerDebugFunctions(debugEngine);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method called on every logical frame
	 */
	@Override
	public void onFrame() {
		try {
			// Check if any units have completed
			unitsUnderConstruction.removeIf(new Predicate<Unit>() {
				@Override
				public boolean test(bwapi.Unit unit) {
					if (unit.isCompleted()) {
						botState.unitComplete(unit);
						return true;
					}
					return false;
				}
			});
			// // Base occupation detection
			// for (Base b : baseManager) {
			// int bx = b.getX();
			// int by = b.getY();
			// // If we can see the base
			// if (game.isVisible(bx / 32, by / 32)) {
			// // Find the closest resource depot
			// Unit closestCC = null;
			// double closestDistance = Double.MIN_VALUE;
			// for (Unit u : game.getAllUnits()) {
			// // Only check resource depots
			// UnitType type = u.getType();
			// if (!type.isResourceDepot()) {
			// continue;
			// }
			// // Calculate the distance
			// double newDistance = Point.distance(bx, by, u.getX(),
			// u.getY());
			// if (newDistance < BASE_RADIUS) {
			// if (closestCC == null
			// || newDistance < closestDistance) {
			// closestCC = u;
			// closestDistance = newDistance;
			// }
			// }
			// }
			// // Categorize the base
			// if (closestCC == null) {
			// b.setStatus(BaseStatus.UNOCCUPIED);
			// } else {
			// if (closestCC.getPlayer() == game.self()) {
			// b.setStatus(BaseStatus.OCCUPIED_SELF);
			// } else {
			// b.setStatus(BaseStatus.OCCUPIED_ENEMY);
			// }
			// }
			// }
			// }

			// Allow the bot to act
			// Bot state updates
			botState = botState.act();
			// BuildManager check build order
			// buildManager.checkMinimums();
			// Micro units
			// microManager.act();

			// Auto economy
			for (Base b : baseManager.getMyBases()) {
				if (b.commandCenter == null) {
					continue;
				}

				b.gatherResources();

				// // Train SCVS if necessary
				// // This can't go in the build queue since it is specific to a
				// // command center!
				// if (game.self().minerals() >= 50
				// && !b.commandCenter.isTraining()) {
				// for (Entry<Integer, MineralResource> mineral : b.minerals
				// .entrySet()) {
				// if (mineral.getValue().getNumGatherers() < 2) {
				// // Do training
				// b.commandCenter.train(UnitType.Terran_SCV);
				// break;
				// }
				// }
				// }
			}

			// Auto build
			// for (BuildingPlan toBuild : buildManager.buildingQueue) {
			// // If we have the minerals and gas
			// if (game.self().minerals() > toBuild.getType().mineralPrice()
			// && game.self().gas() >= toBuild.getType().gasPrice()) {
			// // If it has a builder, tell them to hurry up!
			// if (toBuild.hasBuilder()) {
			// toBuild.builder.build(toBuild);
			// } else {
			// // If it isn't being built yet
			// baseManager.getBuilder().build(toBuild);
			// }
			// }
			// }
			// Auto train
			// for (UnitType toTrain : buildManager.unitQueue)
			// if (toTrain != null) {
			// UnitType trainFrom = toTrain.whatBuilds(); // TODO
			//
			// // TODO only go through my units
			// for (Unit u : game.getAllUnits()) {
			// if (u.getType() == trainFrom && !u.isTraining()) {
			// u.train(toTrain);
			// break;
			// }
			// }
			// }

			// Draw debug information on screen
			debugEngine.draw();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onEnd(boolean winner) {
	}

	public void onNukeDetect(Position p) {
	}

	@Override
	public void onUnitDiscover(Unit unit) {
		try {
			// Update botstate
			botState = botState.unitDiscover(unit);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onUnitEvade(Unit unit) {
	}

	@Override
	public void onUnitShow(Unit unit) {
	}

	@Override
	public void onUnitHide(Unit unit) {
	}

	@Override
	public void onUnitCreate(Unit unit) {
		try {
			if (unit.getPlayer() == game.self()) {
				unitsUnderConstruction.add(unit);
				microManager.unitCreate(unit);
			}
			botState = botState.unitCreate(unit);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onUnitDestroy(Unit unit) {
		try {
			// If a unit is canceled from a build queue or building is cancelled
			// under construction
			unitsUnderConstruction.remove(unit);

			// Remove workers from the baseManager
			baseManager.removeWorker(unit);
			// Deletes units from microManager
			microManager.unitDestroy(unit);

			// Allow the bot state to act
			botState = botState.unitDestroy(unit);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onUnitMorph(Unit unit) {
		onUnitCreate(unit);
	}

	@Override
	public void onUnitRenegade(Unit unit) {

	}

	@Override
	public void onSaveGame(String s) {

	}

	@Override
	public void onUnitComplete(Unit unit) {
		try {
			UnitType type = unit.getType();
			if (unit.getPlayer() == game.self()) {
				System.out.println("Unit complete: " + type.toString());

				buildManager.doneBuilding(unit);

				if (type == UnitType.Terran_SCV) {
					// Add new workers to nearest base
					Base base = baseManager.getClosestBase(unit.getX(),
							unit.getY());
					base.addWorker(unit);
				} else if (type == UnitType.Terran_Command_Center) {
					// Add command centers to nearest base
					Base base = baseManager.getClosestBase(unit.getX(),
							unit.getY());
					base.commandCenter = unit;
					base.setStatus(BaseStatus.OCCUPIED_SELF);
				}

				botState = botState.unitComplete(unit);
			}

			if (type == UnitType.Resource_Mineral_Field
					|| type == UnitType.Resource_Mineral_Field_Type_2
					|| type == UnitType.Resource_Mineral_Field_Type_3) {
				Base base = baseManager
						.getClosestBase(unit.getX(), unit.getY());
				base.minerals.add(new MineralResource(unit));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onSendText(String s) {
		if (s.startsWith("/")) {
			String[] command = s.substring(1).split(" ");
			debugEngine.onReceiveCommand(command);
		}
	}

	/**
	 * Register my own debug functions to the debugEngine.
	 * 
	 * @param de
	 *            The debugEngine to be used. Should usually be its own
	 *            debugEngine.
	 */
	private void registerDebugFunctions(DebugEngine de) {
		de.registerDebugFunction(new DebugModule("fps") {
			private static final int yBottom = 285;

			@Override
			public void draw(DebugEngine engine) throws ShapeOverflowException {
				engine.drawTextScreen(10, yBottom - 15 * 2,
						"Frame: " + game.getFrameCount());
				engine.drawTextScreen(10, yBottom - 15, "FPS: " + game.getFPS());
				engine.drawTextScreen(10, yBottom, "APM: " + game.getAPM());
			}
		});
		de.registerDebugFunction(new DebugModule("botstate") {
			@Override
			public void draw(DebugEngine engine) throws ShapeOverflowException {
				engine.drawTextScreen(5, 5, "Bot state: "
						+ botState.getClass().toString());
			}
		});
		de.registerDebugFunction(new DebugModule("construction") {
			@Override
			public void draw(DebugEngine engine) throws ShapeOverflowException {
				String uucString = "";
				for (Unit u : unitsUnderConstruction) {
					uucString += u.getType().toString() + ", ";
				}
				engine.drawTextScreen(5, 60, "unitsUnderConstruction: "
						+ uucString);
			}
		});
		de.registerDebugFunction(new DebugModule("supply") {
			@Override
			public void draw(DebugEngine engine) throws ShapeOverflowException {
				engine.drawTextScreen(550, 15, "Supply: "
						+ game.self().supplyUsed() + "/"
						+ game.self().supplyTotal());
			}
		});
	}

}
