package state;

public class FirstFrameState extends BotState {
	public FirstFrameState() {
		super();
		System.out.print("Starting BotState... ");
		System.out.println("Success!");
	}

	@Override
	public BotState onFrame() {
		return new StarportRush(this);
	}
}
