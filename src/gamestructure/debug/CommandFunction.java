package gamestructure.debug;

import java.util.List;

@FunctionalInterface
public interface CommandFunction {
	public void apply(List<String> t1, DebugEngine t2)
			throws InvalidCommandException;
}
