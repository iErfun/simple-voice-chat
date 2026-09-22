package de.maxhenkel.voicechat.voice.client;

import de.maxhenkel.voicechat.voice.common.AudioUtils;

/**
 * Uncapped mic gain with soft saturation (no hard digital clipping).
 * High gain makes the signal louder; peaks are gently compressed instead of hard-clipped.
 */
public class VolumeManager {

    public static final double MIN_GAIN = -40D;
    public static final double MAX_GAIN = 200D;

    private static final double SOFT_KNEE = 0.9D;
    private static final double FULL_SCALE = Short.MAX_VALUE;

    public VolumeManager() {
    }

    /**
     * Changes the volume of 16-bit mono audio in place.
     * Applies linear gain then soft saturation so loud peaks never hard-clip.
     *
     * @param audio  the audio data
     * @param gainDb the gain in dB
     */
    public void adjustVolume(short[] audio, double gainDb) {
        if (gainDb <= MIN_GAIN) {
            for (int i = 0; i < audio.length; i++) {
                audio[i] = 0;
            }
            return;
        }

        double multiplier = AudioUtils.dbToLinear(gainDb);
        double knee = SOFT_KNEE * FULL_SCALE;

        for (int i = 0; i < audio.length; i++) {
            double sample = (double) audio[i] * multiplier;
            double abs = Math.abs(sample);
            if (abs > knee) {
                double sign = sample >= 0D ? 1D : -1D;
                double over = (abs - knee) / (FULL_SCALE - knee);
                sample = sign * (knee + (FULL_SCALE - knee) * Math.tanh(over));
            }
            if (sample > FULL_SCALE) {
                sample = FULL_SCALE;
            } else if (sample < -FULL_SCALE) {
                sample = -FULL_SCALE;
            }
            audio[i] = (short) sample;
        }
    }
}
