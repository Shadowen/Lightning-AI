package micromanager;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import pathfinder.Node;
import pathfinder.PathingManager;
import datastructure.Base;
import datastructure.BaseManager;
import datastructure.BaseStatus;
import datastructure.BuildingPlan;
import datastructure.GasResource;
import datastructure.Resource;
import datastructure.Worker;
import datastructure.WorkerTask;
import bwapi.Color;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;
import bwapi.WeaponType;
import bwta.BWTA;
import gamestructure.DebugEngine;
import gamestructure.DebugModule;
import gamestructure.Debuggable;
import gamestructure.GameHandler;
import gamestructure.ShapeOverflowException;

public class MicroManager implements Debuggable {

	private GameHandler game;
	private BaseManager baseManager;
	private PathingManager pathingManager;

	private int mapWidth;
	private int mapHeight;
	private double[][] targetMap;
	private double[][] threatMap;
	private static final long THREAT_MAP_REFRESH_DELAY = 1000;

	private Base scoutingTarget;
	private Queue<Point> scoutPath;
	private Unit scoutingUnit;

	private HashMap<UnitType, HashMap<Integer, UnitAgent>> units;

	public MicroManager(GameHandler igame, BaseManager ibaseManager,
			PathingManager ipathingManager, DebugEngine debugEngine) {
		game = igame;
		baseManager = ibaseManager;
		pathingManager = ipathingManager;

		mapWidth = game.getMapWidth();
		mapHeight = game.getMapHeight();
		targetMap = new double[mapWidth + 1][mapHeight + 1];
		threatMap = new double[mapWidth + 1][mapHeight + 1];

		units = new HashMap<UnitType, HashMap<Integer, UnitAgent>>();

		scoutPath = new ArrayDeque<Point>();

		// Update threat map
		new Timer().scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				// Reset target and threat counter
				for (int x = 0; x < mapWidth; x++) {
					for (int y = 0; y < mapHeight; y++) {
						targetMap[x][y] = 0;
						threatMap[x][y] = 0;
					}
				}

				// Loop through enemy units
				for (Unit u : game.getAllUnits()) { // TODO
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

		}, 0, THREAT_MAP_REFRESH_DELAY);

		registerDebugFunctions(debugEngine);
	}

	public void act() {
		// Move scouting unit(s)
		scout();
	}

	private void scout() {
		// If I have no scouting unit assigned, don't scout
		if (scoutingUnit == null) {
			return;
		}

		// Acquire a target if necessary
		if (scoutingTarget == null
				|| game.isVisible(scoutingTarget.getX() / 32,
						scoutingTarget.getY() / 32)) {
			scoutingTarget = getScoutingTarget();
			// Clear previous path
			scoutPath.clear();
		}

		try {
			// Issue commands as appropriate
			if (scoutingUnit != null && scoutingTarget != null) {
				if (scoutPath.size() < 15) {
					// Path planned is short
					scoutPath = pathingManager.findGroundPath(
							scoutingUnit.getX(), scoutingUnit.getY(),
							scoutingTarget.getX(), scoutingTarget.getY(),
							scoutingUnit.getType(), 256);
				}

				Point moveTarget;
				double distanceToCheckPoint;
				while (true) {
					moveTarget = scoutPath.element();
					distanceToCheckPoint = Point.distance(scoutingUnit.getX(),
							scoutingUnit.getY(), moveTarget.x, moveTarget.y);

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
				scoutingUnit.move(new Position(moveTarget.x, moveTarget.y));
			}
		} catch (NullPointerException e) {
			System.out.println("Pathfinder failed to find a path...");
		}
	}

	private Base getScoutingTarget() {
		Base target = null;
		for (Base b : baseManager) {
			// Scout all mains
			if (b.getLocation().isStartLocation()) {
				if (b.getPlayer() == game.getNeutralPlayer()
						&& (target == null || b.getLastScouted() < target
								.getLastScouted())) {
					target = b;
				}
			}

		}

		// If there is still no target
		if (target == null) {
			for (Base b : baseManager) {
				// Scout all expos
				if (b.getPlayer() == game.getNeutralPlayer()
						&& (target == null || b.getLastScouted() < target
								.getLastScouted())) {
					target = b;
				}
			}
		}
		return target;
	}

	public void setScoutingUnit(Unit unit) {
		Worker w = baseManager.getWorker(scoutingUnit);
		if (w != null) {
			w.setBase(baseManager.main);
		}
		scoutingUnit = unit;
	}

	public boolean isScouting() {
		return scoutingUnit != null;
	}

	public void unitCreate(Unit unit) {
	}

	public void unitDestroyed(Unit unit) {
		Iterator<Entry<UnitType, HashMap<Integer, UnitAgent>>> i = units
				.entrySet().iterator();
		while (i.hasNext()) {
			i.next().getValue().remove(unit);
		}

		if (unit == scoutingUnit) {
			scoutingUnit = null;
			scoutPath = new ArrayDeque<Point>();
		}
	}

	@Override
	public void registerDebugFunctions(DebugEngine debugEngine) {
		// Threat map
		debugEngine.registerDebugModule(new DebugModule("threats") {
			@Override
			public void draw(DebugEngine engine) throws ShapeOverflowException {
				// Actually draw
				for (int x = 1; x < mapWidth; x++) {
					for (int y = 1; y < mapHeight; y++) {
						engine.drawCircleMap(x * 32, y * 32,
								(int) Math.round(threatMap[x][y]), Color.Red,
								false);
					}
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
		// debugEngine.registerDebugFunction(new DebugModule("cooldowns") {
		// @Override
		// public void draw(DebugEngine engine) throws ShapeOverflowException {
		// for (Entry<UnitType, HashMap<Integer, UnitAgent>> unitTypeMap : units
		// .entrySet()) {
		// for (Entry<Integer, UnitAgent> entry : unitTypeMap
		// .getValue().entrySet()) {
		// UnitAgent ua = entry.getValue();
		// Unit u = ua.unit;
		// UnitType unitType = u.getType();
		// if (unitType.groundWeapon().targetsGround() ||
		// unitType.airWeapon().targetsAir() {
		// int cooldownBarSize = 20;
		// int cooldownRemaining = u.getGroundWeaponCooldown();
		// int maxCooldown =
		// unitType.groundWeapon(); // TODO
		// engine.drawLine(u.getX(), u.getY(), u.getX()
		// + cooldownBarSize, u.getY(), Color.Green,
		// false);
		// engine.drawLine(u.getX(), u.getY(), u.getX()
		// + cooldownRemaining * cooldownBarSize
		// / maxCooldown, u.getY(), Color.Red, false);
		// }
		// }
		// }
		// }
		// });
		// // Scouting Target
		// debugEngine.registerDebugFunction(new DebugModule("scouting") {
		// @Override
		// public void draw(DebugEngine engine) throws ShapeOverflowException {
		// if (scoutingTarget != null) {
		// int x = scoutingTarget.getX();
		// int y = scoutingTarget.getY();
		// engine.drawLine(x - 10, y - 10, x + 10, y + 10,
		// BWColor.RED, false);
		// engine.drawLine(x + 10, y - 10, x - 10, y + 10,
		// BWColor.RED, false);
		// }
		//
		// for (Point p : scoutPath) {
		// engine.drawBox(p.x - 4, p.y - 4, p.x + 4, p.y + 4,
		// BWColor.YELLOW, false, false);
		// }
		// }
		// });
	}
}
