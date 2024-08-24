package decimator;

import java.util.ArrayList;
import java.util.Arrays;

import Filters.FilterBand;
import Filters.FilterParams;
import Filters.FilterType;
import PamController.PamControlledUnit;
import PamController.PamController;
import PamDetection.RawDataUnit;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamProcess;
import PamguardMVC.PamRawDataBlock;

/**
 * New decimator processe, based on the DecimatorWorker class
 * which can be use to upsample as well as decimate. 
 * @author dg50
 *
 */
public class DecimatorProcessW extends PamProcess {

	private DecimatorControl decimatorControl;
	
	private DecimatorParams decimatorParams;

	private DecimatorWorker decimatorWorker;

	private PamRawDataBlock outputDataBlock;

	private double decimateFactor = 1;

	private float sourceSampleRate;

	public DecimatorProcessW(DecimatorControl decimatorControl) {
		super(decimatorControl, null);
		this.decimatorControl = decimatorControl;
		this.decimatorParams = decimatorControl.decimatorParams;
		
		addOutputDataBlock(outputDataBlock = new PamRawDataBlock(decimatorControl.getUnitName() + " Data", this,
				0, decimatorControl.decimatorParams.newSampleRate));
	}
	
	public DecimatorProcessW(PamControlledUnit controller, DecimatorParams decimatorParams) {
		super(controller, null);
		this.decimatorParams = decimatorParams;
		
		addOutputDataBlock(outputDataBlock = new PamRawDataBlock(controller.getUnitName()+" Decimator Data", this,
				0, decimatorParams.newSampleRate));

	}

	@Override
	public void pamStart() {
		outputDataBlock.reset();
		if (decimatorWorker != null) {
			decimatorWorker.reset();
		}
	}

	@Override
	public void pamStop() {
		// TODO Auto-generated method stub

	}
	@Override
	public void setSampleRate(float sampleRate, boolean notify) {
		sourceSampleRate = sampleRate;
		//		super.setSampleRate(decimatorControl.decimatorParams.newSampleRate, false);	
		//		this.sourceSampleRate = sampleRate;
		if (decimatorParams != null) {
			super.setSampleRate(decimatorParams.newSampleRate, notify);
		}
	}


	@Override
	public void masterClockUpdate(long milliSeconds, long sampleNumber) {
		super.masterClockUpdate(milliSeconds, (long) (sampleNumber/decimateFactor));
	}

	@Override
	public void prepareProcess() {
		System.out.println("Calling prepare process on decimator");
		super.setupProcess();
		newSettings();
	}
	
	protected synchronized void newSettings() {
		if(this.decimatorControl!=null) {
			this.decimatorParams = decimatorControl.decimatorParams;
		}
		PamRawDataBlock rawDataBlock = PamController.getInstance().
				getRawDataBlock(this.decimatorParams.rawDataSource);
		if (rawDataBlock != getParentDataBlock()) {
			setParentDataBlock(rawDataBlock);
		}
		if (getParentDataBlock() != null) {
			sourceSampleRate = rawDataBlock.getSampleRate();
			if (sourceSampleRate == 0) {
				return;
			}
			this.decimatorParams.channelMap &= getParentDataBlock().getChannelMap();
			outputDataBlock.setChannelMap(this.decimatorParams.channelMap);
			decimateFactor = sourceSampleRate / this.decimatorParams.newSampleRate;
			this.setSampleRate(sourceSampleRate, true);
			setupDecimator();
		}
	}

	@Override
	public void newData(PamObservable o, PamDataUnit arg) {
		RawDataUnit rawDataUnit = (RawDataUnit) arg;
		processData(rawDataUnit);
	}

	private synchronized void setupDecimator() {
		decimatorParams.filterParams = checkFilterParams(decimatorParams.filterParams);
		decimateFactor = sourceSampleRate / decimatorParams.newSampleRate;
		decimatorWorker = new DecimatorWorker(decimatorParams, outputDataBlock.getChannelMap(), sourceSampleRate, decimatorParams.newSampleRate);
		if(this.decimatorControl!=null) {
			this.decimatorControl.decimatorParams = this.decimatorParams;
		}
	}

	/**
	 * Check the filter parameters are sensible. 
	 * @param filterParams existing parameters
	 * @return checked parameters. 
	 */
	private FilterParams checkFilterParams(FilterParams filterParams) {
		if (sourceSampleRate == 0) {
			return filterParams; // don't do anything. 
		}
		DecimatorParams params = decimatorParams;
		if (filterParams == null) {
			filterParams = new FilterParams();
			filterParams.filterType = FilterType.BUTTERWORTH;
			filterParams.filterBand = FilterBand.LOWPASS;
			filterParams.filterOrder = 6;
			filterParams.lowPassFreq = Math.max(1, params.newSampleRate)/2.f;
			params.filterParams = filterParams;
		}
		filterParams.lowPassFreq = Math.min(filterParams.lowPassFreq, sourceSampleRate/2);
		return filterParams;
	}

	private synchronized void processData(RawDataUnit rawDataUnit) {
		if ((rawDataUnit.getChannelBitmap() & outputDataBlock.getChannelMap()) == 0) {
			return;
		}
		RawDataUnit decData = decimatorWorker.process(rawDataUnit);
		if (decData != null) {
			outputDataBlock.addPamData(decData);
		}
	}

	public PamRawDataBlock getOutputDataBlock() {
		return outputDataBlock;
	}
	
	@Override
	public ArrayList getCompatibleDataUnits(){
		return new ArrayList<Class<? extends PamDataUnit>>(Arrays.asList(RawDataUnit.class));
	}

}
