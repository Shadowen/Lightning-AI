package botstate;


public class FirstFrameState extends BotState {

	public FirstFrameState() {
		super();
	}

	@Override
	public BotState act() {
		return new StarportRush(this);
	}
}
