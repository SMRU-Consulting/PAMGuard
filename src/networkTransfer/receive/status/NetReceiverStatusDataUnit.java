package networkTransfer.receive.status;

import PamController.PamController;
import PamUtils.PamCalendar;
import PamguardMVC.PamDataUnit;

public class NetReceiverStatusDataUnit extends PamDataUnit{
	
	private static PamController pamController;
	
	private long pamguardUptimeMillis;
	private int nBuoysConnected;
	private double cpuUsageAverage;
	private String pamguardRunState;

	public NetReceiverStatusDataUnit(long timeMilliseconds,BuoyStatusDataUnit buoyData) {
		super(timeMilliseconds);
		if(pamController==null) {
			pamController = PamController.getInstance();
		}

	}
	
	private String getPamRunning() {
		int pamStatus = pamController.getPamStatus();
		switch(pamStatus) {
			case PamController.PAM_IDLE:
				return "PAM_IDLE";
			case PamController.PAM_RUNNING:
				return "PAM_RUNNING";
			case PamController.PAM_STALLED:
				return "PAM_STALLED";
			case PamController.PAM_STOPPING:
				return "PAM_STOPPING";
			case PamController.PAM_INITIALISING:
				return "PAM_INITIALIZING";
			default:
				return "UNKNOWN PAM STATE "+pamStatus;
		}
	}
	
	public void setMetrics(BuoyStatusDataUnit buoyData) {
		this.pamguardRunState = getPamRunning();
		this.pamguardUptimeMillis = PamCalendar.getTimeInMillis()-PamCalendar.getSessionStartTime();
		//this.nBuoysConnected = buoyData.
	}

}
