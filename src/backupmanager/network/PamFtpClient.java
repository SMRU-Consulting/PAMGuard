package backupmanager.network;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.Proxy;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamguardMVC.PamProcess;
import backupmanager.BackupManager;
import backupmanager.bespoke.BespokeIdentity;
import backupmanager.bespoke.BespokeSettings;

public class PamFtpClient extends PamProcess implements PamSettings{
	
	private FTPSClient ftp;
	private BackupManager backupManager;
	private FTPClientParams ftpParams = new FTPClientParams();
    private boolean badProxy;
	
	
	public PamFtpClient(PamControlledUnit pamControlledUnit) {
		super(pamControlledUnit, null);
		this.backupManager = (BackupManager) pamControlledUnit;
		PamSettingManager.getInstance().registerSettings(this);
	}
	
	public FTPClientParams getParams() {
		return ftpParams;
	}
	
	public int addMenuItems(JMenuItem menu,Frame parentFrame) {
		JMenuItem add = new JMenuItem("Configure FTP Client");
		add.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				FTPClientParams params = PamFtpParamsDialog.showDialog(parentFrame, backupManager, ftpParams);
				if(params!=null) {
					ftpParams = params;
				}
			}
		});
		menu.add(add);
		
		return 1;
	}
	
	public void connect() throws TransferLoginException {
		
		if(this.isConnected()) {
			System.out.println("FTP Client is Already Connected. Will not attempt to connect again.");
			return;
		}
		
		if(this.ftpParams==null) {
			System.out.println("Cannot connect to ftp server because ftp client parameters do not exist.");
			return;
		}
		
		this.ftp = new FTPSClient("TLSv1.2",true);
		
        ftp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
        System.out.println("Attempting FTPS connection");
        try {
        	ftp.setDefaultTimeout(5000);
			ftp.connect(this.ftpParams.host,990);
        	ftp.setSoTimeout(5000);

		} catch (IOException e) {
			e.printStackTrace();
            throw new TransferLoginException("Exception in connecting to FTP Server",e);
		}
        int reply = ftp.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            try {
				ftp.disconnect();
			} catch (IOException e) {
				e.printStackTrace();
			}
            throw new TransferLoginException("Exception in connecting to FTP Server. Code: "+reply+" message: "+ftp.getReplyString(),null);
        }

        try {
			boolean success = ftp.login(this.ftpParams.user, this.ftpParams.password);
			if(!success) {
	            throw new TransferLoginException("Failed to login to FTP server at "+this.ftpParams.host,null);
			}
			boolean fileTypeSuccess = ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
			if(!fileTypeSuccess) {
	            throw new TransferLoginException("Failed to set file type to binary. Error code: "+ftp.getReplyCode()+"\n Message: "+ftp.getReplyString(),null);
			}
		} catch (IOException e) {
			e.printStackTrace();
            throw new TransferLoginException("Exception in connecting to FTP Server",e);
		}
        ftp.enterLocalPassiveMode();
        if(this.badProxy) {
        	ftp.setProxy(Proxy.NO_PROXY);
        }
		
	}
	
	
	public void disconnect() {
		if(this.ftp==null) {
			return;
		}
		 try {
			ftp.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	public boolean isConnected() {
		if(this.ftp==null) {
			return false;
		}
		return ftp.isConnected();
	}

	
	public boolean isOpen() {
		if(this.ftp==null) {
			return false;
		}
		return ftp.isConnected();
	}

	
	public void mkdir(String targetDir) throws IOException, TransferFailedException {
		if(this.ftp==null) {
			throw new TransferFailedException("FTP Client has not been initialized. Will not send data to server.");
		}
		ftp.makeDirectory(targetDir);
	}

	
	public void copyLocalToRemote(String from, String to, String fileName) throws IOException, TransferFailedException {
		if(this.ftp==null) {
			throw new TransferFailedException("FTP Client has not been initialized. Will not send data to server.");
		}
		String fullRemote = to+"/"+fileName;
		Path fullLocal = Paths.get(from,fileName);
		boolean success = ftp.storeFile(fullRemote, new FileInputStream(fullLocal.toFile()));
        if(!success) {
        	throw new TransferFailedException("Transfer failed to server. Keeping in retry bucket.");
        }
	}
	
	public void setBadProxy() {
		this.badProxy = true;
	}

	
	public ArrayList<String> listFileNames(String remotePath) throws IOException, TransferFailedException {
		if(this.ftp==null) {
			throw new TransferFailedException("FTP Client has not been initialized. Cannot list remote files.");
		}
		FTPFile[] files = ftp.listFiles(remotePath);
		ArrayList<String> fileNames = new ArrayList<String>();
		for(FTPFile nextFtpFile:files) {
			fileNames.add(nextFtpFile.getName());
		}
		return fileNames;
          
	}
	

	@Override
	public void pamStart() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pamStop() {
		
	}

	@Override
	public String getUnitName() {
		return backupManager.getUnitName();
	}

	@Override
	public String getUnitType() {
		return "FTP Client";
	}

	@Override
	public Serializable getSettingsReference() {
		return ftpParams;
	}

	@Override
	public long getSettingsVersion() {
		return FTPClientParams.serialVersionUID;
	}

	@Override
	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		ftpParams = (FTPClientParams) pamControlledUnitSettings.getSettings();
		return true;
	}
	

}
