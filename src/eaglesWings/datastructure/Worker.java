package eaglesWings.datastructure;

import eaglesWings.gamestructure.GameHandler;
import javabot.model.Unit;

public class Worker {

	private Unit unit;

	private Resource currentResource;
	private GameHandler game;

	private WorkerTask currentTask;
	private Base base;

	public Worker(GameHandler g, Unit u) {
		game = g;
		unit = u;
	}

	public void setBase(Base b) {
		base = b;
	}

	public boolean isIdle() {
		return unit.isIdle();
	}

	public void gather() {
		game.gather(unit.getID(), currentResource.getID());
	}

	public void gather(Resource r) {
		game.gather(unit.getID(), r.getID());

		if (r instanceof MineralResource) {
			setTask(WorkerTask.Mining_Minerals, r);
		} else if (r instanceof GasResource) {
			setTask(WorkerTask.Mining_Gas, r);
		}
	}

	public void build(BuildingPlan toBuild) {
		game.build(unit.getID(), toBuild);
		toBuild.setBuilder(this);
		currentTask = WorkerTask.Constructing_Building;
	}

	public WorkerTask getTask() {
		return currentTask;
	}

	public void setTask(WorkerTask task, Resource newResource) {
		currentTask = task;
		if (currentResource != null) {
			currentResource.removeGatherer();
		}
		currentResource = newResource;
		if (task == WorkerTask.Mining_Minerals || task == WorkerTask.Mining_Gas) {
			newResource.addGatherer();
		} else if (task == WorkerTask.Scouting) {
			base.removeWorker(getID());
		}
	}

	public int getX() {
		return unit.getX();
	}

	public int getY() {
		return unit.getY();
	}

	public int getID() {
		return unit.getID();
	}

	public Unit getUnit() {
		return unit;
	}

	public void unitDestroyed() {
		if (currentResource != null) {
			currentResource.removeGatherer();
		}
	}
}
