package botstate;

import java.util.Optional;

import datastructure.BaseManager;
import datastructure.Worker;
import datastructure.WorkerTask;
import gamestructure.GameHandler;
import micromanager.MicroManager;

public class FirstFrameState extends BotState {
	public FirstFrameState() {
		super();
		System.out.print("Starting BotState... ");
		System.out.println("Success!");
	}

	@Override
	public BotState act() {
		// TODO get scouting pathing working
		if (!MicroManager.isScouting()) {
			Optional<Worker> w = BaseManager.getFreeWorker();
			if (!w.isPresent()) {
				GameHandler.sendText("Can't scout since no workers available!");
			} else {
				w.get().setTask(WorkerTask.SCOUTING);
				MicroManager.setScoutingUnit(w.get().getUnit());
			}
		}
		return new StarportRush(this);
	}
}
