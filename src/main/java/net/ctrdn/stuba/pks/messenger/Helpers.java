package net.ctrdn.stuba.pks.messenger;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.Random;
import javax.swing.JOptionPane;
import net.ctrdn.stuba.pks.messenger.net.DefaultPeerIdentity;
import net.ctrdn.stuba.pks.messenger.net.PeerIdentity;
import net.ctrdn.stuba.pks.messenger.net.PeerStatus;
import net.ctrdn.stuba.pks.messenger.net.listener.ListenerMode;

public class Helpers {

    public static final void showExceptionMessage(Throwable thrwbl) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        thrwbl.printStackTrace(ps);
        JOptionPane.showMessageDialog(null, baos.toString(), "Exception", JOptionPane.ERROR_MESSAGE);
    }

    public static final PeerIdentity getLocalIdentity(MainFrame mf) {
        final ListenerMode mode = (((String) mf.getComboBoxMode().getSelectedItem()).startsWith("Server")) ? ListenerMode.SERVER : ListenerMode.CLIENT;
        final String localName = mf.getFieldIdentity().getText();
        final int localPort = Integer.parseInt(mf.getFieldPort().getText());
        final long finalIdent = new Random(new Date().getTime()).nextLong();

        return new DefaultPeerIdentity(finalIdent, mode, PeerStatus.ACTIVE, null, localPort, localName);
    }
}
