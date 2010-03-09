package org.basex.gui.dialog;

import static org.basex.core.Text.*;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.border.EmptyBorder;
import org.basex.core.Main;
import org.basex.core.Prop;
import org.basex.core.proc.List;
import org.basex.gui.GUI;
import org.basex.gui.GUIProp;
import org.basex.gui.GUIConstants.Msg;
import org.basex.gui.layout.BaseXBack;
import org.basex.gui.layout.BaseXButton;
import org.basex.gui.layout.BaseXCheckBox;
import org.basex.gui.layout.BaseXCombo;
import org.basex.gui.layout.BaseXFileChooser;
import org.basex.gui.layout.BaseXLabel;
import org.basex.gui.layout.BaseXLayout;
import org.basex.gui.layout.BaseXTabs;
import org.basex.gui.layout.BaseXTextField;
import org.basex.gui.layout.TableLayout;
import org.basex.io.IO;
import org.basex.util.StringList;

/**
 * Dialog window for specifying the options for importing a file system.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Christian Gruen
 */
public final class DialogCreateFS extends Dialog {
  /** Available databases. */
  private final StringList db;
  /** Database info. */
  private final BaseXLabel info;
  /** Parsing complete filesystem. */
  private final BaseXCheckBox all;
  /** Browse button. */
  private final BaseXButton browse;
  /** ID3 parsing. */
  private final BaseXCheckBox meta;
  /** Context inclusion. */
  private final BaseXCheckBox cont;
  /** XML inclusion. */
  private final BaseXCheckBox xml;
  /** Button panel. */
  private final BaseXBack buttons;
  /** ComboBox. */
  private final BaseXCombo maxsize;

  /** Path summary flag. */
  private final BaseXCheckBox pathindex;
  /** Text index flag. */
  private final BaseXCheckBox txtindex;
  /** Attribute value index flag. */
  private final BaseXCheckBox atvindex;
  /** Fulltext index flag. */
  private final BaseXCheckBox ftxindex;
  /** Full-text indexing. */
  private final BaseXCheckBox[] ft = new BaseXCheckBox[4];

  /** Directory path. */
  final BaseXTextField path;
  /** Database name. */
  final BaseXTextField dbname;

