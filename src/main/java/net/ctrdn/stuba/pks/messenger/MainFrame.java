package net.ctrdn.stuba.pks.messenger;

import com.google.common.base.Preconditions;
import java.awt.Window;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.text.DefaultCaret;
import net.ctrdn.stuba.pks.messenger.exception.InitializationException;
import net.ctrdn.stuba.pks.messenger.exception.ListenerException;
import net.ctrdn.stuba.pks.messenger.exception.MessageException;
import net.ctrdn.stuba.pks.messenger.exception.UserInterfaceException;
import net.ctrdn.stuba.pks.messenger.net.Listener;
import net.ctrdn.stuba.pks.messenger.net.ListenerEventCallback;
import net.ctrdn.stuba.pks.messenger.net.ListenerMode;
import net.ctrdn.stuba.pks.messenger.net.Message;
import net.ctrdn.stuba.pks.messenger.net.PeerIdentity;
import net.ctrdn.stuba.pks.messenger.net.PeerStatus;

public class MainFrame extends javax.swing.JFrame {

    private class ListenerEventCallbackImpl implements ListenerEventCallback {

        private final MainFrame mf = MainFrame.this;
        private final List<PeerIdentity> peerIdentityList = new ArrayList<>();
        private final Map<PeerIdentity, Date> peerIdentityLastIdentMap = new ConcurrentHashMap<>();

        @Override
        public void onListenerStarted(ListenerMode mode) {
            mf.comboInterfaceList.setEnabled(false);
            mf.comboMode.setEnabled(false);
            mf.fieldPort.setEnabled(false);
            mf.fieldIdentity.setEnabled(false);
            mf.buttonControl.setText("Stop");
            mf.labelStatus.setText("Running (" + mode.toString() + ")");
            mf.labelAddress.setText("0.0.0.0:" + mf.fieldPort.getText());
            mf.logMessage("[Listener] Listener has started in mode " + mode.toString() + " on 0.0.0.0:" + mf.fieldPort.getText() + " with identifier " + mf.listener.getLocalIdentity().getIdntifier());

            if (mode == ListenerMode.CLIENT) {
                mf.fieldMessage.setEnabled(true);
                mf.spinnerMtu.setEnabled(true);
                mf.buttonSend.setEnabled(true);
            }
        }

        @Override
        public void onListenerStopped() {
            mf.comboInterfaceList.setEnabled(true);
            mf.comboMode.setEnabled(true);
            mf.fieldPort.setEnabled(true);
            mf.fieldIdentity.setEnabled(true);
            mf.buttonControl.setText("Start");
            mf.labelStatus.setText("Stopped");
            mf.labelAddress.setText("-");
            mf.logMessage("[Listener] Listener has been stopped");
            mf.listener = null;
            mf.listReceivers.setModel(new DefaultListModel());

            mf.fieldMessage.setEnabled(false);
            mf.spinnerMtu.setEnabled(false);
            mf.buttonSend.setEnabled(false);
        }

        @Override
        public void onListenerTick() {
            boolean updateNeeded = false;
            Date currentDate = new Date();
            for (Map.Entry<PeerIdentity, Date> entry : this.peerIdentityLastIdentMap.entrySet()) {
                if (currentDate.getTime() - entry.getValue().getTime() > 30000) {
                    mf.logMessage("[Listener] Peer " + entry.getKey().getInetAddress().getHostAddress() + ":" + entry.getKey().getPort() + " did not send identity packet in 30 seconds - removing");
                    this.peerIdentityList.remove(entry.getKey());
                    this.peerIdentityLastIdentMap.remove(entry.getKey());
                    updateNeeded = true;
                }
            }
            if (updateNeeded) {
                this.reloadReceiverList();
            }
        }

