package networkTransfer.receive;

import java.io.File;

import PamController.PamControlledUnitSettings;
import PamController.PamSettingManager;
import PamController.PamSettingsGroup;
import networkTransfer.NetworkParams;
import networkTransfer.send.NetworkSender;

public class NetReceiveConfigBuilder {
	
	public static void duplicateFileForNetReceive(String filePath) {
		File standardConfigFilePath = new File(filePath);
		PamSettingsGroup pamSettings = PamSettingManager.getInstance().loadSettings(standardConfigFilePath);
		PamControlledUnitSettings netSendParamSet = null;
		for(PamControlledUnitSettings unitSettings:pamSettings.getUnitSettings()) {
			if(unitSettings.getUnitType().equals(NetworkSender.UNIT_TYPE)) {
				netSendParamSet = unitSettings;
				break;
			}
		}
		if(netSendParamSet==null) {
			return;
		}
		
		NetworkReceiveParams networkReceiveParams = (NetworkReceiveParams) ((NetworkParams) netSendParamSet.getSettings()).clone();
		networkReceiveParams.stationId = "BaseStation";
		
		PamSettingsGroup newSettingsGroup = new PamSettingsGroup(System.currentTimeMillis());
		for(PamControlledUnitSettings unitSettings:pamSettings.getUnitSettings()) {
			if(unitSettings.getUnitType().equals(NetworkSender.UNIT_TYPE)) {
				continue;
			}
			newSettingsGroup.addSettings(unitSettings);
		}
		
//		PamControlledUnitSettings netReceiveUnitSettings = new PamControlledUnitSettings(networkReceiveParams);
//		newSettingsGroup.addSettings();
		
	}
	
	

}
