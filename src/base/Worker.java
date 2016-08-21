package base;

import java.awt.Point;

import build.BuildingPlan;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;
import gamestructure.GameHandler;
import micro.GroundAgent;
import micro.UnitTask;
import pathing.NoPathFoundException;

public class Worker extends GroundAgent {
	private Resource currentResource;
	private Base base;
	private boolean hasResetGasBuild;
	private static final double VELOCITY_SCALE_FACTOR = 5;
	private static final int GAS_FREEZE_DISTANCE = 10;

	public Worker(Unit u) {
		super(u);
	}

	public void move(int x, int y) {
		unit.move(new Position(x, y));
	}

	public void build(BuildingPlan toBuild) {
		toBuild.setBuilder(this);
		task = UnitTask.CONSTRUCTING;
	}

	protected void beforeTaskChange() {
		super.beforeTaskChange();

		if (currentResource != null) {
			currentResource.removeGatherer(this);
			currentResource = null;
		}
	}

	public void setTaskMiningMinerals(MineralResource newResource) {
		beforeTaskChange();

		task = UnitTask.MINERALS;
		newResource.addGatherer(this);
		currentResource = newResource;
	}

	public void setTaskMiningMinerals() {
		beforeTaskChange();
		task = UnitTask.MINERALS;
	}

	public void setTaskMiningGas(GasResource newResource) {
		beforeTaskChange();

		task = UnitTask.GAS;
		newResource.addGatherer(this);
		currentResource = newResource;
	}

	public void setTaskGasFreeze(GasResource resource) {
		beforeTaskChange();

		task = UnitTask.GAS_FREEZE;
		currentResource = resource;
	}

	protected void setBase(Base b) {
		base = b;
	}

	public Base getBase() {
		return base;
	}

	public Resource getCurrentResource() {
		return currentResource;
	}

	@Override
	public void act() {
		super.act();
		if (task == UnitTask.IDLE) {
			task = UnitTask.MINERALS;
		}
		if (task == UnitTask.MINERALS) {
			if (base == null) {
				System.err.println("No base found for worker!");
			}
			// Get back to work
			if (currentResource != null) {
				if (!unit.isGatheringMinerals()) {
					unit.gather(currentResource.unit);
				}
				return;
			}

			// Try to assign one worker to each mineral first
			MineralResource mineral = null;
			double distance = 0;

			// This variable is the loop counter. It only allows maxMiners
			// to gather each resource patch each loop.
			int maxMiners = 1;
			boolean workerAssigned = false;
			while (!workerAssigned && maxMiners <= 2) {
				for (MineralResource m : base.minerals) {
					if (m.getNumGatherers() < maxMiners) {
						// Find closest mineral patch
						double newDistance = Point.distance(unit.getX(), unit.getY(), m.getX(), m.getY());
						if (mineral == null || newDistance < distance) {
							mineral = m;
							distance = newDistance;
							workerAssigned = true;
						}
					}
				}

				maxMiners++;
			}
			setTaskMiningMinerals(mineral);
			// Worker could not be assigned a patch as the base is
			// supersaturated
			if (!workerAssigned) {
				GameHandler.sendText("Warning: Base is supersaturated!");
			}
		}
		if (task == UnitTask.GAS) {
			// Get back to work
			if (currentResource != null) {
				if (!unit.isGatheringGas()) {
					unit.gather(currentResource.unit);
				}
				return;
			}
		}
		if (task == UnitTask.GAS_FREEZE) {
			int predictedUnitX = (int) Math
					.round(unit.getPosition().getX() + unit.getVelocityX() * VELOCITY_SCALE_FACTOR);
			int predictedUnitY = (int) Math
					.round(unit.getPosition().getY() + unit.getVelocityY() * VELOCITY_SCALE_FACTOR);
			Position predictedPosition = new Position(predictedUnitX, predictedUnitY);
			if (predictedPosition.getDistance(currentResource.unit) > GAS_FREEZE_DISTANCE) {
				if (currentResource.unit.getType() == UnitType.Resource_Vespene_Geyser) {
					unit.build(UnitType.Terran_Refinery, currentResource.unit.getTilePosition());
					hasResetGasBuild = false;
				} else if (currentResource.unit.getPlayer().equals(GameHandler.getSelfPlayer())) {
					if (unit.getVelocityX() == 0 && unit.getVelocityY() == 0 && !hasResetGasBuild) {
						unit.haltConstruction();
						hasResetGasBuild = true;
					} else {
						unit.rightClick(currentResource.unit);
						System.out.println("Resume");
						hasResetGasBuild = false;
					}
				}
			} else {
				if (unit.isConstructing()) {
					unit.haltConstruction();
				}
				unit.stop();
				if (unit.getVelocityX() == 0 && unit.getVelocityY() == 0
						&& currentResource.getUnit().getPlayer().equals(GameHandler.getSelfPlayer())
						&& !currentResource.unit.isCompleted())
					currentResource.getUnit().cancelConstruction();
			}
		}
	}

	public void unitDestroyed() {
		if (currentResource != null) {
			currentResource.removeGatherer(this);
		}
	}
}
