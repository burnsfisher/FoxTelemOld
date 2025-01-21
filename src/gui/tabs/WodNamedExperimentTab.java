package gui.tabs;

import common.Spacecraft;
import telemetry.BitArrayLayout;

@SuppressWarnings("serial")
public class WodNamedExperimentTab extends NamedExperimentTab {

	public WodNamedExperimentTab(Spacecraft sat, String displayName, BitArrayLayout displayLayout, BitArrayLayout displayLayout2, int displayType) {
		super(sat, displayName, displayLayout, displayLayout2, displayType);
	}
	
}
