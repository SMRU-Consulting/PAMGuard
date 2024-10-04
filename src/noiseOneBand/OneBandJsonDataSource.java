package noiseOneBand;

import PamguardMVC.PamDataUnit;
import jsonStorage.JSONObjectDataSource;

public class OneBandJsonDataSource extends JSONObjectDataSource<OneBandJsonData>{
	
	public OneBandJsonDataSource() {
		super();
		objectData = new OneBandJsonData();
	}
	
	@Override
	protected void addClassSpecificFields(PamDataUnit pamDataUnit) {
		OneBandDataUnit noiseDu = (OneBandDataUnit) pamDataUnit;
		objectData.rms = noiseDu.getRms();
		objectData.peakpeak = noiseDu.getPeakPeak();
		objectData.sel = noiseDu.getIntegratedSEL();
		objectData.millis = noiseDu.getTimeMilliseconds();
	}

	@Override
	protected void setObjectType(PamDataUnit pamDataUnit) {
		objectData.identifier = -2000;
		
	}

}
