package networkTransfer.send;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;

import PamModel.parametermanager.ManagedParameters;
import PamModel.parametermanager.PamParameterSet;
import PamModel.parametermanager.PrivatePamParameterData;
import PamModel.parametermanager.PamParameterSet.ParameterSetType;
import PamguardMVC.PamDataBlock;
import networkTransfer.NetworkParams;

public class NetworkSendParams extends NetworkParams implements Serializable, Cloneable, ManagedParameters {

	public static final long serialVersionUID = 1L;
	
	public static final int NETWORKSEND_BYTEARRAY = 0;
	
	public static final int NETWORKSEND_JSON = 1;
	
	public static final int NETWORKSEND_BOTH = 2;
	
	public int stationId1;
	
	public int stationId2;
		
	public int sendingFormat;
	
	public String persistenceDir;
	
	
	/**
	 * Max number of queued Objects. 
	 */
	public int maxQueuedObjects = 1000;
	
	/**
	 * Max queue size in kilobytes
	 */
	public int maxQueueSize = 10000;
	
	private ArrayList<String> selectedDataBlocks;
	
	/**
	 * Set send selection for an individual datablock. 
	 * @param dataBlock datablock 
	 * @param doSend true - will send, false don't send. 
	 */
	public void setDataBlock(PamDataBlock dataBlock, boolean doSend) {
		if (selectedDataBlocks == null) {
			selectedDataBlocks = new ArrayList<String>();
		}
		if (doSend) {
			String foundName = findDataBlock(dataBlock);
			if (foundName == null) {
				selectedDataBlocks.add(dataBlock.getDataName());
			}
		}
		else {
			String found = findDataBlock(dataBlock);
			if (found != null){
				selectedDataBlocks.remove(found);
			}
		}
	}
	
	/**
	 * Find if a datablock is listed in the list of wanted datablocks. 
	 * If it is, return a reference to the string in the array list. 
	 * @param dataBlock datablock. 
	 * @return reference to string name. 
	 */
	public String findDataBlock(PamDataBlock dataBlock) {
		if (selectedDataBlocks == null) {
			return null;
		}
		String dbName = dataBlock.getDataName();
		for (String aName:selectedDataBlocks) {
			if (aName.equals(dbName)) {
				return aName;
			}
		}
		return null;
	}
	
	
	
	@Override
	public NetworkSendParams clone() {
		return (NetworkSendParams) super.clone();
	}

	public void clearDataBlocks() {
		if (selectedDataBlocks == null) {
			return;
		}
		selectedDataBlocks.clear();
	}
	
	@Override
	public PamParameterSet getParameterSet() {
		PamParameterSet ps = PamParameterSet.autoGenerate(this, ParameterSetType.DETECTOR);
		try {
			Field field = this.getClass().getDeclaredField("selectedDataBlocks");
			ps.put(new PrivatePamParameterData(this, field) {
				@Override
				public Object getData() throws IllegalArgumentException, IllegalAccessException {
					return selectedDataBlocks;
				}
			});
		} catch (NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		}
		return ps;
	}


}
