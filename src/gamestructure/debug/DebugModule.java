package gamestructure.debug;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A single module to be used in the {@link DebugManager}. Modules should be
 * instantiated as abstract inner classes within the class it is intended to
 * debug for. Each module should independently display one pertinent piece of
 * information from its enclosing class.<br>
 * Modules must ultimately be registered to the DebugEngine using
 * {@link DebugManager#createDebugModule}.
 * 
 * @author wesley
 *
 */
public class DebugModule {
	/**
	 * The name of this DebugModule. It must be and can only be set during
	 * construction. This is used when parsing commands to the
	 * {@link #DebugEngine}.
	 */
	public final String name;
	/**
	 * Basic functionality for all DebugModules is activating and deactivating.
	 * When this is true, the DebugModule will paint. When this is false, it
	 * will not. This is checked in {@link #drawIfActive}.<br>
	 */
	private boolean active = false;
	/**
	 * Commands to be executed if seen. Commands mapped to <b>null</b> will be
	 * executed if there are no commands following the name of the debug module.
	 */
	private Map<String, CommandFunction> commands = new HashMap<>();
	/**
	 * Any {@link DebugModule}s nested inside this one. Commands will be passed
	 * down the hierarchy.
	 */
	private Map<String, DebugModule> subModules = new HashMap<>();
	/** The name of the last added command or submodule */
	private String lastAdded;
	/** No valid last added command or submodule */
	private static final int LAT_NONE = 0;
	/** The last thing added was a command */
	private static final int LAT_COMMAND = 1;
	/** The last thing added was a submdoule */
	private static final int LAT_SUBMODULE = 2;
	/**
	 * Keeps track of the type of the last added thing.<br>
	 * <b><u>Valid values:</u></b>
	 * <ul>
	 * <li>{@link #LAT_NONE} = {@value #LAT_NONE}</li>
	 * <li>{@link #LAT_COMMAND} = {@value #LAT_COMMAND}</li>
	 * <li>{@link #LAT_SUBMODULE} = {@value #LAT_SUBMODULE}</li>
	 * </ul>
	 */
	private int lastAddedTo = LAT_NONE;
	/**
	 * The function called when {@link #drawIfActive()} is invoked. Should be
	 * set by {@link #setDraw(DrawFunction)}.
	 */
	private DrawFunction draw = () -> {
	};

	/**
	 * Constructs a DebugModule with the specified name.
	 * 
	 * @param iname
	 *            The name to use when referring to the module.
	 */
	DebugModule(String iname) {
		name = iname;
		commands.put(null, this::setActive);
	}

	public DebugModule addCommand(String command, CommandFunction action) {
		commands.put(command, action);
		lastAdded = command;
		lastAddedTo = LAT_COMMAND;
		return this;
	}

	public DebugModule addSubmodule(String name) {
		DebugModule debugModule = new DebugModule(name);
		subModules.put(name, debugModule);
		lastAdded = name;
		lastAddedTo = LAT_SUBMODULE;
		return debugModule;
	}

	public DebugModule addAlias(String alias) {
		return addAlias(alias, lastAdded);
	}

	protected DebugModule addAlias(String alias, String old) {
		switch (lastAddedTo) {
		case LAT_COMMAND:
			addCommand(alias, commands.get(old));
			break;
		case LAT_SUBMODULE:
			subModules.put(alias, subModules.get(old));
			break;
		default:
		}
		return this;
	}

	public DebugModule setDraw(DrawFunction c) {
		draw = c;
		lastAddedTo = LAT_NONE;
		return this;
	}

	/**
	 * Draws the DebugModule if and only if it is active by calling
	 * {@link #draw}.
	 * 
	 * @throws ShapeOverflowException
	 *             If the engine throws a ShapeOverflowException, it propagates
	 *             outward here.
	 */
	final void drawIfActive() throws ShapeOverflowException {
		if (active) {
			for (DebugModule s : subModules.values()) {
				s.drawIfActive();
			}
			draw.run();
		}
	}

	/**
	 * This function is called when a command prefixed with the module's name is
	 * received. This function should be overridden for additional control over
	 * commands.
	 * 
	 * @param command
	 *            The line of commands received. The line is separated by
	 *            whitespace into the array.
	 *            <ul>
	 *            <li><b>command[0]</b> is either the module's name or "all".
	 *            </li>
	 *            </ul>
	 * @throws InvalidCommandException
	 *             if the command cannot be parsed
	 */
	final void onReceiveCommand(List<String> command) throws InvalidCommandException {
		System.out.println(name + " is parsing command..." + command);
		// All
		if (command.size() > 1 && command.get(0).equals("all")) {
			setActive(true);
			for (DebugModule s : subModules.values()) {
				s.onReceiveCommand(command);
			}
			return;
		}
		// A command in my list
		if (commands.containsKey(command.get(0))) {
			System.out.print("Found command " + command + " ");
			commands.get(command.get(0)).apply(command);
			return;
		}
		// A submodule in my list
		if (subModules.containsKey(command.get(0))) {
			subModules.get(command.get(0)).onReceiveCommand(command.subList(1, command.size()));
			return;
		}

		throw new InvalidCommandException(name, command);
	}

	protected void setActive(List<String> command) {
		active = !active;
	}

	public void setActive(boolean setTo) {
		active = setTo;
	}
}
