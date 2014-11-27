package javabot.botstate;

import java.awt.Point;
import java.util.ArrayList;

import javabot.datastructure.Base;
import javabot.datastructure.BuildingPlan;
import javabot.model.Unit;
import javabot.types.UnitType.UnitTypes;
import javabot.util.BWColor;

public class MassMarineState extends BotState {
	int frameCount;
	private double[][] threatMap;

	public MassMarineState(BotState oldState) {
		super(oldState);
		frameCount = 0;
		threatMap = new double[game.getMap().getHeight()][game.getMap()
				.getWidth()];
	}

	@Override
	public BotState act() {
		autoEconomy();
		autoSupplies(5);
		autoBuild();
		autoTrain();

		// Build marines
		if (buildManager.getToTrain() == null) {
			buildManager.addToQueue(UnitTypes.Terran_Marine);
		}

		if (game.getSelf().getMinerals() > 200
				&& !buildManager.buildQueueContains(UnitTypes.Terran_Barracks)
				&& game.countMyUnit(UnitTypes.Terran_Barracks) < 4) {
			// Add more barracks
			buildManager.addToQueue(UnitTypes.Terran_Barracks);
		}

		// Map threats
		// Reset threat counter
		for (int x = 0; x < 128; x++) {
			for (int y = 0; y < 128; y++) {
				threatMap[x][y] = 0;
			}
		}
		// Count the threats
		for (Unit u : game.getEnemyUnits()) {
			// Get the x and y grid point coordinates
			int x = u.getX() / 32;
			int y = u.getY() / 32;
			// Get the ground weapon's range
			double radius = game.getWeaponType(
					game.getUnitType(u.getTypeID()).getGroundWeaponID())
					.getMaxRange() + 32;
			game.drawCircle(u.getX(), u.getY(), (int) radius, BWColor.BLACK,
					false, false);
			double threat = 1;
			ArrayList<Point> threatPoints = generateCircleCoordinates(x, y,
					radius);
			for (Point p : threatPoints) {
				threatMap[p.x][p.y] += threat * (radius - p.distance(x, y))
						/ radius;
			}
		}
		drawThreatMap();

		// Attack
		frameCount++;
		if (frameCount % 10 == 0) {
			for (Unit u : game.getMyUnits()) {
				if (u.getTypeID() == UnitTypes.Terran_Marine.ordinal()) {
					int closestEnemyID = game.getClosestEnemy(u);
					Unit enemyUnit = game.getUnit(closestEnemyID);
					if (closestEnemyID != -1) {
						if (u.getGroundWeaponCooldown() > 0
								|| u.getAirWeaponCooldown() > 0) {
							// Attack is on cooldown - retreat
							Point destPoint = retreat(u.getX(), u.getY(), 64);
							game.drawText(u.getX(), u.getY(), "Retreating",
									false);
							game.drawLine(u.getX(), u.getY(), destPoint.x,
									destPoint.y, BWColor.GREEN, false);
							game.move(u.getID(), destPoint.x, destPoint.y);
						} else if (Point.distance(u.getX(), u.getY(),
								enemyUnit.getX(), enemyUnit.getY()) <= game
								.getWeaponType(
										game.getUnitType(u.getTypeID())
												.getGroundWeaponID())
								.getMaxRange()) {
							// Attack
							game.drawText(u.getX(), u.getY(), "Attacking",
									false);
							game.drawLine(u.getX(), u.getY(), enemyUnit.getX(),
									enemyUnit.getY(), BWColor.RED, false);
							game.attack(u.getID(), enemyUnit.getID());

							// Retreat immediately after attack?
							// Point destPoint = retreat(u.getX(), u.getY(),
							// 64);
							// game.drawText(u.getX(), u.getY(), "Retreating",
							// false);
							// game.drawLine(u.getX(), u.getY(), destPoint.x,
							// destPoint.y, BWColor.GREEN, false);
							// game.move(u.getID(), destPoint.x, destPoint.y);
						} else {
							// Move in on an attack run
							game.drawText(u.getX(), u.getY(), "Attack Run",
									false);
							game.drawLine(u.getX(), u.getY(), enemyUnit.getX(),
									enemyUnit.getY(), BWColor.YELLOW, false);
							game.move(u.getID(), enemyUnit.getX(),
									enemyUnit.getY());
						}
					} else {
						// Idle
						game.drawText(u.getX(), u.getY(), "No target", false);
					}
				}
			}
		}
		return this;

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

	private ArrayList<Point> generateCircleCoordinates(int cx, int cy, double r) {
		ArrayList<Point> points = new ArrayList<Point>();
		for (int x = (int) Math.floor(-r); x < r; x++) {
			int y1 = (int) Math
					.round(Math.sqrt(Math.pow(r, 2) - Math.pow(x, 2)));
			int y2 = -y1;
			for (int y = y2; y < y1; y++) {
				if (x + cx > 0 && x + cx < game.getMap().getWidth()
						&& y + cy > 0 && y + cy < game.getMap().getHeight()) {
					points.add(new Point(x + cx, y + cy));
				}
			}
		}
		return points;
	}

	public BotState unitComplete(int unitID) {
		super.unitComplete(unitID);
		return this;
	}

	private void drawThreatMap() {
		// Actually draw
		for (int x = 1; x < game.getMap().getWidth(); x++) {
			for (int y = 1; y < game.getMap().getHeight(); y++) {
				game.drawCircle(x * 32, y * 32,
						(int) Math.round(threatMap[x][y]), BWColor.RED, false,
						false);
			}
		}
	}
}