  /**
   * Default constructor.
   * @param main reference to the main window
   */
  public DialogCreateFS(final GUI main) {
    super(main, CREATEFSTITLE);
    db = List.list(main.context);

    // create panels
    final BaseXBack p1 = new BaseXBack();
    p1.setLayout(new TableLayout(3, 1));
    p1.setBorder(8, 8, 8, 8);

    BaseXBack p = new BaseXBack();
    p.setLayout(new TableLayout(7, 2, 6, 0));

    final Prop prop = gui.context.prop;
    final GUIProp gprop = gui.prop;

    final BaseXLabel lab = new BaseXLabel(IMPORTFSTEXT, false, true);
    lab.setBorder(new EmptyBorder(4, 4, 0, 0));
    p.add(lab);

    browse = new BaseXButton(BUTTONBROWSE, this);
    browse.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        final IO file = new BaseXFileChooser(DIALOGFC, path.getText(),
            main).select(BaseXFileChooser.Mode.DOPEN);
        if(file != null) {
          path.setText(file.path());
          dbname.setText(file.dbname().replaceAll("[^\\w.-]", ""));
        }
      }
    });
    p.add(browse);

    path = new BaseXTextField(gprop.get(GUIProp.FSBACKING), this);
    path.addKeyListener(keys);
    p.add(path);

    all = new BaseXCheckBox(IMPORTALL, gprop.is(GUIProp.FSALL), this);
    all.setToolTipText(IMPORTALLINFO);
    all.setBorder(new EmptyBorder(4, 4, 0, 0));
    all.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        action(null);
      }
    });
    p.add(all);

    final BaseXLabel lab1 = new BaseXLabel(CREATENAME, false, true);
    lab1.setBorder(new EmptyBorder(4, 4, 4, 0));
    p.add(lab1);
    p.add(new BaseXLabel(""));

    dbname = new BaseXTextField(gprop.get(GUIProp.FSDBNAME), this);
    dbname.addKeyListener(keys);
    p.add(dbname);
    p1.add(p);

    info = new BaseXLabel(" ");
    info.setBorder(20, 0, 10, 0);
    p1.add(info);

    // Metadata panel
    final BaseXBack p2 = new BaseXBack();
    p2.setLayout(new TableLayout(4, 1));
    p2.setBorder(8, 8, 8, 8);

    // Include metadata checkbox
    BaseXLabel label = new BaseXLabel(IMPORTFSTEXT1, false, true);
    p2.add(label);
    meta = new BaseXCheckBox(IMPORTMETA, prop.is(Prop.FSMETA), 12, this);
    p2.add(meta);

    label = new BaseXLabel(IMPORTFSTEXT2, false, true);
    p2.add(label);

    p = new BaseXBack();
    p.setLayout(new BorderLayout());

    cont = new BaseXCheckBox(IMPORTCONT, prop.is(Prop.FSCONT), this);
    cont.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        action(null);
      }
    });
    p.add(cont, BorderLayout.WEST);

    xml = new BaseXCheckBox(IMPORTXML, prop.is(Prop.FSXML), this);
    xml.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        action(null);
      }
    });
    p.add(xml, BorderLayout.SOUTH);

    maxsize = new BaseXCombo(IMPORTFSMAX, this);

    final int m = prop.num(Prop.FSTEXTMAX);
    int i = -1;
    while(++i < IMPORTFSMAXSIZE.length - 1) {
      if(IMPORTFSMAXSIZE[i] == m) break;
    }
    maxsize.setSelectedIndex(i);

    p.add(maxsize, BorderLayout.EAST);
    BaseXLayout.setWidth(p, p2.getPreferredSize().width);
    p2.add(p);

    final BaseXBack p3 = new BaseXBack();
    p3.setLayout(new TableLayout(6, 1, 0, 0));
    p3.setBorder(8, 8, 8, 8);

    pathindex = new BaseXCheckBox(INFOPATHINDEX,
        prop.is(Prop.PATHINDEX), 0, this);
    p3.add(pathindex);
    p3.add(new BaseXLabel(PATHINDEXINFO, true, false));

    txtindex = new BaseXCheckBox(INFOTEXTINDEX,
        prop.is(Prop.TEXTINDEX), 0, this);
    p3.add(txtindex);
    p3.add(new BaseXLabel(TXTINDEXINFO, true, false));

    atvindex = new BaseXCheckBox(INFOATTRINDEX,
        prop.is(Prop.ATTRINDEX), 0, this);
    p3.add(atvindex);
    p3.add(new BaseXLabel(ATTINDEXINFO, true, false));

    final BaseXBack p4 = new BaseXBack();
    p4.setLayout(new TableLayout(10, 1, 0, 0));
    p4.setBorder(8, 8, 8, 8);

    ftxindex = new BaseXCheckBox(INFOFTINDEX, false, 0, this);
    p4.add(ftxindex);
    p4.add(new BaseXLabel(FTINDEXINFO, true, false));

    final String[] cb = { CREATEWC, CREATESTEM, CREATECS, CREATEDC };
    final boolean[] val = { prop.is(Prop.WILDCARDS), prop.is(Prop.STEMMING),
        prop.is(Prop.CASESENS), prop.is(Prop.DIACRITICS)
    };
    for(int f = 0; f < ft.length; f++) {
      ft[f] = new BaseXCheckBox(cb[f], val[f], this);
      p4.add(ft[f]);
    }

    final BaseXTabs tabs = new BaseXTabs(this);
    tabs.addTab(GENERALINFO, p1);
    tabs.addTab(METAINFO, p2);
    tabs.addTab(INDEXINFO, p3);
    tabs.addTab(FTINFO, p4);
    set(tabs, BorderLayout.CENTER);

    // create buttons
    buttons = okCancel(this);

    set(buttons, BorderLayout.SOUTH);

    action(null);
    finish(null);
  }

  @Override
  public void action(final Object cmp) {
    final boolean ftx = ftxindex.isSelected();
    for(final BaseXCheckBox f : ft) f.setEnabled(ftx);

    final boolean sel = !all.isSelected();
    path.setEnabled(sel);
    browse.setEnabled(sel);
    maxsize.setEnabled(cont.isSelected());

    boolean cAll; // import all is chosen?
    boolean cNam; // dbname given?

    final Prop prop = gui.context.prop;
    final GUIProp gprop = gui.prop;
    final String nm = dbname.getText().trim();
    cNam = !nm.isEmpty();
    if(cNam) gprop.set(GUIProp.FSDBNAME, nm);
    ok = cNam;

    cAll = all.isSelected();
    if(cAll) gprop.set(GUIProp.FSBACKING, path.getText());

    if(!cAll && cNam) {
      final String p = path.getText().trim();
      final IO file = IO.get(p);
      cAll = !p.isEmpty() && file.exists();
    }
    ok &= cAll;

    String inf = null;

    if(!ok) {
      if(!cAll) inf = PATHWHICH;
      if(!cNam) inf = DBWHICH;
    }

    Msg icon = Msg.ERR;
    if(ok) {
      ok = DialogCreate.dbValid(nm);
      if(!ok) {
        inf = Main.info(INVALID, EDITNAME);
      } else if(db.contains(nm)) {
        inf = prop.is(Prop.FUSE) ? RENAMEOVERBACKING : RENAMEOVER;
        icon = Msg.WARN;
      }
    }
    info.setText(inf, icon);
    enableOK(buttons, BUTTONOK, ok);
  }

  @Override
  public void close() {
    if(!ok) return;

    final Prop prop = gui.context.prop;
    prop.set(Prop.FSCONT, cont.isSelected());
    prop.set(Prop.FSMETA, meta.isSelected());
    prop.set(Prop.FSXML, xml.isSelected());
    prop.set(Prop.FSTEXTMAX, IMPORTFSMAXSIZE[maxsize.getSelectedIndex()]);
    prop.set(Prop.PATHINDEX, pathindex.isSelected());
    prop.set(Prop.TEXTINDEX, txtindex.isSelected());
    prop.set(Prop.ATTRINDEX, atvindex.isSelected());
    prop.set(Prop.FTINDEX, ftxindex.isSelected());
    prop.set(Prop.WILDCARDS, ft[0].isSelected());
    prop.set(Prop.STEMMING, ft[1].isSelected());
    prop.set(Prop.CASESENS, ft[2].isSelected());
    prop.set(Prop.DIACRITICS, ft[3].isSelected());

    final GUIProp gprop = gui.prop;
    gprop.set(GUIProp.FSALL, all.isSelected());
    gprop.set(GUIProp.FSBACKING, path.getText());
    gprop.set(GUIProp.FSDBNAME, dbname.getText());
    super.close();
  }
}