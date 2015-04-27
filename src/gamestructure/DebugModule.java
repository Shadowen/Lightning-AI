package gamestructure;

public abstract class DebugModule {
	/**
	 * The null module is a debugModule designed to absorb any function calls
	 * with no side effects.
	 */
	private static DebugModule nullModule = new DebugModule("Null Module") {
		@Override
		public void draw(DebugEngine engine) throws ShapeOverflowException {
		}
	};

	private String name;
	private boolean active = false;

	public DebugModule(String iname) {
		name = iname;
	}

	public String getName() {
		return name;
	}

	public void setActive(boolean newState) {
		active = newState;
	}

	public boolean isActive() {
		return active;
	}

	public void drawIfActive(DebugEngine engine) throws ShapeOverflowException {
		if (active) {
			draw(engine);
		}
	}

	public abstract void draw(DebugEngine engine) throws ShapeOverflowException;

	public void onReceiveCommand(String[] command) {
		active = !active;
	}

	/**
	 * Get the singleton instance of nullModule, a debugModule whose functions
	 * can be called without any effects.
	 * 
	 * @return the null debugModule.
	 */
	public static DebugModule getNullModule() {
		return nullModule;
	}

}
