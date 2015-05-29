package gamestructure.debug;

import java.util.List;

public class InvalidCommandException extends Exception {
	private final String name;
	private final String command;

	public InvalidCommandException(String iname, List<String> icommand) {
		name = iname;
		command = String.join(" ", icommand);
	}

	@Override
	public String getMessage() {
		return "Invalid command: " + name + " did not have command \""
				+ command + "\"";
	}
}
