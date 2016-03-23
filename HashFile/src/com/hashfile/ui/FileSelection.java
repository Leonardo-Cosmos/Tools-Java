package com.hashfile.ui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides a collection of objects of java.io.File class for a transfer operation.
 * @author Leonardo
 *
 */
public class FileSelection implements Transferable {

	private DataFlavor[] flavors = {DataFlavor.javaFileListFlavor};
	
	private List<File> fileList;
	
	public FileSelection(File[] files) {
		if (files != null) {
			fileList = new ArrayList<File>(files.length);
			for (File file : files) {
				fileList.add(file);
			}
		} else {
			fileList = new ArrayList<File>(0);
		}
	}
	
	public FileSelection(List<File> fileList) {
		if (fileList != null) {
			this.fileList = fileList;
		} else {
			this.fileList = new ArrayList<File>(0);
		}
	}
	
	@Override
	public Object getTransferData(DataFlavor flavor)
			throws UnsupportedFlavorException, IOException {
		if (flavor.equals(DataFlavor.javaFileListFlavor)) {
			return fileList;
		} else {
			throw new UnsupportedFlavorException(flavor);
		}
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return flavors;
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		if (flavor.equals(DataFlavor.javaFileListFlavor)) {
			return true;
		} else {
			return false;
		}
	}

}
