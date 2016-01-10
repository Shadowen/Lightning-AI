package base;

import java.awt.Rectangle;

import build.BuildingPlan;
import bwapi.Position;
import bwapi.Unit;
import gamestructure.GameHandler;
import micro.UnitAgent;
import micro.UnitTask;
import pathing.InvalidStartNodeException;
import pathing.NoPathFoundException;
import pathing.PathFinder;

public class Worker extends UnitAgent {
	private Resource currentResource;
	private Base base;
	/** The destination building of the current path */
	private Rectangle pathTargetBox;

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

	@Override
	public void findPath(Position toWhere, int length) throws NoPathFoundException {
		pathTargetBox = null;
		super.findPath(toWhere, length);
	}

	public void findPath(Rectangle toWhere, int length) throws NoPathFoundException {
		pathTarget = null;
		// If we already have a decent path
		if (pathTargetBox != null && pathTargetBox.equals(toWhere)
				&& (path.size() >= 1.0 / 3 * length || pathOriginalSize <= 1.0 / 3 * length)) {
			System.out.println(GameHandler.getFrameCount() + ": Reusing old path");
			return;
		}
		// Otherwise make a new path
		System.out.println(GameHandler.getFrameCount() + ": New path " + (pathTargetBox != null) + ","
				+ (pathTargetBox != null && pathTargetBox.equals(toWhere)) + "," + (path.size() >= 1.0 / 3 * length));
		try {
			path = PathFinder.findGroundPath(unit, toWhere, length);
			pathTargetBox = toWhere;
			pathOriginalSize = path.size();
		} catch (InvalidStartNodeException e) {
			e.printStackTrace();
		}
	}
}
