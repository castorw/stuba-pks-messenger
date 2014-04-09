package net.ctrdn.stuba.pks.messenger;

import com.google.common.base.Preconditions;
import java.awt.Window;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.text.DefaultCaret;
import net.ctrdn.stuba.pks.messenger.exception.InitializationException;
import net.ctrdn.stuba.pks.messenger.exception.ListenerException;
import net.ctrdn.stuba.pks.messenger.exception.UserInterfaceException;
import net.ctrdn.stuba.pks.messenger.net.DefaultPeerIdentity;
import net.ctrdn.stuba.pks.messenger.net.listener.Listener;
import net.ctrdn.stuba.pks.messenger.net.listener.ListenerMode;
import net.ctrdn.stuba.pks.messenger.net.PeerIdentity;
import net.ctrdn.stuba.pks.messenger.net.PeerStatus;

public class MainFrame extends javax.swing.JFrame {

    private DefaultListenerCallback listenerCallback;
    private Listener listener = null;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void enableOSXFullscreen(Window window) throws InitializationException {
        Preconditions.checkNotNull(window);
        try {
            Class util = Class.forName("com.apple.eawt.FullScreenUtilities");
            Class params[] = new Class[]{Window.class, Boolean.TYPE};
            Method method = util.getMethod("setWindowCanFullScreen", params);
            method.invoke(util, window, true);
        } catch (ClassNotFoundException e1) {
        } catch (Exception e) {
            InitializationException finalEx = new InitializationException("Unable to enable native Mac OS X fullscreen");
            finalEx.addSuppressed(e);
            throw finalEx;
        }
    }

    public MainFrame() throws InitializationException {
        MainFrame.enableOSXFullscreen(this);
        initComponents();
    }

    protected void logMessage(String message) {
        this.getTaApplicationLog().append(message + "\n");
    }

