package telemetry.payloads;

import java.util.StringTokenizer;

import telemetry.BitArrayLayout;
import telemetry.FramePart;

public class PayloadWODExperiment extends PayloadWOD {
		
	public PayloadWODExperiment(BitArrayLayout lay) {
		super(lay);
	}
	
	public PayloadWODExperiment(int id, int resets, long uptime, String date, StringTokenizer st, BitArrayLayout lay) {
		super(id, resets, uptime, date, st, lay);	

	}

	@Override
	protected void init() {
		type = TYPE_WOD_EXP;
		fieldValue = new int[layout.NUMBER_OF_FIELDS];
	}
	
	@Override
	public String toString() {
		copyBitsToFields();
		String s = "WOD EXP PAYLOAD\n";
		s = s + "RESET: " + getRawValue(WOD_RESETS);
		s = s + "  UPTIME: " + getRawValue(WOD_UPTIME);
		s = s + "  TYPE: " +  type + "\n";

		return s;
	}
	
}
