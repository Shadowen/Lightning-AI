package javabot.gamestructure;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import javabot.BWAPIEventListener;
import javabot.botstate.BotState;
import javabot.botstate.FirstFrameState;
import javabot.datastructure.Base;
import javabot.datastructure.BuildingPlan;
import javabot.datastructure.Resource;
import javabot.model.*;
import javabot.types.*;
import javabot.types.OrderType.OrderTypeTypes;
import javabot.types.UnitType.UnitTypes;
import javabot.util.BWColor;

public class JavaBot implements BWAPIEventListener {

	private GameHandler game;
	private Hashtable<Integer, Unit> unitsUnderConstruction;

	private BotState botState;

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

	private int mapWidth;
	private int mapHeight;

	// Method called at the beginning of the game.
	public void gameStarted() {
		System.out.println("Game Started");

		// allow me to manually control units during the game
		game.enableUserInput();

		// analyze the map
		game.loadMapData(true);

		// ============== YOUR CODE GOES HERE =======================

		// This is called at the beginning of the game. You can
		// initialize some data structures (or do something similar)
		// if needed. For example, you should maintain a memory of seen
		// enemy buildings.

		game.printText("This map is called " + game.getMap().getName());
		game.printText("Enemy race ID: "
				+ String.valueOf(game.getEnemies().get(0).getRaceID())); // Z=0,T=1,P=2

		game.drawTargets(true);

		// ==========================================================
		// Initialize
		unitsUnderConstruction = new Hashtable<Integer, Unit>();

		botState = new FirstFrameState(game);
		botState.registerDebugFunctions(game);

		mapWidth = game.getMap().getWidth();
		mapHeight = game.getMap().getHeight();
		threatMap = new double[mapHeight][mapWidth];

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
		String uucString = "";
		for (Entry<Integer, Unit> u : unitsUnderConstruction.entrySet()) {
			uucString += game.getUnitType(u.getValue().getTypeID()).getName()
					+ ", ";
			if (u.getValue().isCompleted()) {
				game.sendText("Unit completed: "
						+ game.getUnitType(u.getValue().getTypeID()).getName());
				unitsUnderConstruction.remove(u.getKey());
				unitComplete(u.getKey());
			}
		}
		game.drawText(5, 60, "unitsUnderConstruction: " + uucString, true);
		game.drawText(1000, 5, "Supply: " + game.getSelf().getSupplyUsed()
				+ "/" + game.getSelf().getSupplyTotal(), true);

		// Allow the bot to act
		try {
			botState = botState.act();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// Draw debug information on screen
		game.drawDebug();

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
			botState = botState.unitComplete(unitID);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void unitDestroy(int unitID) {
		try {
			if (unitsUnderConstruction.containsKey(unitID)) {
				unitsUnderConstruction.remove(unitID);
				game.sendText("Unit under construction destroyed!");
			}
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
	}

	public void unitShow(int unitID) {
	}

	public void keyPressed(int keyCode) {
	}

	private double[][] threatMap;

	private void drawThreatMap() {
		// Actually draw
		for (int x = 1; x < mapWidth; x++) {
			for (int y = 1; y < mapHeight; y++) {
				game.drawCircle(x * 32, y * 32,
						(int) Math.round(threatMap[x][y]), BWColor.RED, false,
						false);
			}
		}
	}
}
