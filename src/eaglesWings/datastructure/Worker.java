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

	public void move(int x, int y) {
		game.move(unit.getID(), x, y);
	}

	public void gather(Resource r) {
		game.gather(unit.getID(), r.getID());

		if (r instanceof MineralResource) {
			setTask(WorkerTask.Mining_Minerals, r);
		} else if (r instanceof GasResource) {
			setTask(WorkerTask.Mining_Gas, r);
		} else {
			game.sendText("Error in gathering");
			setTask(WorkerTask.SCOUTING, null);
		}
	}

	public void build(BuildingPlan toBuild) {
		game.build(unit.getID(), toBuild.getTx(), toBuild.getTy(),
				toBuild.getTypeID());
		toBuild.setBuilder(this);
		currentTask = WorkerTask.Constructing_Building;
	}

	public WorkerTask getTask() {
		return currentTask;
	}

	public void setTask(WorkerTask task, Resource newResource) {
		currentTask = task;

		if (task == WorkerTask.Mining_Minerals || task == WorkerTask.Mining_Gas) {
			if (currentResource != null) {
				currentResource.removeGatherer();
			}
			if (newResource != null) {
				newResource.addGatherer();
			}
		} else if (task == WorkerTask.SCOUTING) {
			if (base != null) {
				base.removeWorker(getID());
			}
		}

		currentResource = newResource;
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

	public Resource getCurrentResource() {
		return currentResource;
	}

	public void unitDestroyed() {
		if (currentResource != null) {
			currentResource.removeGatherer();
		}
	}
}
