package com.hashfile.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceAdapter;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;

import com.hashfile.security.HashProgressChangedEvent;
import com.hashfile.security.HashProgressChangedListener;
import com.hashfile.security.HashedFile;

public class HashFrame extends JFrame {
		
	/**
	 * Serial version UID of this class.
	 */
	private static final long serialVersionUID = -6089829844511075386L;
	
	private static final Integer DEFAULT_WIDTH = 1000;
	private static final Integer DEFAULT_HEIGHT = 600;
	private static final Integer DEFAULT_X = 200;
	private static final Integer DEFAULT_Y = 100;
	
	private JMenuBar menuBar;
	
	private JMenu fileMenu;
	private JMenuItem addFileMenuItem;
	private JMenuItem addDirMenuItem;
	private JMenuItem cancelMenuItem;
	private JMenuItem exitMenuItem;
	
	private JMenu tableMenu;
	private JCheckBoxMenuItem upperCaseMenuItem;
	private JMenuItem clearMenuItem;
	private JMenuItem removeMenuItem;
	
	private JPopupMenu popupMenu;
	
	private JPanel panel;
	
	private JTable table;
	private JScrollPane scrollPane;
	
	private JToolBar toolBar;
	
	private JLabel progressLabel;
	private JProgressBar progressBar;
	
	private final JFileChooser fileChooser;
	
	private final HashedFileTableModel tableModel;
	
	private final HashFileListener hashFileListener; 
	
	private final Queue<File> fileQueue;
	
	private HashFileCancellationChecker cancellationChecker;
	
	private final HashThread hashThread;
	
	public HashFrame() {
		tableModel = new HashedFileTableModel();
		
		fileChooser = new JFileChooser();
		fileChooser.setMultiSelectionEnabled(true);
		
		hashFileListener = new HashFileListener();
		
		fileQueue = new LinkedList<File>();
		
		cancellationChecker = new HashFileCancellationChecker();
		
		hashThread = new HashThread();
		hashThread.start();
		
		initializeComponets();
	}
	
	private void initializeComponets() {
		addWindowListener(new HashFrameWindowListener());
		
		setTitle("Hash Files");
		setLayout(new BorderLayout());
		
		menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		fileMenu = new JMenu("File");
		menuBar.add(fileMenu);
		
		addFileMenuItem = new JMenuItem("Add File...");
		addFileMenuItem.addActionListener(new AddFileMenuItemActionListener());
		fileMenu.add(addFileMenuItem);
		
		addDirMenuItem = new JMenuItem("Add Directory...");
		addDirMenuItem.addActionListener(new AddDirMenuItemActionListener());
		fileMenu.add(addDirMenuItem);
		
		cancelMenuItem = new JMenuItem("Cancel");
		cancelMenuItem.addActionListener(new CancelMenuItemActionListener());
		fileMenu.add(cancelMenuItem);
		
		exitMenuItem = new JMenuItem("Exit");
		exitMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		fileMenu.add(exitMenuItem);
		
		tableMenu = new JMenu("Table");
		menuBar.add(tableMenu);
		
		upperCaseMenuItem = new JCheckBoxMenuItem("Upper Case");
		upperCaseMenuItem.addActionListener(new UpperCaseCheckBoxMenuItemActionListener());
		tableMenu.add(upperCaseMenuItem);
		
		clearMenuItem = new JMenuItem("Clear");
		clearMenuItem.addActionListener(new ClearMenuItemActionListener());
		tableMenu.add(clearMenuItem);
		
		removeMenuItem = new JMenuItem("Remove");
		removeMenuItem.addActionListener(new RemoveMenuItemActionListener());
		tableMenu.add(removeMenuItem);
		
		panel = new JPanel();
		panel.setLayout(new BorderLayout());
		add(panel, BorderLayout.CENTER);
				
		table = new JTable(tableModel);		
		scrollPane = new JScrollPane(table);
		DropTarget dropTarget = new DropTarget(scrollPane, new TableDropTargetListener());
		scrollPane.setDropTarget(dropTarget);
		DragSource dragSource = DragSource.getDefaultDragSource();
		dragSource.createDefaultDragGestureRecognizer(table,
				DnDConstants.ACTION_COPY, new TableDragGestureListener());
		
		panel.add(scrollPane, BorderLayout.CENTER);
		
		popupMenu = new JPopupMenu();
		popupMenu.add(new JMenuItem(addFileMenuItem.getText()))
			.addActionListener(new AddFileMenuItemActionListener());
		popupMenu.add(new JMenuItem(addDirMenuItem.getText()))
			.addActionListener(new AddDirMenuItemActionListener());
		popupMenu.add(new JMenuItem(removeMenuItem.getText()))
			.addActionListener(new RemoveMenuItemActionListener());
		popupMenu.add(new JMenuItem(clearMenuItem.getText()))
			.addActionListener(new ClearMenuItemActionListener());
		table.setComponentPopupMenu(popupMenu);
		scrollPane.setComponentPopupMenu(popupMenu);
		
		toolBar = new JToolBar();
		toolBar.setLayout(new FlowLayout(FlowLayout.LEFT));
		add(toolBar, BorderLayout.SOUTH);
		
		progressLabel = new JLabel();
		progressLabel.setVisible(false);
		toolBar.add(progressLabel);
		
		progressBar = new JProgressBar();
		progressBar.setVisible(false);
		toolBar.add(progressBar);
	}
		
