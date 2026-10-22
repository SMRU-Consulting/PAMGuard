package AIS;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.json.JSONObject;

import NMEA.NMEADataBlock;
import NMEA.NMEADataUnit;
import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamSettingManager;
import PamController.PamSettings;

public class AISControl extends PamControlledUnit implements PamSettings {

	ProcessAISData aisProcess;

	AISControl aisControl;

	NMEADataBlock nmeaDataBlock;

	AISParameters aisParameters = new AISParameters();

	public AISControl(String unitName) {
		super("AIS Processing", unitName);
		aisControl = this;
		addPamProcess(aisProcess = new ProcessAISData(this));

		PamSettingManager.getInstance().registerSettings(this);

		aisProcess.findNMEADataBlock();

		// REad in some AIS data which caused a crash to debug. 
		//		AISBugSearch bs = new AISBugSearch();
		//		bs.runTestData(this);
	}

	class AISShipData implements ActionListener {
		Frame parentFrame;
		public AISShipData(Frame parentFrame) {
			super();
			this.parentFrame = parentFrame;
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			AISStringsTable.show(aisControl);
		}
	}

	class AISDisplayOptions implements ActionListener {
		Frame parentFrame;
		public AISDisplayOptions(Frame parentFrame) {
			super();
			this.parentFrame = parentFrame;
		}
		@Override
		public void actionPerformed(ActionEvent e) {

			AISParameters newParams = AISDisplayDialog.showDialog(parentFrame, aisParameters);
			if (newParams != null) {
				aisParameters = newParams.clone();
			}
		}
	}

	class AISSettings implements ActionListener {
		Frame parentFrame;
		public AISSettings(Frame parentFrame) {
			super();
			this.parentFrame = parentFrame;
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			AISParameters newParams = AISSettingsDialog.showDialog(parentFrame, aisParameters);
			if (newParams != null) {
				aisParameters = newParams.clone();
				aisProcess.findNMEADataBlock();
			}
		}
	}

	public JMenuItem createAISMenu(Frame parentFrame) {
		JMenuItem menuItem;
		JMenu subMenu = new JMenu("AIS Ship Reporting");
		menuItem = new JMenuItem("AIS Options ...");
		menuItem.addActionListener(new AISSettings(parentFrame));
		subMenu.add(menuItem);
		menuItem = new JMenuItem("Show Data ...");
		menuItem.addActionListener(new AISShipData(parentFrame));
		subMenu.add(menuItem);
		return subMenu;
	}

	@Override
	public JMenuItem createDetectionMenu(Frame parentFrame) {
		return createAISMenu(parentFrame);
	}

	@Override
	public JMenuItem createDisplayMenu(Frame parentFrame) {
		JMenuItem menuItem;
		JMenu subMenu = new JMenu("AIS Ship Reporting");
		menuItem = new JMenuItem("Display Options ...");
		menuItem.addActionListener(new AISDisplayOptions(parentFrame));
		subMenu.add(menuItem);
		menuItem = new JMenuItem("Show All Ship Data ...");
		menuItem.addActionListener(new AISShipData(parentFrame));
		subMenu.add(menuItem);
		return subMenu;
	}

	@Override
	public void notifyModelChanged(int changeType) {
		super.notifyModelChanged(changeType);
		aisProcess.noteNewSettings();
	}
	
	@Override
	public String getModuleSummary(boolean clear, String format) {
		long count = aisProcess.getAisMessageCountSummary(clear);
		if(format.equals("json")) {
			JSONObject summaryJson = new JSONObject();
			AISDataUnit lastUnit = aisProcess.getOutputDataBlock().getLastUpdatedUnit();
			if(lastUnit==null) {
				summaryJson.put("lastAisMessage", "No last AIS message available.");
			}else {
				JSONObject lastVesselJson = new JSONObject();
				lastVesselJson.put("Note", "this is a summary, only reporting the last ais data unit at the time requested.");
				lastVesselJson.put("LastDataTime", lastUnit.getTimeMilliseconds());
				lastVesselJson.put("MMSI", lastUnit.mmsiNumber);
				if(lastUnit.getPositionReport()!=null) {
					lastVesselJson.put("hasPosReport", true);
					lastVesselJson.put("SOG", lastUnit.getPositionReport().speedOverGround);
					lastVesselJson.put("COG", lastUnit.getPositionReport().courseOverGround);
					lastVesselJson.put("lat", lastUnit.getPositionReport().getLatitude());
					lastVesselJson.put("lon", lastUnit.getPositionReport().getLongitude());
				}else {
					lastVesselJson.put("hasPosReport", false);
				}
				if(lastUnit.getStaticData()!=null) {
					lastVesselJson.put("hasStaticReport", true);
					lastVesselJson.put("name", lastUnit.getStaticData().shipName);
					lastVesselJson.put("IMO", lastUnit.getStaticData().imoNumber);
					lastVesselJson.put("shipType", lastUnit.getStaticData().shipType);
					lastVesselJson.put("shipName", lastUnit.getStaticData().shipName);

				}else {
					lastVesselJson.put("hasStaticReport", false);
				}
				summaryJson.put("lastAisMessage", lastVesselJson);
			}
			summaryJson.put("aisMessageCount", count);
			
//			aisProcess.getOutputDataBlock().get
			
			return summaryJson.toString();
		}
		return "";
	}

	@Override
	public Serializable getSettingsReference() {
		return aisParameters;
	}

	@Override
	public long getSettingsVersion() {
		return AISParameters.serialVersionUID;
	}

	@Override
	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {

		this.aisParameters = ((AISParameters) pamControlledUnitSettings.getSettings()).clone();

		return true;
	}
}
