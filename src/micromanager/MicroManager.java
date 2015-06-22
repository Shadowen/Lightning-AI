package micromanager;

import gamestructure.GameHandler;
import gamestructure.debug.DebugManager;
import gamestructure.debug.DrawEngine;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;

import pathfinder.PathingManager;
import bwapi.Color;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;
import datastructure.Base;
import datastructure.BaseManager;
import datastructure.Worker;

public final class MicroManager {
	private static int mapWidth;
	private static int mapHeight;
	private static double[][] targetMap;
	private static double[][] threatMap;

	private static Optional<Base> scoutingTarget;
	private static Queue<Point> scoutPath;
	private static Optional<Unit> scoutingUnit;

	private static HashMap<UnitType, HashMap<Unit, UnitAgent>> units;

	public static void init() {
		System.out.print("Starting MicroManager... ");
		mapWidth = GameHandler.getMapWidth();
		mapHeight = GameHandler.getMapHeight();
		targetMap = new double[mapWidth + 1][mapHeight + 1];
		threatMap = new double[mapWidth + 1][mapHeight + 1];

		units = new HashMap<UnitType, HashMap<Unit, UnitAgent>>();

		scoutPath = new ArrayDeque<Point>();

		registerDebugFunctions();
		System.out.println("Success!");
	}

	/** This constructor should never be called. */
	private MicroManager() {
	}

	public static void act() {
		updateMap();
		// Move attacking units
		micro();
		// Move scouting unit(s)
		scout();
	}

	private static void updateMap() {
		// Update threat map
		// Reset target and threat counter
		for (int x = 0; x < mapWidth; x++) {
			for (int y = 0; y < mapHeight; y++) {
				targetMap[x][y] = 0;
				threatMap[x][y] = 0;
			}
		}

		// Loop through enemy units
		for (Unit u : GameHandler.getEnemyUnits()) {
			UnitType unitType = u.getType();
			// Get the x and y grid point coordinates
			int x = u.getX() / 32;
			int y = u.getY() / 32;

			// Update target map
			double targetValue = 0;
			if (unitType.isWorker()) {
				targetValue = 1;
			}
			int radius = 10;
			int startX = Math.max(x - radius, 0);
			int endX = Math.min(x + radius, mapWidth);
			for (int cx = startX; cx < endX; cx++) {
				int remainingRadius = radius - Math.abs(cx - x);
				int startY = Math.max(y - remainingRadius, 0);
				int endY = Math.min(y + remainingRadius, mapHeight);
				for (int cy = startY; cy < endY; cy++) {
					targetMap[cx][cy] += targetValue
							/ (Point.distance(x, y, cx, cy) + 1);
				}
			}

			// Update threat map
			double threatValue = 1;
			radius = u.getType().airWeapon().maxRange() / 32;
			startX = Math.max(x - radius, 0);
			endX = Math.min(x + radius, mapWidth);
			for (int cx = startX; cx < endX; cx++) {
				int remainingRadius = radius - Math.abs(cx - x);
				int startY = Math.max(y - remainingRadius, 0);
				int endY = Math.min(y + remainingRadius, mapHeight);
				for (int cy = startY; cy < endY; cy++) {
					threatMap[cx][cy] += threatValue * radius
							/ (Point.distance(x, y, cx, cy) + 1);
				}
			}
		}
	}

	private static void micro() {
		Point[][] movementMap = new Point[mapWidth + 1][mapHeight + 1];
		for (int x = 0; x < mapWidth; x++) {
			for (int y = 0; y < mapHeight; y++) {
				movementMap[x][y] = new Point(
						(int) (threatMap[x + 1][y] - threatMap[x - 1][y]),
						(int) (threatMap[x][y + 1] - threatMap[x][y - 1]));
			}
		}
		for (UnitAgent ua : units.get(UnitType.Terran_Wraith).values()) {
			Unit u = ua.unit;
			int x = u.getX();
			int y = u.getY();
			u.move(new Position(x + movementMap[x][y].x, y
					+ movementMap[x][y].y));
		}
	}

	private static void scout() {
		// If I have no scouting unit assigned, don't scout
		if (!scoutingUnit.isPresent()) {
			return;
		}

		// Acquire a target if necessary
		if (!scoutingTarget.isPresent()
				|| GameHandler.isVisible(scoutingTarget.get().getX() / 32,
						scoutingTarget.get().getY() / 32)) {
			scoutingTarget = getScoutingTarget();
			// Clear previous path
			scoutPath.clear();
		}

		try {
			// Issue commands as appropriate
			if (scoutingUnit.isPresent() && scoutingTarget.isPresent()) {
				if (scoutPath.size() < 15) {
					// Path planned is short
					scoutPath = PathingManager.findGroundPath(scoutingUnit
							.get().getX(), scoutingUnit.get().getY(),
							scoutingTarget.get().getX(), scoutingTarget.get()
									.getY(), scoutingUnit.get().getType(), 256);
				}

				Point moveTarget;
				double distanceToCheckPoint;
				while (true) {
					moveTarget = scoutPath.element();
					distanceToCheckPoint = Point.distance(scoutingUnit.get()
							.getX(), scoutingUnit.get().getY(), moveTarget.x,
							moveTarget.y);

					if (distanceToCheckPoint > 128) {
						// Recalculate a path
						scoutPath.clear();
						return;
					} else if (distanceToCheckPoint > 64) {
						// Keep following existing path
						break;
					} else {
						// Checkpoint
						scoutPath.remove();
					}
				}

				// Issue a movement command
				scoutingUnit.get().move(
						new Position(moveTarget.x, moveTarget.y));
			}
		} catch (NullPointerException e) {
			System.out.println("Pathfinder failed to find a path...");
		}
	}

