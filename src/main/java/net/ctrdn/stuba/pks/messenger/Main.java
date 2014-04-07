package net.ctrdn.stuba.pks.messenger;

import net.ctrdn.stuba.pks.messenger.exception.InitializationException;

public class Main {

    public static void main(String[] args) {
        try {
            MainFrame mf = new MainFrame();
            mf.initialize();
            mf.setVisible(true);
        } catch (InitializationException ex) {
            Helpers.showExceptionMessage(ex);
        }
    }
}
