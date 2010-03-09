package org.basex.core.proc;

import static org.basex.core.Text.*;
import java.io.IOException;
import java.util.Properties;
import org.basex.core.Context;
import org.basex.core.Main;
import org.basex.core.Proc;
import org.basex.core.ProgressException;
import org.basex.core.Prop;
import org.basex.data.DOTSerializer;
import org.basex.data.XMLSerializer;
import org.basex.io.CachedOutput;
import org.basex.io.IO;
import org.basex.io.NullOutput;
import org.basex.io.PrintOutput;
import org.basex.query.QueryException;
import org.basex.query.QueryProcessor;
import org.basex.query.item.Item;
import org.basex.query.iter.Iter;
import org.basex.util.Performance;

/**
 * Abstract class for database queries.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Christian Gruen
 */
abstract class AQuery extends Proc {
  /** Query processor. */
  private QueryProcessor qp;
  /** Parsing time. */
  private long pars;
  /** Compilation time. */
  private long comp;
  /** Evaluation time. */
  private long eval;
  /** Printing time. */
  private long prnt;

  /**
   * Protected constructor.
   * @param p command properties
   * @param a arguments
   */
  protected AQuery(final int p, final String... a) {
    super(p, a);
  }

  /**
   * Returns a new query instance.
   * @param query query
   * @param out output reference
   * @return query instance
   */
  protected final boolean query(final String query, final PrintOutput out) {
    final int runs = Math.max(1, prop.num(Prop.RUNS));
    String err = null;
    try {
      // define serialization parameters
      final Properties props = new Properties();
      if(prop.is(Prop.WRAPOUTPUT)) {
        props.setProperty("wrap-prefix", NAMELC);
        props.setProperty("wrap-uri", URL);
      }

      final boolean ser = prop.is(Prop.SERIALIZE);
      int s = 0;
      for(int i = 0; i < runs; i++) {
        final Performance per = new Performance();

        qp = new QueryProcessor(query, context);
        progress(qp);

        qp.parse();
        pars += per.getTime();
        if(i == 0) plan(qp, false);
        qp.compile();
        comp += per.getTime();
        if(i == 0) plan(qp, true);

        final XMLSerializer xml = new XMLSerializer(
            i == 0 && ser ? out : new NullOutput(!ser), props);

        if(context.prop.is(Prop.CACHEQUERY)) {
          result = qp.query();
          eval += per.getTime();
          result.serialize(xml);
          s = result.size();
        } else {
          final Iter ir = qp.iter();
          eval += per.getTime();
          s = 0;
          Item it;
          while((it = ir.next()) != null) {
            checkStop();
            it.serialize(xml);
            s++;
          }
        }
        xml.close();
        qp.close();
        prnt += per.getTime();
      }
      // dump some query info
      evalInfo(out, s, runs);

      if(ser && (prop.is(Prop.INFO) || prop.is(Prop.XMLPLAN))) out.println();
      out.flush();
      return true;
    } catch(final QueryException ex) {
      Main.debug(ex);
      err = ex.getMessage();
    } catch(final IOException ex) {
      Main.debug(ex);
      err = ex.getMessage();
    } catch(final ProgressException ex) {
      err = PROGERR;
    }
    // close processor after exceptions
    try { qp.close(); } catch(final IOException ex) { /* ignored */ }
    return error(err);
  }

  /**
   * Checks if the query performs updates.
   * @param ctx context reference
   * @param qu query
   * @return result of check
   */
  protected boolean updating(final Context ctx, final String qu) {
    try {
      final QueryProcessor proc = new QueryProcessor(qu, ctx);
      proc.parse();
      return proc.ctx.updating;
    } catch(final QueryException ex) {
      return true;
    }
  }

  /**
   * Performs the first argument as XQuery and returns a node set.
   */
  protected final void queryNodes() {
    try {
      result = new QueryProcessor(args[0], context).queryNodes();
    } catch(final QueryException ex) {
      Main.debug(ex);
      error(ex.getMessage());
    }
  }

  /**
   * Adds evaluation information to the information string.
   * @param out output stream
   * @param hits information
   * @param runs number of runs
   */
  private void evalInfo(final PrintOutput out, final long hits,
      final int runs) {
    if(!prop.is(Prop.INFO)) return;
    final String opt = qp.info(prop.is(Prop.ALLINFO));
    if(!opt.isEmpty()) info(opt);
    info(QUERYPARSE + Performance.getTimer(pars, runs));
    info(QUERYCOMPILE + Performance.getTimer(comp, runs));
    info(QUERYEVALUATE + Performance.getTimer(eval, runs));
    info(QUERYPRINT + Performance.getTimer(prnt, runs));
    info(QUERYTOTAL + perf.getTimer(runs));
    info(QUERYHITS + hits + " " + (hits == 1 ? VALHIT : VALHITS));
    info(QUERYPRINTED + Performance.format(out.size()));
    info(QUERYMEM, Performance.getMem());
  }

  /**
   * Creates query plans.
   * @param qu query reference
   * @param c compiled flag
   */
  private void plan(final QueryProcessor qu, final boolean c) {
    if(c != prop.is(Prop.COMPPLAN)) return;

    // show dot plan
    try {
      if(prop.is(Prop.DOTPLAN)) {
        final CachedOutput co = new CachedOutput();
        final DOTSerializer ser = new DOTSerializer(co);
        qu.plan(ser);
        ser.close();
        final String dot = "plan.dot";
        IO.get(dot).write(co.finish());
        new ProcessBuilder(prop.get(Prop.DOTTY), dot).start();
      }
      // show XML plan
      if(prop.is(Prop.XMLPLAN)) {
        final CachedOutput co = new CachedOutput();
        qu.plan(new XMLSerializer(co));
        info(QUERYPLAN);
        info.add(co.finish());
        info.add(NL);
      }
    } catch(final Exception ex) {
      Main.debug(ex);
    }
  }
}