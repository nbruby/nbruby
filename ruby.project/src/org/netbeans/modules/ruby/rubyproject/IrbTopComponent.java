package org.netbeans.modules.ruby.rubyproject;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PipedInputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.JTextComponent;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.demo.TextAreaReadline;
import org.jruby.internal.runtime.ValueAccessor;
import org.netbeans.api.ruby.platform.RubyInstallation;
import org.openide.ErrorManager;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.Task;
import org.openide.util.TaskListener;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * IRB window.
 * This class is heavily based on IRBConsole in the JRuby distribution,
 * but changed since IRBConsole extends from JFrame and we want to extend
 * TopComponent (which is a JPanel).
 * 
 * @todo Use the equivalent of "jirb -rirb/completion" to get autocompletion?
 *    (include "irb/completion"). See http://jira.codehaus.org/browse/JRUBY-389?page=all
 * @todo It might be interesting to set the mime type of the embedded
 *   text pane to Ruby, and see if syntax highlighting works. Might
 *   need some tweaks, e.g. a derived mode for shell ruby.
 *   Also, if the TextAreaReadline messes with attributes in the StyledDocument,
 *   we're hosed. The NetBeans editor GuardedDocument implementation does not like that.
 * @todo Use output2's APIs: AbstractOutputTab - it has a lot of good
 *   logic for keeping the pane scrolled to track output, locking the caret on the
 *   last line, etc.
 */
final class IrbTopComponent extends TopComponent {
    
    private boolean finished = true;
    private JTextPane text;

    private static IrbTopComponent instance;
    /** path to the icon used by the component and its open action */
    static final String ICON_PATH = "org/netbeans/modules/ruby/rubyproject/jruby.png"; // NOI18N

    private static final String PREFERRED_ID = "IrbTopComponent"; // NOI18N

    private IrbTopComponent() {
        initComponents();
        setName(NbBundle.getMessage(IrbTopComponent.class, "CTL_IrbTopComponent"));
        setToolTipText(NbBundle.getMessage(IrbTopComponent.class, "HINT_IrbTopComponent"));
        setIcon(ImageUtilities.loadImage(ICON_PATH, true));
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {

        setLayout(new java.awt.BorderLayout());
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

    /**
     * Gets default instance. Do not use directly: reserved for *.settings files only,
     * i.e. deserialization routines; otherwise you could get a non-deserialized instance.
     * To obtain the singleton instance, use {@link findInstance}.
     */
    public static synchronized IrbTopComponent getDefault() {
        if (instance == null) {
            instance = new IrbTopComponent();
        }
        return instance;
    }

    /**
     * Obtain the IrbTopComponent instance. Never call {@link #getDefault} directly!
     */
    public static synchronized IrbTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            // Internal error message - NOI18N
            ErrorManager.getDefault().log(ErrorManager.WARNING,
                    "Cannot find MyWindow component. It will not be located properly in the window system."); // NOI18N
            return getDefault();
        }
        if (win instanceof IrbTopComponent) {
            return (IrbTopComponent)win;
        }
        // Internal error message - NOI18N
        ErrorManager.getDefault().log(ErrorManager.WARNING,
                "There seem to be multiple components with the '" + PREFERRED_ID + // NOI18N
                "' ID. That is a potential source of errors and unexpected behavior."); // NOI18N
        return getDefault();
    }

    public @Override int getPersistenceType() {
        return TopComponent.PERSISTENCE_ALWAYS;
    }

    public @Override void componentOpened() {
        if (finished) {
            // Start a new one
            finished = false;
            removeAll();
            createTerminal();
        }
    }

    public @Override void componentClosed() {
        // Leave the terminal session running
    }
    
    @Override
    public void componentActivated() {
        // Make the caret visible. See comment under componentDeactivated.
        if (text != null) {
            Caret caret = text.getCaret();
            if (caret != null) {
                caret.setVisible(true);
            }
        }
    }

    @Override
    public void componentDeactivated() {
        // I have to turn off the caret when the window loses focus. Text components
        // normally do this by themselves, but the TextAreaReadline component seems
        // to mess around with the editable property of the text pane, and
        // the caret will not turn itself on/off for noneditable text areas.
        if (text != null) {
            Caret caret = text.getCaret();
            if (caret != null) {
                caret.setVisible(false);
            }
        }
    }
    
    /** replaces this in object stream */
    public @Override Object writeReplace() {
        return new ResolvableHelper();
    }

    protected @Override String preferredID() {
        return PREFERRED_ID;
    }

    final static class ResolvableHelper implements Serializable {
        private static final long serialVersionUID = 1L;
        public Object readResolve() {
            return IrbTopComponent.getDefault();
        }
    }

