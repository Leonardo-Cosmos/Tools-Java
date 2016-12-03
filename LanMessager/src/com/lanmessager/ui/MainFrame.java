package com.lanmessager.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;

import org.apache.log4j.Logger;

import com.lanmessager.config.UserConfig;
import com.lanmessager.file.FileQueueGenerator;
import com.lanmessager.file.FileIdentifier;
import com.lanmessager.module.DigestFileTask;
import com.lanmessager.module.FriendInfo;
import com.lanmessager.module.ReceiveDirTask;
import com.lanmessager.module.SendDirTask;
import com.lanmessager.net.TransferFileServer;
import com.lanmessager.net.host.HostInfo;
import com.lanmessager.net.host.HostInfoHelper;
import com.lanmessager.net.message.FriendOfflineMessage;
import com.lanmessager.net.message.FriendOnlineMessage;
import com.lanmessager.net.message.ReceiveDirMessage;
import com.lanmessager.net.message.ReceiveFileMessage;
import com.lanmessager.net.message.SendDirMessage;
import com.lanmessager.net.message.SendFileMessage;
import com.lanmessager.worker.ChatReceiveWorker;
import com.lanmessager.worker.ChatSendWorker;
import com.lanmessager.worker.DigestFileWorker;
import com.lanmessager.worker.FileReceiveWorker;
import com.lanmessager.worker.FileSendWorker;
import com.lanmessager.worker.NotifyReceiveWorker;
import com.lanmessager.worker.NotifySendWorker;

