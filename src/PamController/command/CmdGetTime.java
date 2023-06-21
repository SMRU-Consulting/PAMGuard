package PamController.command;

import PamUtils.PamCalendar;

public class CmdGetTime extends ExtCommand {

	public CmdGetTime() {
		super("gettime", true);
	}

	@Override
	public String execute(String command) {
		return PamCalendar.formatDBDateTime(System.currentTimeMillis(), true);
	}

	@Override
	public String getHint() {
		return "Get the device time (from the system clocks)";
	}

}
