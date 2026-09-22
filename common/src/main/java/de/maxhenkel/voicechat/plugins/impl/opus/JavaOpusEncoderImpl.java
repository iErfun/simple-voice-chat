package de.maxhenkel.voicechat.plugins.impl.opus;

import org.concentus.OpusApplication;
import org.concentus.OpusBandwidth;
import org.concentus.OpusConstants;
import org.concentus.OpusEncoder;
import org.concentus.OpusMode;
import org.concentus.OpusSignal;
import de.maxhenkel.opus4j.OpusEncoder.Application;

/**
 * Client uncap Opus encoder:
 * - Force CELT-only mode
 * - Maximum bitrate (OPUS_BITRATE_MAX, limited by payload size)
 * - Fullband, complexity 10, unconstrained VBR
 * - No speech-oriented filters (AUDIO application + MUSIC signal bias)
 */
public class JavaOpusEncoderImpl implements de.maxhenkel.voicechat.api.opus.OpusEncoder {

    protected OpusEncoder opusEncoder;
    protected byte[] buffer;
    protected int sampleRate;
    protected int frameSize;
    protected Application application;

    public JavaOpusEncoderImpl(int sampleRate, int frameSize, int maxPayloadSize, Application application) {
        this.sampleRate = sampleRate;
        this.frameSize = frameSize;
        this.application = application;
        this.buffer = new byte[Math.max(maxPayloadSize, 1275)];
        open(maxPayloadSize);
    }

    private void open(int maxPayloadSize) {
        if (opusEncoder != null) {
            return;
        }
        try {
            opusEncoder = new OpusEncoder(sampleRate, 1, getApplication(application));

            // --- UNCAP / FILTERLESS ---
            opusEncoder.setForceMode(OpusMode.MODE_CELT_ONLY);
            opusEncoder.setBitrate(OpusConstants.OPUS_BITRATE_MAX);
            opusEncoder.setComplexity(10);
            opusEncoder.setMaxBandwidth(OpusBandwidth.OPUS_BANDWIDTH_FULLBAND);
            opusEncoder.setBandwidth(OpusBandwidth.OPUS_BANDWIDTH_FULLBAND);
            opusEncoder.setSignalType(OpusSignal.OPUS_SIGNAL_MUSIC);
            opusEncoder.setUseVBR(true);
            opusEncoder.setUseConstrainedVBR(false);
            opusEncoder.setUseInbandFEC(false);
            opusEncoder.setPacketLossPercent(0);
            opusEncoder.setUseDTX(false);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create uncapped Opus encoder", e);
        }
    }

    @Override
    public byte[] encode(short[] rawAudio) {
        if (isClosed()) {
            throw new IllegalStateException("Encoder is closed");
        }

        int result;
        try {
            result = opusEncoder.encode(rawAudio, 0, frameSize, buffer, 0, buffer.length);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode audio", e);
        }

        if (result < 0) {
            throw new RuntimeException("Failed to encode audio data");
        }

        byte[] audio = new byte[result];
        System.arraycopy(buffer, 0, audio, 0, result);
        return audio;
    }

    @Override
    public void resetState() {
        if (isClosed()) {
            throw new IllegalStateException("Encoder is closed");
        }
        opusEncoder.resetState();
    }

    @Override
    public boolean isClosed() {
        return opusEncoder == null;
    }

    @Override
    public void close() {
        if (isClosed()) {
            return;
        }
        opusEncoder = null;
    }

    public static OpusApplication getApplication(Application application) {
        switch (application) {
            case VOIP:
            default:
                return OpusApplication.OPUS_APPLICATION_VOIP;
            case AUDIO:
                return OpusApplication.OPUS_APPLICATION_AUDIO;
            case LOW_DELAY:
                return OpusApplication.OPUS_APPLICATION_RESTRICTED_LOWDELAY;
        }
    }
}
