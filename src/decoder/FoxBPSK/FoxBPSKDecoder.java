package decoder.FoxBPSK;

	import java.io.IOException;
import java.util.Scanner;

import javax.sound.sampled.AudioFormat;
	import javax.sound.sampled.UnsupportedAudioFileException;

	import common.Config;
	import common.Log;
import common.Performance;
import decoder.Decoder;
	import decoder.FoxBitStream;
import decoder.RfData;
import decoder.SlowSpeedBitStream;
	import decoder.SourceAudio;
import decoder.SourceIQ;
import filter.AGCFilter;
import filter.DcRemoval;
import filter.RaisedCosineFilter;
import gui.MainWindow;
import measure.RtMeasurement;
import measure.SatMeasurementStore;
import measure.SatPc32DDE;
import telemetry.FoxFramePart;
import telemetry.Frame;
import telemetry.Header;
import telemetry.HighSpeedFrame;
import telemetry.HighSpeedHeader;
import telemetry.PayloadCameraData;
import telemetry.PayloadHERCIhighSpeed;
import telemetry.PayloadMaxValues;
import telemetry.PayloadMinValues;
import telemetry.PayloadRadExpData;
import telemetry.PayloadRtValues;
import telemetry.SlowSpeedFrame;
import telemetry.SlowSpeedHeader;

	public class FoxBPSKDecoder extends Decoder {
		public static final int BITS_PER_SECOND_1200 = 1200;
		private int lastDataValue[];
		private int clockOffset = 0;
		private double[] cosTab;
		private double[] sinTab;
		DcRemoval audioDcFilter;
		
		double[] pskAudioData;
		
		/**
	     * This holds the stream of bits that we have not decoded. Once we have several
	     * SYNC words, this is flushed of processed bits.
	     */
	    protected FoxBitStream bitStream = null;  // Hold bits until we turn them into decoded frames
	    
		public FoxBPSKDecoder(SourceAudio as, int chan) {
			super("1200bps BPSK", as, chan);
			init();
		}

		@Override
		protected void init() {
			Log.println("Initializing 1200bps BPSK decoder: ");
			
			bitStream = new SlowSpeedBitStream(this);
			BITS_PER_SECOND = BITS_PER_SECOND_1200;
			SAMPLE_WINDOW_LENGTH = 60;  
			bucketSize = currentSampleRate / BITS_PER_SECOND; // Number of samples that makes up one bit
			
			BUFFER_SIZE =  SAMPLE_WINDOW_LENGTH * bucketSize;
			SAMPLE_WIDTH = 4;
			if (SAMPLE_WIDTH < 1) SAMPLE_WIDTH = 1;
			CLOCK_TOLERANCE = bucketSize/2;
			CLOCK_REOVERY_ZERO_THRESHOLD = 20;
			initWindowData();
			
			lastDataValue = new int[bucketSize];
			
			filter = new AGCFilter(audioSource.audioFormat, (BUFFER_SIZE));
			audioDcFilter = new DcRemoval(0.9999d);
			filter.init(currentSampleRate, 0, 0);
			//filter = new RaisedCosineFilter(audioSource.audioFormat, BUFFER_SIZE);
			//filter.init(currentSampleRate, 1200, 65);
			
			cosTab = new double[SINCOS_SIZE];
			sinTab = new double[SINCOS_SIZE];
			
			for (int n=0; n<SINCOS_SIZE; n++) {
				cosTab[n] = Math.cos(n*2.0*Math.PI/SINCOS_SIZE);
				sinTab[n] = Math.sin(n*2.0*Math.PI/SINCOS_SIZE);
			}
			pskAudioData = new double[BUFFER_SIZE];
		}

		protected void resetWindowData() {
			super.resetWindowData();
			
		}
		
		public double[] getBasebandData() {
			return pskAudioData;
		}
		
		/**
		 * Sample the buckets (one bucket per bit) to determine the change in phase and hence
		 * the bit that each bucket contains.  We use the following approach:
		 * Each sample in each bit is multiplied by the corresponding sample in the previous bit.  These multiplications
		 * These multiplications are Integrated (summed) over the bit
		 * If the total sum is positive then the phase did not change and we have a 1
		 * If the total sum is negative then the phase did change and we have a 0
		 * We store the data for the last bit so that we can use it as the "previous bit" for the first calculation next time
		 * 
		 * While we are sampling, we keep track of the clock offset in case we need to adjust it.
		 * 
		 */
		private double vcoPhase = 0.0;
		private static final double RX_CARRIER_FREQ = 1200.0;
		//private static final double VCO_PHASE_INC = 2.0*Math.PI*RX_CARRIER_FREQ/(double)48000;
		private static final int SINCOS_SIZE = 256;

		protected void sampleBuckets() {
			for (int i=0; i < SAMPLE_WINDOW_LENGTH; i++) {
				for (int s=0; s < bucketSize; s++) {
					//sampleWithVCO(dataValues[i][s], i, s);
					double value = dataValues[i][s]/ 32768.0;
//////					value = audioDcFilter.filter(value);		
					RxDownSample(value, value, i, s);
					pskAudioData[i*bucketSize+s] = energy1/dmEnergy[dmPeakPos]-1;
					int eyeValue = (int)(32768*(energy1/dmEnergy[dmPeakPos]-1));
					eyeData.setData(i,s,eyeValue);
				}
				int offset = recoverClockOffset();
				if (middleSample[i] == false)
					eyeData.setOffsetLow(i, SAMPLE_WIDTH, offset );
				else
					eyeData.setOffsetHigh(i, SAMPLE_WIDTH, offset);
			}
			int offset = recoverClockOffset();
			eyeData.offsetEyeData(offset); // rotate the data so that it matches the clock offset

	//	Scanner scanner = new Scanner(System.in);
	//		System.out.println("Press enter");
	//	String username = scanner.next();
		}
		
		
		
	
		/**
		 * Determine if the bit sampling buckets are aligned with the data. This is calculated when the
		 * buckets are sampled
		 * 
		 */
		@Override
		public int recoverClockOffset() {
			
			return clockOffset;
		}
		
		protected double[] recoverClock(int factor) {

			/*
	    	if (clockOffset > 0) {
	    	// There are 40 samples in a 1200bps bucket. The clock offset 
	    		double[] clockData = new double[clockOffset];
	    		if (Config.debugClock) Log.println("Advancing clock " + clockOffset + " samples");
	    		int nBytesRead = read(clockData);
	    		if (nBytesRead != (clockOffset)) {
	    			if (Config.debugClock) Log.println("ERROR: Could not advance clock");
	    		} else {
	    			// This is the new clock offsest
	    			// Reprocess the data in the current window
	    			return clockData;
	    		}
	    	} else {
	    		if (Config.debugClock) Log.println("PSK CLOCK STABLE");
	    		return null;
	    	}
	    	*/
	    	return null;
		}

		@Override
		protected void processBitsWindow() {
			Performance.startTimer("findSync");
			boolean found = bitStream.findSyncMarkers(SAMPLE_WINDOW_LENGTH);
			Performance.endTimer("findSync");
			if (found) {
				processPossibleFrame();
			}
		}

		private Frame decodedFrame = null;
		/**
		 *  THIS IS THE FOX-1A-D decode, used for TESTING ONLY.  This needs to be replaced with the routine to decode 1E frames
		 */
		protected void processPossibleFrame() {
			
			//Performance.startTimer("findFrames");
			decodedFrame = bitStream.findFrames();
			//Performance.endTimer("findFrames");
			if (decodedFrame != null && !decodedFrame.corrupt) {
				Performance.startTimer("Store");
				// Successful frame
				eyeData.lastErasureCount = bitStream.lastErasureNumber;
				eyeData.lastErrorsCount = bitStream.lastErrorsNumber;
				//eyeData.setBER(((bitStream.lastErrorsNumber + bitStream.lastErasureNumber) * 10.0d) / (double)bitStream.SYNC_WORD_DISTANCE);
				if (Config.storePayloads) {
					if (decodedFrame instanceof SlowSpeedFrame) {
						SlowSpeedFrame ssf = (SlowSpeedFrame)decodedFrame;
						FoxFramePart payload = ssf.getPayload();
						SlowSpeedHeader header = ssf.getHeader();
						if (Config.storePayloads) Config.payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), payload);
						
						// Capture measurements once per payload or every 5 seconds ish
						addMeasurements(header, decodedFrame, bitStream.lastErrorsNumber, bitStream.lastErasureNumber);
						if (Config.autoDecodeSpeed)
							MainWindow.inputTab.setViewDecoder1();  // FIXME - not sure I should call the GUI from the DECODER, but works for now.
					} else {
						HighSpeedFrame hsf = (HighSpeedFrame)decodedFrame;
						HighSpeedHeader header = hsf.getHeader();
						PayloadRtValues payload = hsf.getRtPayload();
						Config.payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), payload);
						PayloadMaxValues maxPayload = hsf.getMaxPayload();
						Config.payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), maxPayload);
						PayloadMinValues minPayload = hsf.getMinPayload();
						Config.payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), minPayload);
						PayloadRadExpData[] radPayloads = hsf.getRadPayloads();
						Config.payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), radPayloads);
						if (Config.satManager.hasCamera(header.getFoxId())) {
							PayloadCameraData cameraData = hsf.getCameraPayload();
							if (cameraData != null)
								Config.payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), cameraData);
						}
						if (Config.satManager.hasHerci(header.getFoxId())) {
							PayloadHERCIhighSpeed[] herciDataSet = hsf.getHerciPayloads();
							if (herciDataSet != null)
								Config.payloadStore.add(header.getFoxId(), header.getUptime(), header.getResets(), herciDataSet);
						}
						// Capture measurements once per payload or every 5 seconds ish
						addMeasurements(header, decodedFrame, bitStream.lastErrorsNumber, bitStream.lastErasureNumber);
						if (Config.autoDecodeSpeed)
							MainWindow.inputTab.setViewDecoder2();
					}

				}
				if (Config.uploadToServer)
					try {
						Config.rawFrameQueue.add(decodedFrame);
					} catch (IOException e) {
						// Don't pop up a dialog here or the user will get one for every frame decoded.
						// Write to the log only
						e.printStackTrace(Log.getWriter());
					}
				framesDecoded++;
				Performance.endTimer("Store");
			} else {
				if (Config.debugBits) Log.println("SYNC marker found but frame not decoded\n");
				//clockLocked = false;
			}
		}
			
		/**
		 * The PSK demodulation algorithm is based on code by Howard and the Funcube team
		 */
		private static final int DOWN_SAMPLE_FILTER_SIZE = 27;
		private static final double[] dsFilter = {
			-6.103515625000e-004F,  /* filter tap #    0 */
			-1.220703125000e-004F,  /* filter tap #    1 */
			+2.380371093750e-003F,  /* filter tap #    2 */
			+6.164550781250e-003F,  /* filter tap #    3 */
			+7.324218750000e-003F,  /* filter tap #    4 */
			+7.629394531250e-004F,  /* filter tap #    5 */
			-1.464843750000e-002F,  /* filter tap #    6 */
			-3.112792968750e-002F,  /* filter tap #    7 */
			-3.225708007813e-002F,  /* filter tap #    8 */
			-1.617431640625e-003F,  /* filter tap #    9 */
			+6.463623046875e-002F,  /* filter tap #   10 */
			+1.502380371094e-001F,  /* filter tap #   11 */
			+2.231445312500e-001F,  /* filter tap #   12 */
			+2.518310546875e-001F,  /* filter tap #   13 */
			+2.231445312500e-001F,  /* filter tap #   14 */
			+1.502380371094e-001F,  /* filter tap #   15 */
			+6.463623046875e-002F,  /* filter tap #   16 */
			-1.617431640625e-003F,  /* filter tap #   17 */
			-3.225708007813e-002F,  /* filter tap #   18 */
			-3.112792968750e-002F,  /* filter tap #   19 */
			-1.464843750000e-002F,  /* filter tap #   20 */
			+7.629394531250e-004F,  /* filter tap #   21 */
			+7.324218750000e-003F,  /* filter tap #   22 */
			+6.164550781250e-003F,  /* filter tap #   23 */
			+2.380371093750e-003F,  /* filter tap #   24 */
			-1.220703125000e-004F,  /* filter tap #   25 */
			-6.103515625000e-004F   /* filter tap #   26 */
		};
