package eaglesWings.gamestructure;

public abstract class DebugModule {
	private String name;
	private boolean active = true;

	public DebugModule(String iname) {
		name = iname;
	}

	public abstract void draw(DebugEngine engine) throws ShapeOverflowException;

	public void receiveCommand(String command) {
		if (command.contains(name)) {
			active = !active;
		}
	}
}
