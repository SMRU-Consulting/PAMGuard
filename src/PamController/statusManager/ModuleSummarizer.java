package PamController.statusManager;

import org.json.JSONArray;
import org.json.JSONObject;

import PamController.PamControlledUnit;
import PamController.PamController;
import PamController.command.SummaryCommand;
import PamUtils.PamCalendar;

public class ModuleSummarizer {
	
	private static long lastCallTime = 0;
	
	public static String getModulesSummary(boolean clear, String format) {
		PamController pamController = PamController.getInstance();
		int nMod = pamController.getNumControlledUnits();
		PamControlledUnit aModule;
		String totalString;
		String aString;
		if (lastCallTime == 0) {
			lastCallTime = PamCalendar.getSessionStartTime();
		}
		long nowTime = PamCalendar.getTimeInMillis();
		totalString = PamCalendar.formatDBDateTime(lastCallTime) + "-" + PamCalendar.formatDBDateTime(nowTime);
		int usedModules = 0;
		
		JSONObject returnJson = new JSONObject();
		JSONArray jsonSummaries = new JSONArray();
		for (int i = 0; i < nMod; i++) {
			aModule = pamController.getControlledUnit(i);
			try {
				aString = aModule.getModuleSummary(clear, format);
			}catch(Exception e) {
				System.out.println("Unable to process module summary for "+aModule.getUnitName()+": "+e.getMessage());
				continue;
			}
			
			if (aString == null) {
				continue;
			}
			usedModules ++;
			totalString += String.format("\n<%s>%s:%s<\\%s>", aModule.getShortUnitType(), 
					aModule.getUnitName(), aString, aModule.getShortUnitType());
			if(format.equals("json")) {
				try {
					JSONObject moduleSummary = new JSONObject();
					moduleSummary.put("moduleType", aModule.getUnitType());
					moduleSummary.put("moduleName", aModule.getUnitName());
					if(aString.startsWith("{")) {
						moduleSummary.put("moduleSummary", new JSONObject(aString));

					}else {
						moduleSummary.put("moduleSummary", new JSONObject("{\"Pamguard Module has no summary\":\"None\"}"));
					}
					jsonSummaries.put(moduleSummary);
				}
				catch (Exception e) {
					e.printStackTrace();
					System.out.println("Json error in " + aString);
					return "";
				}
			}
			
		}
		
		if(format.equals("json")) {
			returnJson.put("moduleSummaries", jsonSummaries);
			returnJson.put("summaryStartTime", lastCallTime);
			returnJson.put("summaryEndTime", nowTime);
		}
		
		if (clear) {
			lastCallTime = nowTime;
		}
		
		if(format.equals("json")) return returnJson.toString();
		
		return totalString;
	}

	/**
	 * Send back a valid message if errors were caught
	 * ST 10/22/26
	 */
	public static String getModuleSummaryErrorResponse(String format, String message) {
		switch(format) {
		case SummaryCommand.CSV:
			return "error,"+message;
		case SummaryCommand.XML:
			return "<moduleSummary error=true>"+message+"</moduleSummary>";
		case SummaryCommand.JSON:
			try {
				JSONObject returnJson = new JSONObject();
				JSONArray jsonSummaries = new JSONArray();
				JSONObject moduleSummary = new JSONObject();
				moduleSummary.put("moduleType", "None");
				moduleSummary.put("moduleName", "None");
				JSONObject errorSummmary = new JSONObject();
				errorSummmary.put("error","Pamguard encountered an error collecting module summaries: "+message+". See pamguard logs for more information.");
				moduleSummary.put("moduleSummary", errorSummmary);
				jsonSummaries.put(moduleSummary);
				returnJson.put("moduleSummaries", jsonSummaries);
				returnJson.put("summaryStartTime", lastCallTime);
				long nowTime = PamCalendar.getTimeInMillis();
				returnJson.put("summaryEndTime", nowTime);
				return returnJson.toString();
			}catch(Exception e) {
				e.printStackTrace();
				return "";
			}
		}
		return "";
	}
	
}
