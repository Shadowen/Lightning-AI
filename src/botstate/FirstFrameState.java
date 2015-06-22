package botstate;

public class FirstFrameState extends BotState {

	public FirstFrameState() {
		super();
		System.out.print("Starting BotState... ");
		System.out.println("Success!");
	}

	@Override
	public BotState act() {
		return new StarportRush(this);
	}
}
