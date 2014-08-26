package javabot;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javabot.model.*;
import javabot.types.*;
import javabot.types.OrderType.OrderTypeTypes;
import javabot.types.UnitType.UnitTypes;
import javabot.util.BWColor;

public class JavaBot implements BWAPIEventListener {

	private JNIBWAPI bwapi;

	// Some miscelaneous variables. Feel free to add yours.
	private int gameLoopCounter = 0;
	private List<Base> bases;

	public static void main(String[] args) {
		new JavaBot();
	}

	public JavaBot() {
		bwapi = new JNIBWAPI(this);
		bwapi.start();
	}

	public void connected() {
		bwapi.loadTypeData();
	}

	private int mapWidth;
	private int mapHeight;

	// Method called at the beginning of the game.
	public void gameStarted() {
		System.out.println("Game Started");

		// allow me to manually control units during the game
		bwapi.enableUserInput();

		// set game speed to 30 (0 is the fastest. Tournament speed is 20)
		// You can also change the game speed from within the game by "/speed X"
		// command.
		bwapi.setGameSpeed(30);

		// analyze the map
		bwapi.loadMapData(true);

		// ============== YOUR CODE GOES HERE =======================

		// This is called at the beginning of the game. You can
		// initialize some data structures (or do something similar)
		// if needed. For example, you should maintain a memory of seen
		// enemy buildings.

		bwapi.printText("This map is called " + bwapi.getMap().getName());
		bwapi.printText("My race ID: "
				+ String.valueOf(bwapi.getSelf().getRaceID())); // Z=0,T=1,P=2
		bwapi.printText("Enemy race ID: "
				+ String.valueOf(bwapi.getEnemies().get(0).getRaceID())); // Z=0,T=1,P=2

		bwapi.drawTargets(true);

		// ==========================================================
		// Initialize
		mapWidth = bwapi.getMap().getWidth();
		mapHeight = bwapi.getMap().getHeight();
		threatMap = new double[mapHeight][mapWidth];

		Timer t = new Timer();

		t.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				// Reset threat counter
				for (int x = 0; x < 128; x++) {
					for (int y = 0; y < 128; y++) {
						threatMap[x][y] = 0;
					}
				}

				// Count the threats
				for (Unit u : bwapi.getEnemyUnits()) {
					// Get the x and y grid point coordinates
					int x = u.getX() / 32;
					int y = u.getY() / 32;
					// Get the air weapon's range
					double radius = bwapi.getWeaponType(
							bwapi.getUnitType(u.getTypeID()).getAirWeaponID())
							.getMaxRange() / 32 + 2;
					double threat = 1;
					ArrayList<Point> threatPoints = generateCircleCoordinates(
							x, y, radius);
					for (Point p : threatPoints) {
						threatMap[p.x][p.y] += threat
								* (radius - p.distance(x, y)) / radius;
					}
				}
			}

