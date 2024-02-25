package com.shajikhan.ladspa.amprack;

public class Resample {
    public static byte[] resample(byte[] data, int length, boolean stereo, int inFrequency, int outFrequency) {
        if (inFrequency < outFrequency)
            return upsample(data, length, stereo, inFrequency, outFrequency);

        if (inFrequency > outFrequency)
            return downsample(data, length, stereo, inFrequency, outFrequency);

        return trimArray(data, length);
    }

    /**
     * Basic upsampling algorithm. Uses linear approximation to fill in the
     * missing data.
     *
     * @param data          Input data
     * @param length        The current size of the input array (usually, data.length)
     * @param inputIsStereo True if input is inputIsStereo
     * @param inFrequency   Frequency of input
     * @param outFrequency  Frequency of output
     *
     * @return Upsampled audio data
     */
    private static byte[] upsample(byte[] data, int length, boolean inputIsStereo, int inFrequency, int outFrequency) {

        // Special case for no action
        if (inFrequency == outFrequency)
            return trimArray(data, length);

        double scale = (double) inFrequency / (double) outFrequency;
        double pos = 0;
        byte[] output;

        if (!inputIsStereo) {
            output = new byte[(int) (length / scale)];
            for (int i = 0; i < output.length; i++) {
                int inPos = (int) pos;
                double proportion = pos - inPos;
                if (inPos >= length - 1) {
                    inPos = length - 2;
                    proportion = 1;
                }

                output[i] = (byte) Math.round(data[inPos] * (1 - proportion) + data[inPos + 1] * proportion);
                pos += scale;
            }
        } else {
            output = new byte[2 * (int) ((length / 2) / scale)];
            for (int i = 0; i < output.length / 2; i++) {
                int inPos = (int) pos;
                double proportion = pos - inPos;

                int inRealPos = inPos * 2;
                if (inRealPos >= length - 3) {
                    inRealPos = length - 4;
                    proportion = 1;
                }

                output[i * 2] = (byte) Math.round(data[inRealPos] * (1 - proportion) + data[inRealPos + 2] * proportion);
                output[i * 2 + 1] = (byte) Math.round(data[inRealPos + 1] * (1 - proportion) + data[inRealPos + 3] * proportion);
                pos += scale;
            }
        }

        return output;
    }

    /**
     * Basic downsampling algorithm. Uses linear approximation to reduce data.
     *
     * @param data          Input data
     * @param length        The current size of the input array (usually, data.length)
     * @param inputIsStereo True if input is inputIsStereo
     * @param inFrequency   Frequency of input
     * @param outFrequency  Frequency of output
     *
     * @return Downsampled audio data
     */
    private static byte[] downsample(byte[] data, int length, boolean inputIsStereo, int inFrequency, int outFrequency) {

        // Special case for no action
        if (inFrequency == outFrequency)
            return trimArray(data, length);

        double scale = (double) outFrequency / (double) inFrequency;
        byte[] output;
        double pos = 0;
        int outPos = 0;

        if (!inputIsStereo) {
            double sum = 0;
            output = new byte[(int) (length * scale)];
            int inPos = 0;

            while (outPos < output.length) {
                double firstVal = data[inPos++];
                double nextPos = pos + scale;
                if (nextPos >= 1) {
                    sum += firstVal * (1 - pos);
                    output[outPos++] = (byte) Math.round(sum);
                    nextPos -= 1;
                    sum = nextPos * firstVal;
                } else {
                    sum += scale * firstVal;
                }
                pos = nextPos;

                if (inPos >= length && outPos < output.length) {
                    output[outPos++] = (byte) Math.round(sum / pos);
                }
            }
        } else {
            double sum1 = 0, sum2 = 0;
            output = new byte[2 * (int) ((length / 2) * scale)];
            int inPos = 0;

            while (outPos < output.length) {
                double firstVal = data[inPos++], nextVal = data[inPos++];
                double nextPos = pos + scale;
                if (nextPos >= 1) {
                    sum1 += firstVal * (1 - pos);
                    sum2 += nextVal * (1 - pos);
                    output[outPos++] = (byte) Math.round(sum1);
                    output[outPos++] = (byte) Math.round(sum2);
                    nextPos -= 1;
                    sum1 = nextPos * firstVal;
                    sum2 = nextPos * nextVal;
                } else {
                    sum1 += scale * firstVal;
                    sum2 += scale * nextVal;
                }
                pos = nextPos;

                if (inPos >= length && outPos < output.length) {
                    output[outPos++] = (byte) Math.round(sum1 / pos);
                    output[outPos++] = (byte) Math.round(sum2 / pos);
                }
            }
        }

        return output;
    }

    /**
     * @param data   Data
     * @param length Length of valid data
     *
     * @return Array trimmed to length (or same array if it already is)
     */
    public static byte[] trimArray(byte[] data, int length) {
        if (data.length == length)
            return data;

        byte[] output = new byte[length];
        System.arraycopy(output, 0, data, 0, length);
        return output;
    }
}
