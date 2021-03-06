package org.basex.gui.view.editor;

import static org.basex.core.Text.*;
import static org.basex.gui.layout.BaseXKeys.*;
import static org.basex.util.Token.*;

import java.awt.event.*;
import java.io.*;
import java.util.regex.*;

import org.basex.build.*;
import org.basex.core.*;
import org.basex.core.cmd.*;
import org.basex.core.parse.*;
import org.basex.gui.*;
import org.basex.gui.editor.*;
import org.basex.gui.layout.*;
import org.basex.io.*;
import org.basex.query.*;
import org.basex.util.*;

/**
 * This class extends the text editor by XQuery features.
 *
 * @author BaseX Team 2005-12, BSD License
 * @author Christian Gruen
 */
final class EditorArea extends Editor {
  /** Pattern for detecting library modules. */
  private static final Pattern LIBMOD_PATTERN = Pattern.compile(
  "^(xquery( version ['\"].*?['\"])?( encoding ['\"].*?['\"])? ?; ?)?module namespace.*");

  /** File label. */
  final BaseXLabel label;

  /** File in tab. */
  IOFile file;
  /** Timestamp. */
  long tstamp;
  /** Flag for modified content. */
  boolean modified;
  /** Last input. */
  byte[] last;
  /** This flag indicates if the input is an XQuery main module. */
  boolean xquery = true;
  /** This flag indicates if the input is a command script. */
  boolean script = true;

  /** View reference. */
  final EditorView view;

  /**
   * Constructor.
   * @param v view reference
   * @param f file reference
   */
  EditorArea(final EditorView v, final IOFile f) {
    super(true, v.gui);
    view = v;
    file = f;
    label = new BaseXLabel(f.name());
    setSyntax(f, false);

    addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(final FocusEvent e) {
        // refresh query path and work directory
        final String path = file.path();
        gui.context.prop.set(Prop.QUERYPATH, path);
        gui.gprop.set(GUIProp.WORKPATH, file.dirPath());
        // reload file if it has been changed
        if(!reopen(false)) {
          // skip parsing if editor contains file that is currently marked as erroneous
          if(!path.equals(view.errFile)) release(Action.PARSE);
        }
      }
    });
  }

  /**
   * Returns {@code true} if a file instance exists for the edited text.
   * @return result of check
   */
  boolean opened() {
    return tstamp != 0;
  }

  /**
   * Initializes the text.
   * @param t text to be set
   */
  public void initText(final byte[] t) {
    last = t;
    super.setText(t);
    hist = new History(text.text());
  }

  @Override
  public void setText(final byte[] t) {
    last = getText();
    super.setText(t);
  }

  @Override
  public void mouseReleased(final MouseEvent e) {
    super.mouseReleased(e);
    view.posCode.invokeLater();
  }

  @Override
  public void keyPressed(final KeyEvent e) {
    final byte[] t = text.text();
    super.keyPressed(e);

    if(FINDERROR.is(e)) view.jumpToError();

    if(t != text.text()) resetError();
    view.posCode.invokeLater();
  }

  @Override
  public void keyTyped(final KeyEvent e) {
    final byte[] t = text.text();
    super.keyTyped(e);
    if(t != text.text()) resetError();
  }

  @Override
  public void keyReleased(final KeyEvent e) {
    super.keyReleased(e);
    if(!e.isActionKey() && !modifier(e) && !FINDERROR.is(e)) {
      release(EXEC.is(e) ? Action.EXECUTE : Action.CHECK);
    }
  }

  @Override
  protected void release(final Action action) {
    view.refreshControls(false);
    final byte[] in = getText();
    final boolean eq = eq(in, last);
    if(eq && action == Action.CHECK) return;
    last = in;

    script = file.hasSuffix(IO.BXSSUFFIX);
    xquery = !script && !opened() || file.hasSuffix(IO.XQSUFFIXES);
    String input = string(in);
    if(xquery) {
      // check if input is/might be an xquery main module
      if(input.isEmpty()) input = "()";
      xquery = !module(in);
      if(xquery && (action == Action.EXECUTE || gui.gprop.is(GUIProp.EXECRT))) {
        // execute query if forced, or if realtime execution is activated
        gui.execute(true, new XQuery(input));
      } else {
        // parse query
        gui.context.prop.set(Prop.QUERYPATH, file.path());
        final QueryContext qc = new QueryContext(gui.context);
        try {
          if(!xquery) qc.module(input);
          else qc.parse(input);
          view.info(OK, true, false);
        } catch(final QueryException ex) {
          view.info(Util.message(ex), false, false);
        } finally {
          qc.close();
        }
      }
    } else if(action == Action.EXECUTE && script) {
      // execute query if forced, or if realtime execution is activated
      gui.execute(true, new Execute(input));
    } else if(script || file.hasSuffix(IO.XMLSUFFIXES) ||
        file.hasSuffix(IO.XSLSUFFIXES) || file.hasSuffix(IO.HTMLSUFFIXES)) {
      try {
        new EmptyBuilder(new IOContent(in), gui.context).build();
        if(script) new CommandParser(input, gui.context).parse();
        view.info(OK, true, false);
      } catch(final Exception ex) {
        view.info(Util.message(ex), false, false);
      }
    } else if(action != Action.CHECK) {
      view.info(OK, true, false);
    }
  }

  /**
   * Reverts the contents of the currently opened editor.
   * @param enforce enforce reload
   * @return {@code true} if file was opened
   */
  public boolean reopen(final boolean enforce) {
    if(opened()) {
      final long ts = file.timeStamp();
      if((tstamp != ts || enforce) && (!modified ||
          BaseXDialog.confirm(gui, Util.info(REOPEN_FILE_X, file.name())))) {
        try {
          setText(file.read());
          file(file);
          release(Action.PARSE);
          return true;
        } catch(final IOException ex) {
          BaseXDialog.error(gui, FILE_NOT_OPENED);
        }
      }
      tstamp = ts;
    }
    return false;
  }

  /**
   * Updates the file reference, timestamp and history.
   * @param f file
   */
  void file(final IOFile f) {
    file = f;
    tstamp = f.timeStamp();
    setSyntax(file, true);
    hist.save();
    view.refreshHistory(file);
    view.refreshControls(true);
  }

  /**
   * Highlights the error.
   * @param pos error position
   */
  void jumpError(final int pos) {
    requestFocusInWindow();
    setCaret(pos);
  }

  /**
   * Analyzes the first 80 characters to decide if the query is a module.
   * @param qu query to check
   * @return result of check
   */
  private static boolean module(final byte[] qu) {
    final String start = QueryProcessor.removeComments(string(qu), 80);
    return LIBMOD_PATTERN.matcher(start).matches();
  }
}
