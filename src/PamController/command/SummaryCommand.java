package PamController.command;

import org.json.JSONArray;
import org.json.JSONObject;

import PamController.PamControlledUnit;
import PamController.PamController;
import PamController.statusManager.ModuleSummarizer;
import PamUtils.PamCalendar;

/**
 * Gets a summary string made up of strings from 
 * multiple modules. 
 * @author Doug Gillespie
 *
 */
public class SummaryCommand extends ExtCommand {
	

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
		return ModuleSummarizer.getModulesSummary(true,format);
	}
	

	@Override
	public String getHint() {
		return "Get summary information about each running process";
	}
}