	private void hashFile(File[] files) {
		synchronized (fileQueue) {
			for (File file : files) {
				fileQueue.add(file);
			}
			
			cancellationChecker.resume();
			fileQueue.notify();
		}
	}
	
	private class HashFrameWindowListener extends WindowAdapter {

		@Override
		public void windowClosed(WindowEvent e) {
		}

		@Override
		public void windowClosing(WindowEvent e) {
			// Saves properties to object.
			Properties properties = new Properties();
			
			Dimension size = getSize();
			properties.setProperty("Width", new Integer(size.width).toString());
			properties.setProperty("Height", new Integer(size.height).toString());
			
			Point location = getLocation();
			properties.setProperty("X", new Integer(location.x).toString());
			properties.setProperty("Y", new Integer(location.y).toString());
			
			File selectedFile = fileChooser.getSelectedFile();
			if (selectedFile != null) {
				properties.setProperty("FileChooserPath", selectedFile.getPath());
			}
			
			// Saves properties to file.
			try {
				OutputStream outputStream = new FileOutputStream("WindowConfig.properties"); 
				properties.store(outputStream, null);
			} catch (FileNotFoundException ex) {
				System.out.println("Fail to save properties file.");
				System.out.println(ex.getMessage());
				ex.printStackTrace();
			} catch (IOException ex) {
				System.out.println("Fail to save properties file.");
				System.out.println(ex.getMessage());
				ex.printStackTrace();
			}
		}

		@Override
		public void windowOpened(WindowEvent e) {
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			
			// Sets default properties.
			Properties properties = new Properties();
			properties.setProperty("Width", DEFAULT_WIDTH.toString());
			properties.setProperty("Height", DEFAULT_HEIGHT.toString());
			properties.setProperty("X", DEFAULT_X.toString());
			properties.setProperty("Y", DEFAULT_Y.toString());
			
			// Loads properties from file.
			try {
				FileInputStream inputStream = new FileInputStream("WindowConfig.properties");
				properties.load(inputStream);
			} catch (FileNotFoundException ex) {
				System.out.println("Fail to load properties file.");
				System.out.println(ex.getMessage());
				ex.printStackTrace();
			} catch (IOException ex) {
				System.out.println("Fail to load properties file.");
				System.out.println(ex.getMessage());
				ex.printStackTrace();
			}
			
			// Loads properties from object.
			try {
				int width = Integer.parseInt(properties.getProperty(
						"Width"));
				int height = Integer.parseInt(properties.getProperty(
						"Height"));
				setSize(width, height);
				
				int x = Integer.parseInt(properties.getProperty(
						"X"));
				int y = Integer.parseInt(properties.getProperty(
						"Y"));
				setLocation(x, y);
			} catch (NumberFormatException ex) {
				System.out.println("Fail to parse number of properties.");
				System.out.println(ex.getMessage());
				ex.printStackTrace();
			}
			
			String selectedFilePath = properties.getProperty("FileChooserPath");
			if (selectedFilePath != null) {
				fileChooser.setSelectedFile(new File(selectedFilePath));
			}
		}
		
	}
	
