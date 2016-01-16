package base;

import java.awt.Point;

import build.BuildingPlan;
import bwapi.Position;
import bwapi.Unit;
import gamestructure.GameHandler;
import micro.GroundAgent;
import micro.UnitTask;
import pathing.NoPathFoundException;

public class Worker extends GroundAgent {
	private Resource currentResource;
	private Base base;

	public Worker(Unit u) {
		super(u);
		task = UnitTask.IDLE;
	}

	public void move(int x, int y) {
		unit.move(new Position(x, y));
	}

	public void gather(Resource r) {
		unit.gather(r.getUnit());

		if (r instanceof MineralResource) {
			setTask(UnitTask.MINERALS, r);
		} else if (r instanceof GasResource) {
			setTask(UnitTask.GAS, r);
		} else {
			GameHandler.sendText("Error in gathering");
			setTask(UnitTask.SCOUTING, null);
		}
	}

	public void build(BuildingPlan toBuild) {
		toBuild.setBuilder(this);
		task = UnitTask.CONSTRUCTING;
	}

	public void setTask(UnitTask task, Resource newResource) {
		super.setTask(task);

		if (task == UnitTask.MINERALS || task == UnitTask.GAS) {
			if (currentResource != null) {
				currentResource.removeGatherer();
			}
			if (newResource != null) {
				newResource.addGatherer();
			}
		} else if (task == UnitTask.SCOUTING) {
			if (base != null) {
				base.removeWorker(this);
			}
		}

		currentResource = newResource;
	}

	public void setBase(Base b) {
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
		if (task == UnitTask.SCOUTING) {
			try {
				scout();
			} catch (NoPathFoundException e) {
				setTask(UnitTask.IDLE);
				e.printStackTrace();
			}
		}
		if (task == UnitTask.IDLE) {
			setTask(UnitTask.MINERALS);
		}
		if (task == UnitTask.MINERALS || task == UnitTask.GAS) {
			if (base == null) {
				System.err.println("No base found for worker!");
			}
			// Get back to work
			if (currentResource != null) {
				gather(currentResource);
				return;
			}

			// Try to assign one worker to each mineral first
			Resource mineral = null;
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
							gather(mineral);
							workerAssigned = true;
						}
					}
				}

				maxMiners++;
			}

			// Worker could not be assigned a patch as the base is
			// supersaturated
			if (!workerAssigned) {
				GameHandler.sendText("Warning: Base is supersaturated!");
			}
		}
	}

	public void unitDestroyed() {
		if (currentResource != null) {
			currentResource.removeGatherer();
		}
	}
}
