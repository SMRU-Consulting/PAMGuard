package backupmanager.network;

public class TransferFailedException extends Exception{
	
	public TransferFailedException(Exception e) {
		super(e);
	}
	
	public TransferFailedException(String mesage) {
		super(mesage);
	}
	
	public TransferFailedException(String message, Exception e) {
		super(message, e);
	}

}
