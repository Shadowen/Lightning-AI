package gamestructure.debug;

@FunctionalInterface
public interface DrawFunction {
	public void accept(DebugEngine e) throws ShapeOverflowException;
}
