package botstate;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import datastructure.Base;
import datastructure.BuildingPlan;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;
import bwta.BaseLocation;

public class MassMarineState extends BotState {
	int frameCount = 0;
	int enemyLocation = 3;
	private int armySize = 0;

	public MassMarineState(BotState oldState) {
		super(oldState);
	}

	@Override
	public BotState act() {
		// Build marines
		if (buildManager.unitQueue.size() == 0) {
			buildManager.addToQueue(UnitType.Terran_Marine);
		}

		if (game.getSelfPlayer().minerals() > 200
				&& buildManager.getMyUnitCount(UnitType.Terran_Barracks)
						+ buildManager
								.getCountInQueue(UnitType.Terran_Barracks) < 4) {
			// Add more barracks
			buildManager.addToQueue(UnitType.Terran_Barracks);
		}

		// Attack
		frameCount++;
		if (frameCount % 5 == 0) {
			for (Unit u : game.getAllUnits()) { // TODO only my units
				if (u.getType() == UnitType.Terran_Marine) {
					Unit enemyUnit = game.getClosestEnemy(u);
					if (enemyUnit != null
							&& Point.distance(u.getX(), u.getY(),
									enemyUnit.getX(), enemyUnit.getY()) < 500) {
						// Attack
						// game.attack(u.getID(), enemyUnit.getID());
					} else if (buildManager
							.getMyUnitCount(UnitType.Terran_Marine) > armySize) {
						{
							// Scout all bases
							List<BaseLocation> baseLocations = BWTA
									.getBaseLocations();
							BaseLocation baseLoc = baseLocations
									.get(enemyLocation);
							int x = baseLoc.getX();
							int y = baseLoc.getY();
							u.move(baseLoc.getPosition());

							if (game.isVisible(x / 32, y / 32)) {
								Unit closestEnemy = game.getClosestEnemy(x, y);
								if (closestEnemy == null
										|| Point.distance(x, y,
												closestEnemy.getX(),
												closestEnemy.getY()) > 100) {
									enemyLocation++;
								}
							}
							if (enemyLocation >= baseLocations.size()) {
								enemyLocation = 0;
								// Consider using
								// baseLocation.isStartingLocation()
							}
						}
					}
				}
			}
		}
		return this;

	}

	public BotState unitDestroyed(int unitID) {
		armySize++;
		return this;
	}
}
