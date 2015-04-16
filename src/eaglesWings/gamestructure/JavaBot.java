package eaglesWings.gamestructure;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import eaglesWings.botstate.BotState;
import eaglesWings.botstate.FirstFrameState;
import eaglesWings.datastructure.Base;
import eaglesWings.datastructure.BaseManager;
import eaglesWings.datastructure.BaseStatus;
import eaglesWings.datastructure.BuildManager;
import eaglesWings.datastructure.BuildingPlan;
import eaglesWings.datastructure.MineralResource;
import eaglesWings.datastructure.Resource;
import eaglesWings.micromanager.MicroManager;
import eaglesWings.pathfinder.PathingManager;
import javabot.BWAPIEventListener;
import javabot.model.*;
import javabot.types.UnitType;
import javabot.types.UnitType.UnitTypes;

public class JavaBot implements BWAPIEventListener {

	private GameHandler game;
	// Only contains my units under construction
	private Hashtable<Integer, Unit> unitsUnderConstruction;

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
		game = new GameHandler(this);
		game.start();
	}

	public void connected() {
		game.loadTypeData();
	}

	// Method called at the beginning of the game.
	public void gameStarted() {
		try {
			// allow me to manually control units during the game
			game.enableUserInput();
			// analyze the map
			game.loadMapData(true);
			game.drawTargets(true);

			// Initialize
			unitsUnderConstruction = new Hashtable<Integer, Unit>();

			// Start all the modules
			baseManager = new BaseManager(game);
			buildManager = new BuildManager(game, baseManager);
			pathingManager = new PathingManager(game, baseManager);
			microManager = new MicroManager(game, baseManager, pathingManager);
			botState = new FirstFrameState(game, baseManager, buildManager,
					microManager, pathingManager);

			baseManager.registerDebugFunctions(game);
			buildManager.registerDebugFunctions(game);
			pathingManager.registerDebugFunctions(game);
			microManager.registerDebugFunctions(game);
			game.registerDebugFunction(new DebugModule("fps") {
				private Queue<Long> fpsQueue = new ArrayDeque<Long>();

				@Override
				public void draw(DebugEngine engine)
						throws ShapeOverflowException {
					long currentTime = System.currentTimeMillis();
					fpsQueue.add(currentTime);
					while (fpsQueue.peek() < currentTime - 1000) {
						fpsQueue.remove();
					}

					engine.drawText(20, 285, "Frame: " + game.getFrameCount(),
							true);
					engine.drawText(20, 300, "FPS: " + fpsQueue.size(), true);
				}
			});
			game.registerDebugFunction(new DebugModule("botstate") {
				@Override
				public void draw(DebugEngine engine)
						throws ShapeOverflowException {
					engine.drawText(5, 5, "Bot state: "
							+ botState.getClass().toString(), true);
				}
			});
			game.registerDebugFunction(new DebugModule("construction") {
				@Override
				public void draw(DebugEngine engine)
						throws ShapeOverflowException {
					String uucString = "";
					for (Entry<Integer, Unit> u : unitsUnderConstruction
							.entrySet()) {
						uucString += game.getUnitType(u.getValue().getTypeID())
								.getName() + ", ";
					}
					engine.drawText(5, 60, "unitsUnderConstruction: "
							+ uucString, true);
				}
			});
			game.registerDebugFunction(new DebugModule("supply") {
				@Override
				public void draw(DebugEngine engine)
						throws ShapeOverflowException {
					engine.drawText(500, 15, "Supply: "
							+ game.getSelf().getSupplyUsed() + "/"
							+ game.getSelf().getSupplyTotal(), true);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Method called on every logical frame
	public void gameUpdate() {
		try {
			// Check if any units have completed
			Iterator<Entry<Integer, Unit>> unitsUnderConstructionSet = unitsUnderConstruction
					.entrySet().iterator();
			while (unitsUnderConstructionSet.hasNext()) {
				Entry<Integer, Unit> entry = unitsUnderConstructionSet.next();
				int unitID = entry.getKey();
				Unit u = entry.getValue();
				if (u.isCompleted()) {
					unitsUnderConstructionSet.remove();
					unitComplete(unitID);
				}
			}

			// Base occupation detection
			for (Base b : baseManager) {
				int bx = b.getX();
				int by = b.getY();
				// If we can see the base
				if (game.isVisible(bx / 32, by / 32)) {
					// Find the closest resource depot
					Unit closestCC = null;
					double closestDistance = Double.MIN_VALUE;
					for (Unit u : game.getAllUnits()) {
						// Only check resource depots
						UnitTypes type = UnitTypes.values()[u.getTypeID()];
						if (!GameHandler.resourceDepotTypes.contains(type)) {
							continue;
						}
						// Calculate the distance
						double newDistance = Point.distance(bx, by, u.getX(),
								u.getY());
						if (newDistance < BASE_RADIUS) {
							if (closestCC == null
									|| newDistance < closestDistance) {
								closestCC = u;
								closestDistance = newDistance;
							}
						}
					}
					// Categorize the base
					if (closestCC == null) {
						b.setStatus(BaseStatus.UNOCCUPIED);
					} else {
						if (closestCC.getPlayerID() == game.getSelf().getID()) {
							b.setStatus(BaseStatus.OCCUPIED_SELF);
						} else {
							b.setStatus(BaseStatus.OCCUPIED_ENEMY);
						}
					}
				}
			}

			// Allow the bot to act
			// Bot state updates
			botState = botState.act();
			// BuildManager check build order
			buildManager.checkMinimums();
			// Micro units
			microManager.act();

			// Auto economy
			for (Base b : baseManager.getMyBases()) {
				if (b.commandCenter == null) {
					continue;
				}

				b.gatherResources();

				// Train SCVS if necessary
				// This can't go in the build queue since it is specific to a
				// command center!
				if (game.getSelf().getMinerals() >= 50
						&& b.commandCenter.getTrainingQueueSize() == 0) {
					for (Entry<Integer, MineralResource> mineral : b.minerals
							.entrySet()) {
						if (mineral.getValue().getNumGatherers() < 2) {
							// Do training
							game.train(b.commandCenter.getID(),
									UnitTypes.Terran_SCV.ordinal());
							break;
						}
					}
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
			Unit unit = game.getUnit(unitID);
			if (unit.getPlayerID() == game.getSelf().getID()) {
				unitsUnderConstruction.put(unitID, unit);
				microManager.unitCreate(unitID);
			}
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
			game.sendText("Unit complete: "
					+ game.getUnitType(typeID).getName());

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
			// Update botstate
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
		unitCreate(unitID);
	}

	public void unitShow(int unitID) {
	}

	public void keyPressed(int keyCode) {
		game.sendText(String.valueOf(keyCode));
	}
}
