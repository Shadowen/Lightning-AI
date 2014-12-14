package eaglesWings.gamestructure;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import eaglesWings.botstate.BotState;
import eaglesWings.botstate.FirstFrameState;
import eaglesWings.datastructure.Base;
import eaglesWings.datastructure.BaseManager;
import eaglesWings.datastructure.BuildManager;
import eaglesWings.datastructure.BuildingPlan;
import eaglesWings.datastructure.Resource;
import javabot.BWAPIEventListener;
import javabot.model.*;
import javabot.types.*;
import javabot.types.OrderType.OrderTypeTypes;
import javabot.types.UnitType.UnitTypes;
import javabot.util.BWColor;

public class JavaBot implements BWAPIEventListener {

	private GameHandler game;
	private Hashtable<Integer, Unit> unitsUnderConstruction;

	private BotState botState;
	private BaseManager baseManager;
	private BuildManager buildManager;

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

	// private int mapWidth;
	// private int mapHeight;

	// Method called at the beginning of the game.
	public void gameStarted() {
		// allow me to manually control units during the game
		game.enableUserInput();
		// analyze the map
		game.loadMapData(true);
		game.drawTargets(true);

		// Initialize
		unitsUnderConstruction = new Hashtable<Integer, Unit>();

		baseManager = new BaseManager(game);
		buildManager = new BuildManager(game, baseManager);
		botState = new FirstFrameState(game, baseManager, buildManager);

		baseManager.registerDebugFunctions(game);
		buildManager.registerDebugFunctions(game);
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
		// mapWidth = game.getMap().getWidth();
		// mapHeight = game.getMap().getHeight();
		// threatMap = new double[mapHeight][mapWidth];

		// Timer t = new Timer();
		//
		// t.scheduleAtFixedRate(new TimerTask() {
		// @Override
		// public void run() {
		// // Reset threat counter
		// for (int x = 0; x < 128; x++) {
		// for (int y = 0; y < 128; y++) {
		// threatMap[x][y] = 0;
		// }
		// }
		//
		// // Count the threats
		// for (Unit u : game.getEnemyUnits()) {
		// // Get the x and y grid point coordinates
		// int x = u.getX() / 32;
		// int y = u.getY() / 32;
		// // Get the ground weapon's range
		// double radius = game
		// .getWeaponType(
		// game.getUnitType(u.getTypeID())
		// .getGroundWeaponID()).getMaxRange() / 32 + 2;
		// double threat = 1;
		// ArrayList<Point> threatPoints = generateCircleCoordinates(
		// x, y, radius);
		// for (Point p : threatPoints) {
		// threatMap[p.x][p.y] += threat
		// * (radius - p.distance(x, y)) / radius;
		// }
		// }
		// }
		//
		// private ArrayList<Point> generateCircleCoordinates(int cx, int cy,
		// double r) {
		// ArrayList<Point> points = new ArrayList<Point>();
		// for (int x = (int) Math.floor(-r); x < r; x++) {
		// int y1 = (int) Math.round(Math.sqrt(Math.pow(r, 2)
		// - Math.pow(x, 2)));
		// int y2 = -y1;
		// for (int y = y2; y < y1; y++) {
		// if (x + cx > 0 && x + cx < mapWidth && y + cy > 0
		// && y + cy < mapHeight) {
		// points.add(new Point(x + cx, y + cy));
		// }
		// }
		// }
		// return points;
		// }
		//
		// }, 1000, 1000);
	}

	// private Point retreat(int x, int y, int distance) {
	// if (distance <= 0) {
	// return new Point(x, y);
	// }
	//
	// Point bestRetreat = new Point();
	//
	// // Grid coordinates of x and y
	// int gx = (int) Math.round(x / 32);
	// int gy = (int) Math.round(y / 32);
	//
	// double minValue = Double.MAX_VALUE;
	// double threatMapValue = threatMap[gx + 1][gy + 1];
	// if (threatMapValue < minValue) {
	// bestRetreat.x = x + 32;
	// bestRetreat.y = y + 32;
	// minValue = threatMapValue;
	// }
	// threatMapValue = threatMap[gx + 1][gy - 1];
	// if (threatMapValue < minValue) {
	// bestRetreat.x = x + 32;
	// bestRetreat.y = y - 32;
	// minValue = threatMapValue;
	// }
	// threatMapValue = threatMap[gx - 1][gy + 1];
	// if (threatMapValue < minValue) {
	// bestRetreat.x = x - 32;
	// bestRetreat.y = y + 32;
	// minValue = threatMapValue;
	// }
	// threatMapValue = threatMap[gx - 1][gy - 1];
	// if (threatMapValue < minValue) {
	// bestRetreat.x = x - 32;
	// bestRetreat.y = y - 32;
	// minValue = threatMapValue;
	// }
	//
	// return retreat(bestRetreat.x, bestRetreat.y, distance - 32);
	// }

