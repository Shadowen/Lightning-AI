package pathfinder;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 
 * @author wesley
 *
 * @param <T>
 *            The type of element the Quadtree is to store.
 */
public class Quadtree<T extends Shape> {
	/** Stores the {@link QTElement} corresponding to each item stored. */
	private Map<T, QTElement<T>> elements;

	/** Root level of the Quadtree. */
	private QTNode<T> root;
	/**
	 * Shapes that were inserted into the quadtree that are out of bounds of the
	 * root level.
	 */
	private Set<QTElement<T>> outOfBounds;

	/** Create a new Quadtree. It has no elements initially. */
	public Quadtree(int w) {
		elements = new HashMap<>();
		root = new QTNode<>(new Rectangle(0, 0, w, w));
		outOfBounds = new HashSet<>();
	}

	/**
	 * Change the bounding rectangle of the quadtree. All current elements will
	 * be re-inserted into the new tree.
	 * 
	 * @param newBounds
	 *            The new bounding rectangle
	 */
	public void setBounds(Rectangle newBounds) {
		// Use outOfBounds as a temp to store all the objects we are
		// transferring
		outOfBounds.addAll(root.objects);
		root = new QTNode<T>(newBounds);

		// Add all the shapes to the new root
		Iterator<QTElement<T>> it = outOfBounds.iterator();
		while (it.hasNext()) {
			QTElement<T> s = it.next();
			if (root.insert(s)) {
				it.remove();
			}
		}
	}

	/**
	 * Add an element to the quadtree. Shapes that do not lie within the
	 * quadtree are stored for later insertion.
	 * 
	 * @param t
	 *            the shape to be added
	 */
	public void insert(T t) {
		QTElement<T> e = new QTElement<T>(t);
		elements.put(t, e);
		if (!root.insert(e)) {
			outOfBounds.add(e);
		}
	}

	/**
	 * Removes an element from the quadtree.
	 * 
	 * @param t
	 *            The element to be removed
	 * @return <b>true</b> if the element was succesfully removed
	 */
	public boolean remove(T t) {
		QTElement<T> e = elements.get(t);
		if (outOfBounds.remove(e)) {
			return true;
		}
		return root.remove(e);
	}

	/**
	 * Process the tree nodes in pre-order.
	 * 
	 * @param consumer
	 *            the consumer that processes the nodes
	 */
	public void processNodes(Consumer<QTNode<T>> consumer) {
		root.processNodes(consumer);
	}

	/**
	 * @return the depth of the deepest part of the tree
	 */
	public int getTotalDepth() {
		return root.getDepthBelow() + 1;
	}

	/**
	 * Gets the element at the point given.
	 * 
	 * @param point
	 *            the point of interest
	 * @return an {@link Optional} containing the element
	 */
	public Optional<T> getAt(Point point) {
		return root.getAt(point).map(e -> e.payload);
	}

	public Set<T> getDownAndRight() {
		// TODO Auto-generated method stub
		return null;
	}

	public Set<T> getNeighbors(T currentElement) {
		Set<T> toReturn = new HashSet<T>();
		// TODO
		toReturn.addAll(elements.get(currentElement).containingNode.parent.objects.stream().map(o -> o.payload)
				.collect(Collectors.toList()));
		return toReturn;
	}

	public Stream<T> stream() {
		return root.stream().map(e -> e.payload);
	}
}
