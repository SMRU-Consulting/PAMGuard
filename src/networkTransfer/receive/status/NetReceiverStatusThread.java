package networkTransfer.receive.status;

import PamController.PamControlledUnit;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamProcess;
import PamguardMVC.String;

public class NetReceiverStatusManager extends PamProcess {
	
	NetworkReceiver netReceiver;

	public NetReceiverStatusManager(PamControlledUnit pamControlledUnit) {
		super(pamControlledUnit, null);
		netReceiver = (NetworkReceiver) netReceiver;
	}

	@Override
	public String getObserverName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void pamStart() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pamStop() {
		// TODO Auto-generated method stub
		
	}

}