	// Method called on every frame (approximately 30x every second).
	public void gameUpdate() {
		// Check if any units have completed
		for (Entry<Integer, Unit> u : unitsUnderConstruction.entrySet()) {
			if (u.getValue().isCompleted()) {
				unitsUnderConstruction.remove(u.getKey());
				unitComplete(u.getKey());
			}
		}

		// Allow the bot to act
		try {
			// Bot state updates
			botState = botState.act();

			// BuildManager check build order
			buildManager.checkMinimums();

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

		/*
		 * for (Unit u : game.getMyUnits()) { if (u.getTypeID() ==
		 * UnitTypes.Terran_SCV.ordinal()) { // SCVs } else if (u.getTypeID() ==
		 * UnitTypes.Terran_Wraith.ordinal()) { // Wraith micro int
		 * closestEnemyID = game.getClosestEnemy(u); Unit enemyUnit =
		 * game.getUnit(closestEnemyID); if (closestEnemyID != -1) { if
		 * (u.getGroundWeaponCooldown() > 0 || u.getAirWeaponCooldown() > 0) {
		 * // Attack is on cooldown - retreat Point destPoint =
		 * retreat(u.getX(), u.getY(), 64); game.drawText(u.getX(), u.getY(),
		 * "Retreating", false); game.drawLine(u.getX(), u.getY(), destPoint.x,
		 * destPoint.y, BWColor.GREEN, false); game.move(u.getID(), destPoint.x,
		 * destPoint.y); } else if (Point.distance(u.getX(), u.getY(),
		 * enemyUnit.getX(), enemyUnit.getY()) <= game .getWeaponType(
		 * game.getUnitType(u.getTypeID()) .getAirWeaponID()).getMaxRange() +
		 * 32) { // Attack game.drawText(u.getX(), u.getY(), "Attacking",
		 * false); game.drawLine(u.getX(), u.getY(), enemyUnit.getX(),
		 * enemyUnit.getY(), BWColor.RED, false); game.attack(u.getID(),
		 * enemyUnit.getID());
		 * 
		 * // Retreat immediately after attack? Point destPoint =
		 * retreat(u.getX(), u.getY(), 64); game.drawText(u.getX(), u.getY(),
		 * "Retreating", false); game.drawLine(u.getX(), u.getY(), destPoint.x,
		 * destPoint.y, BWColor.GREEN, false); game.move(u.getID(), destPoint.x,
		 * destPoint.y); } else { // Move in on an attack run
		 * game.drawText(u.getX(), u.getY(), "Attack Run", false);
		 * game.drawLine(u.getX(), u.getY(), enemyUnit.getX(), enemyUnit.getY(),
		 * BWColor.YELLOW, false); game.move(u.getID(), enemyUnit.getX(),
		 * enemyUnit.getY()); } } else { // Idle game.drawText(u.getX(),
		 * u.getY(), "No target", false); } } }
		 */
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
			if (unitsUnderConstruction.containsKey(unitID)) {
				unitsUnderConstruction.remove(unitID);
				game.sendText("Unit under construction destroyed!");
			}

			// Remove workers from the baseManager
			baseManager.removeWorker(unitID);

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

	// private double[][] threatMap;
	//
	// private void drawThreatMap() {
	// // Actually draw
	// for (int x = 1; x < mapWidth; x++) {
	// for (int y = 1; y < mapHeight; y++) {
	// game.drawCircle(x * 32, y * 32,
	// (int) Math.round(threatMap[x][y]), BWColor.RED, false,
	// false);
	// }
	// }
	// }
}
