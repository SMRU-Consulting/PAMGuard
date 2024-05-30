package netTxControl_2;

public class NetTransmitException extends Exception {

	public NetTransmitException(String message, Exception e) {
		super(message,e);
	}
	
	public NetTransmitException(Exception e) {
		super(e);
	}
}
