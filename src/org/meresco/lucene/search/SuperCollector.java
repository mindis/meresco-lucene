package org.meresco.lucene.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.AtomicReaderContext;

public abstract class SuperCollector<SubCollectorType extends SubCollector> {

	protected List<SubCollectorType> subs = new ArrayList<SubCollectorType>();

	/**
	 * Called before collecting from each {@link AtomicReaderContext} in a
	 * separate thread. The returned {@link SubCollector} need not be thread
	 * safe as it's scope is limited to one segment.
	 * 
	 * The SubCollector is kept in a list and accessible by
	 * {@link #subCollectors()}.
	 * 
	 * @param context
	 *            next atomic reader context
	 * @throws IOException 
	 */
	public SubCollector subCollector(AtomicReaderContext context) throws IOException {
		SubCollectorType sub = this.createSubCollector(context);
		this.subs.add(sub);
		return sub;
	}

	/**
	 * Lower level factory method for SubCollectors.
	 * 
	 * @param context
	 *            is an AtomicReaderContext
	 * @return SubCollector for this context
	 * @throws IOException 
	 */
	abstract protected SubCollectorType createSubCollector(AtomicReaderContext context) throws IOException;
}
