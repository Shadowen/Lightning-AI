package eaglesWings.micromanager;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import javabot.model.Unit;
import javabot.types.UnitType.UnitTypes;
import javabot.util.BWColor;
import eaglesWings.gamestructure.DebugEngine;
import eaglesWings.gamestructure.DebugModule;
import eaglesWings.gamestructure.Debuggable;
import eaglesWings.gamestructure.GameHandler;

public class MicroManager implements Debuggable {
	GameHandler game;

	private int mapWidth;
	private int mapHeight;
	private double[][] threatMap;

	private HashMap<Integer, UnitAgent> marines;

	public MicroManager(GameHandler igame) {
		game = igame;

		mapWidth = game.getMap().getWidth();
		mapHeight = game.getMap().getHeight();
		threatMap = new double[mapWidth][mapHeight];

		marines = new HashMap<Integer, UnitAgent>();

		new Timer().scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				// Reset threat counter
				for (int x = 0; x < mapWidth; x++) {
					for (int y = 0; y < mapHeight; y++) {
						threatMap[x][y] = 0;
					}
				}

				// Count the threats
				for (Unit u : game.getEnemyUnits()) {
					// Get the x and y grid point coordinates
					int x = u.getX() / 32;
					int y = u.getY() / 32;
					// Get the ground weapon's range
					double radius = game
							.getWeaponType(
									game.getUnitType(u.getTypeID())
											.getGroundWeaponID()).getMaxRange() / 32 + 2;
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

	public void micro() {
		for (Entry<Integer, UnitAgent> entry : marines.entrySet()) {
			UnitAgent ua = entry.getValue();
			Unit myUnit = ua.unit;
			Unit enemyUnit = game.getClosestEnemy(myUnit);

			// Get the unit to move if there's a target
			if (enemyUnit == null) {
				ua.task = UnitTask.IDLE;
				break;
			} else if (ua.task == UnitTask.IDLE) {
				ua.task = UnitTask.ATTACK_RUN;
			}

			// Calculate some values
			int maxCooldown = game.getWeaponType(
					game.getUnitType(myUnit.getTypeID()).getAirWeaponID())
					.getDamageCooldown();
			int range = game.getWeaponType(
					game.getUnitType(myUnit.getTypeID()).getGroundWeaponID())
					.getMaxRange();
			int enemyRange = game
					.getWeaponType(
							game.getUnitType(enemyUnit.getTypeID())
									.getGroundWeaponID()).getMaxRange();

			// FSM
			if (ua.task == UnitTask.ATTACK_RUN) {
				// Move in on an attack run
				game.drawText(myUnit.getX(), myUnit.getY(), "Attack Run", false);
				game.drawLine(myUnit.getX(), myUnit.getY(), enemyUnit.getX(),
						enemyUnit.getY(), BWColor.YELLOW, false);
				game.move(myUnit.getID(), enemyUnit.getX(), enemyUnit.getY());
				// Fire when in range
				if (Point.distance(myUnit.getX(), myUnit.getY(),
						enemyUnit.getX(), enemyUnit.getY()) <= range) {
					ua.task = UnitTask.FIRING;
				}
			} else if (ua.task == UnitTask.FIRING) {
				// Attack
				game.drawText(myUnit.getX(), myUnit.getY(), "Attacking", false);
				game.drawLine(myUnit.getX(), myUnit.getY(), enemyUnit.getX(),
						enemyUnit.getY(), BWColor.RED, false);
				game.attack(myUnit.getID(), enemyUnit.getID());
				// Trigger animation lock
				ua.task = UnitTask.ANIMATION_LOCK;
				ua.timeout = 5;
			} else if (ua.task == UnitTask.ANIMATION_LOCK) {
				// Can leave animation lock
				game.drawText(myUnit.getX(), myUnit.getY(), "Animation Lock ("
						+ ua.timeout + ")", false);
				if (ua.timeout <= 0) {
					// Keep attacking
					if (range > enemyRange) {
						// Should kite
						ua.timeout = maxCooldown + 10;
						ua.task = UnitTask.RETREATING;
					} else {
						// Just fire all day!
						ua.task = UnitTask.FIRING;
					}
				}
			} else if (ua.task == UnitTask.RETREATING) {
				// Attack is on cooldown - retreat
				Point destPoint = retreat(myUnit.getX(), myUnit.getY(), 64);
				game.drawText(myUnit.getX(), myUnit.getY(), "Retreating", false);
				game.drawLine(myUnit.getX(), myUnit.getY(), destPoint.x,
						destPoint.y, BWColor.GREEN, false);
				game.move(myUnit.getID(), destPoint.x, destPoint.y);
				// Switch to attack run when ready
				if (ua.timeout <= 0) {
					ua.task = UnitTask.ATTACK_RUN;
				}
			}

			// Reduce the timeout every frame
			ua.timeout -= 1;
		}
	}

	private Point retreat(int x, int y, int distance) {
		// Grid coordinates of x and y
		int gx = (int) Math.round(x / 32);
		int gy = (int) Math.round(y / 32);
		if (distance <= 0 || gx <= 0 || gy <= 0 || gx >= mapWidth
				|| gy >= mapHeight) {
			return new Point(x, y);
		}

		Point bestRetreat = new Point();

		double minValue = Double.MAX_VALUE;
		double threatMapValue = threatMap[gx + 1][gy + 1];
		if (threatMapValue <= minValue) {
			bestRetreat.x = x + 32;
			bestRetreat.y = y + 32;
			minValue = threatMapValue;
		}
		threatMapValue = threatMap[gx + 1][gy - 1];
		if (threatMapValue <= minValue) {
			bestRetreat.x = x + 32;
			bestRetreat.y = y - 32;
			minValue = threatMapValue;
		}
		threatMapValue = threatMap[gx - 1][gy + 1];
		if (threatMapValue <= minValue) {
			bestRetreat.x = x - 32;
			bestRetreat.y = y + 32;
			minValue = threatMapValue;
		}
		threatMapValue = threatMap[gx - 1][gy - 1];
		if (threatMapValue <= minValue) {
			bestRetreat.x = x - 32;
			bestRetreat.y = y - 32;
			minValue = threatMapValue;
		}

		return retreat(bestRetreat.x, bestRetreat.y, distance - 32);
	}

	@Override
	public void registerDebugFunctions(GameHandler g) {
		// Threat map
		g.registerDebugFunction(new DebugModule() {
			@Override
			public void draw(DebugEngine engine) {
				// Actually draw
				for (int x = 1; x < mapWidth; x++) {
					for (int y = 1; y < mapHeight; y++) {
						game.drawCircle(x * 32, y * 32,
								(int) Math.round(threatMap[x][y]), BWColor.RED,
								false, false);
					}
				}
			}
		});
		// Weapon cooldown bars
		g.registerDebugFunction(new DebugModule() {
			@Override
			public void draw(DebugEngine engine) {
				for (Entry<Integer, UnitAgent> entry : marines.entrySet()) {
					UnitAgent ua = entry.getValue();
					Unit u = ua.unit;
					int cooldownBarSize = 20;
					int cooldownRemaining = u.getGroundWeaponCooldown();
					int maxCooldown = game.getWeaponType(
							game.getUnitType(u.getTypeID()).getAirWeaponID())
							.getDamageCooldown();
					game.drawLine(u.getX(), u.getY(), u.getX()
							+ cooldownBarSize, u.getY(), BWColor.GREEN, false);
					game.drawLine(u.getX(), u.getY(),
							u.getX() + cooldownRemaining * cooldownBarSize
									/ maxCooldown, u.getY(), BWColor.RED, false);
				}
			}
		});
	}

	public void unitCreate(int unitID) {
		Unit unit = game.getUnit(unitID);
		if (unit.getTypeID() == UnitTypes.Terran_Marine.ordinal()) {
			marines.put(unitID, new UnitAgent(game, unit));
		}
	}

	public void unitDestroy(int unitID) {
		marines.remove(unitID);
	}
}
