package extract;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import PamController.PSFXReadWriter;
import PamController.PamControlledUnit;
import PamController.PamSettingManager;
import PamController.PamSettingsGroup;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import binaryFileStorage.BinaryHeader;
import binaryFileStorage.BinaryHeaderAndFooter;
import binaryFileStorage.BinaryOfflineDataMapPoint;
import binaryFileStorage.BinaryStore;
import binaryFileStorage.ModuleFooter;
import binaryFileStorage.ModuleHeader;
import pamguard.GlobalArguments;

public class BinaryFileReader {
	
	private File currentPsfx;
	private ArrayList<File> currentBinaryFileList;
	private Path currentBinaryBaseDir;
	private String currentStreamNameLong;
	private BinaryStore currentBinaryStore;
	
	public BinaryFileReader() {
		currentPsfx = new File("");
	}
	
	public void flushMVC() {
		if(PamController.PamController.getInstance()!=null) {
			PamController.PamController.getInstance().pamStop();
			PamController.PamController.getInstance().pamClose();
			PamController.PamController.getInstance().destroyModel();
		}
		System.gc();
	}
	
	public void setPSFX(File psfxPath) {
		//flushMVC();
		if(currentPsfx.equals(psfxPath)) {
			System.out.println("Loading the same psfx path in memory. Returning.");
			return;
		}
		System.out.println("Loading PSFX: "+psfxPath.toString());
		currentPsfx = psfxPath;
		PamSettingManager.remote_psf = psfxPath.toString();
		//PamController.PamController.create(PamController.PamController.RUN_EXTRACT);
		flushMVC();
		PamSettingsGroup psg = PSFXReadWriter.getInstance().loadFileSettings(psfxPath);
		PamController.PamController.getInstance().loadOldSettings(psg);
		PamControlledUnit unit =  PamController.PamController.getInstance().findControlledUnit("Binary Storage");
		if(unit==null) {
			System.out.println("No binary storage in .psfx file");
			return;
		}
		currentBinaryStore = (BinaryStore) unit;
	}
	
	public ArrayList<PamDataUnit> processBinaryFile(String pgdfPath){
		File pgdf = new File(pgdfPath);		
		currentBinaryStore.getBinaryStoreSettings().setStoreLocation(pgdf.getParent());
		return processPGDF(pgdf);
	}
	
	public ArrayList<PamDataUnit> processBinaryFileFolder(String directoryPath){
		ArrayList<PamDataUnit> unitList = new ArrayList<PamDataUnit>();
		File dir = new File(directoryPath);
		currentBinaryStore.getBinaryStoreSettings().setStoreLocation(dir.toString());
		for(File nextFile:dir.listFiles()) {
			unitList.addAll(processPGDF(nextFile));
		}
		return unitList;
	}
	
	private ArrayList<PamDataUnit> processPGDF(File pgdf){
		BinaryHeaderAndFooter headAndFoot = currentBinaryStore.getFileHeaderAndFooter(pgdf);
		BinaryHeader header = headAndFoot.binaryHeader; 
		//ModuleHeader moduleHeader = headAndFoot.moduleHeaderData;
		String moduleName = header.getModuleName();
		String moduleType = header.getModuleType();
		String streamName = header.getStreamName();
					
		String longDataName = moduleName + ", " + streamName;
		
		//PamController.PamController.getInstance().getDataBlock(null, moduleName);
		PamDataBlock thisBlock = PamController.PamController.getInstance().getDataBlockByLongName(longDataName);
		//thisBlock
		//PamDataBlock thisBlock = module.getPamProcess(1).getOutputDataBlock(0);
		
		ModuleHeader moduleHeader = thisBlock.getBinaryDataSource().sinkModuleHeader(headAndFoot.moduleHeaderData, header);
		ModuleFooter moduleFooter = thisBlock.getBinaryDataSource().sinkModuleFooter(headAndFoot.moduleFooterData, header, moduleHeader);

		BinaryOfflineDataMapPoint point = new BinaryOfflineDataMapPoint(currentBinaryStore,pgdf,header,headAndFoot.binaryFooter,moduleHeader,moduleFooter,null);

		DataLoadSink sink = new DataLoadSink();
		
		currentBinaryStore.loadData(thisBlock, point, 0,System.currentTimeMillis(),sink);
		
		return thisBlock.getDataCopy();
	}
	
	
	
	
}