        @Override
        public void onIdentityBroadcastReceived(PeerIdentity peerIdentity) {
            mf.logMessage("[Listener] Identity packet received (address=" + peerIdentity.getInetAddress().getHostAddress() + ", port=" + peerIdentity.getPort() + ", ident=" + peerIdentity.getIdntifier() + ", name=" + peerIdentity.getPeerName() + ", mode=" + peerIdentity.getListenerMode().toString() + " status=" + peerIdentity.getPeerStatus().toString() + ")");
            if (peerIdentity.getIdntifier() == mf.listener.getLocalIdentity().getIdntifier()) {
                mf.logMessage("[Listener] Identity from self - ignored");
                return;
            }
            boolean updateNeeded = false;
            PeerIdentity foundPeerIdentity = null;
            for (PeerIdentity currentId : this.peerIdentityList) {
                if (currentId.getIdntifier() == peerIdentity.getIdntifier()) {
                    foundPeerIdentity = currentId;
                    break;
                }
            }
            if (foundPeerIdentity != null && peerIdentity.getPeerStatus() == PeerStatus.LEAVING) {
                this.peerIdentityList.remove(foundPeerIdentity);
                this.peerIdentityLastIdentMap.remove(foundPeerIdentity);
                updateNeeded = true;
            } else if (foundPeerIdentity == null) {
                this.peerIdentityList.add(peerIdentity);
                this.peerIdentityLastIdentMap.put(peerIdentity, new Date());
                updateNeeded = true;
            } else {
                this.peerIdentityLastIdentMap.remove(foundPeerIdentity);
                this.peerIdentityLastIdentMap.put(foundPeerIdentity, new Date());
            }
            if (updateNeeded) {
                this.reloadReceiverList();
            }
        }

        @Override
        public void onMessageReceived(Message message) {
            try {
                PeerIdentity foundId = null;
                for (PeerIdentity currentIdentity : this.peerIdentityList) {
                    if (currentIdentity.getInetAddress().equals(message.getSenderAddress()) && currentIdentity.getPort() == message.getSenderPort()) {
                        foundId = currentIdentity;
                        break;
                    }
                }
                String appendLines = "[Received message from ";
                appendLines += ((foundId == null) ? "_unknown_ (" : foundId.getPeerName() + " (");
                appendLines += message.getSenderAddress().getHostAddress() + ":" + message.getSenderPort() + ", ";
                appendLines += ((message.getReceivedFrameCount() == message.getTotalFragmentCount()) ? "C" : "I");
                appendLines += ", " + message.getReceivedFrameCount() + " of " + message.getTotalFragmentCount() + " fragments" + "]\n";
                appendLines += new String(message.getMessage()) + "\n\n";

                mf.textareaMessage.append(appendLines);
            } catch (MessageException ex) {
                mf.handleException(ex);
            }
        }

        @Override
        public void onListenerLogEvent(String message) {
            mf.logMessage("[Listener] " + message);
        }

        private void reloadReceiverList() {
            try {
                String selectedItem = (String) mf.listReceivers.getSelectedValue();
                boolean selectedItemFound = false;
                DefaultListModel cmodel = new DefaultListModel();
                for (PeerIdentity listingId : this.peerIdentityList) {
                    String newObj = "(" + ((listingId.getListenerMode() == ListenerMode.CLIENT) ? "C" : "S") + ") " + new String(listingId.getPeerName().getBytes("UTF-8")) + " (" + listingId.getInetAddress().getHostAddress() + ":" + listingId.getPort() + ")";
                    cmodel.addElement(newObj);
                    if (newObj.equals(selectedItem)) {
                        selectedItemFound = true;
                    }
                }
                mf.listReceivers.setModel(cmodel);
                if (selectedItemFound) {
                    mf.listReceivers.setSelectedValue(selectedItem, true);
                }
            } catch (UnsupportedEncodingException ex) {
                mf.handleException(ex);
            }
        }
    }

    private ListenerEventCallbackImpl listenerCallback;
    private final List<NetworkInterface> networkInterfaceList = new ArrayList<>();
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

    private void logMessage(String message) {
        this.textareaLog.append(message + "\n");
    }

