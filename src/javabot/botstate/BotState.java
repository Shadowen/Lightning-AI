package javabot.botstate;

import java.util.ArrayList;
import java.util.List;

import javabot.datastructure.Base;
import javabot.datastructure.GameHandler;

public abstract class BotState {
	public GameHandler game;
	public List<Base> bases;

	protected BotState(GameHandler igame) {
		game = igame;
		bases = new ArrayList<Base>();
	}

	protected BotState(BotState oldState) {
		game = oldState.game;
		bases = oldState.bases;
	}

	public abstract BotState act();
}
