package datastructure;

import bwapi.Position;
import bwapi.Unit;
import gamestructure.GameHandler;
import micromanager.UnitAgent;
import micromanager.UnitTask;

public class Worker extends UnitAgent {
	private Resource currentResource;
	private Base base;

	public Worker(Unit u) {
		super(u);
		task = UnitTask.IDLE;
	}

	public boolean isIdle() {
		return unit.isIdle();
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
		unit.build(toBuild.getType(), toBuild.getTilePosition());
		toBuild.setBuilder(this);
		task = UnitTask.CONSTRUCTING;
	}

	public UnitTask getTask() {
		return task;
	}

	public void setTask(UnitTask task) {
		setTask(task, null);
	}

	public void setTask(UnitTask task, Resource newResource) {
		this.task = task;

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

	public void unitDestroyed() {
		if (currentResource != null) {
			currentResource.removeGatherer();
		}
	}

}