public class MainFrame extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3059606329100964103L;

	private static final Logger LOGGER = Logger.getLogger(MainFrame.class.getSimpleName());
	
	private static final String FRAME_TITLE = "LAN Messenger - (%s)";
	
	private static final String FILE_MENU_TEXT = "File";
	private static final String SEND_FILE_MENU_ITEM_TEXT = "Send file to friend...";
	private static final String SEND_DIR_MENU_ITEM_TEXT = "Send directory to friend...";
	private static final String DIGEST_FILE_MENU_ITEM_TEXT = "Digest file...";
	private static final String CLEAR_MENU_ITEM_TEXT = "Clear ";
	
	private static final String FRIEND_MENU_TEXT = "Friend";
	private static final String REFRESH_FRIEND_LIST_MENU_ITEM_TEXT = "Refresh friend list";
	private static final String ADD_FRIEND_MENU_ITEM_TEXT = "Add friend...";
	private static final String REMOVE_FRIEND_MENU_ITEM_TEXT = "Remove friend";
	private static final String CHANGE_USER_NAME_MENU_ITEM_TEXT = "Change user name...";
	
	private static final String SEND_FILE_POPUP_MENU_ITEM_TEXT = "Send file...";
	private static final String REMOVE_FRIEND_POPUP_MENU_ITEM_TEXT = "Remove friend";
	
	private JFileChooser openFileChooser;
	private JFileChooser saveFileChooser;

	private JPopupMenu popupMenu;
	private JList<FriendInfo> friendList;
	private JSplitPane splitPane;
	private ChatPanel chatPanel;

	private DefaultListModel<FriendInfo> friendListModel = new DefaultListModel<>();
	private Map<String, DigestFileTask> digestFileTaskMap = new HashMap<>();
	private Map<String, SendFilePanel> sendFilePanelMap = new HashMap<>();
	private Map<String, ReceiveFilePanel> receiveFilePanelMap = new HashMap<>();
	private Map<String, SendDirTask> sendDirTaskMap = new HashMap<>();
	private Map<String, ReceiveDirTask> receiveDirTaskMap = new HashMap<>();
	private Map<String, String> sendFileDirMap = new HashMap<>();
	private Map<String, String> receiveFileDirMap = new HashMap<>();
	
	private static HostInfo localHostInfo;
	private String userName;

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

		addWindowListener(new WindowAdapter() {

			@Override
			public void windowOpened(WindowEvent e) {
				UserConfig userConfig = UserConfig.instance;
				try {
					userConfig.load();
				} catch (IOException ex) {
					JOptionPane.showConfirmDialog(MainFrame.this, "Load user configure failed.");
				}
				
				setSize(userConfig.getWindowSizeWidth(), userConfig.getWindowSizeHeight());
				
				setLocation(userConfig.getWindowLocationX(), userConfig.getWindowLocationY());
				
				splitPane.setDividerLocation(userConfig.getSplitPaneDividerLocation());
				
				String selectedOpenFilePath = userConfig.getOpenFilePath();
				if (null != selectedOpenFilePath) {
					openFileChooser.setSelectedFile(new File(selectedOpenFilePath));
				}
				
				String selectedSaveFilePath = userConfig.getSaveFilePath();
				if (null != selectedSaveFilePath) {
					saveFileChooser.setSelectedFile(new File(selectedSaveFilePath));
				}
				
				String userName = userConfig.getUserName();
				if (null == userName) {
					userName = localHostInfo.getName();
				}
				setUserName(userName);

				initWorker();
			}
			
			@Override
			public void windowClosing(WindowEvent e) {
				destroyWorker();

				UserConfig userConfig = UserConfig.instance;
				
				Dimension size = getSize();
				userConfig.setWindowSizeWidth(size.width);
				userConfig.setWindowSizeHeight(size.height);
				
				Point location = getLocation();
				userConfig.setWindowLocationX(location.x);
				userConfig.setWindowLocationY(location.y);
				
				int dividerLocation = splitPane.getDividerLocation();
				userConfig.setSplitPaneDividerLocation(dividerLocation);
				
				File selectedOpenFile = openFileChooser.getSelectedFile();
				if (null != selectedOpenFile) {
					userConfig.setOpenFilePath(selectedOpenFile.getAbsolutePath());
				} else {
					userConfig.setOpenFilePath("");
				}
				
				File selectedSaveFile = saveFileChooser.getSelectedFile();
				if (null != selectedSaveFile) {
					userConfig.setSaveFilePath(selectedSaveFile.getAbsolutePath());
				} else {
					userConfig.setSaveFilePath("");
				}
				
				userConfig.setUserName(getUserName());
				
				try {
					userConfig.save();
				} catch (IOException ex) {
					JOptionPane.showConfirmDialog(MainFrame.this, "Save user configure failed.");
				}
			}
			
			@Override
			public void windowClosed(WindowEvent e) {
				System.exit(0);
			}
		});
	}

	private String getUserName() {
		return userName;
	}

	private void setUserName(String userName) {
		this.userName = userName;
		this.setTitle(String.format(FRAME_TITLE, this.userName));
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
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout());
		
		setTitle("LAN Messager");

		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu(FILE_MENU_TEXT);

		JMenuItem sendFileMenuItem = new JMenuItem(SEND_FILE_MENU_ITEM_TEXT);
		sendFileMenuItem.addActionListener(e -> {
			sendFileToSelectedFriend();
		});
		fileMenu.add(sendFileMenuItem);
		
		JMenuItem sendDirMenuItem = new JMenuItem(SEND_DIR_MENU_ITEM_TEXT);
		sendDirMenuItem.addActionListener(e -> {
			sendDirectoryToSelectedFriend();
		});
		fileMenu.add(sendDirMenuItem);
		
		JMenuItem digestMenuItem = new JMenuItem(DIGEST_FILE_MENU_ITEM_TEXT);
		digestMenuItem.addActionListener(e -> {
			digestFile();
		});
		fileMenu.add(digestMenuItem);
		
		JMenuItem clearMenuItem = new JMenuItem(CLEAR_MENU_ITEM_TEXT);
		clearMenuItem.addActionListener(e -> {
			chatPanel.clearCompletedTaskPanel();
		});
		fileMenu.add(clearMenuItem);

		menuBar.add(fileMenu);
		
		JMenu friendMenu = new JMenu(FRIEND_MENU_TEXT);
		
		JMenuItem refreshFriendListMenuItem = new JMenuItem(REFRESH_FRIEND_LIST_MENU_ITEM_TEXT);
		refreshFriendListMenuItem.addActionListener(e -> {
			refreshFriendList();
		});
		friendMenu.add(refreshFriendListMenuItem);
		
		JMenuItem addFriendMenuItem = new JMenuItem(ADD_FRIEND_MENU_ITEM_TEXT);
		addFriendMenuItem.addActionListener(e -> {
			addFriendFromDialog();
		});
		friendMenu.add(addFriendMenuItem);
		
		JMenuItem removeFriendMenuItem = new JMenuItem(REMOVE_FRIEND_MENU_ITEM_TEXT);
		removeFriendMenuItem.addActionListener(e -> {
			removeSelectedFriend();
		});
		friendMenu.add(removeFriendMenuItem);
		
		JMenuItem changeUserNameMenuItem = new JMenuItem(CHANGE_USER_NAME_MENU_ITEM_TEXT);
		changeUserNameMenuItem.addActionListener(e -> {
			changeUserName();
		});
		friendMenu.add(changeUserNameMenuItem);
		
		menuBar.add(friendMenu);
		
		add(menuBar, BorderLayout.NORTH);

		popupMenu = new JPopupMenu();
		
		JMenuItem sendPopupMenuItem = new JMenuItem(SEND_FILE_POPUP_MENU_ITEM_TEXT);
		sendPopupMenuItem.addActionListener(e -> {
			sendFileToSelectedFriend();
		});
		popupMenu.add(sendPopupMenuItem);
		
		JMenuItem removeFriendPopupMenuItem = new JMenuItem(REMOVE_FRIEND_POPUP_MENU_ITEM_TEXT);
		removeFriendPopupMenuItem.addActionListener(e -> {
			removeSelectedFriend();
		});
		popupMenu.add(removeFriendPopupMenuItem);

		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

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

		chatPanel = new ChatPanel();

		// scrollPane.add(chatPanel);
		scrollPane.setViewportView(chatPanel);

		splitPane.add(scrollPane, JSplitPane.RIGHT);

		// splitPane.setDividerSize(20);
		splitPane.setOneTouchExpandable(true);

		add(splitPane, BorderLayout.CENTER);
		
		openFileChooser = new JFileChooser();
		saveFileChooser = new JFileChooser();
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

		refreshFriendList();

		/* Setup chat server. */
		chatReceiver.addSendFileListener(event -> {
			SendFileMessage message = event.getMessage();
			if (message.getDirId() == null) {
				prepareReceiveFile(message.getFileName(), message.getFileSize(), message.getFileId(),
						message.getSenderAddress());
			} else {
				makeDirAndReceive(message.getSubPath(), message.getDirId(),
						message.getFileName(), message.getFileSize(),
						message.getFileId(), message.getSenderAddress());
			}
		});
		chatReceiver.addReceiveFileListener(event -> {
			ReceiveFileMessage message = event.getMessage();
			if (message.isAccept()) {
				startSendFile(message.getFileId());
			} else {
				abortSendFile(message.getFileId());
			}
		});
		chatReceiver.addSendDirListener(event -> {
			SendDirMessage message = event.getMessage();
			prepareReceiveDirectory(message.getDirName(), message.getDirId(), 
					message.getSenderAddress());
		});
		chatReceiver.addReceiveDirListener(event -> {
			ReceiveDirMessage message = event.getMessage();
			if (message.isAccept()) {
				startSendDir(message.getDirId());
			} else {
				abortSendDir(message.getDirId());
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
				task.getPanel().succeed(event.getFileDigestResult());
			}
			digestFileTaskMap.remove(event.getFileId());
		});
		digestWorker.addProgressUpdatedListeners(event -> {
			String fileId = event.getFileId();
			DigestFileTask task = digestFileTaskMap.get(fileId);
			if (task == null) {
				LOGGER.warn("Digest file task is not found: " + fileId);
				return;
			}
			
			task.getPanel().updateProgress(event.getProcessed(), event.getTotal());
		});
		
		fileSendWorker.addCompletedListener(event -> {
			String fileId = event.getFileId();
			if (!sendFilePanelMap.containsKey(fileId)) {
				LOGGER.warn("Cannot find send file panel: " + fileId);
				return;
			}
			SendFilePanel panel = sendFilePanelMap.get(fileId);
			if (event.isCancelled()) {
				panel.cancel();
			} else if (event.isFailed()) {
				panel.fail(event.getCause().getMessage());
			} else {
				panel.succeed(event.getFileDigestResult());
			}
			
			String dirId = sendFileDirMap.get(fileId);
			if (dirId != null) {
				pollFileAndSend(dirId);
				sendFileDirMap.remove(fileId);
			}
		});
		fileSendWorker.addProgressUpdatedListeners(event -> {
			String fileId = event.getFileId();
			if (!sendFilePanelMap.containsKey(fileId)) {
				LOGGER.warn("Cannot find send file panel: " + fileId);
				return;
			}
			SendFilePanel panel = sendFilePanelMap.get(fileId);
			panel.updateProgress(event.getProcessed(), event.getTotal());
		});
		
		fileReceiveWorker.addCompletedListener(event -> {
			String fileId = event.getFileId();
			if (!receiveFilePanelMap.containsKey(fileId)) {
				LOGGER.warn("Cannot find receive file panel: " + fileId);
				return;
			}
			ReceiveFilePanel panel = receiveFilePanelMap.get(fileId);
			if (event.isCancelled()) {
				panel.cancel();
			} else if (event.isFailed()) {
				panel.fail(event.getCause().getMessage());
			} else {
				panel.succeed(event.getFileDigestResult());
			}
			
			String dirId = receiveFileDirMap.get(fileId);
			if (dirId != null) {
				receiveFileDirMap.remove(fileId);
			}
		});
		fileReceiveWorker.addProgressUpdatedListeners(event -> {
			String fileId = event.getFileId();
			if (!receiveFilePanelMap.containsKey(fileId)) {
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
		offlineMessage.setName(getUserName());
		offlineMessage.setAddress(localHostInfo.getAddress());
		notifySender.send(HostInfoHelper.BROADCAST_ADRESS, offlineMessage);

		chatReceiver.stop();
		
		fileReceiveWorker.stopReceiveServer();
		fileReceiveWorker.shutdown();
		
		fileSendWorker.shutdown();
		
		digestWorker.shutdown();
	}
	
	/**
	 *  Send broadcast to notify that this host is online.
	 */
	private void refreshFriendList() {
		FriendOnlineMessage onlineMessage = new FriendOnlineMessage();
		onlineMessage.setName(getUserName());
		onlineMessage.setAddress(localHostInfo.getAddress());
		notifySender.send(HostInfoHelper.BROADCAST_ADRESS, onlineMessage);
	}

	/**
	 * Manually add friend via input text dialog.
	 */
	private void addFriendFromDialog() {
		String name = JOptionPane.showInputDialog(this, "Name", "Add friend", JOptionPane.QUESTION_MESSAGE);
		if (name == null) {
			// User click cancel button.
			return;
		}
		if (name.trim().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Please input a name for this friend", "Add friend", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		String address = JOptionPane.showInputDialog(this, "IP address", "Add friend", JOptionPane.QUESTION_MESSAGE);
		if (address == null) {
			// User click cancel button.
			return;
		}
		if (address.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Please input IP address", "Add friend", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		addFriend(name, HostInfoHelper.getInetAddress(address));
	}
	
	private void addFriend(String name, String address) {
		LOGGER.info(String.format("Add friend: %s (%s)", name, address));
		
		if (name == null || address == null) {
			LOGGER.info("Ignore empty friend information.");
			return;
		}
		if (localHostInfo.getAddress().equals(address)) {
			LOGGER.info("Ignore broadcast from self. (" + address + ")");
			return;
		}
				
		for (int i = 0; i < friendListModel.size(); i++) {
			FriendInfo friendInfo = friendListModel.getElementAt(i);
			if (friendInfo.getAddress().equals(address)) {
				if (friendInfo.getName().equals(name)) {
					LOGGER.info("Ignore added friend. (" + address + ")");
					return;
				} else {
					friendInfo.setName(name);
					
					LOGGER.info("Rename added friend. (" + address + ")");
					return;
				}
			}
		}

		FriendInfo friendInfo = new FriendInfo();
		friendInfo.setName(name);
		friendInfo.setAddress(address);
		friendListModel.addElement(friendInfo);

		/* Reply local host to remote friend. */
		FriendOnlineMessage onlineMessage = new FriendOnlineMessage();
		onlineMessage.setName(getUserName());
		onlineMessage.setAddress(localHostInfo.getAddress());
		notifySender.send(address, onlineMessage);
	}

	/**
	 * Remove selected friend from list.
	 */
	private void removeSelectedFriend() {
		FriendInfo friendInfo = getSelectedFriend();
		if (friendInfo != null) {
			removeFriend(friendInfo.getName(), friendInfo.getAddress());
		}
	}
	
	private void removeFriend(String name, String address) {
		LOGGER.info(String.format("Remove friend: %s (%s)", name, address));
		for (int i = 0; i < friendListModel.size(); i++) {
			FriendInfo friendInfo = friendListModel.getElementAt(i);
			if (friendInfo.getAddress().equals(address)) {
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
			
			LOGGER.info(String.format("Digest file: %s", file.getAbsolutePath()));
			
			DigestFilePanel panel = new DigestFilePanel(file.getName());
			chatPanel.addPanel(panel);
			panel.addCancelButtonActionListener(event -> {
				digestWorker.cancel(fileId);
			});
			panel.start();
			
			DigestFileTask task = new DigestFileTask();
			task.setFile(file);
			task.setPanel(panel);
			
			digestFileTaskMap.put(fileId, task);
			
			digestWorker.digest(fileId, file);
		}
	}

	private FriendInfo getSelectedFriend() {
		FriendInfo friendInfo = null;
		int selectedIndex = friendList.getSelectedIndex();
		if (selectedIndex != -1) {
			friendInfo = friendListModel.get(selectedIndex);
		}
		return friendInfo;
	}
	
	private void sendFileToSelectedFriend() {
		FriendInfo friendInfo = getSelectedFriend();
		if (friendInfo != null) {
			String receiverAddress = friendInfo.getAddress();
			prepareSendFile(receiverAddress);
		}
	}
	
	private void prepareSendFile(String receiverAddress) {
		openFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		int openFileOption = openFileChooser.showOpenDialog(this);
		if (JFileChooser.APPROVE_OPTION == openFileOption) {
			File file = openFileChooser.getSelectedFile();
			String fileId = FileIdentifier.generateIdentifierString(file);
			
			LOGGER.info(String.format("Send file %s to %s", file.getAbsolutePath(), receiverAddress));

			fileSendWorker.register(receiverAddress, fileId, file, file.length());

			SendFilePanel panel = new SendFilePanel(file.getName(), getFriendName(receiverAddress));
			chatPanel.addPanel(panel);
			sendFilePanelMap.put(fileId, panel);
			panel.addCancelButtonActionListener(event -> cancelSendFile(fileId));
			
			SendFileMessage message = new SendFileMessage();
			message.setFileSize(file.length());
			message.setFileName(file.getName());
			message.setFileId(fileId);
			message.setSenderAddress(localHostInfo.getAddress());
			chatSender.send(receiverAddress, message);
		}
	}

	/**
	 * Start sending file when remote host accept.
	 */
	private void startSendFile(String fileId) {
		if (!sendFilePanelMap.containsKey(fileId)) {
			LOGGER.warn("Cannot find send file panel: " + fileId);
			return;
		}
		SendFilePanel panel = sendFilePanelMap.get(fileId);
		panel.start();
		fileSendWorker.send(fileId);
	}

	/**
	 * Abort sending file when remote host reject.
	 */
	private void abortSendFile(String fileId) {		
		if (!sendFilePanelMap.containsKey(fileId)) {
			LOGGER.warn("Cannot find send file panel: " + fileId);
			return;
		}
		fileSendWorker.unregister(fileId);
		SendFilePanel panel = sendFilePanelMap.get(fileId);
		panel.abort();
		sendFilePanelMap.remove(fileId);
	}

	private void cancelSendFile(String fileId) {
		fileSendWorker.cancel(fileId);
	}

	private void prepareReceiveFile(String fileName, long fileSize, String fileId, String senderAddress) {
		ReceiveFilePanel panel = new ReceiveFilePanel(fileName, getFriendName(senderAddress));
		chatPanel.addPanel(panel);
		receiveFilePanelMap.put(fileId, panel);
		panel.addCancelButtonActionListener(event -> cancelReceiveFile(fileId));		
		panel.addAcceptButtonActionListener(e -> {
			File savedFile = saveFileChooser.getSelectedFile();
			if (savedFile != null) {
				saveFileChooser.setSelectedFile(new File(savedFile.getParent(), fileName));
			} else {
				saveFileChooser.setSelectedFile(new File(fileName));
			}
			saveFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			int saveFileOption = saveFileChooser.showSaveDialog(this);
			if (JFileChooser.APPROVE_OPTION == saveFileOption) {
				File file = saveFileChooser.getSelectedFile();
				
				LOGGER.info(String.format("Receive file %s from %s", file.getAbsolutePath(), senderAddress));
				
				startReceiveFile(file, fileSize, fileId, senderAddress);

				
			} else if (JFileChooser.APPROVE_OPTION == saveFileOption) {
				abortReceiveFile(fileId, senderAddress);
			}
		});		
		panel.addAbortButtonActionListener(e -> {
			abortReceiveFile(fileId, senderAddress);
		});
	}

	private void startReceiveFile(File file, long fileSize, String fileId, String senderAddress) {
		if (!receiveFilePanelMap.containsKey(fileId)) {
			LOGGER.warn("Cannot find receive file panel: " + fileId);
			return;
		}
		ReceiveFilePanel panel = receiveFilePanelMap.get(fileId);
		panel.start();
		fileReceiveWorker.receive(fileId, file, fileSize);

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
		if (!receiveFilePanelMap.containsKey(fileId)) {
			LOGGER.warn("Cannot find receive file panel: " + fileId);
			return;
		}
		ReceiveFilePanel panel = receiveFilePanelMap.get(fileId);
		panel.abort();
		receiveFilePanelMap.remove(fileId);
		
		ReceiveFileMessage message = new ReceiveFileMessage();
		message.setAccept(false);
		message.setFileId(fileId);
		message.setReceiverPort(TransferFileServer.PORT_TRANSFER_FILE);
		chatSender.send(senderAddress, message);
	}

	private void cancelReceiveFile(String fileId) {
		fileReceiveWorker.cancel(fileId);
	}
	
	private void sendDirectoryToSelectedFriend() {
		FriendInfo friendInfo = getSelectedFriend();
		if (friendInfo != null) {
			String receiverAddress = friendInfo.getAddress();
			prepareSendDirectory(receiverAddress);
		}
	}
	
	private void prepareSendDirectory(String receiverAddress) {
		openFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int openFileOption = openFileChooser.showOpenDialog(this);
		if (JFileChooser.APPROVE_OPTION == openFileOption) {
			File dir = openFileChooser.getSelectedFile();
			String dirId = FileIdentifier.generateIdentifierString(dir);
			
			LOGGER.info(String.format("Send directory %s to %s", dir.getAbsolutePath(), receiverAddress));
			
			SendDirPanel panel = new SendDirPanel(dir.getName(), getFriendName(receiverAddress));
			panel.addCancelButtonActionListener(event -> cancelSendDirectory(dirId));
			chatPanel.addPanel(panel);
			
			Queue<String> fileQueue = FileQueueGenerator.getFileQueueOfDir(dir);
			
			SendDirTask task = new SendDirTask();
			task.setDir(dir);
			task.setPanel(panel);
			task.setReceiverAddress(receiverAddress);
			task.setFileQueue(fileQueue);
			sendDirTaskMap.put(dirId, task);
			
			SendDirMessage message = new SendDirMessage();
			message.setDirName(dir.getName());
			message.setDirId(dirId);
			message.setSenderAddress(localHostInfo.getAddress());
			chatSender.send(receiverAddress, message);
		}
	}
	
	private void startSendDir(String dirId) {
		if (!sendDirTaskMap.containsKey(dirId)) {
			LOGGER.warn("Cannot find send file task: " + dirId);
			return;
		}
		SendDirTask task = sendDirTaskMap.get(dirId);
		SendDirPanel panel = task.getPanel();
		panel.start();
		
		pollFileAndSend(dirId);
	}
	
	private void pollFileAndSend(String dirId) {
		if (!sendDirTaskMap.containsKey(dirId)) {
			LOGGER.warn("Cannot find send file task: " + dirId);
			return;
		}
		SendDirTask task = sendDirTaskMap.get(dirId);
		Queue<String> fileQueue = task.getFileQueue();
		String filePath = fileQueue.poll();
		if (filePath == null) {
			SendDirPanel panel = task.getPanel();
			panel.succeed(null);
		} else {
			String receiverAddress = task.getReceiverAddress();
			
			File file = new File(filePath);
			String fileId = FileIdentifier.generateIdentifierString(file);
			
			sendFileDirMap.put(fileId, dirId);
			
			File dir = task.getDir();
			String subPath = filePath.substring(dir.getAbsolutePath().length());
			
			LOGGER.info(String.format("Send %s of dir %s to %s",
					subPath, dir.getAbsolutePath(), receiverAddress));
			
			fileSendWorker.register(receiverAddress, fileId, file, file.length());
			
			SendDirPanel sendDirPanel = task.getPanel();
			SendFilePanel sendFilePanel = new SendFilePanel(file.getName(), getFriendName(receiverAddress));
			sendDirPanel.addPanel(sendFilePanel);
			sendFilePanelMap.put(fileId, sendFilePanel);
			sendFilePanel.addCancelButtonActionListener(event -> cancelSendFile(fileId));
			
			SendFileMessage message = new SendFileMessage();
			message.setFileSize(file.length());
			message.setFileName(file.getName());
			message.setFileId(fileId);
			message.setSubPath(subPath);
			message.setDirId(dirId);
			message.setSenderAddress(localHostInfo.getAddress());
			chatSender.send(receiverAddress, message);
		}
	}
	
	private void abortSendDir(String dirId) {
		if (!sendDirTaskMap.containsKey(dirId)) {
			LOGGER.warn("Cannot find send file task: " + dirId);
			return;
		}
		SendDirTask task = sendDirTaskMap.get(dirId);
		SendDirPanel panel = task.getPanel();
		panel.abort();
	}
	
	private void cancelSendDirectory(String dirId) {
		
	}
	
	private void prepareReceiveDirectory(String dirName, String dirId, String senderAddress) {
		ReceiveDirPanel panel = new ReceiveDirPanel(dirName, getFriendName(senderAddress));
		panel.addCancelButtonActionListener(event -> cancelReceiveDirectory(dirId));
		panel.addAcceptButtonActionListener(e -> {
			File savedFile = saveFileChooser.getSelectedFile();
			if (savedFile != null) {
				saveFileChooser.setSelectedFile(new File(savedFile.getParent(), dirName));
			} else {
				saveFileChooser.setSelectedFile(new File(dirName));
			}
			saveFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int savedFileOption = saveFileChooser.showSaveDialog(this);
			if (JFileChooser.APPROVE_OPTION == savedFileOption) {
				File dir = saveFileChooser.getSelectedFile();
				
				LOGGER.info(String.format("Receive directory %s from %s", dir.getAbsolutePath(), senderAddress));
				
				startReceiveDirectory(dir, dirId, senderAddress);
			}
		});
		panel.addAbortButtonActionListener(e -> {
			abortReceiveDirectory(dirId, senderAddress);
		});
		chatPanel.addPanel(panel);
		
		ReceiveDirTask task = new ReceiveDirTask();
		task.setPanel(panel);
		receiveDirTaskMap.put(dirId, task);
	}
	
	private void startReceiveDirectory(File dir, String dirId, String senderAddress) {
		if (!receiveDirTaskMap.containsKey(dirId)) {
			LOGGER.warn("Cannot find receive directory task: " + dirId);
			return;
		}
		ReceiveDirTask task = receiveDirTaskMap.get(dirId);
		task.setDir(dir);
		ReceiveDirPanel panel = task.getPanel();
		panel.start();
		
		ReceiveDirMessage message = new ReceiveDirMessage();
		message.setAccept(true);
		message.setDirId(dirId);
		message.setReceiverPort(TransferFileServer.PORT_TRANSFER_FILE);
		chatSender.send(senderAddress, message);
	}
	
	private void makeDirAndReceive(String subPath, String dirId,
			String fileName, long fileSize, String fileId, String senderAddress) {
		if (!receiveDirTaskMap.containsKey(dirId)) {
			LOGGER.warn("Cannot find receive directory task: " + dirId);
			return;
		}
		receiveFileDirMap.put(fileId, dirId);
		
		ReceiveDirTask task = receiveDirTaskMap.get(dirId);
		File dir = task.getDir();
		File file = new File(dir, subPath);
		
		LOGGER.info(String.format("Receive %s of dir %s to %s",
				subPath, dir.getAbsolutePath(), senderAddress));
		
		ReceiveDirPanel receiveDirPanel = task.getPanel();
		ReceiveFilePanel receiveFilePanel = new ReceiveFilePanel(fileName, getFriendName(senderAddress));
		receiveDirPanel.addPanel(receiveFilePanel);
		receiveFilePanelMap.put(fileId, receiveFilePanel);
		
		receiveFilePanel.start();
		fileReceiveWorker.receive(fileId, file, fileSize);
		
		ReceiveFileMessage message = new ReceiveFileMessage();
		message.setAccept(true);
		message.setFileId(fileId);
		message.setReceiverPort(TransferFileServer.PORT_TRANSFER_FILE);
		chatSender.send(senderAddress, message);
	}
	
	private void abortReceiveDirectory(String dirId, String senderAddress) {
		if (!receiveDirTaskMap.containsKey(dirId)) {
			LOGGER.warn("Cannot find receive directory task: " + dirId);
			return;
		}
		ReceiveDirTask task = receiveDirTaskMap.get(dirId);
		ReceiveDirPanel panel = task.getPanel();
		panel.abort();
		
		ReceiveDirMessage message = new ReceiveDirMessage();
		message.setAccept(false);
		message.setDirId(dirId);
		message.setReceiverPort(TransferFileServer.PORT_TRANSFER_FILE);
		chatSender.send(senderAddress, message);
	}
	
	private void cancelReceiveDirectory(String dirId) {
		
	}
	
	
	private void changeUserName() {
		String name = (String) JOptionPane.showInputDialog(this, "Name", "Change user name",
				JOptionPane.QUESTION_MESSAGE, null, null, getUserName());
		if (name == null) {
			// User click cancel button.
			return;
		}
		if (name.trim().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Please input a name", "Change user name", JOptionPane.ERROR_MESSAGE);
			return;
		}

		LOGGER.info(String.format("Change user name to %s", name));
		
		setUserName(name);
	}
	
	/**
	 * Retrieve friend name by specified friend address.
	 * @param address
	 * @return Name of friend if he/she is in list, otherwise the address.
	 */
	private String getFriendName(String address) {
		for (int i = 0; i < friendListModel.size(); i++) {
			FriendInfo friendInfo = friendListModel.getElementAt(i);
			if (friendInfo.getAddress().equals(address)) {
				return friendInfo.getName();
			}
		}
		return address;
	}
	
}
