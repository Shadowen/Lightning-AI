package pathfinder;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

class QTElement<T extends Shape> implements Shape {
	public QTNode<T> containingNode;
	public T payload;

	public QTElement(T ipayload) {
		payload = ipayload;
	}

	@Override
	public boolean contains(Point2D arg0) {
		return payload.contains(arg0);
	}

	@Override
	public boolean contains(Rectangle2D arg0) {
		return payload.contains(arg0);
	}

	@Override
	public boolean contains(double arg0, double arg1) {
		return payload.contains(arg0, arg1);
	}

	@Override
	public boolean contains(double arg0, double arg1, double arg2, double arg3) {
		return payload.contains(arg0, arg1, arg2, arg3);
	}

	@Override
	public Rectangle getBounds() {
		return payload.getBounds();
	}

	@Override
	public Rectangle2D getBounds2D() {
		return payload.getBounds2D();
	}

	@Override
	public PathIterator getPathIterator(AffineTransform arg0) {
		return payload.getPathIterator(arg0);
	}

	@Override
	public PathIterator getPathIterator(AffineTransform arg0, double arg1) {
		return payload.getPathIterator(arg0, arg1);
	}

	@Override
	public boolean intersects(Rectangle2D arg0) {
		return payload.intersects(arg0);
	}

	@Override
	public boolean intersects(double arg0, double arg1, double arg2, double arg3) {
		return payload.intersects(arg0, arg1, arg2, arg3);
	}
}