    protected void handleException(Throwable thrwbl) {
        if (UserInterfaceException.class.isAssignableFrom(thrwbl.getClass())) {
            JOptionPane.showMessageDialog(null, thrwbl.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            Helpers.showExceptionMessage(thrwbl);
        }
    }

    public void initialize() {
        DefaultComboBoxModel modeComboModel = new DefaultComboBoxModel();
        modeComboModel.addElement("Server (Receiver)");
        modeComboModel.addElement("Client (Transmitter)");
        this.getComboBoxMode().setModel(modeComboModel);
        this.getFieldMessage().setEnabled(false);
        this.getButtonSend().setEnabled(false);
        this.getFieldMtu().setEnabled(false);
        this.getFieldMtu().setValue(64);
        this.fieldIpAddress.setEnabled(false);
        this.buttonAddPeer.setEnabled(false);
        DefaultCaret taLogCaret = (DefaultCaret) this.getTaApplicationLog().getCaret();
        taLogCaret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        DefaultCaret taRecvCaret = (DefaultCaret) this.getTaMessageLog().getCaret();
        taRecvCaret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    }

    public void removeListener() {
        this.listener = null;
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        comboBoxMode = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        labelStatus = new javax.swing.JLabel();
        labelAddress = new javax.swing.JLabel();
        buttonControl = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        listNeighbors = new javax.swing.JList();
        jToolBar1 = new javax.swing.JToolBar();
        fieldIpAddress = new javax.swing.JTextField();
        buttonAddPeer = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        fieldIdentity = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        fieldPort = new javax.swing.JTextField();
        jPanel2 = new javax.swing.JPanel();
        jSplitPane2 = new javax.swing.JSplitPane();
        jPanel3 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        taMessageLog = new javax.swing.JTextArea();
        jToolBar2 = new javax.swing.JToolBar();
        fieldMessage = new javax.swing.JTextField();
        fieldMtu = new javax.swing.JSpinner();
        buttonSend = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        taApplicationLog = new javax.swing.JTextArea();
        jLabel8 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jSplitPane1.setDividerLocation(310);

        jPanel1.setMinimumSize(new java.awt.Dimension(250, 100));

        jLabel2.setText("Mode:");

        comboBoxMode.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel3.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jLabel3.setText("Status:");

        jLabel4.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jLabel4.setText("Address:");

        labelStatus.setText("-");

        labelAddress.setText("-");

        buttonControl.setText("Start");
        buttonControl.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonControlActionPerformed(evt);
            }
        });

        jLabel5.setText("Neighbors:");

        listNeighbors.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jScrollPane1.setViewportView(listNeighbors);

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        fieldIpAddress.setText("enter ip:port to add server...");
        fieldIpAddress.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                fieldIpAddressFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                fieldIpAddressFocusLost(evt);
            }
        });
        jToolBar1.add(fieldIpAddress);

        buttonAddPeer.setText("Add...");
        buttonAddPeer.setFocusable(false);
        buttonAddPeer.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonAddPeer.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonAddPeer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAddPeerActionPerformed(evt);
            }
        });
        jToolBar1.add(buttonAddPeer);

        jLabel6.setText("UDP port:");

        fieldIdentity.setText("MessengerHost");

        jLabel9.setText("Identity:");

        fieldPort.setText("9321");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(labelStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jScrollPane1)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboBoxMode, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(buttonControl))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel6)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(fieldPort))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel9)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(fieldIdentity))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel5)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(jLabel4)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(labelAddress, javax.swing.GroupLayout.PREFERRED_SIZE, 231, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addContainerGap())))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(comboBoxMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(fieldPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(fieldIdentity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonControl)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(labelStatus))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(labelAddress))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 249, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jSplitPane1.setLeftComponent(jPanel1);

        jPanel2.setPreferredSize(new java.awt.Dimension(250, 494));

        jSplitPane2.setDividerLocation(300);
        jSplitPane2.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        jLabel7.setText("Message Log");

        taMessageLog.setEditable(false);
        taMessageLog.setColumns(20);
        taMessageLog.setLineWrap(true);
        taMessageLog.setRows(5);
        jScrollPane2.setViewportView(taMessageLog);

        jToolBar2.setFloatable(false);
        jToolBar2.setRollover(true);

        fieldMessage.setText("enter message here...");
        fieldMessage.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                fieldMessageFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                fieldMessageFocusLost(evt);
            }
        });
        jToolBar2.add(fieldMessage);

        fieldMtu.setMinimumSize(new java.awt.Dimension(80, 28));
        fieldMtu.setPreferredSize(new java.awt.Dimension(80, 28));
        jToolBar2.add(fieldMtu);

        buttonSend.setText("Send...");
        buttonSend.setFocusable(false);
        buttonSend.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonSend.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonSend.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSendActionPerformed(evt);
            }
        });
        jToolBar2.add(buttonSend);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel7)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 344, Short.MAX_VALUE)
                    .addComponent(jToolBar2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 239, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jToolBar2, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jSplitPane2.setTopComponent(jPanel3);

        taApplicationLog.setEditable(false);
        taApplicationLog.setColumns(20);
        taApplicationLog.setRows(5);
        jScrollPane3.setViewportView(taApplicationLog);

        jLabel8.setText("Application Log");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel8)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 344, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(jLabel8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 155, Short.MAX_VALUE)
                .addContainerGap())
        );

        jSplitPane2.setRightComponent(jPanel4);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane2)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane2)
        );

        jSplitPane1.setRightComponent(jPanel2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 675, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.Alignment.TRAILING)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonControlActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonControlActionPerformed
        if (this.getListener() == null) {
            try {
                this.listenerCallback = new DefaultListenerCallback(this);
                this.listener = new Listener(Helpers.getLocalIdentity(this));
                this.getListener().addCallback(this.listenerCallback);
                new Thread(this.getListener()).start();
            } catch (ListenerException ex) {
                this.handleException(ex);
            }
        } else {
            this.getListener().stop();
        }
    }//GEN-LAST:event_buttonControlActionPerformed

    private void buttonSendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSendActionPerformed
        try {
            if (this.getListener() == null || this.getListener().getLocalIdentity().getListenerMode() != ListenerMode.CLIENT) {
                throw new UserInterfaceException("Listener is not started or not running in client mode.");
            }
            if (this.getListNeighbors().getSelectedValue() == null) {
                throw new UserInterfaceException("Invalid target selection.");
            }
            PeerIdentity target = (PeerIdentity) this.getListNeighbors().getSelectedValue();
            if (target.getListenerMode() == ListenerMode.CLIENT) {
                throw new UserInterfaceException("You cannot send message to client (C), only to server (S).");
            }
            int fragments = this.getListener().sendMessage(target, (Integer) this.getFieldMtu().getValue(), this.getFieldMessage().getText());
            this.getTaMessageLog().append("[Sent message to " + target.getPeerName() + " (" + target.getInetAddress().getHostAddress() + ":" + target.getPort() + ") in " + fragments + " fragments]\n" + this.getFieldMessage().getText() + "\n\n");
            if (this.getFieldMessage().isFocusOwner()) {
                this.getFieldMessage().setText("");
            } else {
                this.getFieldMessage().setText("enter message here...");
            }
        } catch (UserInterfaceException | ListenerException ex) {
            this.handleException(ex);
        }
    }//GEN-LAST:event_buttonSendActionPerformed

    private void fieldMessageFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_fieldMessageFocusGained
        if (this.getFieldMessage().getText().equals("enter message here...")) {
            this.getFieldMessage().setText("");
        }
    }//GEN-LAST:event_fieldMessageFocusGained

    private void fieldMessageFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_fieldMessageFocusLost
        if (this.getFieldMessage().getText().equals("")) {
            this.getFieldMessage().setText("enter message here...");
        }
    }//GEN-LAST:event_fieldMessageFocusLost

    private void buttonAddPeerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAddPeerActionPerformed
        try {
            String[] addrSplit = this.fieldIpAddress.getText().split(":");
            final InetAddress peerAddress = Inet4Address.getByName(addrSplit[0]);
            final int peerPort = (addrSplit.length == 1) ? this.getListener().getLocalIdentity().getPort() : Integer.parseInt(addrSplit[1]);
            this.listenerCallback.addStaticPeerIdentity(new DefaultPeerIdentity(0, ListenerMode.SERVER, PeerStatus.ACTIVE, peerAddress, peerPort, "[Custom Entry]"));
        } catch (UnknownHostException ex) {
            this.handleException(ex);
        }
    }//GEN-LAST:event_buttonAddPeerActionPerformed

    private void fieldIpAddressFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_fieldIpAddressFocusGained
        if (this.fieldIpAddress.getText().equals("enter ip:port to add server...")) {
            this.fieldIpAddress.setText("");
        }
    }//GEN-LAST:event_fieldIpAddressFocusGained

    private void fieldIpAddressFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_fieldIpAddressFocusLost
        if (this.fieldIpAddress.getText().equals("")) {
            this.fieldIpAddress.setText("enter ip:port to add server...");
        }
    }//GEN-LAST:event_fieldIpAddressFocusLost


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonAddPeer;
    private javax.swing.JButton buttonControl;
    private javax.swing.JButton buttonSend;
    private javax.swing.JComboBox comboBoxMode;
    private javax.swing.JTextField fieldIdentity;
    private javax.swing.JTextField fieldIpAddress;
    private javax.swing.JTextField fieldMessage;
    private javax.swing.JSpinner fieldMtu;
    private javax.swing.JTextField fieldPort;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JToolBar jToolBar2;
    private javax.swing.JLabel labelAddress;
    private javax.swing.JLabel labelStatus;
    private javax.swing.JList listNeighbors;
    private javax.swing.JTextArea taApplicationLog;
    private javax.swing.JTextArea taMessageLog;
    // End of variables declaration//GEN-END:variables

    public javax.swing.JComboBox getComboBoxMode() {
        return comboBoxMode;
    }

    public javax.swing.JTextField getFieldIdentity() {
        return fieldIdentity;
    }

    public javax.swing.JTextField getFieldIpAddress() {
        return fieldIpAddress;
    }

    public javax.swing.JTextField getFieldMessage() {
        return fieldMessage;
    }

    public javax.swing.JSpinner getFieldMtu() {
        return fieldMtu;
    }

    public javax.swing.JTextField getFieldPort() {
        return fieldPort;
    }

    public javax.swing.JLabel getLabelAddress() {
        return labelAddress;
    }

    public javax.swing.JLabel getLabelStatus() {
        return labelStatus;
    }

    public javax.swing.JList getListNeighbors() {
        return listNeighbors;
    }

    public javax.swing.JTextArea getTaApplicationLog() {
        return taApplicationLog;
    }

    public javax.swing.JTextArea getTaMessageLog() {
        return taMessageLog;
    }

    public javax.swing.JButton getButtonAddPeer() {
        return buttonAddPeer;
    }

    public javax.swing.JButton getButtonControl() {
        return buttonControl;
    }

    public javax.swing.JButton getButtonSend() {
        return buttonSend;
    }

    public Listener getListener() {
        return listener;
    }
}
