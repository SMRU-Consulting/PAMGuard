package export.MLExport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import PamguardMVC.PamDataUnit;
import export.PamDataUnitExporter;
import us.hebi.matlab.mat.format.Mat5;
import us.hebi.matlab.mat.format.Mat5File;
import us.hebi.matlab.mat.types.Matrix;
import us.hebi.matlab.mat.types.Sink;
import us.hebi.matlab.mat.types.Sinks;
import us.hebi.matlab.mat.types.Struct;
import us.hebi.matlab.mat.util.Casts;


/**
 * Handles the conversion of data units into MATLAB structures. 
 * @author Jamie Macaulay 
 *
 */
public class MLDetectionsManager implements PamDataUnitExporter {
	
	public static final String extension = "mat";

	/**
	 * 
	 * All the possible MLDataUnitExport export classes. 
	 */
	ArrayList<MLDataUnitExport> mlDataUnitsExport = new ArrayList<MLDataUnitExport>(); 


	public MLDetectionsManager(){
		mlDataUnitsExport.add(new MLClickExport()); 
		mlDataUnitsExport.add(new MLWhistleMoanExport()); 
		mlDataUnitsExport.add(new MLRawExport()); 
	}
	
	@Override
	public boolean hasCompatibleUnits(Class dataUnitType) {
		for (int i=0; i<mlDataUnitsExport.size(); i++){
			//check whether the same. ;
			//System.out.println(" dataUnits.get(j).getClass(): " + dataUnits.get(j).getClass());
			//System.out.println(" mlDataUnitsExport.get(i).getUnitClass(): " + mlDataUnitsExport.get(i).getUnitClass());
			if (mlDataUnitsExport.get(i).getUnitClass().isAssignableFrom(dataUnitType)) {
				//System.out.println("FOUND THE DATA UNIT!");
				return true; 
			}
		}
		return false;
	}

	@Override
	public boolean exportData(File fileName, List<PamDataUnit> dataUnits, boolean append) {
		
		
		try {
			Mat5File matFile = Mat5.newMatFile();
			Sink sink = Sinks.newMappedFile(fileName,  Casts.sint32(1000000));

			matFile.writeTo(sink);//Streams the data into a MAT file?
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		return false;
	}

	/**
	 * Check whether there are compatible data units to be exported. 
	 * @param dataUnits - the data unit list
	 * @return true if MATLAB export is possible for the current data units. 
	 */
	public boolean hasCompatibleUnits(List<PamDataUnit> dataUnits) {
		//first need to figure out how many data units there are. 
		for (int j=0; j<dataUnits.size(); j++){
			if (hasCompatibleUnits(dataUnits.get(j).getClass())) return true;
		}
		return false; 
	}

	/**
	 * Sort a list of data units into lists of the same type of units. Convert to a list of structures. 
	 * @param dataUnits - a list of data units to convert to matlab structures. 
	 * @return list of list of MATLAB strucutures ready for saving to .mat file. 
	 */
	public Struct dataUnits2MAT(List<PamDataUnit> dataUnits){

		//ArrayList<ArrayList<PamDataUnit>> struct = new ArrayList<ArrayList<PamDataUnit>>(); 		
		//if there's a mixed bunch of data units then we want separate arrays of structures. So a structure of arrays of structures.
		//so, need to sort compatible data units.  		
	
		Struct list = Mat5.newStruct();

		//keep a track of the data units that have been transcribed. This means data units that are multiple types
		//(e.g. a raw data holder and click) are not added to two different list of structures. 
		boolean[] alreadyStruct = new boolean[dataUnits.size()];

		//iterate through possible export functions. 
		for (int i=0; i<mlDataUnitsExport.size(); i++){

			//first need to figure out how many data units there are. 
			int n=0; 
			for (int j=0; j<dataUnits.size(); j++){
				//check whether the same. 
				if (mlDataUnitsExport.get(i).getUnitClass().isAssignableFrom(dataUnits.get(j).getClass())) {
					n++;
				}
			}


			if (n==0) continue; //no need to do anything else. There are no data units of this type. 

			//create a structure for each type of data unit. 
			Struct mlStructure= Mat5.newStruct(new int[]{n, 1});
					
			float sampleRate = -1;
			

			n=0; 
			//allocate the class now. 
			for (int j=0; j<dataUnits.size(); j++){
				//check whether the same. 
				if (mlDataUnitsExport.get(i).getUnitClass().isAssignableFrom(dataUnits.get(j).getClass()) && !alreadyStruct[j]) {
					
					mlStructure=mlDataUnitsExport.get(i).detectionToStruct(mlStructure, dataUnits.get(j), n); 
					
					sampleRate = dataUnits.get(j).getParentDataBlock().getSampleRate(); 
					n++; 
					alreadyStruct[j] = true;
				}
			}

			if (n>=1) {
				list.set(mlDataUnitsExport.get(i).getName(),mlStructure);
				list.set(mlDataUnitsExport.get(i).getName()+"_sR", Mat5.newScalar(sampleRate)); 
			}
		}

		//now ready to save. 
		return list; 

	}

	@Override
	public String getFileExtension() {
		return extension;
	}

	@Override
	public String getIconString() {
		return "file-matlab"; 
	}

	@Override
	public String getName() {
		return "MATLAB";
	}
	
	public static void main(String args[]) {
		
		String fileName = "/Users/au671271/MATLAB-Drive/MATLAB/PAMGUARD/_test/export_test.mat";
		
		try {
			Mat5File matFile = Mat5.newMatFile();
					
			
			Struct mlStruct = Mat5.newStruct(3, 1);
			Matrix triggerMap = Mat5.newScalar(Math.random()); 

			mlStruct.set("triggerMap", 0, triggerMap);
			mlStruct.set("triggerMap", 1, triggerMap);
			mlStruct.set("triggerMap", 2, triggerMap);

			matFile.addArray("test_struct", mlStruct);

			//basic method to write to a file
			Mat5.writeToFile(matFile, fileName);
			

//			Sink sink = Sinks.newMappedFile(new File(fileName),  Casts.sint32(1000000));
//			
//			matFile.writeTo(sink);
//
//			sink.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	


}