    public void createTerminal() {
        text = new JTextPane();

        text.setMargin(new Insets(8,8,8,8));
        text.setCaretColor(new Color(0xa4, 0x00, 0x00));
        text.setBackground(new Color(0xf2, 0xf2, 0xf2));
        text.setForeground(new Color(0xa4, 0x00, 0x00));
        
        // From core/output2/**/AbstractOutputPane
        Integer i = (Integer) UIManager.get("customFontSize"); //NOI18N
        int size;
        if (i != null) {
            size = i.intValue();
        } else {
            Font f = (Font) UIManager.get("controlFont"); // NOI18N
            size = f != null ? f.getSize() : 11;
        }
        text.setFont(new Font ("Monospaced", Font.PLAIN, size)); //NOI18N
        setBorder (BorderFactory.createEmptyBorder());
        
        // Try to initialize colors from NetBeans properties, see core/output2
        Color c = UIManager.getColor("nb.output.selectionBackground"); // NOI18N
        if (c != null) {
            text.setSelectionColor(c);
        }
        
        //Object value = Settings.getValue(BaseKit.class, SettingsNames.CARET_COLOR_INSERT_MODE);
        //Color caretColor;
        //if (value instanceof Color) {
        //    caretColor = (Color)value;
        //} else {
        //    caretColor = SettingsDefaults.defaultCaretColorInsertMode;
        //}
        //text.setCaretColor(caretColor);
        //text.setBackground(UIManager.getColor("text")); //NOI18N
        //Color selectedFg = UIManager.getColor ("nb.output.foreground.selected"); //NOI18N
        //if (selectedFg == null) {
        //    selectedFg = UIManager.getColor("textText") == null ? Color.BLACK : //NOI18N
        //       UIManager.getColor("textText"); //NOI18N
        //}
        //
        //Color unselectedFg = UIManager.getColor ("nb.output.foreground"); //NOI18N
        //if (unselectedFg == null) {
        //    unselectedFg = selectedFg;
        //}
        //text.setForeground(unselectedFg);
        //text.setSelectedTextColor(selectedFg);
        //
        //Color selectedErr = UIManager.getColor ("nb.output.err.foreground.selected"); //NOI18N
        //if (selectedErr == null) {
        //    selectedErr = new Color (164, 0, 0);
        //}
        //Color unselectedErr = UIManager.getColor ("nb.output.err.foreground"); //NOI18N
        //if (unselectedErr == null) {
        //    unselectedErr = selectedErr;
        //}
        
        
        JScrollPane pane = new JScrollPane();
        pane.setViewportView(text);
        pane.setBorder(BorderFactory.createLineBorder(Color.darkGray));
        add(pane);
        validate();

        final Ruby runtime = getRuntime(text);
        RequestProcessor.Task task = RequestProcessor.getDefault().create(new Runnable() {
        //RequestProcessor.getDefault().post(new Runnable() {
            @Override
            public void run() {
                startIRB(runtime);
            }
        });
        task.addTaskListener(new TaskListener() {
            @Override
            public void taskFinished(Task task) {
                finished = true;
                //tar.writeMessage(" " + NbBundle.getMessage(IrbTopComponent.class, "IrbGoodbye") + " "); // NOI18N
                text.setEditable(false);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        IrbTopComponent.this.close();
                        IrbTopComponent.this.removeAll();
                        text = null;
                    }
                });
            }
        });
        task.schedule(10);
        
        // [Issue 91208]  avoid of putting cursor in IRB console on line where is not a prompt
        text.addMouseListener(new MouseAdapter() {
           @Override
           public void mouseClicked(MouseEvent ev) {
               final int mouseX = ev.getX();
               final int mouseY = ev.getY();
               // Ensure that this is done after the textpane's own mouse listener
               SwingUtilities.invokeLater(new Runnable() {
                    @Override
                   public void run() {
                       // Attempt to force the mouse click to appear on the last line of the text input
                       int pos = text.getDocument().getEndPosition().getOffset()-1;
                       if (pos == -1) {
                           return;
                       }

                       try {
                           Rectangle r = text.modelToView(pos);

                           if (mouseY >= r.y) {
                               // The click was on the last line; try to set the X to the position where
                               // the user clicked since perhaps it was an attempt to edit the existing
                               // input string. Later I could perhaps cast the text document to a StyledDocument,
                               // then iterate through the document positions and locate the end of the
                               // input prompt (by comparing to the promptStyle in TextAreaReadline).
                               r.x = mouseX;
                               pos = text.viewToModel(r.getLocation());
                           }

                           text.getCaret().setDot(pos);
                       } catch (BadLocationException ble) {
                           // do nothing - see #154991
                       }
                   }
               });
           }
        });
    }
    
    // package-private for unit-test only
    static Ruby getRuntime(final JTextComponent text) {
        final TextAreaReadline tar = new TextAreaReadline(text,
                " " + NbBundle.getMessage(IrbTopComponent.class, "IrbWelcome") + " \n\n"); // NOI18N
        // Ensure that ClassPath can find libraries etc.
        RubyInstallation.getInstance().setJRubyLoadPaths();

        final PipedInputStream pipeIn = new PipedInputStream();
        final RubyInstanceConfig config = new RubyInstanceConfig() {{
            setInput(pipeIn);
            setOutput(new PrintStream(tar.getOutputStream()));
            setError(new PrintStream(tar.getOutputStream()));
            setObjectSpaceEnabled(false);
            //setArgv(args);
        }};
        final Ruby runtime = Ruby.newInstance(config);

        runtime.getGlobalVariables().defineReadonly("$$", new ValueAccessor(runtime.newFixnum(System.identityHashCode(runtime))));
        runtime.getLoadService().init(new ArrayList());

        tar.hookIntoRuntime(runtime);
        return runtime;
    }
    
    private static void startIRB(final Ruby runtime) {
        runtime.evalScriptlet("require 'irb'; require 'irb/completion'; IRB.start"); // NOI18N
    }

    @Override
    public void requestFocus() {
        if (text != null) {
            text.requestFocus();
        }
    }

    @Override
    public boolean requestFocusInWindow() {
        if (text != null) {
            return text.requestFocusInWindow();
        }
        
        return false;
    }
}