package gamestructure.debug;

import java.util.List;

public class InvalidCommandException extends Exception {
	private final String name;
	private final String command;

	/**
	 * Create a new Exception caused by the given command executing on the given
	 * submodule.
	 * 
	 * @param iname
	 *            the name of the submodule throwing the exception
	 * @param icommand
	 *            the remaining command string at the site of exception
	 * */
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
