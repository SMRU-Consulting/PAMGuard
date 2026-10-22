package noiseOneBand;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import org.json.JSONObject;

import PamController.*;
import PamController.soundMedium.GlobalMediumManager;
import PamView.PamTabPanel;
import noiseOneBand.offline.OneBandSummaryTask;
import offlineProcessing.DataCopyTask;
import offlineProcessing.OLProcessDialog;
import offlineProcessing.OfflineTaskGroup;

public class OneBandControl extends PamControlledUnit implements PamSettings {
	/**
	 * The number of measures taken by {@link OneBandControl}.
	 * See {@link OneBandControl#getMeasurementName(int)} for description and units of each measure.
	 */
	public static final int NMEASURES = 4;

	private final OneBandProcess oneBandProcess;
	private final OneBandPulseProcess pulseProcess;
	protected OneBandParameters oneBandParameters = new OneBandParameters();
	private final OneBandTabPanel dBHtTabPanel;
	private OfflineTaskGroup offlineTaskGroup;
	private OLProcessDialog olProcessDialog;
	
	public OneBandControl(String unitName) {
		super("NoiseBand", unitName);
		addPamProcess(oneBandProcess = new OneBandProcess(this));
		addPamProcess(pulseProcess = new OneBandPulseProcess(this));
		dBHtTabPanel = new OneBandTabPanel(this);
		PamSettingManager.getInstance().registerSettings(this);
		oneBandProcess.setupProcess();
		if (isViewer) {
			setToolbarComponent(new DbHtToolbar());
		}
	}

	/**
	 * @return the dbHtProcess
	 */
	public OneBandProcess getOneBandProcess() {
		return oneBandProcess;
	}

	/**
	 * @return the dbHtParameters
	 */
	public OneBandParameters getParameters() {
		return oneBandParameters;
	}

	/* (non-Javadoc)
	 * @see PamController.PamControlledUnit#getTabPanel()
	 */
	@Override
	public PamTabPanel getTabPanel() {
		return dBHtTabPanel;
	}

	/**
	 * Gets the measurement name for a given index (0 <= index < {@link OneBandControl#NMEASURES}).
	 * This requires access to the Global Medium Manager to report on the correct units.
	 *
	 * @param index The index of the measure.
	 * @return The text representation of the  relevant measure, including units.
	 */
	public static String getMeasurementName(int index) {
		GlobalMediumManager gmm = PamController.getInstance().getGlobalMediumManager();
		String dbRef = gmm.getdBRefString();
		String selDbRef = gmm.getdBSELString();

        switch (index) {
            case 0:
                return String.format("RMS (%s)", dbRef);
            case 1:
                return String.format("0-Peak (%s)", dbRef);
            case 2:
                return String.format("Peak-Peak (%s)", dbRef);
            case 3:
                return String.format("Integrated SEL (%s)", selDbRef);
            default:
                throw new IllegalStateException("Unexpected value: " + index);
        }
	}

	/* (non-Javadoc)
	 * @see PamController.PamControlledUnit#createDetectionMenu(java.awt.Frame)
	 */
	@Override
	public JMenuItem createDetectionMenu(Frame parentFrame) {
		JMenuItem menuItem = new JMenuItem(getUnitName() + " settings...");
		menuItem.addActionListener(new SettingsMenu(parentFrame));
		return menuItem;
	}
	
	@Override
	public JMenuItem createDisplayMenu(Frame parentFrame) {
		return dBHtTabPanel.createDisplayMenu(parentFrame);
	}

	private class SettingsMenu implements ActionListener {

		Frame parentFrame;
		public SettingsMenu(Frame parentFrame) {
			this.parentFrame = parentFrame;
		}
		@Override
		public void actionPerformed(ActionEvent arg0) {
			settingsMenuAction(parentFrame);
		}
		
	}
	
	private class DbHtToolbar extends JPanel {

		public DbHtToolbar() {
			super();
			setLayout(new FlowLayout(FlowLayout.LEFT));
			JButton b = new JButton("Export Data ...");
			add(b);
			b.addActionListener(new OfflineTaskAction());
		}
		
	}
	
	private class OfflineTaskAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			runOfflineTasks();
		}
	}
	
	private void runOfflineTasks() {
		if (offlineTaskGroup == null) {
			offlineTaskGroup = new OfflineTaskGroup(this, getUnitName());
			offlineTaskGroup.setPrimaryDataBlock(oneBandProcess.getMeasureDataBlock());
			offlineTaskGroup.addTask(new DataCopyTask<OneBandDataUnit>(oneBandProcess.getMeasureDataBlock()));
			OneBandSummaryTask task = new OneBandSummaryTask(this, oneBandProcess.getMeasureDataBlock());
			offlineTaskGroup.addTask(task);
		}
		if (olProcessDialog == null) {
			olProcessDialog = new OLProcessDialog(getGuiFrame(), offlineTaskGroup, "Noise Data Export");
		}
		olProcessDialog.setVisible(true);
	}

	public void settingsMenuAction(Frame parentFrame) {
		OneBandParameters newParams = OneBandDialog.showDialog(this, parentFrame);
		if (newParams != null) {
			oneBandParameters = newParams.clone();
			oneBandProcess.setupProcess();
			dBHtTabPanel.newParams();
		}
	}

	@Override
	public Serializable getSettingsReference() {
		return oneBandParameters;
	}

	@Override
	public long getSettingsVersion() {
		return OneBandParameters.serialVersionUID;
	}

	@Override
	public boolean restoreSettings(
			PamControlledUnitSettings pamControlledUnitSettings) {
		oneBandParameters = ((OneBandParameters) pamControlledUnitSettings.getSettings()).clone();
		return true;
	}

	@Override
	public String getModuleSummary(boolean clear, String format) {
		List<OneBandDataUnit> lastUnitSet = oneBandProcess.getAllLastChannelMeasurements();
		if (lastUnitSet == null) {
			return null;
		}
		if(format.equals("json")) {
			if(oneBandProcess.getNChannelProcesses()==0) {
				return "";
			}
			JSONObject summaryJSON = new JSONObject();
			for (int i = 0; i < oneBandProcess.getNChannelProcesses(); i++) {
				double rms = 0;
				long timestamp = 0;
				double zeroPeak = 0;
				double peakPeak = 0;
				if(i<lastUnitSet.size()) {
					OneBandDataUnit channelUnit = lastUnitSet.get(i);
					if(channelUnit!=null) {
						rms = channelUnit.getRms();
						zeroPeak = channelUnit.getZeroPeak();
						peakPeak = channelUnit.getPeakPeak();
						timestamp = channelUnit.getTimeMilliseconds();
					}
				}
				String jsonString = String.format("{\"rms\":%3.1f,\"zeroPeak\":%3.1f,\"peakPeak\":%3.1f,\"timestamp\":%d,\"intervalSeconds\":%d}", 
						rms,zeroPeak,peakPeak,timestamp,oneBandParameters.measurementInterval);
				summaryJSON.put("channel"+i,new JSONObject(jsonString));
			}
			return summaryJSON.toString();
		}
		
		return super.getModuleSummary(clear, format);
	}

	@Override
	public Object getShortUnitType() {
		return "Noise";
	}

	/**
	 * @return the dbhtPulseProcess
	 */
	public OneBandPulseProcess getPulseProcess() {
		return pulseProcess;
	}

	@Override
	public void notifyModelChanged(int changeType) {
		super.notifyModelChanged(changeType);
		switch (changeType) {
		case PamControllerInterface.INITIALIZATION_COMPLETE:
			dBHtTabPanel.newParams();
			break;
		}
	}
}
