package gamestructure.debug;

@FunctionalInterface
public interface DrawFunction {
	public void run() throws ShapeOverflowException;
}
