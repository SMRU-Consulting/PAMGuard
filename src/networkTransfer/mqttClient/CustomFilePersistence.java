/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    https://www.eclipse.org/legal/epl-2.0
 * and the Eclipse Distribution License is available at 
 *   https://www.eclipse.org/org/documents/edl-v10.php
 *
 * Contributors:
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
package networkTransfer.mqttClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttPersistable;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.internal.FileLock;
import org.eclipse.paho.client.mqttv3.internal.MqttPersistentData;
import org.eclipse.paho.client.mqttv3.persist.PersistanceFileFilter;
import org.eclipse.paho.client.mqttv3.persist.PersistanceFileNameFilter;

/**
 * An implementation of the {@link MqttClientPersistence} interface that provides
 * file based persistence.
 * 
 * A directory is specified when the Persistence object is created. When the persistence
 * is then opened (see {@link #open(String, String)}), a sub-directory is made beneath the base
 * for this client ID and connection key. This allows one persistence base directory
 * to be shared by multiple clients.
 * 
 * The sub-directory's name is created from a concatenation of the client ID and connection key
 * with any instance of '/', '\\', ':' or ' ' removed.
 */
public class CustomFilePersistence implements MqttClientPersistence {
	private static final String MESSAGE_FILE_EXTENSION = ".msg";
	private static final String MESSAGE_BACKUP_FILE_EXTENSION = ".bup";
	private static final String LOCK_FILENAME = ".lck"; 

	private File dataDir;
	private File clientDir = null;
	private FileLock fileLock = null;
	
	//TODO
	private static FilenameFilter FILENAME_FILTER;
	
	private static FilenameFilter getFilenameFilter(){
		if(FILENAME_FILTER == null){
			FILENAME_FILTER =  new PersistanceFileNameFilter(MESSAGE_FILE_EXTENSION);
		}
		return FILENAME_FILTER;
	}
	
	public CustomFilePersistence()  { //throws MqttPersistenceException {
		this(System.getProperty("user.dir"));
	}
	
	/**
	 * Create an file-based persistent data store within the specified directory.
	 * @param directory the directory to use.
	 */
	public CustomFilePersistence(String directory) { //throws MqttPersistenceException {
		dataDir = new File(directory);
	}
	
	public void open(String clientId, String theConnection) throws MqttPersistenceException {
		
		if (dataDir.exists() && !dataDir.isDirectory()) {
			throw new MqttPersistenceException();
		} else if (!dataDir.exists() ) {
			if (!dataDir.mkdirs()) {
				throw new MqttPersistenceException();
			}
		} 
		if (!dataDir.canWrite()) {
			throw new MqttPersistenceException();
		}
		
		
		StringBuffer keyBuffer = new StringBuffer();
		for (int i=0;i<clientId.length();i++) {
			char c = clientId.charAt(i);
			if (isSafeChar(c)) {
				keyBuffer.append(c);
			}
		}
		keyBuffer.append("-");
		for (int i=0;i<theConnection.length();i++) {
			char c = theConnection.charAt(i);
			if (isSafeChar(c)) {
				keyBuffer.append(c);
			}
		}

		synchronized (this) {
			if (clientDir == null) {
				String key = keyBuffer.toString();
				clientDir = new File(dataDir, key);

				if (!clientDir.exists()) {
					clientDir.mkdir();
				}
			}

			try {
				//If lock was previously acquired, release before requesting a new one
				if(fileLock != null){
					fileLock.release();
				}

				fileLock = new FileLock(clientDir, LOCK_FILENAME);
	 		} catch (Exception e) {
	 			// TODO - This shouldn't be here according to the interface
	 			// See https://github.com/eclipse/paho.mqtt.java/issues/178
				//throw new MqttPersistenceException(MqttPersistenceException.REASON_CODE_PERSISTENCE_IN_USE);
			}

			// Scan the directory for .backup files. These will
			// still exist if the JVM exited during addMessage, before
			// the new message was written to disk and the backup removed.
			restoreBackups(clientDir);
		}
	}

	/**
	 * Checks whether the persistence has been opened.
	 * @throws MqttPersistenceException if the persistence has not been opened.
	 */
	public void checkIsOpen() throws MqttPersistenceException {
		if (clientDir == null) {
			throw new MqttPersistenceException();
		}
	}

	public void close() throws MqttPersistenceException {
		
		synchronized (this) {
			// checkIsOpen();
			if (fileLock != null) {
				fileLock.release();
			}

			if (getFiles().length == 0) {
				clientDir.delete();
			}
			clientDir = null;
		}
	}

