package org.basex.query.flwor;

import org.basex.query.*;
import org.basex.query.expr.ParseExpr;
import org.basex.query.item.Item;
import org.basex.query.var.*;
import org.basex.util.InputInfo;

/**
 * Abstract container for order by clauses.
 *
 * @author BaseX Team 2005-12, BSD License
 * @author Christian Gruen
 */
public abstract class OrderBy extends ParseExpr {
  /** Ascending/descending order. */
  boolean desc;
  /** Order for empty expressions. */
  boolean lst;

  /**
   * Empty constructor for stable sorting.
   * @param ii input info
   */
  OrderBy(final InputInfo ii) {
    super(ii);
  }

  /**
   * Returns the sort key in the given context.
   * @param ctx query context
   * @param i current position
   * @return sort key
   * @throws QueryException query exception
   */
  abstract Item key(final QueryContext ctx, final int i) throws QueryException;

  @Override
  public OrderBy remove(final Var v) {
    return this;
  }
}
