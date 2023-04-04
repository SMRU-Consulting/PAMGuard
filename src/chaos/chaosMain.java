package chaos;

import java.io.File;

import PamController.PSFXReadWriter;
import PamController.PamGUIManager;
import PamController.PamSettingManager;
import PamController.PamSettingsGroup;
import PamguardMVC.ThreadedObserverRepository;

public class chaosMain {
	
	
	static String psfx1 = "C:\\Users\\12066\\git\\cloud-software-development\\SMRUDevManage\\src\\test\\resources\\psfx\\LIMEKILN_PB328.psfx";
	static String psfx2 = "C:\\Users\\12066\\git\\cloud-software-development\\SMRUDevManage\\src\\test\\resources\\psfx\\LimekilnClicks_Baseline.psfx";
	
	public static void main(String [] args) {
		PamGUIManager.setType(PamGUIManager.NOGUI);
		
		PamSettingManager.remote_psf = psfx1;

		PamController.PamController.create(PamController.PamController.RUN_NOTHING);

		
		PamController.PamController.getInstance().pamStop();
		PamController.PamController.getInstance().pamClose();
		PamController.PamController.getInstance().destroyModel();
		ThreadedObserverRepository.getInstance().destroyAllObservers();
		
		int runTimes = 1000;
		
		while(runTimes>0) {
			PamSettingsGroup psg = PSFXReadWriter.getInstance().loadFileSettings(new File(psfx2));
			PamController.PamController.getInstance().loadOldSettings(psg);
			
			PamController.PamController.getInstance().pamStop();
			PamController.PamController.getInstance().pamClose();
			PamController.PamController.getInstance().destroyModel();
			ThreadedObserverRepository.getInstance().destroyAllObservers();
			
			PamSettingsGroup psg2 = PSFXReadWriter.getInstance().loadFileSettings(new File(psfx1));
			PamController.PamController.getInstance().loadOldSettings(psg2);
			
			PamController.PamController.getInstance().pamStop();
			PamController.PamController.getInstance().pamClose();
			PamController.PamController.getInstance().destroyModel();
			ThreadedObserverRepository.getInstance().destroyAllObservers();
			
		    float usedPer = (float)(Runtime.getRuntime().maxMemory()-Runtime.getRuntime().freeMemory())/(float)Runtime.getRuntime().maxMemory();

			System.out.println("Cleaning garbage. Before clean. Free mem:"+Runtime.getRuntime().freeMemory()
					+", Max mem: "+Runtime.getRuntime().maxMemory()
					+", Heap size: "+Runtime.getRuntime().totalMemory()
					+", Used mem percent: "+usedPer);
			
			if(runTimes==2) {
				int x= 0;
			}
			
		}
		
		
		
		
		

	}
}
