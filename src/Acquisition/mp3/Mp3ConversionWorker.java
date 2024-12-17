package Acquisition.mp3;

import javax.sound.sampled.AudioFormat;

import PamDetection.RawDataUnit;
import net.sourceforge.lame.lowlevel.LameEncoder;
import net.sourceforge.lame.mp3.Lame;
import net.sourceforge.lame.mp3.MPEGMode;


public class Mp3ConversionWorker {
	
	private CompressedRawDataBlock outputDataBlock;
	private LameEncoder encoder;
	private AudioFormat inputAudioFormat;
	
	private static final int GOOD_QUALITY_BITRATE = 256;
	
	private byte[] inputBuffer;
	private byte[] outputBuffer;

	
	public Mp3ConversionWorker(CompressedRawDataBlock outputDataBlock, float sourceFs) {
		this.outputDataBlock = outputDataBlock;
		this.inputAudioFormat = new AudioFormat(sourceFs, 64, 1, true, true);
	}
	
	public void setupConverter() {
		encoder = new LameEncoder(inputAudioFormat, GOOD_QUALITY_BITRATE, MPEGMode.MONO, Lame.QUALITY_MIDDLE, false);
		inputBuffer = new byte[encoder.getPCMBufferSize()];
		outputBuffer = new byte[encoder.getPCMBufferSize()];
	}
	
	public void encodeData(RawDataUnit newRawData) {
		newRawData.getRawData();
	}
	

}
