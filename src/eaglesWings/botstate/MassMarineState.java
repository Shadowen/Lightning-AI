package eaglesWings.botstate;

import java.awt.Point;
import java.util.ArrayList;

import eaglesWings.datastructure.Base;
import eaglesWings.datastructure.BuildingPlan;
import javabot.model.BaseLocation;
import javabot.model.Unit;
import javabot.types.UnitType.UnitTypes;
import javabot.util.BWColor;

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
			buildManager.addToQueue(UnitTypes.Terran_Marine);
		}

		if (game.getSelf().getMinerals() > 200
				&& buildManager.countMyUnit(UnitTypes.Terran_Barracks)
						+ buildManager.countInQueue(UnitTypes.Terran_Barracks) < 4) {
			// Add more barracks
			buildManager.addToQueue(UnitTypes.Terran_Barracks);
		}

		// Attack
		frameCount++;
		if (frameCount % 5 == 0) {
			for (Unit u : game.getMyUnits()) {
				if (u.getTypeID() == UnitTypes.Terran_Marine.ordinal()) {
					Unit enemyUnit = game.getClosestEnemy(u);
					if (enemyUnit != null
							&& Point.distance(u.getX(), u.getY(),
									enemyUnit.getX(), enemyUnit.getY()) < 500) {
						// Attack
						//game.attack(u.getID(), enemyUnit.getID());
					} else if (buildManager
							.countMyUnit(UnitTypes.Terran_Marine) > armySize) {
						{
							// Scout all bases
							ArrayList<BaseLocation> baseLocations = game
									.getMap().getBaseLocations();
							BaseLocation baseLoc = baseLocations
									.get(enemyLocation);
							int x = baseLoc.getX();
							int y = baseLoc.getY();
							game.move(u.getID(), x, y);

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
