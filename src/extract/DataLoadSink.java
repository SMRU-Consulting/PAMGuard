package extract;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.background.BackgroundDataUnit;
import binaryFileStorage.BinaryDataSink;
import binaryFileStorage.BinaryFooter;
import binaryFileStorage.BinaryHeader;
import binaryFileStorage.BinaryObjectData;
import binaryFileStorage.BinaryTypes;
import binaryFileStorage.ModuleFooter;
import binaryFileStorage.ModuleHeader;

public class DataLoadSink implements BinaryDataSink {

		@Override
		public void newFileHeader(BinaryHeader binaryHeader) {
		}

		@Override
		public void newModuleHeader(BinaryObjectData binaryObjectData, ModuleHeader moduleHeader) {
		}

		@Override
		public void newModuleFooter(BinaryObjectData binaryObjectData, ModuleFooter moduleFooter) {
		}

		@Override
		public void newFileFooter(BinaryObjectData binaryObjectData, BinaryFooter binaryFooter) {
		}

		@Override
		public boolean newDataUnit(BinaryObjectData binaryObjectData, PamDataBlock dataBlock, PamDataUnit dataUnit) {
			if (binaryObjectData.getObjectType() == BinaryTypes.BACKGROUND_DATA) {
				dataBlock.getBackgroundManager().addData((BackgroundDataUnit) dataUnit);
				return true;
			}
			else {
				dataBlock.addPamData(dataUnit, dataUnit.getUID());
				return true;
			}
		}

		@Override
		public void newDatagram(BinaryObjectData binaryObjectData) {
		}
		
	}