    private void handleException(Throwable thrwbl) {
        if (UserInterfaceException.class.isAssignableFrom(thrwbl.getClass())) {
            JOptionPane.showMessageDialog(null, thrwbl.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            Helpers.showExceptionMessage(thrwbl);
        }
    }

    private void enumerateNetworkInterfaces() {
        this.networkInterfaceList.clear();
        DefaultComboBoxModel nicComboModel = new DefaultComboBoxModel();
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface nic = en.nextElement();
                this.networkInterfaceList.add(nic);
                String address = "n/a";
                for (Enumeration<InetAddress> ae = nic.getInetAddresses(); ae.hasMoreElements();) {
                    InetAddress inetAddress = ae.nextElement();
                    if (Inet4Address.class.isAssignableFrom(inetAddress.getClass())) {
                        address = inetAddress.getHostAddress();
                        break;
                    }
                }
                nicComboModel.addElement(nic.getDisplayName() + "( " + address + ")");
            }
            this.comboInterfaceList.setModel(nicComboModel);
        } catch (SocketException ex) {
            this.handleException(ex);
        }
    }

    private void enumerateModes() {
        DefaultComboBoxModel modeComboModel = new DefaultComboBoxModel();
        modeComboModel.addElement("Server (Receiver)");
        modeComboModel.addElement("Client (Transmitter)");
        this.comboMode.setModel(modeComboModel);
    }

    public void initialize() {
        this.enumerateNetworkInterfaces();
        this.enumerateModes();

        this.fieldMessage.setEnabled(false);
        this.buttonSend.setEnabled(false);
        this.spinnerMtu.setEnabled(false);
        this.spinnerMtu.setValue(64);

        DefaultCaret taLogCaret = (DefaultCaret) this.textareaLog.getCaret();
        taLogCaret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        DefaultCaret taRecvCaret = (DefaultCaret) this.textareaMessage.getCaret();
        taRecvCaret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        buttonGroup3 = new javax.swing.ButtonGroup();
        buttonGroup4 = new javax.swing.ButtonGroup();
        buttonGroup5 = new javax.swing.ButtonGroup();
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        comboInterfaceList = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        comboMode = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        labelStatus = new javax.swing.JLabel();
        labelAddress = new javax.swing.JLabel();
        buttonControl = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        listReceivers = new javax.swing.JList();
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
        textareaMessage = new javax.swing.JTextArea();
        jToolBar2 = new javax.swing.JToolBar();
        fieldMessage = new javax.swing.JTextField();
        spinnerMtu = new javax.swing.JSpinner();
        buttonSend = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        textareaLog = new javax.swing.JTextArea();
        jLabel8 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jSplitPane1.setDividerLocation(310);

        jPanel1.setMinimumSize(new java.awt.Dimension(250, 100));

        jLabel1.setText("Interface:");

        comboInterfaceList.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel2.setText("Mode:");

        comboMode.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

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

        jLabel5.setText("Available receivers:");

        listReceivers.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jScrollPane1.setViewportView(listReceivers);

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        fieldIpAddress.setText("enter IPv4 address");
        jToolBar1.add(fieldIpAddress);

        buttonAddPeer.setText("Add...");
        buttonAddPeer.setFocusable(false);
        buttonAddPeer.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonAddPeer.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
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
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboInterfaceList, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboMode, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(buttonControl))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel6)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(fieldPort))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel5)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(jLabel4)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(labelAddress, javax.swing.GroupLayout.PREFERRED_SIZE, 231, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel9)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(fieldIdentity)))
                        .addContainerGap())))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(comboInterfaceList, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(comboMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
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
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 216, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jSplitPane1.setLeftComponent(jPanel1);

        jPanel2.setPreferredSize(new java.awt.Dimension(250, 494));

        jSplitPane2.setDividerLocation(300);
        jSplitPane2.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        jLabel7.setText("Message Log");

        textareaMessage.setEditable(false);
        textareaMessage.setColumns(20);
        textareaMessage.setRows(5);
        jScrollPane2.setViewportView(textareaMessage);

        jToolBar2.setFloatable(false);
        jToolBar2.setRollover(true);

        fieldMessage.setText("enter message here...");
        jToolBar2.add(fieldMessage);

        spinnerMtu.setMinimumSize(new java.awt.Dimension(80, 28));
        spinnerMtu.setPreferredSize(new java.awt.Dimension(80, 28));
        jToolBar2.add(spinnerMtu);

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

        textareaLog.setEditable(false);
        textareaLog.setColumns(20);
        textareaLog.setRows(5);
        jScrollPane3.setViewportView(textareaLog);

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
        if (this.listener == null) {
            final ListenerMode mode = (((String) this.comboMode.getSelectedItem()).startsWith("Server")) ? ListenerMode.SERVER : ListenerMode.CLIENT;
            InterfaceAddress ifaceAddress = null;

            NetworkInterface nic = this.networkInterfaceList.get(this.comboInterfaceList.getSelectedIndex());
            for (InterfaceAddress ifaceAddr : nic.getInterfaceAddresses()) {
                InetAddress currentInetAddress = ifaceAddr.getAddress();
                InetAddress currentBroadcastAddress = ifaceAddr.getBroadcast();
                if (currentBroadcastAddress != null && currentInetAddress != null) {
                    ifaceAddress = ifaceAddr;
                    break;
                }
            }
            final String localName = this.fieldIdentity.getText();
            final int localPort = Integer.parseInt(this.fieldPort.getText());
            final long finalIdent = new Random(new Date().getTime()).nextLong();

            PeerIdentity localIdentity = new PeerIdentity() {

                @Override
                public InetAddress getInetAddress() {
                    return null;
                }

                @Override
                public int getPort() {
                    return localPort;
                }

                @Override
                public PeerStatus getPeerStatus() {
                    return PeerStatus.ACTIVE;
                }

                @Override
                public String getPeerName() {
                    return localName;
                }

                @Override
                public ListenerMode getListenerMode() {
                    return mode;
                }

                @Override
                public long getIdntifier() {
                    return finalIdent;
                }
            };

            try {
                this.listenerCallback = new ListenerEventCallbackImpl();
                this.listener = new Listener(localIdentity, mode);
                this.listener.addEventCallback(this.listenerCallback);
                new Thread(this.listener).start();
            } catch (ListenerException ex) {
                this.handleException(ex);
            }
        } else {
            this.listener.stop();
        }
    }//GEN-LAST:event_buttonControlActionPerformed

    private void buttonSendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSendActionPerformed
        try {
            if (this.listener == null || this.listener.getMode() != ListenerMode.CLIENT) {
                throw new UserInterfaceException("Listener is not started or not running in client mode.");
            }
            if (this.listReceivers.getSelectedIndex() < 0 || this.listReceivers.getSelectedIndex() >= this.listenerCallback.peerIdentityList.size()) {
                throw new UserInterfaceException("Invalid target selection.");
            }
            PeerIdentity target = this.listenerCallback.peerIdentityList.get(this.listReceivers.getSelectedIndex());
            if (target.getListenerMode() == ListenerMode.CLIENT) {
                throw new UserInterfaceException("You cannot send message to client (C), only to server (S).");
            }
            this.listener.sendMessage(target, (Integer) this.spinnerMtu.getValue(), this.fieldMessage.getText());
        } catch (UserInterfaceException | ListenerException ex) {
            this.handleException(ex);
        }
    }//GEN-LAST:event_buttonSendActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonAddPeer;
    private javax.swing.JButton buttonControl;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.ButtonGroup buttonGroup3;
    private javax.swing.ButtonGroup buttonGroup4;
    private javax.swing.ButtonGroup buttonGroup5;
    private javax.swing.JButton buttonSend;
    private javax.swing.JComboBox comboInterfaceList;
    private javax.swing.JComboBox comboMode;
    private javax.swing.JTextField fieldIdentity;
    private javax.swing.JTextField fieldIpAddress;
    private javax.swing.JTextField fieldMessage;
    private javax.swing.JTextField fieldPort;
    private javax.swing.JLabel jLabel1;
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
    private javax.swing.JList listReceivers;
    private javax.swing.JSpinner spinnerMtu;
    private javax.swing.JTextArea textareaLog;
    private javax.swing.JTextArea textareaMessage;
    // End of variables declaration//GEN-END:variables
}
