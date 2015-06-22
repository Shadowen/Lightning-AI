package botstate;

import gamestructure.GameHandler;

import java.awt.Point;
import java.util.List;
import java.util.Optional;

import static datastructure.BuildManager.buildManager;
import bwapi.PositionOrUnit;
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
		if (buildManager().unitQueue.size() == 0) {
			buildManager().addToQueue(UnitType.Terran_Marine);
		}

		if (GameHandler.getSelfPlayer().minerals() > 200
				&& buildManager().getMyUnitCount(UnitType.Terran_Barracks)
						+ buildManager().getCountInQueue(
								UnitType.Terran_Barracks) < 4) {
			// Add more barracks
			buildManager().addToQueue(UnitType.Terran_Barracks);
		}

		// Attack
		frameCount++;
		if (frameCount % 5 == 0) {
			for (Unit u : GameHandler.getAllUnits()) { // TODO only my units
				if (u.getType() == UnitType.Terran_Marine) {
					Optional<Unit> enemyUnit = GameHandler.getClosestEnemy(u);
					if (enemyUnit.isPresent()
							&& Point.distance(u.getX(), u.getY(), enemyUnit
									.get().getX(), enemyUnit.get().getY()) < 500) {
						// Attack
						u.attack(new PositionOrUnit(enemyUnit.get()));
					} else if (buildManager().getMyUnitCount(
							UnitType.Terran_Marine) > armySize) {
						{
							// Scout all bases
							List<BaseLocation> baseLocations = BWTA
									.getBaseLocations();
							BaseLocation baseLoc = baseLocations
									.get(enemyLocation);
							int x = baseLoc.getX();
							int y = baseLoc.getY();
							u.move(baseLoc.getPosition());

							if (GameHandler.isVisible(x / 32, y / 32)) {
								Optional<Unit> closestEnemy = GameHandler
										.getClosestEnemyUnit(x, y);
								if (!closestEnemy.isPresent()
										|| Point.distance(x, y, closestEnemy
												.get().getX(), closestEnemy
												.get().getY()) > 100) {
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