	private static Optional<Base> getScoutingTarget() {
		Optional<Base> target = Optional.empty();
		for (Base b : BaseManager.getBases()) {
			// Scout all mains
			if (b.getLocation().isStartLocation()) {
				if (b.getPlayer() == GameHandler.getNeutralPlayer()
						&& (!target.isPresent() || b.getLastScouted() < target
								.get().getLastScouted())) {
					target = Optional.of(b);
				}
			}
		}
		if (target.isPresent()) {
			return target;
		}

		// If there is still no target
		for (Base b : BaseManager.getBases()) {
			// Scout all expos
			if (b.getPlayer() == GameHandler.getNeutralPlayer()
					&& (!target.isPresent() || b.getLastScouted() < target
							.get().getLastScouted())) {
				target = Optional.of(b);
			}
		}
		return target;
	}

	public static void setScoutingUnit(Unit unit) {
		Optional<Worker> ow = BaseManager.getWorker(scoutingUnit.get());
		ow.ifPresent(w -> w.setBase(BaseManager.main));
		scoutingUnit = Optional.of(unit);
	}

	public static boolean isScouting() {
		return scoutingUnit.isPresent();
	}

	public static void unitCreate(Unit unit) {
	}

	public static void unitDestroyed(Unit unit) {
		units.get(unit.getType()).remove(unit);
		if (scoutingUnit.filter(su -> su.equals(unit)).isPresent()) {
			scoutingUnit = Optional.empty();
			scoutPath = new ArrayDeque<Point>();
		}
	}

	public static void registerDebugFunctions() {
		// Threat map
		DebugManager.createDebugModule("threats").setDraw(() -> {
			// Actually draw
				for (int x = 1; x < mapWidth; x++) {
					for (int y = 1; y < mapHeight; y++) {
						DrawEngine.drawCircleMap(x * 32, y * 32,
								(int) Math.round(threatMap[x][y]), Color.Red,
								false);
					}
				}
			});
		// Movement map
		DebugManager.createDebugModule("movement").setDraw(() -> {
			// Actually draw
				for (int x = 1; x < mapWidth; x++) {
					for (int y = 1; y < mapHeight; y++) {
						DrawEngine.drawArrowMap(x, y, x, y, Color.Green);
					}
				}
			});
		// Target map
		// debugEngine.registerDebugFunction(new DebugModule("targets") {
		// @Override
		// public void draw(DebugEngine engine) throws ShapeOverflowException{
		// // Actually draw
		// for (int tx = 1; tx < mapWidth - 1; tx++) {
		// int x = tx * 32;
		// for (int ty = 1; ty < mapHeight - 1; ty++) {
		// int y = ty * 32;
		//
		// int north = (int) Math.round(targetMap[tx][ty - 1]);
		// int east = (int) Math.round(targetMap[tx + 1][ty]);
		// int south = (int) Math.round(targetMap[tx][ty - 1]);
		// int west = (int) Math.round(targetMap[tx - 1][ty]);
		// engine.drawArrow(x, y, x + (east - west) * 5, y
		// + (north - south) * 5, BWColor.TEAL, false);
		// }
		// }
		// }
		// });
		// Weapon cooldown bars
		DebugManager
				.createDebugModule("cooldowns")
				.setDraw(
						() -> {
							for (Entry<UnitType, HashMap<Unit, UnitAgent>> unitTypeMap : units
									.entrySet()) {
								for (Entry<Unit, UnitAgent> entry : unitTypeMap
										.getValue().entrySet()) {
									UnitAgent ua = entry.getValue();
									Unit u = ua.unit;
									UnitType unitType = u.getType();
									int cooldownBarSize = 20;
									int cooldownRemaining = u
											.getGroundWeaponCooldown();
									int maxCooldown = unitType.groundWeapon()
											.damageCooldown();
									DrawEngine.drawLineMap(u.getX(), u.getY(),
											u.getX() + cooldownBarSize,
											u.getY(), Color.Green);
									DrawEngine.drawLineMap(u.getX(), u.getY(),
											u.getX() + cooldownRemaining
													* cooldownBarSize
													/ maxCooldown, u.getY(),
											Color.Red);
								}
							}
						});
		// Scouting Target
		DebugManager.createDebugModule("scouting").setDraw(
				() -> {
					if (scoutingTarget.isPresent()) {
						int x = scoutingTarget.get().getX();
						int y = scoutingTarget.get().getY();
						DrawEngine.drawLineMap(x - 10, y - 10, x + 10, y + 10,
								Color.Red);
						DrawEngine.drawLineMap(x + 10, y - 10, x - 10, y + 10,
								Color.Red);
					}
					for (Point p : scoutPath) {
						DrawEngine.drawBoxMap(p.x - 4, p.y - 4, p.x + 4,
								p.y + 4, Color.Yellow, false);
					}
				});
	}
}