	private class AddFileMenuItemActionListener implements ActionListener {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			if (JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(null)) {
				File[] files = fileChooser.getSelectedFiles();
				hashFile(files);
			}
		}
	}
	
	private class AddDirMenuItemActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if (JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(null)) {
				File[] files = fileChooser.getSelectedFiles();
				hashFile(files);
			}
		}		
	}
	
	private class CancelMenuItemActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			cancellationChecker.cancel();	
		}
	}
	
	private class UpperCaseCheckBoxMenuItemActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			JCheckBoxMenuItem item = (JCheckBoxMenuItem)e.getSource();
			if (item.isSelected()) {
				tableModel.setUpperCase(true);
			} else {
				tableModel.setUpperCase(false);
			}		
		}		
	}
	
	private class ClearMenuItemActionListener implements ActionListener {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			tableModel.clear();
		}
	}

	private class RemoveMenuItemActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			int[] selectedRows = table.getSelectedRows();
			for (int i = selectedRows.length -1; i >= 0; i--) {
				tableModel.remove(selectedRows[i]);
			}
		}		
	}
	
	/*private class PopupListener extends MouseAdapter {
		
		@Override
		public void mousePressed(MouseEvent e) {
			showPopup(e);
		}
		
		@Override
		public void mouseReleased(MouseEvent e) {
			showPopup(e);
		}
		
		private void showPopup(MouseEvent e) {
			if (e.isPopupTrigger()) {
				popupMenu.show(e.getComponent(),
						e.getX(), e.getY());
			}
		}
	}*/

	private class TableDropTargetListener extends DropTargetAdapter {

		@SuppressWarnings("unchecked")
		@Override
		public void drop(DropTargetDropEvent dtde) {
			try {
				if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
					dtde.acceptDrop(DnDConstants.ACTION_LINK);
					
					Object transferData = dtde.getTransferable()
						.getTransferData(DataFlavor.javaFileListFlavor);
					List<File> fileList = (List<File>)transferData;
					hashFile(fileList.toArray(new File[0]));
					
					dtde.dropComplete(true);
					table.updateUI();
				} else {
					dtde.rejectDrop();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			} catch (UnsupportedFlavorException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	private class TableDragSourceListener extends DragSourceAdapter {

		@Override
		public void dragDropEnd(DragSourceDropEvent dsde) {
			if (dsde.getDropSuccess()) {

			} else {
				
			}
		}
	}
	
	private class TableDragGestureListener implements DragGestureListener {

		@Override
		public void dragGestureRecognized(DragGestureEvent dge) {
			int[] selectedRows = table.getSelectedRows();
			List<File> fileList = new ArrayList<File>(selectedRows.length);
			for (int row : selectedRows) {
				fileList.add(tableModel.get(row).getFile());
			}
			FileSelection fileSelection = new FileSelection(fileList);
			dge.startDrag(DragSource.DefaultMoveDrop, fileSelection,
					new TableDragSourceListener());
		}
	}
	
	private class HashFileListener implements HashProgressChangedListener {

		@Override
		public void Report(HashProgressChangedEvent e) {
			int progressValue = (int)(progressBar.getMaximum() * 
					e.getUpdatedLength() / e.getFileLength());
			progressBar.setValue(progressValue);
		}		
	}
	
	private class HashFileCancellationChecker implements CancellationChecker {

		private boolean isCancelled;
		
		public HashFileCancellationChecker() {
			isCancelled = false;
		}
		
		@Override
		public boolean isCancelled() {
			return isCancelled;
		}
		
		public void cancel() {
			isCancelled = true;
		}
		
		public void resume() {
			isCancelled = false;			
		}
	}
	
	private class HashThread extends Thread {
		
		public HashThread() {
		}
		
		public void run() {
			while (true) {
				// Retrieves files from queue until the queue is empty.
				
				File[] files = null;
				
				synchronized (fileQueue) {
					if (!fileQueue.isEmpty()) {
						files = new File[fileQueue.size()];
						fileQueue.toArray(files);
						fileQueue.clear();
					} else {
						try {
							fileQueue.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				
				if (files != null) {
					progressLabel.setVisible(true);
					progressBar.setVisible(true);
					
					execute(files);		

					progressBar.setVisible(false);
					progressLabel.setVisible(false);	
				}
			}
		}
		
		private void execute(File[] files) {
			if (cancellationChecker.isCancelled()) {
				return;
			}
			
			HashedFile hashedFile = null;
			for (File file : files) {
				if (file.isDirectory()) {
					execute(file.listFiles());
				}
				
				if (file.isFile()) {
					if (tableModel.contains(file.getPath())) {
						continue;
					}
					
					progressLabel.setText(file.getName());
					
					hashedFile = new HashedFile(file);
					hashedFile.setCancellationChecker(cancellationChecker);
					hashedFile.addProgressChangedListener(hashFileListener);
					
					try {
						hashedFile.calculateHash();
					} catch (Exception e) {
						JOptionPane.showConfirmDialog(null, e.getStackTrace(),
								"Fail to hash file", JOptionPane.ERROR_MESSAGE);
					}
					
					if (cancellationChecker.isCancelled()) {
						break;
					} else {
						tableModel.add(hashedFile);
					}
					
					progressLabel.setText("");
				}
			}
		}
	}
}
