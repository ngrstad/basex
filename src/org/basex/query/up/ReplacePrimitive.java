package org.basex.query.up;

import org.basex.data.Data;
import org.basex.data.MemData;
import org.basex.query.item.DBNode;
import org.basex.query.item.Nod;

/**
 * Represents a replace primitive.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-09, ISC License
 * @author Lukas Kircher
 */
public final class ReplacePrimitive extends UpdatePrimitive {
  /** Nodes replacing the target. */
  final MemData r;
  /** Replacing nodes are attributes. */
  final boolean a;

  /**
   * Constructor.
   * @param n target node
   * @param replace replace nodes
   * @param attr replacing nodes are attributes
   */
  public ReplacePrimitive(final Nod n, final MemData replace, 
      final boolean attr) {
    super(n);
    a = attr;
    r = replace;
  }

  @Override
  public void apply() {
    if(!(node instanceof DBNode)) return;
    final DBNode n = (DBNode) node;
    final Data d = n.data;
    final int k = Nod.kind(n.type);
    final int par = d.parent(n.pre, d.kind(n.pre));
    if(a)
      UpdateFunctions.insertAttributes(n.pre, par, d, r);
    else d.insertSeq(n.pre + d.size(n.pre, k), par , r);
    d.delete(n.pre);
  }
}
