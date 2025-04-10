package generalDatabase.backup;

import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import PamView.dialog.PamDialogPanel;
import backupmanager.BackupManager;
import backupmanager.FileLocation;
import backupmanager.action.ActionMaker;
import backupmanager.action.BackupException;
import backupmanager.action.FtpSettings;
import backupmanager.filter.BackupFilterParams;
import backupmanager.network.TransferFailedException;
import backupmanager.settings.ActionSettings;
import backupmanager.stream.BackupStream;
import backupmanager.stream.StreamItem;
import backupmanager.swing.CopyFileDialogPanel;
import generalDatabase.DBControlUnit;
import generalDatabase.DBSystem;
import generalDatabase.sqlite.SqliteSystem;

public class SQLiteSafeFTPBackup extends SQLCloneDatabase{
	
	protected SQLiteFtpSettings ftpSettings;
	protected String localCloneLocation;

	public SQLiteSafeFTPBackup(ActionMaker actionMaker, BackupStream backupStream) {
		super(actionMaker, backupStream);

	}
	
	@Override
	public boolean doAction(BackupManager backupManager, BackupStream backupStream, StreamItem streamItem) throws BackupException {
		boolean actionSuccess = super.doAction(backupManager,backupStream,streamItem);
		if(!actionSuccess) {
			return false;
		}
		if(this.lastNewDatabasePath==null) {
			throw new BackupException("Could not find path to transfer: "+this.lastNewDatabasePath);
		}
		actionSuccess = ftpClonedSQLiteFile(this.lastNewDatabasePath);
		if(!actionSuccess) {
			return false;
		}
		return deleteLastTmp();
		
	}
	
	@Override
	protected String getLocalDestination() throws BackupException {
		if(this.localCloneLocation==null) {
			setLocalLocation();
		}
		return this.localCloneLocation;
	}
	
	protected void setLocalLocation() throws BackupException {
		try {
			DBControlUnit dbControlUnit = DBControlUnit.findDatabaseControl();
			DBSystem dbSys = dbControlUnit.getDatabaseSystem();
			if(dbSys==null || !(dbSys instanceof SqliteSystem)) {
				throw new BackupException("Could not find valid sqlite system");
			}
			String dbLoc = dbSys.getDatabaseName();
			File dbFileFolder;
			dbFileFolder = new File(dbLoc).getParentFile();
			File targetDirectory = Paths.get(dbFileFolder.toString(),"database_backup").toFile();
			if(!targetDirectory.exists()) {
				targetDirectory.mkdirs();
			}
			
			this.localCloneLocation = targetDirectory.getAbsolutePath();
		}catch(Exception e) {
			e.printStackTrace();
			throw new BackupException("No destination folder set");
		}
	}
	
	public boolean deleteLastTmp() {
		if(this.lastNewDatabasePath==null) {
			return false;
		}
		File lastTmpFile = new File(this.lastNewDatabasePath);
		if(lastTmpFile.exists()) {
			lastTmpFile.delete();
			return true;
		}
		return false;
	}
	
	public boolean ftpClonedSQLiteFile(String clonedFileToTransfer) throws BackupException {
		String destination = ftpSettings.destLocation.path;


		File srcFile = new File(clonedFileToTransfer);
		if (srcFile.exists() == false) {
			throw new BackupException("Source file " + clonedFileToTransfer + " doesn't exist");
		}
		
		String destinationDir = destination+"/"+srcFile.getParentFile().getName();
		
		return fileAction(srcFile, destinationDir);
	}
	
	protected boolean fileAction(File source, String destDir) throws BackupException {
		try {
			BackupManager.getBackupManager().getFtpClient().mkdir(destDir);
		}catch(IOException | TransferFailedException e) {
			System.out.println("Directory already exists");
		}
		try {
			BackupManager.getBackupManager().getFtpClient().copyLocalToRemote(source.getParent(), destDir, source.getName());
		} catch (IOException | TransferFailedException e) {
			throw new BackupException(e.getMessage());
		}
		return true;
	}
	
	public boolean requiresFTPConnection() {
		return true;
	}
	
	@Override
	public String getName() {
		return "Clone and FTP db to " + ftpSettings;
	}
	
	@Override
	public ActionSettings getSettings() {
		if (ftpSettings == null) {
			ftpSettings = new SQLiteFtpSettings(this.getClass().getName());
		}
		return ftpSettings;
	}

	@Override
	public boolean setSettings(ActionSettings settings) {
		if (settings instanceof SQLiteFtpSettings) {
			this.ftpSettings = (SQLiteFtpSettings) settings;
		}
		else {
			return false;
		}
		BackupFilterParams backupFilterParams = settings.getBackupFilterParams();
		if (backupFilterParams != null && getBackupFilter() != null) {
			getBackupFilter().setFilterParams(backupFilterParams);
		}
		return true;
	}
	
	@Override
	public PamDialogPanel getDialogPanel(Window owner) {
		return new SQLiteFtpDialog(this, owner);
	}

	
	/**
	 * @return the copySettings
	 */
	public SQLiteFtpSettings getFtpSettings() {
		if (getBackupFilter() != null & ftpSettings != null) {
			ftpSettings.setBackupFilterParams(getBackupFilter().getFilterParams());
		}
		return ftpSettings;
	}

	/**
	 * @param copySettings the copySettings to set
	 */
	public void setFtpSettings(SQLiteFtpSettings ftpSettings) {
		this.ftpSettings = ftpSettings;
	}
}