			private ArrayList<Point> generateCircleCoordinates(int cx, int cy,
					double r) {
				ArrayList<Point> points = new ArrayList<Point>();
				for (int x = (int) Math.floor(-r); x < r; x++) {
					int y1 = (int) Math.round(Math.sqrt(Math.pow(r, 2)
							- Math.pow(x, 2)));
					int y2 = -y1;
					for (int y = y2; y < y1; y++) {
						if (x + cx > 0 && x + cx < mapWidth && y + cy > 0
								&& y + cy < mapHeight) {
							points.add(new Point(x + cx, y + cy));
						}
					}
				}
				return points;
			}

		}, 1000, 1000);
	}

	public int getClosestEnemy(Unit toWho) {
		double closestDistance = Double.MAX_VALUE;
		Unit closestUnit = null;
		for (Unit u : bwapi.getEnemyUnits()) {
			double distanceX = toWho.getX() - u.getX();
			double distanceY = toWho.getY() - u.getY();
			double distance = Math.sqrt(Math.pow(distanceX, 2)
					+ Math.pow(distanceY, 2));

			if (distance < closestDistance) {
				closestUnit = u;
				closestDistance = distance;
			}
		}
		if (closestUnit != null) {
			return closestUnit.getID();
		}
		return -1;
	}

	public Point getUnitVector(Point start, Point dest) {
		double distance = 1;
		return new Point((int) ((dest.x - start.x) / distance * 1000),
				(int) ((dest.y - start.y) / distance * 1000));
	}

	private Point retreat(int x, int y, int distance) {
		if (distance <= 0) {
			return new Point(x, y);
		}

		Point bestRetreat = new Point();

		// Grid coordinates of x and y
		int gx = (int) Math.round(x / 32);
		int gy = (int) Math.round(y / 32);

		double minValue = Double.MAX_VALUE;
		double threatMapValue = threatMap[gx + 1][gy + 1];
		if (threatMapValue < minValue) {
			bestRetreat.x = x + 32;
			bestRetreat.y = y + 32;
			minValue = threatMapValue;
		}
		threatMapValue = threatMap[gx + 1][gy - 1];
		if (threatMapValue < minValue) {
			bestRetreat.x = x + 32;
			bestRetreat.y = y - 32;
			minValue = threatMapValue;
		}
		threatMapValue = threatMap[gx - 1][gy + 1];
		if (threatMapValue < minValue) {
			bestRetreat.x = x - 32;
			bestRetreat.y = y + 32;
			minValue = threatMapValue;
		}
		threatMapValue = threatMap[gx - 1][gy - 1];
		if (threatMapValue < minValue) {
			bestRetreat.x = x - 32;
			bestRetreat.y = y - 32;
			minValue = threatMapValue;
		}

		return retreat(bestRetreat.x, bestRetreat.y, distance - 32);
	}

	// Method called on every frame (approximately 30x every second).
	public void gameUpdate() {
		// Initialization that only occurs on the first frame
		if (bwapi.getFrameCount() == 1) {
			// Set up the main
			bases = new ArrayList<Base>();
			Base mainBase = new Base();
			List<Unit> units = bwapi.getMyUnits();
			for (Unit u : units) {
				if (u.getTypeID() == UnitTypes.Terran_SCV.ordinal()) {
					mainBase.workers.add(u);
				} else if (u.getTypeID() == UnitTypes.Terran_Command_Center
						.ordinal()) {
					mainBase.commandCenter = u;
				}
			}
			List<BaseLocation> baseLocations = bwapi.getMap()
					.getBaseLocations();
			for (BaseLocation location : baseLocations) {
				if (location.getX() == mainBase.commandCenter.getX()
						&& location.getY() == mainBase.commandCenter.getY()) {
					mainBase.location = location;
					break;
				}
			}
			bases.add(mainBase);
			// Send the first four workers to mine
			for (Unit u : mainBase.workers) {
				bwapi.rightClick(
						u.getID(),
						getClosestUnitOfType(u.getX(), u.getY(),
								UnitTypes.Resource_Mineral_Field).getID());
			}

			bwapi.sendText("First frame initialization complete!");
		}

		// Draw debug information on screen
		drawDebugInfo();

		// Make sure all workers at bases are mining
		for (Base b : bases) {
			for (Unit u : b.workers) {
				if (u.isIdle()) {
					bwapi.rightClick(
							u.getID(),
							getClosestUnitOfType(u.getX(), u.getY(),
									UnitTypes.Resource_Mineral_Field).getID());
				}
			}
		}

		for (Unit u : bwapi.getMyUnits()) {
			if (u.getTypeID() == UnitTypes.Terran_SCV.ordinal()) {
				// SCVs
			} else if (u.getTypeID() == UnitTypes.Terran_Wraith.ordinal()) {
				// Wraith micro
				int closestEnemyID = getClosestEnemy(u);
				Unit enemyUnit = bwapi.getUnit(closestEnemyID);
				if (closestEnemyID != -1) {
					if (u.getGroundWeaponCooldown() > 0
							|| u.getAirWeaponCooldown() > 0) {
						// Attack is on cooldown - retreat
						Point destPoint = retreat(u.getX(), u.getY(), 64);
						bwapi.drawText(u.getX(), u.getY(), "Retreating", false);
						bwapi.drawLine(u.getX(), u.getY(), destPoint.x,
								destPoint.y, BWColor.GREEN, false);
						bwapi.move(u.getID(), destPoint.x, destPoint.y);
					} else if (Point.distance(u.getX(), u.getY(),
							enemyUnit.getX(), enemyUnit.getY()) <= bwapi
							.getWeaponType(
									bwapi.getUnitType(u.getTypeID())
											.getAirWeaponID()).getMaxRange() + 32) {
						// Attack
						bwapi.drawText(u.getX(), u.getY(), "Attacking", false);
						bwapi.drawLine(u.getX(), u.getY(), enemyUnit.getX(),
								enemyUnit.getY(), BWColor.RED, false);
						bwapi.attack(u.getID(), enemyUnit.getID());

						// Retreat immediately after attack?
						Point destPoint = retreat(u.getX(), u.getY(), 64);
						bwapi.drawText(u.getX(), u.getY(), "Retreating", false);
						bwapi.drawLine(u.getX(), u.getY(), destPoint.x,
								destPoint.y, BWColor.GREEN, false);
						bwapi.move(u.getID(), destPoint.x, destPoint.y);
					} else {
						// Move in on an attack run
						bwapi.drawText(u.getX(), u.getY(), "Attack Run", false);
						bwapi.drawLine(u.getX(), u.getY(), enemyUnit.getX(),
								enemyUnit.getY(), BWColor.YELLOW, false);
						bwapi.move(u.getID(), enemyUnit.getX(),
								enemyUnit.getY());
					}
				} else {
					// Idle
					bwapi.drawText(u.getX(), u.getY(), "No target", false);
				}
			}
		}

		if (gameLoopCounter % 30 == 0) {
			for (Base b : bases) {
				// Train SCVS if necessary
				if (b.workers.size() < b.getMineralCount() * 2) {
					if (b.commandCenter.getTrainingQueueSize() < 1
							&& bwapi.getSelf().getMinerals() >= 50) {
						bwapi.train(b.commandCenter.getID(),
								UnitTypes.Terran_SCV.ordinal());
					}
				}
			}

			if (bwapi.getSelf().getSupplyUsed() > bwapi.getSelf()
					.getSupplyTotal() - 3) {
				Unit builder = bases.get(0).getBuilder();
				Point buildLocation = getBuildTile(builder.getID(),
						UnitTypes.Terran_Supply_Depot.ordinal(),
						builder.getX(), builder.getY());
				bwapi.build(builder.getID(), buildLocation.x, buildLocation.y,
						UnitTypes.Terran_Supply_Depot.ordinal());
			}
		}

		gameLoopCounter++;
	}

	private Unit getClosestUnitOfType(int x, int y, UnitTypes type) {
		Unit closest = null;
		double closestDistance = Double.MAX_VALUE;
		for (Unit u : bwapi.getNeutralUnits()) {
			if (u.getTypeID() == type.ordinal()) {
				double distance = Point.distance(x, y, u.getX(), u.getY());
				if (distance < closestDistance) {
					closestDistance = distance;
					closest = u;
				}
			}
		}
		return closest;
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

	private Base getClosestBase(int x, int y) {
		Base closest = null;
		double closestDistance = Double.MAX_VALUE;
		for (Base b : bases) {
			double distance = Point.distance(x, y, b.location.getX(),
					b.location.getY());
			if (distance < closestDistance) {
				closestDistance = distance;
				closest = b;
			}
		}
		return closest;
	}

	public void unitCreate(int unitID) {
		Unit u = bwapi.getUnit(unitID);
		if (u.getTypeID() == UnitTypes.Terran_SCV.ordinal()) {
			// Add new workers to nearest base
			Base base = getClosestBase(u.getX(), u.getY());
			base.workers.add(u);
		}
	}

	public void unitDestroy(int unitID) {
		Unit u = bwapi.getUnit(unitID);
		if (u.getTypeID() == UnitTypes.Terran_SCV.ordinal()) {
			for (Base b : bases) {
				if (b.workers.remove(u)) {
					break;
				}
			}
		}
	}

	public void unitDiscover(int unitID) {
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

	// Returns the Point object representing the suitable build tile position
	// for a given building type near specified pixel position (or Point(-1,-1)
	// if not found)
	// (builderID should be our worker)
	public Point getBuildTile(int builderID, int buildingTypeID, int x, int y) {
		Point ret = new Point(-1, -1);
		int maxDist = 3;
		int stopDist = 40;
		int tileX = x / 32;
		int tileY = y / 32;

		// Refinery, Assimilator, Extractor
		if (bwapi.getUnitType(buildingTypeID).isRefinery()) {
			for (Unit n : bwapi.getNeutralUnits()) {
				if ((n.getTypeID() == UnitTypes.Resource_Vespene_Geyser
						.ordinal())
						&& (Math.abs(n.getTileX() - tileX) < stopDist)
						&& (Math.abs(n.getTileY() - tileY) < stopDist)) {
					return new Point(n.getTileX(), n.getTileY());
				}
			}
		}

		while ((maxDist < stopDist) && (ret.x == -1)) {
			for (int i = tileX - maxDist; i <= tileX + maxDist; i++) {
				for (int j = tileY - maxDist; j <= tileY + maxDist; j++) {
					if (bwapi.canBuildHere(builderID, i, j, buildingTypeID,
							false)) {
						// units that are blocking the tile
						boolean unitsInWay = false;
						for (Unit u : bwapi.getAllUnits()) {
							if (u.getID() == builderID) {
								continue;
							}
							if ((Math.abs(u.getTileX() - i) < 4)
									&& (Math.abs(u.getTileY() - j) < 4)) {
								unitsInWay = true;
							}
						}
						if (!unitsInWay) {
							ret.x = i;
							ret.y = j;
							return ret;
						}
						// creep for Zerg (this may not be needed - not tested
						// yet)
						if (bwapi.getUnitType(buildingTypeID).isRequiresCreep()) {
							boolean creepMissing = false;
							for (int k = i; k <= i
									+ bwapi.getUnitType(buildingTypeID)
											.getTileWidth(); k++) {
								for (int l = j; l <= j
										+ bwapi.getUnitType(buildingTypeID)
												.getTileHeight(); l++) {
									if (!bwapi.hasCreep(k, l)) {
										creepMissing = true;
									}
									break;
								}
							}
							if (creepMissing)
								continue;
						}
						// psi power for Protoss (this seems to work out of the
						// box)
						if (bwapi.getUnitType(buildingTypeID).isRequiresPsi()) {
						}
					}
				}
			}
			maxDist += 2;
		}

		if (ret.x == -1) {
			bwapi.printText("Unable to find suitable build position for "
					+ bwapi.getUnitType(buildingTypeID).getName());
		}
		return ret;
	}

	// Draws debug information on the screen.
	// Reimplement this function however you want.
	public void drawDebugInfo() {
		drawThreatMap();

		for (Base b : bases) {
			int x = b.commandCenter.getX() - 32 * 2;
			int y = b.commandCenter.getY() - 32 * 2;
			bwapi.drawBox(x, y, x + 32 * 4, y + 32 * 4, BWColor.TEAL, false,
					false);
			// Count workers, excluding ones that are still training.
			int workerCount = 0;
			for (Unit u : b.workers) {
				if (!u.isTraining()) {
					workerCount++;
				} else {
					bwapi.sendText("Someone is training");
				}
			}
			bwapi.drawText(x + 5, y + 5, "Workers: " + workerCount, false);
		}
		for (Unit u : bwapi.getMyUnits()) {
			if (u.getTypeID() == UnitTypes.Terran_Wraith.ordinal()) {
				bwapi.drawCircle(u.getX(), u.getY(), 12, BWColor.GREEN, true,
						false);
				bwapi.drawLine(u.getX(), u.getY() + 10,
						u.getX() + u.getGroundWeaponCooldown(), u.getY() + 10,
						BWColor.RED, false);
				bwapi.drawCircle(
						u.getX(),
						u.getY(),
						bwapi.getWeaponType(
								bwapi.getUnitType(u.getTypeID())
										.getAirWeaponID()).getMaxRange(),
						BWColor.RED, false, false);
			}
		}
	}

	private double[][] threatMap;

	private void drawThreatMap() {
		// Actually draw
		for (int x = 1; x < mapWidth; x++) {
			for (int y = 1; y < mapHeight; y++) {
				bwapi.drawCircle(x * 32, y * 32,
						(int) Math.round(threatMap[x][y]), BWColor.RED, false,
						false);
			}
		}
	}
}
