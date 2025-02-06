package networkTransfer.receive.status;

import PamguardMVC.PamDataUnit;

public class NetReceiverStatusDataUnit extends PamDataUnit{
	
	private long pamguardUptimeMillis;
	private int nBuoysConnected;
	private double cpuUsageAverage;
	private String pamguardRunState;

	public NetReceiverStatusDataUnit(long timeMilliseconds) {
		super(timeMilliseconds);

	}

}
