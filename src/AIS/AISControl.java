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
		if(format.equals("json")) {
			AISDataUnit lastUnit = aisProcess.getOutputDataBlock().getLastUnit();
			if(lastUnit==null) {
				return "";
			}
			JSONObject json = new JSONObject();
			json.put("Note", "this is a summary, only reporting the last ais unit at the time requested.");
			json.put("LastDataTime", lastUnit.getTimeMilliseconds());
			json.put("MMSI", lastUnit.mmsiNumber);
			if(lastUnit.getPositionReport()!=null) {
				json.put("hasPosReport", true);
				json.put("SOG", lastUnit.getPositionReport().speedOverGround);
				json.put("COG", lastUnit.getPositionReport().courseOverGround);
				json.put("lat", lastUnit.getPositionReport().getLatitude());
				json.put("lon", lastUnit.getPositionReport().getLongitude());
			}else {
				json.put("hasPosReport", false);
			}
			if(lastUnit.getStaticData()!=null) {
				json.put("hasStaticReport", true);
				json.put("name", lastUnit.getStaticData().shipName);
				json.put("IMO", lastUnit.getStaticData().imoNumber);
				json.put("shipType", lastUnit.getStaticData().shipType);
			}else {
				json.put("hasStaticReport", false);
			}
			
			return json.toString();
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