//		private static final double DOWN_SAMPLE_MULT = 0.9*32767.0;	// XXX: Voodoo from Howard?
		private static final int MATCHED_FILTER_SIZE = 65;
		private static final double[] dmFilter = {
			-0.0101130691F,-0.0086975143F,-0.0038246093F,+0.0033563764F,+0.0107237026F,+0.0157790936F,+0.0164594107F,+0.0119213911F,
			+0.0030315224F,-0.0076488191F,-0.0164594107F,-0.0197184277F,-0.0150109226F,-0.0023082460F,+0.0154712381F,+0.0327423589F,
			+0.0424493086F,+0.0379940454F,+0.0154712381F,-0.0243701991F,-0.0750320094F,-0.1244834076F,-0.1568500423F,-0.1553748911F,
			-0.1061032953F,-0.0015013786F,+0.1568500423F,+0.3572048240F,+0.5786381191F,+0.7940228249F,+0.9744923010F,+1.0945250059F,
			+1.1366117829F,+1.0945250059F,+0.9744923010F,+0.7940228249F,+0.5786381191F,+0.3572048240F,+0.1568500423F,-0.0015013786F,
			-0.1061032953F,-0.1553748911F,-0.1568500423F,-0.1244834076F,-0.0750320094F,-0.0243701991F,+0.0154712381F,+0.0379940454F,
			+0.0424493086F,+0.0327423589F,+0.0154712381F,-0.0023082460F,-0.0150109226F,-0.0197184277F,-0.0164594107F,-0.0076488191F,
			+0.0030315224F,+0.0119213911F,+0.0164594107F,+0.0157790936F,+0.0107237026F,+0.0033563764F,-0.0038246093F,-0.0086975143F,
			-0.0101130691F,
			-0.0101130691F,-0.0086975143F,-0.0038246093F,+0.0033563764F,+0.0107237026F,+0.0157790936F,+0.0164594107F,+0.0119213911F,
			+0.0030315224F,-0.0076488191F,-0.0164594107F,-0.0197184277F,-0.0150109226F,-0.0023082460F,+0.0154712381F,+0.0327423589F,
			+0.0424493086F,+0.0379940454F,+0.0154712381F,-0.0243701991F,-0.0750320094F,-0.1244834076F,-0.1568500423F,-0.1553748911F,
			-0.1061032953F,-0.0015013786F,+0.1568500423F,+0.3572048240F,+0.5786381191F,+0.7940228249F,+0.9744923010F,+1.0945250059F,
			+1.1366117829F,+1.0945250059F,+0.9744923010F,+0.7940228249F,+0.5786381191F,+0.3572048240F,+0.1568500423F,-0.0015013786F,
			-0.1061032953F,-0.1553748911F,-0.1568500423F,-0.1244834076F,-0.0750320094F,-0.0243701991F,+0.0154712381F,+0.0379940454F,
			+0.0424493086F,+0.0327423589F,+0.0154712381F,-0.0023082460F,-0.0150109226F,-0.0197184277F,-0.0164594107F,-0.0076488191F,
			+0.0030315224F,+0.0119213911F,+0.0164594107F,+0.0157790936F,+0.0107237026F,+0.0033563764F,-0.0038246093F,-0.0086975143F,
			-0.0101130691F
		};
		private static final int DOWN_SAMPLE_RATE = 9600;
		private static final int BIT_RATE = 1200;
		private static final int SAMPLES_PER_BIT = DOWN_SAMPLE_RATE/BIT_RATE;
		private static final double VCO_PHASE_INC = 2.0*Math.PI*RX_CARRIER_FREQ/(double)DOWN_SAMPLE_RATE;
		private static final double BIT_SMOOTH1 = 1.0/200.0;
		private static final double BIT_SMOOTH2 = 1.0/800.0;
		private static final double BIT_PHASE_INC = 1.0/(double)DOWN_SAMPLE_RATE;
		private static final double BIT_TIME = 1.0/(double)BIT_RATE;

		private int cntRaw, cntDS; 
		private double energy1, energy2;

		private double[][] dsBuf = new double[DOWN_SAMPLE_FILTER_SIZE][2];
		private int dsPos = DOWN_SAMPLE_FILTER_SIZE-1, dsCnt = 0;
		private double HOWARD_FUDGE_FACTOR = 0.9 * 32768.0;

		/**
		 * Down sample from input rate to DOWN_SAMPLE_RATE and low pass filter
		 * @param i
		 * @param q
		 * @param bucketNumber
		 */
		private void RxDownSample(double i, double q, int bucketNumber, int bucketOffset) {
			dsBuf[dsPos][0]=i;
			dsBuf[dsPos][1]=q;
			if (++dsCnt>=(int)currentSampleRate/DOWN_SAMPLE_RATE) {	// typically 48000/9600
				double fi = 0.0, fq = 0.0;
				// apply low pass FIR
				for (int n=0; n<DOWN_SAMPLE_FILTER_SIZE; n++) {
					int dsi = (n+dsPos)%DOWN_SAMPLE_FILTER_SIZE; 
					fi+=dsBuf[dsi][0]*dsFilter[n];
					fq+=dsBuf[dsi][1]*dsFilter[n];
				}
				dsCnt=0;
				// feed down sampled values to demodulator
				RxDemodulate(fi * HOWARD_FUDGE_FACTOR, fq * HOWARD_FUDGE_FACTOR, bucketNumber, bucketOffset);
			}
			dsPos--;
			if (dsPos<0)
				dsPos=DOWN_SAMPLE_FILTER_SIZE-1;
			cntRaw++;
		}

		private double[][] dmBuf = new double[MATCHED_FILTER_SIZE][2];
		private int dmPos = MATCHED_FILTER_SIZE-1;
		private double[] dmEnergy = new double[SAMPLES_PER_BIT+2];
		private double[] eyeEnergy = new double[SAMPLES_PER_BIT+2];
		private int dmBitPos = 0, dmPeakPos = 0, dmNewPeak = 0;
		private double dmEnergyOut = 1.0;
		private int[] dmHalfTable = {4,5,6,7,0,1,2,3};
		private double dmBitPhase = 0.0;
		private double[] dmLastIQ = new double[2];
		private int lastBucketOffset = 0;
		
		/**
		 * Demodulate the DBPSK signal, adjust the clock if needed to stay in sync.  Populate the bit buffer
		 * @param i
		 * @param q
		 * @param bucketNumber
		 */
		private void RxDemodulate(double i, double q, int bucketNumber, int bucketOffset) {
			vcoPhase += VCO_PHASE_INC;
			if (vcoPhase > 2.0*Math.PI)
				vcoPhase -= 2.0*Math.PI;
			// quadrature demodulate carrier to base band with VCO, store in FIR buffer
			dmBuf[dmPos][0]=i*cosTab[(int)(vcoPhase*(double)SINCOS_SIZE/(2.0*Math.PI))%SINCOS_SIZE];
			dmBuf[dmPos][1]=q*sinTab[(int)(vcoPhase*(double)SINCOS_SIZE/(2.0*Math.PI))%SINCOS_SIZE];
			// apply FIR (base band smoothing, root raised cosine)
			double fi = 0.0, fq = 0.0;
			for (int n=0; n<MATCHED_FILTER_SIZE; n++) {
				int dmi = (MATCHED_FILTER_SIZE-dmPos+n);
				fi+=dmBuf[n][0]*dmFilter[dmi];
				fq+=dmBuf[n][1]*dmFilter[dmi];
			}
			dmPos--;
			if (dmPos<0)
				dmPos=MATCHED_FILTER_SIZE-1;

			
			// store smoothed bit energy
			energy1 = fi*fi+fq*fq;
			
			dmEnergy[dmBitPos] = (dmEnergy[dmBitPos]*(1.0-BIT_SMOOTH1))+(energy1*BIT_SMOOTH1);
			
			////System.out.println(dmPeakPos);
			// set a range of eye diagram values given we have downsampled
			// need to offset based on the peak postion in the bit dmPeakPos.  There are 8 samples in a bit, 0-7, so if this is not 3, then we need to shift the data
			/*
			for (int e=lastBucketOffset; e < bucketOffset; e++) {
				if (e-dmPeakPos-3 > 0 && e-dmPeakPos-3 < bucketSize)
					eyeData.setData(bucketNumber,e-dmPeakPos-3, (int)(Math.sqrt(energy1)));
			}
			lastBucketOffset = bucketOffset;
			*/
			/*
			if (bucketOffset/4-dmPeakPos-3 > 0 && bucketOffset/4-dmPeakPos-3 < bucketSize)
				eyeData.setData(bucketNumber,bucketOffset/4-dmPeakPos-3, (int)(Math.sqrt(energy1)));
			*/
			// at peak bit energy? decode 
			if (dmBitPos==dmPeakPos) {
				dmEnergyOut = (dmEnergyOut*(1.0-BIT_SMOOTH2))+(energy1*BIT_SMOOTH2);
				double di = -(dmLastIQ[0]*fi + dmLastIQ[1]*fq);
				double dq = dmLastIQ[0]*fq - dmLastIQ[1]*fi;
				dmLastIQ[0]=fi;
				dmLastIQ[1]=fq;
				energy2 = Math.sqrt(di*di+dq*dq);
				
				// store the energy as the eye diagram, with dmPeakPos in the middle
				// We have 8 samples + 2 
				//for (int e=0; e < 10; e++) {
				//	eyeData.setData(currentBucket, e, (int)Math.sqrt(eyeEnergy[e]));
				//}
				if (energy2>100.0) {	// TODO: work out where these magic numbers come from!
					boolean bit = di<0.0;	// is that a 1 or 0?
					middleSample[bucketNumber] = bit;
					bitStream.addBit(bit);
				}
			}
			// half-way into next bit? reset peak energy point
			if (dmBitPos==dmHalfTable[dmPeakPos]) {
				dmPeakPos = dmNewPeak;
				clockOffset = 4*(dmNewPeak-6); // store the clock offset so we can display the eye diagram "triggered" correctly
			}
			dmBitPos = (dmBitPos+1) % SAMPLES_PER_BIT;
			// advance phase of bit position
			dmBitPhase += BIT_PHASE_INC;
			if (dmBitPhase>=BIT_TIME) {
				dmBitPhase-=BIT_TIME;
				dmBitPos=0;	// TODO: Is this a kludge?
				// rolled round another bit, measure new peak energy position
				double eMax = -1.0e10F;
				for (int n=0; n<SAMPLES_PER_BIT; n++) {
					if (dmEnergy[n]>eMax) {
						dmNewPeak=n;
						eMax=dmEnergy[n];
					}
				}
			}
			cntDS++;

		}
		
	}


