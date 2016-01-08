package botstate;

import java.util.Optional;

import datastructure.BaseManager;
import datastructure.Worker;
import gamestructure.GameHandler;
import micromanager.UnitTask;

public class FirstFrameState extends BotState {
	public FirstFrameState() {
		super();
		System.out.print("Starting BotState... ");
		System.out.println("Success!");
	}

	@Override
	public BotState act() {
		// TODO get scouting pathing working
		Optional<Worker> w = BaseManager.getFreeWorker();
		if (!w.isPresent()) {
			GameHandler.sendText("Can't scout since no workers available!");
		} else {
			w.get().setTask(UnitTask.SCOUTING);
		}
		System.out.println("Sent one worker scouting");
		return new StarportRush(this);
	}
}
