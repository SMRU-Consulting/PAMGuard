package PamController.command;

import org.json.JSONArray;
import org.json.JSONObject;

import PamController.PamControlledUnit;
import PamController.PamController;
import PamUtils.PamCalendar;

/**
 * Gets a summary string made up of strings from 
 * multiple modules. 
 * @author Doug Gillespie
 *
 */
public class SummaryCommand extends ExtCommand {
	
	private long lastCallTime = 0;

	public SummaryCommand() {
		super("summary", true);
	}

	@Override
	public String execute(String command) {
//		String[] cmdBits = CommandManager.splitCommandLine(command);
//		boolean clear = true;
//		// first word is the command, we want one after that. 
//		if (cmdBits.length >= 2 && cmdBits[1] != null) {
//			String bit = cmdBits[1].trim().toLowerCase();
//			if (bit.equals("0") || bit.equals("false")) {
//				clear = false;
//			}
//			
//		}
		String [] splitCommand = command.split(" ");
		String format = "csv";
		if(splitCommand.length>1) format = splitCommand[1];
		return getModulesSummary(true,format);
	}
	
	public String getModulesSummary(boolean clear, String format) {
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
			aString = aModule.getModuleSummary(clear, format);
			if (aString == null) {
				continue;
			}
			usedModules ++;
			totalString += String.format("\n<%s>%s:%s<\\%s>", aModule.getShortUnitType(), 
					aModule.getUnitName(), aString, aModule.getShortUnitType());
			if(format.equals("json")) {
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

	@Override
	public String getHint() {
		return "Get summary information about each running process";
	}
}
