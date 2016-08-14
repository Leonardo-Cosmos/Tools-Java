package com.hashfile.ui;

import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import com.hashfile.security.HashedFile;

/**
 * The table model for display a table of objects of HashedFile class..
 * @author Leonardo
 *
 */
public class HashedFileTableModel extends AbstractTableModel {

	/**
	 * Serial version UID of this class.
	 */
	private static final long serialVersionUID = -3201633510948863362L;

	//private static final int HASHED_FILE_COLUMN_COUNT = 6;
	
	private static final String[] COLUMN_NAMES = 
		new String[] {"File Name", "Directory", "MD5", "SHA1", "SHA-256", "SHA-512", "CRC32"};
	
	private boolean isUpperCase;
	
	private Vector<HashedFile> hashedFiles;
	
	/**
	 * Constructs a new HashedFileTableModel.
	 */
	public HashedFileTableModel() {
		isUpperCase = false;		
		hashedFiles = new Vector<HashedFile>();
	}
	
	@Override
	public int getColumnCount() {
		return COLUMN_NAMES.length;
	}

	@Override
	public int getRowCount() {
		return hashedFiles.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		String result = null;
		
		if (rowIndex >= 0 && rowIndex < hashedFiles.size() &&
				columnIndex >= 0 && columnIndex < COLUMN_NAMES.length) {
			
			HashedFile hashedFile = hashedFiles.get(rowIndex);
			
			switch (columnIndex) {
			case 0:
				result = hashedFile.getFile().getName();
				break;
			
			case 1:
				result = hashedFile.getFile().getParentFile().getPath();
				break;
				
			case 2:
				if (isLowerCase()) {
					result = hashedFile.getHash().getMD5Text().toLowerCase();
				} else {
					result = hashedFile.getHash().getMD5Text().toUpperCase();
				}
				break;
				
			case 3:
				if (isLowerCase()) {
					result = hashedFile.getHash().getSHA1Text().toLowerCase();
				} else {
					result = hashedFile.getHash().getSHA1Text().toUpperCase();
				}
				break;
				
			case 4:
				if (isLowerCase()) {
					result = hashedFile.getHash().getSHA256Text().toLowerCase();
				} else {
					result = hashedFile.getHash().getSHA256Text().toUpperCase();
				}
				break;
				
			case 5:
				if (isLowerCase()) {
					result = hashedFile.getHash().getSHA512Text().toLowerCase();
				} else {
					result = hashedFile.getHash().getSHA512Text().toUpperCase();
				}
				break;
				
			case 6:
				if (isLowerCase()) {
					result = hashedFile.getHash().getCRC32Text().toLowerCase();
				} else {
					result = hashedFile.getHash().getCRC32Text().toUpperCase();
				}
				break;

			default:
				break;
			}
		}
		
		return result;
	}

	@Override
	public int findColumn(String columnName) {
		for (int i = 0; i < COLUMN_NAMES.length; i++) {
			if (COLUMN_NAMES[i].equals(columnName)) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public String getColumnName(int column) {
		if (column >=0 && column < COLUMN_NAMES.length) {
			return COLUMN_NAMES[column];
		} else {
			return null;
		}
	}
	
	/**
	 * Adds the specified HashedFile object at the end of this model.
	 * @param hashedFile the object to add to the model.
	 * @return true.
	 */
	public boolean add(HashedFile hashedFile) {
		hashedFiles.add(hashedFile);
		int rowNumber = hashedFiles.size() - 1;
		fireTableRowsInserted(rowNumber, rowNumber);
		return true;
	}
	
	/**
	 * Removes all elements of this model, leaving it empty. 
	 */
	public void clear() {
		hashedFiles.clear();
		fireTableDataChanged();
	}

	/**
	 * Searches this model for specified file.
	 * @param filePath the file path to look for in this model.
	 * @return true if the file is an element of this model, false otherwise.
	 */
	public boolean contains(String filePath){
		for (HashedFile file : hashedFiles) {
			if (file.getFile().getPath().equals(filePath)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Answers the element at specified location in this model.
	 * @param location the index of the element to return in this model.
	 * @return the element at the specified location.
	 */
	public HashedFile get(int location) {
		return hashedFiles.get(location);
	}
	
	public boolean isLowerCase() {
		return !isUpperCase;
	}
	
	public boolean isUpperCase() {
		return isUpperCase;
	}
	
	/**
	 * Removes the HashedFile object in specified index from this model.
	 * @param index the index of the object to remove from the model.
	 * @return true if the specified object was found, false otherwise.
	 */
	public boolean remove(int index) {
		if (index >= 0 && index < hashedFiles.size()) {
			hashedFiles.remove(index);
			fireTableRowsDeleted(index, index);
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Removes the first occurrence of specified HashedFile object from this model.
	 * @param hashedFile the object to remove from the model.
	 * @return true if the specified object was found, false otherwise.
	 */
	public boolean remove(HashedFile hashedFile) {
		int index = hashedFiles.indexOf(hashedFile);
		return remove(index);
	}
	
	public void setLowerCase(boolean b) {
		isUpperCase = !b;
		fireTableDataChanged();
	}
	
	public void setUpperCase(boolean b) {
		isUpperCase = b;
		fireTableDataChanged();
	}
}
