package PamController.command;

import PamController.statusManager.ModuleSummarizer;

public class SummaryPeekCommand extends SummaryCommand {

	public SummaryPeekCommand() {
		super();
		setName("summarypeek");
	}

	@Override
	public String execute(String command) {
		String [] splitCommand = command.split(" ");
		String format = SummaryCommand.CSV;
		if(splitCommand.length>1) format = splitCommand[1];
		try {
			return ModuleSummarizer.getModulesSummary(false,format);
		}catch(Exception e) {
			System.out.println("Unable to assemble module summaries. Error: "+e.getMessage());
			e.printStackTrace();
			return ModuleSummarizer.getModuleSummaryErrorResponse(format,e.getMessage());
		}
	}

	@Override
	public String getHint() {
		return "Get summary information about each running process, don't clear data";
	}
}
