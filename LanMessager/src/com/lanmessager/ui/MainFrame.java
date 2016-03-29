package com.lanmessager.ui;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;

import org.apache.log4j.Logger;

import com.lanmessager.backgroundworker.ChatSendWorker;
import com.lanmessager.backgroundworker.ChatReceiveWorker;
import com.lanmessager.backgroundworker.DigestFileWorker;
import com.lanmessager.backgroundworker.NotifySendWorker;
import com.lanmessager.backgroundworker.NotifyReceiveWorker;
import com.lanmessager.backgroundworker.FileSendWorker;
import com.lanmessager.backgroundworker.FileReceiveWorker;
import com.lanmessager.communication.TransferFileServer;
import com.lanmessager.communication.host.HostInfo;
import com.lanmessager.communication.host.HostInfoHelper;
import com.lanmessager.communication.message.FriendOfflineMessage;
import com.lanmessager.communication.message.FriendOnlineMessage;
import com.lanmessager.communication.message.ReceiveFileMessage;
import com.lanmessager.communication.message.SendFileMessage;
import com.lanmessager.file.FileIdentifier;
import com.lanmessager.module.DigestFileTask;
import com.lanmessager.module.FriendInfo;

public class MainFrame extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3059606329100964103L;

	private static final Logger LOGGER = Logger.getLogger(MainFrame.class.getSimpleName());

	private static final String FILE_MENU_TEXT = "File";
	private static final String SEND_FILE_MENU_ITEM_TEXT = "Send file to friend...";
	private static final String DIGEST_FILE_MENU_ITEM_TEXT = "Digest file...";
	private static final String SEND_FILE_POPUP_MENU_ITEM_TEXT = "Send file...";

	private int frameWidth = 800;
	private int frameHeight = 600;
	private int splitPaneDividerLocation = 100;

	private final JFileChooser openFileChooser = new JFileChooser();
	private final JFileChooser saveFileChooser = new JFileChooser();

	private JPopupMenu popupMenu;
	private JPanel chatPanel;
	private JList<FriendInfo> friendList;

	private DefaultListModel<FriendInfo> friendListModel = new DefaultListModel<>();
	private Map<String, DigestFileTask> digestFileTaskMap = new HashMap<>();
	private Map<String, SendFilePanel> sendFilePanelMap = new HashMap<>();
	private Map<String, ReceiveFilePanel> receiveFilePanelMap = new HashMap<>();

	private static HostInfo localHostInfo;

	private final NotifySendWorker notifySender = new NotifySendWorker();
	private final NotifyReceiveWorker notifyReceiver = new NotifyReceiveWorker();
	private final ChatSendWorker chatSender = new ChatSendWorker();
	private final ChatReceiveWorker chatReceiver = new ChatReceiveWorker();
	private final FileSendWorker fileSendWorker = new FileSendWorker();
	private final FileReceiveWorker fileReceiveWorker = new FileReceiveWorker();
	private final DigestFileWorker digestWorker = new DigestFileWorker();

	public MainFrame() {

		getLocalHostInfo();

		initComponents();

		initWorker();

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				super.windowClosing(e);

				destroyWorker();

				System.exit(0);
			}
		});
	}

	private void getLocalHostInfo() {
		HostInfo[] hostInfos = HostInfo.getAllLocalHostInfo();
		if (hostInfos.length > 1) {
			String[] addresses = new String[hostInfos.length];
			for (int i = 0; i < hostInfos.length; i++) {
				addresses[i] = hostInfos[i].getAddress();
			}

			Object result = JOptionPane.showInputDialog(this, "IP address list", "Select one IP address",
					JOptionPane.QUESTION_MESSAGE, null, addresses, "");
			if (result == null) {
				/* User cancels. */
				System.exit(0);
			} else {
				/* Save user selection. */
				for (HostInfo hostInfo : hostInfos) {
					if (hostInfo.getAddress().equals(result)) {
						localHostInfo = hostInfo;
						break;
					}
				}

			}
		} else if (hostInfos.length == 1) {
			/* Only one IP address. */
			localHostInfo = hostInfos[0];
		} else {
			/* No IP address. */
			JOptionPane.showMessageDialog(this, "No IP address", "Launch failed", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}
	}

	private void initComponents() {
		setSize(frameWidth, frameHeight);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout());
		
		setTitle("LAN Messager");

		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu(FILE_MENU_TEXT);

		JMenuItem sendMenuItem = new JMenuItem(SEND_FILE_MENU_ITEM_TEXT);
		sendMenuItem.addActionListener(e -> {
			sendFileToSelectedFriend();
		});
		fileMenu.add(sendMenuItem);
		
		JMenuItem digestMenuItem = new JMenuItem(DIGEST_FILE_MENU_ITEM_TEXT);
		digestMenuItem.addActionListener(e -> {
			digestFile();
		});
		fileMenu.add(digestMenuItem);

		menuBar.add(fileMenu);
		add(menuBar, BorderLayout.NORTH);

		popupMenu = new JPopupMenu();
		
		JMenuItem sendPopupMenuItem = new JMenuItem(SEND_FILE_POPUP_MENU_ITEM_TEXT);
		sendPopupMenuItem.addActionListener(e -> {
			sendFileToSelectedFriend();
		});
		popupMenu.add(sendPopupMenuItem);

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

		friendList = new JList<>(friendListModel);
		friendList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		friendList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				super.mouseReleased(e);
				if (e.isPopupTrigger() && !popupMenu.isShowing()) {
					popupMenu.show(friendList, e.getX(), e.getY());
				}
			}
		});

		splitPane.add(friendList, JSplitPane.LEFT);

		JScrollPane scrollPane = new JScrollPane();

		chatPanel = new JPanel();
		BoxLayout boxLayout = new BoxLayout(chatPanel, BoxLayout.Y_AXIS);
		chatPanel.setLayout(boxLayout);

		// scrollPane.add(chatPanel);
		scrollPane.setViewportView(chatPanel);

		splitPane.add(scrollPane, JSplitPane.RIGHT);

		// splitPane.setDividerSize(20);
		splitPane.setDividerLocation(splitPaneDividerLocation);
		splitPane.setOneTouchExpandable(true);

		add(splitPane, BorderLayout.CENTER);
	}

	private void initWorker() {
		/* Setup notify server. */
		notifyReceiver.addFriendOnlineListener(event -> {
			FriendOnlineMessage message = event.getMessage();
			addFriend(message.getName(), message.getAddress());
		});
		notifyReceiver.addFriendOfflineListener(event -> {
			FriendOfflineMessage message = event.getMessage();
			removeFriend(message.getName(), message.getAddress());
		});

		/* Start notify server. */
		notifyReceiver.start();

		/* Send broadcast to notify that this host is online. */
		FriendOnlineMessage onlineMessage = new FriendOnlineMessage();
		onlineMessage.setName(localHostInfo.getName());
		onlineMessage.setAddress(localHostInfo.getAddress());
		notifySender.send(HostInfoHelper.BROADCAST_ADRESS, onlineMessage);

		/* Setup chat server. */
		chatReceiver.addSendFileListener(event -> {
			SendFileMessage message = event.getMessage();
			prepareReceiveFile(message.getFileName(), message.getFileSize(), message.getFileId(),
					message.getSenderAddress());
		});
		chatReceiver.addReceiveFileListener(event -> {
			ReceiveFileMessage message = event.getMessage();
			if (message.isAccept()) {
				startSendFile(message.getFileId());
			} else {
				abortSendFile(message.getFileId());
			}
		});

		/* Start chat server. */
		chatReceiver.start();
		
		/* Digest file worker. */
		digestWorker.addCompletedListener(event -> {
			String fileId = event.getFileId();
			DigestFileTask task = digestFileTaskMap.get(fileId);
			if (task == null) {
				LOGGER.warn("Digest file task is not found:" + fileId);
				return;
			}
			
			if (event.isCancelled()) {
				task.getPanel().cancel();
			} else if (event.isFailed()) {
				task.getPanel().fail(event.getCause().getMessage());
			} else {
				task.getPanel().complete(event.getFileDigestResult());
			}
			digestFileTaskMap.remove(event.getFileId());
		});
		digestWorker.addProgressUpdatedListeners(event -> {
			String fileId = event.getFileId();
			DigestFileTask task = digestFileTaskMap.get(fileId);
			if (task == null) {
				LOGGER.warn("Digest file task is not found:" + fileId);
				return;
			}
			
			task.getPanel().updateProgress(event.getProcessed(), event.getTotal());
		});
		
		fileSendWorker.addCompletedListener(event -> {
			String fileId = event.getFileId();
			if (sendFilePanelMap.containsKey(fileId)) {
				LOGGER.warn("Cannot find send file panel: " + fileId);
				return;
			}
			SendFilePanel panel = sendFilePanelMap.get(fileId);
			if (event.isCancelled()) {
				panel.cancel();
			} else if (event.isFailed()) {
				panel.fail(event.getCause().getMessage());
			} else {
				panel.complete(event.getFileDigestResult());
			}			
		});
		fileSendWorker.addProgressUpdatedListeners(event -> {
			String fileId = event.getFileId();
			if (sendFilePanelMap.containsKey(fileId)) {
				LOGGER.warn("Cannot find send file panel: " + fileId);
				return;
			}
			SendFilePanel panel = sendFilePanelMap.get(fileId);
			panel.updateProgress(event.getProcessed(), event.getTotal());
		});
		
		fileReceiveWorker.addCompletedListener(event -> {
			String fileId = event.getFileId();
			if (receiveFilePanelMap.containsKey(fileId)) {
				LOGGER.warn("Cannot find receive file panel: " + fileId);
				return;
			}
			ReceiveFilePanel panel = receiveFilePanelMap.get(fileId);
			if (event.isCancelled()) {
				panel.cancel();
			} else if (event.isFailed()) {
				panel.fail(event.getCause().getMessage());
			} else {
				panel.complete(event.getFileDigestResult());
			}
		});
		fileReceiveWorker.addProgressUpdatedListeners(event -> {
			String fileId = event.getFileId();
			if (receiveFilePanelMap.containsKey(fileId)) {
				LOGGER.warn("Cannot find receive file panel: " + fileId);
				return;
			}
			ReceiveFilePanel panel = receiveFilePanelMap.get(fileId);
			panel.updateProgress(event.getProcessed(), event.getTotal());
		});
		fileReceiveWorker.startReceiveServer();
	}

	private void destroyWorker() {
		notifyReceiver.stop();

		/* Send broadcast to notify that this host is offline. */
		FriendOfflineMessage offlineMessage = new FriendOfflineMessage();
		offlineMessage.setName(localHostInfo.getName());
		offlineMessage.setAddress(localHostInfo.getAddress());
		notifySender.send(HostInfoHelper.BROADCAST_ADRESS, offlineMessage);

		chatReceiver.stop();
		
		fileReceiveWorker.stopReceiveServer();
	}

	private void addFriend(String name, String address) {
		if (name == null || address == null) {
			LOGGER.info("Ignore empty message.");
			return;
		}
		if (localHostInfo.getName().equals(name) || localHostInfo.getAddress().equals(address)) {
			LOGGER.info("Ignore broadcast from self.");
			return;
		}
				
		for (int i = 0; i < friendListModel.size(); i++) {
			FriendInfo friendInfo = friendListModel.getElementAt(i);
			if (friendInfo.getName().equals(name) && friendInfo.getAddress().equals(address)) {
				LOGGER.info("Ignore added friend.");
				return;
			}
		}

		FriendInfo friendInfo = new FriendInfo();
		friendInfo.setName(name);
		friendInfo.setAddress(address);
		friendListModel.addElement(friendInfo);

		/* Reply local host to remote friend. */
		FriendOnlineMessage onlineMessage = new FriendOnlineMessage();
		onlineMessage.setName(localHostInfo.getName());
		onlineMessage.setAddress(localHostInfo.getAddress());
		notifySender.send(address, onlineMessage);
	}

	private void removeFriend(String name, String address) {
		for (int i = 0; i < friendListModel.size(); i++) {
			FriendInfo friendInfo = friendListModel.getElementAt(i);
			if (friendInfo.getName().equals(name) && friendInfo.getAddress().equals(address)) {

				friendListModel.removeElementAt(i);
				break;
			}
		}
	}
	
	private void digestFile() {
		int openFileOption = openFileChooser.showOpenDialog(this);
		if (JFileChooser.APPROVE_OPTION == openFileOption) {
			File file = openFileChooser.getSelectedFile();			
			String fileId = FileIdentifier.generateIdentifierString(file);
			
			DigestFilePanel panel = new DigestFilePanel(file.getName());
			addPanel(panel);
			panel.addCancelButtonActionListener(event -> {
				digestWorker.cancel(fileId);
			});
			
			DigestFileTask task = new DigestFileTask();
			task.setFile(file);
			task.setPanel(panel);
			
			digestFileTaskMap.put(fileId, task);
			
			digestWorker.digest(fileId, file);
		}
	}

	private void sendFileToSelectedFriend() {
		int selectedIndex = friendList.getSelectedIndex();
		if (selectedIndex != -1) {
			FriendInfo friend = friendListModel.get(selectedIndex);
			String receiverAddress = friend.getAddress();
			prepareSendFile(receiverAddress);
		}
	}
	
	private void prepareSendFile(String receiverAddress) {
		int openFileOption = openFileChooser.showOpenDialog(this);
		if (JFileChooser.APPROVE_OPTION == openFileOption) {
			File file = openFileChooser.getSelectedFile();
			String fileId = FileIdentifier.generateIdentifierString(file);

			fileSendWorker.register(receiverAddress, fileId, file, file.length());

			SendFilePanel panel = new SendFilePanel(file.getName());
			addPanel(panel);
			sendFilePanelMap.put(fileId, panel);
			panel.addCancelButtonActionListener(event -> cancelSendFile(fileId));
			
			SendFileMessage message = new SendFileMessage();
			message.setFileSize(file.length());
			message.setFileName(file.getName());
			message.setFileId(fileId);
			message.setSenderAddress(localHostInfo.getAddress());
			chatSender.send(receiverAddress, message);
			
			//addChatLabel("Send: " + file.getName());
		}
	}

	/**
	 * Start sending file when remote host accept.
	 */
	private void startSendFile(String fileId) {
		fileSendWorker.send(fileId);
	}

	/**
	 * Abort sending file when remote host reject.
	 */
	private void abortSendFile(String fileId) {		
		if (sendFilePanelMap.containsKey(fileId)) {
			LOGGER.warn("Cannot find send file panel: " + fileId);
			return;
		}
		SendFilePanel panel = sendFilePanelMap.get(fileId);
		panel.abort();		
	}

	private void cancelSendFile(String fileId) {
		fileSendWorker.cancel(fileId);
	}

	private void prepareReceiveFile(String fileName, long fileSize, String fileId, String senderAddress) {
		JButton acceptButton = new JButton("Accept");
		acceptButton.addActionListener(e -> {
			File savedFile = saveFileChooser.getSelectedFile();
			if (savedFile != null) {
				saveFileChooser.setSelectedFile(new File(savedFile.getParent(), fileName));
			} else {
				saveFileChooser.setSelectedFile(new File(fileName));
			}
			int saveFileOption = saveFileChooser.showSaveDialog(this);
			if (JFileChooser.APPROVE_OPTION == saveFileOption) {
				File file = saveFileChooser.getSelectedFile();
				startReceiveFile(file, fileSize, fileId, senderAddress);

				ReceiveFilePanel panel = new ReceiveFilePanel(file.getName());
				addPanel(panel);
				receiveFilePanelMap.put(fileId, panel);
				panel.addCancelButtonActionListener(event -> cancelReceiveFile(fileId));
			} else if (JFileChooser.APPROVE_OPTION == saveFileOption) {
				abortReceiveFile(fileId, senderAddress);
			}
		});
		JButton abortButton = new JButton("Abort");
		abortButton.addActionListener(e -> {
			abortReceiveFile(fileId, senderAddress);
		});		

		addChatLabelWithComponents("Receive: " + fileName, acceptButton, abortButton);
	}

	private void startReceiveFile(File file, long fileSize, String fileId, String senderAddress) {
		
		fileReceiveWorker.receive(fileId);

		ReceiveFileMessage message = new ReceiveFileMessage();
		message.setAccept(true);
		message.setFileId(fileId);
		message.setReceiverPort(TransferFileServer.PORT_TRANSFER_FILE);
		chatSender.send(senderAddress, message);
	}

	/**
	 * Abort receiving file and notify remote host abort sending.
	 */
	private void abortReceiveFile(String fileId, String senderAddress) {
		if (receiveFilePanelMap.containsKey(fileId)) {
			LOGGER.warn("Cannot find receive file panel: " + fileId);
			return;
		}
		ReceiveFilePanel panel = receiveFilePanelMap.get(fileId);
		panel.abort();
		
		ReceiveFileMessage message = new ReceiveFileMessage();
		message.setAccept(false);
		message.setFileId(fileId);
		message.setReceiverPort(TransferFileServer.PORT_TRANSFER_FILE);
		chatSender.send(senderAddress, message);
	}

	private void cancelReceiveFile(String fileId) {
		fileReceiveWorker.cancel(fileId);
	}

	private void addPanel(JPanel panel) {
		chatPanel.add(Box.createVerticalStrut(10));
		chatPanel.add(panel);
		chatPanel.validate();
	}
	
	/*private void addChatLabel(String text) {
		JLabel label = new JLabel(text);
		chatPanel.add(label);
		chatPanel.validate();
	}*/

	private void addChatLabelWithComponents(String text, JComponent... components) {
		JLabel label = new JLabel(text);
		chatPanel.add(label);
		for (JComponent component : components) {
			chatPanel.add(component);
		}
		chatPanel.validate();
	}
}
