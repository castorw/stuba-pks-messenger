package net.ctrdn.stuba.pks.messenger;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import javax.swing.JOptionPane;

public class Helpers {

    public static final void showExceptionMessage(Throwable thrwbl) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        thrwbl.printStackTrace(ps);
        JOptionPane.showMessageDialog(null, baos.toString(), "Exception", JOptionPane.ERROR_MESSAGE);
    }
}