	/**
	 * Writes the specified persistent data to the previously specified persistence directory.
	 * This method uses a safe overwrite policy to ensure IO errors do not lose messages.
	 * @param message The {@link MqttPersistable} message to be persisted
	 * @throws MqttPersistenceException if an exception occurs whilst persisting the message
	 */
	public void put(String key, MqttPersistable message) throws MqttPersistenceException {
		checkIsOpen();
		File file = new File(clientDir, key+MESSAGE_FILE_EXTENSION);
		File backupFile = new File(clientDir, key+MESSAGE_FILE_EXTENSION+MESSAGE_BACKUP_FILE_EXTENSION);
		
		if (file.exists()) {
			// Backup the existing file so the overwrite can be rolled-back 
			boolean result = file.renameTo(backupFile);
			if (!result) {
				backupFile.delete();
				file.renameTo(backupFile);
			}
		}
		try {
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(message.getHeaderBytes(), message.getHeaderOffset(), message.getHeaderLength());
			if (message.getPayloadBytes()!=null) {
				fos.write(message.getPayloadBytes(), message.getPayloadOffset(), message.getPayloadLength());
			}
			fos.getFD().sync();
			fos.close();
			if (backupFile.exists()) {
				// The write has completed successfully, delete the backup 
				backupFile.delete();
			}
		}
		catch (IOException ex) {
			throw new MqttPersistenceException(ex);
		} 
		finally {
			if (backupFile.exists()) {
				// The write has failed - restore the backup
				boolean result = backupFile.renameTo(file);
				if (!result) {
					file.delete();
					backupFile.renameTo(file);
				}
			}
		}
	}

	public MqttPersistable get(String key) throws MqttPersistenceException {
		checkIsOpen();
		MqttPersistable result;
		try {
			File file = new File(clientDir, key+MESSAGE_FILE_EXTENSION);
			FileInputStream fis = new FileInputStream(file);
			int size = fis.available();
			byte[] data = new byte[size];
			int read = 0;
			while (read<size) {
				read += fis.read(data,read,size-read);
			}
			fis.close();
			result = new MqttPersistentData(key, data, 0, data.length, null, 0, 0);
		} 
		catch(IOException ex) {
			throw new MqttPersistenceException(ex);
		}
		return result;
	}


	/**
	 * Deletes the data with the specified key from the previously specified persistence directory.
	 */
	public void remove(String key) throws MqttPersistenceException {
		checkIsOpen();
		File file = new File(clientDir, key+MESSAGE_FILE_EXTENSION);
		if (file.exists()) {
			file.delete();
		}
	}
	
	/**
	 * Returns all of the persistent data from the previously specified persistence directory.
	 * @return all of the persistent data from the persistence directory.
	 * @throws MqttPersistenceException if an exception is thrown whilst getting the keys
	 */
	public Enumeration<String> keys() throws MqttPersistenceException {
		checkIsOpen();
		File[] files = getFiles();
		Vector<String> result = new Vector<String>(files.length);
		for (File file : files) {
			String filename = file.getName();
			String key = filename.substring(0, filename.length() - MESSAGE_FILE_EXTENSION.length());
			result.addElement(key);
		}
		return result.elements();
	}
	
	private File[] getFiles() throws MqttPersistenceException {
		checkIsOpen();
		File[] files = clientDir.listFiles(getFilenameFilter());
		if (files == null) {
			throw new MqttPersistenceException();
		}
		return files;
	}
	
	private boolean isSafeChar(char c) {
		return Character.isJavaIdentifierPart(c) || c=='-';
	}
	
	/**
	 * Identifies any backup files in the specified directory and restores them
	 * to their original file. This will overwrite any existing file of the same
	 * name. This is safe as a stray backup file will only exist if a problem
	 * occurred whilst writing to the original file.
	 * @param dir The directory in which to scan and restore backups
	 */
	private void restoreBackups(File dir) throws MqttPersistenceException {
		File[] files = dir.listFiles(new PersistanceFileFilter(MESSAGE_BACKUP_FILE_EXTENSION));

		if (files == null) {
			throw new MqttPersistenceException();
		}

		for (File file : files) {
			File originalFile = new File(dir, file.getName().substring(0, file.getName().length() - MESSAGE_BACKUP_FILE_EXTENSION.length()));
			boolean result = file.renameTo(originalFile);
			if (!result) {
				originalFile.delete();
				file.renameTo(originalFile);
			}
		}
	}

	public boolean containsKey(String key) throws MqttPersistenceException {
		checkIsOpen();
		File file = new File(clientDir, key+MESSAGE_FILE_EXTENSION);
		return file.exists();
	}

	public void clear() throws MqttPersistenceException {
		checkIsOpen();
		File[] files = getFiles();
		for (File file : files) {
			file.delete();
		}
		clientDir.delete();
	}
}
