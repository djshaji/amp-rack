package com.shajikhan.ladspa.amprack;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Pitch {
    public int SAMPLE_RATE = 48000 ;
    String TAG = getClass().getSimpleName();
    public List<String> tunings = new ArrayList<>();
    public static String[][] notesList = {
            {"B1", "61.74"},
            {"C2", "65.41"},
            {"C#2", "69.30"},
            {"D2", "73.42"},
            {"D#2", "77.78"},
            {"E2", "82.41"},
            {"F2", "87.31"},
            {"F#2", "92.50"},
            {"G2", "98.00"},
            {"G#2", "103.83"},
            {"A2", "110.00"},
            {"A#2", "116.54"},
            {"B2", "123.47"},
            {"C3", "130.81"},
            {"C#3", "138.59"},
            {"D3", "146.83"},
            {"D#3", "155.56"},
            {"E3", "164.81"},
            {"F3", "174.61"},
            {"F#3", "185.00"},
            {"G3", "196.00"},
            {"G#3", "207.65"},
            {"A3", "220.00"},
            {"A#3", "233.08"},
            {"B3", "246.94"},
            {"C4", "261.63"},
            {"C#4", "277.18"},
            {"D4", "293.66"},
            {"D#4", "311.13"},
            {"E4", "329.63"},
            {"F4", "349.23"},
            {"F#4", "369.99"},
            {"G4", "392.00"},
            {"G#4", "415.30"},
            {"A4", "440.00"},
    };

    public static String [] [] notes = {
            {"C0","16.35"},
            {"C#0/Db0","17.32"},
            {"D0","18.35"},
            {"D#0/Eb0","19.45"},
            {"E0","20.6"},
            {"F0","21.83"},
            {"F#0/Gb0","23.12"},
            {"G0","24.5"},
            {"G#0/Ab0","25.96"},
            {"A0","27.5"},
            {"A#0/Bb0","29.14"},
            {"B0","30.87"},
            {"C1","32.7"},
            {"C#1/Db1","34.65"},
            {"D1","36.71"},
            {"D#1/Eb1","38.89"},
            {"E1","41.2"},
            {"F1","43.65"},
            {"F#1/Gb1","46.25"},
            {"G1","49"},
            {"G#1/Ab1","51.91"},
            {"A1","55"},
            {"A#1/Bb1","58.27"},
            {"B1","61.74"},
            {"C2","65.41"},
            {"C#2/Db2","69.3"},
            {"D2","73.42"},
            {"D#2/Eb2","77.78"},
            {"E2","82.41"},
            {"F2","87.31"},
            {"F#2/Gb2","92.5"},
            {"G2","98"},
            {"G#2/Ab2","103.83"},
            {"A2","110"},
            {"A#2/Bb2","116.54"},
            {"B2","123.47"},
            {"C3","130.81"},
            {"C#3/Db3","138.59"},
            {"D3","146.83"},
            {"D#3/Eb3","155.56"},
            {"E3","164.81"},
            {"F3","174.61"},
            {"F#3/Gb3","185"},
            {"G3","196"},
            {"G#3/Ab3","207.65"},
            {"A3","220"},
            {"A#3/Bb3","233.08"},
            {"B3","246.94"},
            {"C4","261.63"},
            {"C#4/Db4","277.18"},
            {"D4","293.66"},
            {"D#4/Eb4","311.13"},
            {"E4","329.63"},
            {"F4","349.23"},
            {"F#4/Gb4","369.99"},
            {"G4","392"},
            {"G#4/Ab4","415.3"},
            {"A4","440"},
            {"A#4/Bb4","466.16"},
            {"B4","493.88"},
            {"C5","523.25"},
            {"C#5/Db5","554.37"},
            {"D5","587.33"},
            {"D#5/Eb5","622.25"},
            {"E5","659.25"},
            {"F5","698.46"},
            {"F#5/Gb5","739.99"},
            {"G5","783.99"},
            {"G#5/Ab5","830.61"},
            {"A5","880"},
            {"A#5/Bb5","932.33"},
            {"B5","987.77"},
            {"C6","1046.5"},
            {"C#6/Db6","1108.73"},
            {"D6","1174.66"},
            {"D#6/Eb6","1244.51"},
            {"E6","1318.51"},
            {"F6","1396.91"},
            {"F#6/Gb6","1479.98"},
            {"G6","1567.98"},
            {"G#6/Ab6","1661.22"},
            {"A6","1760"},
            {"A#6/Bb6","1864.66"},
            {"B6","1975.53"},
            {"C7","2093"},
            {"C#7/Db7","2217.46"},
            {"D7","2349.32"},
            {"D#7/Eb7","2489.02"},
            {"E7","2637.02"},
            {"F7","2793.83"},
            {"F#7/Gb7","2959.96"},
            {"G7","3135.96"},
            {"G#7/Ab7","3322.44"},
            {"A7","3520"},
            {"A#7/Bb7","3729.31"},
            {"B7","3951.07"},
            {"C8","4186.01"},
            {"C#8/Db8","4434.92"},
            {"D8","4698.63"},
            {"D#8/Eb8","4978.03"},
            {"E8","5274.04"},
            {"F8","5587.65"},
            {"F#8/Gb8","5919.91"},
            {"G8","6271.93"},
            {"G#8/Ab8","6644.88"},
            {"A8","7040"},
            {"A#8/Bb8","7458.62"},
            {"B8","7902.13"}
    } ;

    public static HashMap<String, Float> note = new HashMap<String, Float>();
    Pitch (int _sample_rate) {
        SAMPLE_RATE = _sample_rate ;
        tunings.add("Automatic Tuning");
        tunings.add("E Standard(E2 A2 D3 G3 B3 e4)");
        tunings.add("D# Standard(D#2 G#2 C#3 F#3 A#3 d#4)");
        tunings.add("D Standard(D2 G2 C3 F3 A3 d4)");
        tunings.add("C# Standard(C#2 F#2 B2 E3 G#3 c#4)");
        tunings.add("C Standard(C2 F2 A#2 D#3 G3 c3)");
        tunings.add("B Standard(B1 E2 A2 D3 F#3 b3)");
        tunings.add("Drop D(D2 A2 D3 G3 B3 e4)");
        tunings.add("Drop C#(C#2 G#2 C#3 F#3 A#3 d#4)");
        tunings.add("Drop C(C2 G2 C3 F3 A3 d4)");
        tunings.add("Drop B(B1 F#2 B2 E3 G#3 c#4)");

        int i = 0;
        for ( i = 0 ; i < notes.length ; i ++) {
            note.put(notes [i][0], Float.valueOf(notes [i][1]));
        }

        Log.d(TAG, "Pitch: " + note);
    }

    private double[] Window( float [] audioBuffer){
        double[] Buffer = new double[audioBuffer.length];
        for (int i = 0; i < audioBuffer.length; i++) {
            Buffer[i] = audioBuffer [i] / 32768.0  ;
        }
        return Buffer;
    }

    public double computePitchFrequency(float [] audioBuffer) {
        //Apply window
        double[] Buffer = Window(audioBuffer);

        int bufferSize = Buffer.length;

        // Autocorrelation function
        double[] difference=Autocorrelation(Buffer);

        // Cumulative mean normalized difference function
        double[]  cumulativeMeanNormalizedDifference = CumulativeMeanNormalizedDifference(difference, bufferSize);

        // Absolute threshold
        int lag = AbsoluteThreshold(cumulativeMeanNormalizedDifference,bufferSize);

        lag = OctaveThreshold(  bufferSize, lag, cumulativeMeanNormalizedDifference);



        //Calculating the interpolated peak using parabolic Interpolation along with absolute and octave based threshold
        double interpolatedPeak = parabolicInterpolation(cumulativeMeanNormalizedDifference  , lag );
        double pitchFrequency = SAMPLE_RATE / interpolatedPeak;

        return pitchFrequency;
    }

    private double[] Autocorrelation(double[] windowedBuffer){
        int bufferSize = windowedBuffer.length;
        double[] difference = new double[bufferSize];

        for (int lag = 0; lag < bufferSize; lag++) {
            for (int index = 0; index < bufferSize - lag; index++) {
                double diff = windowedBuffer[index] - windowedBuffer[index + lag];
                difference[lag] += diff * diff;
            }
        }
        return difference;
    }

    private double[] CumulativeMeanNormalizedDifference(double[] difference, int bufferSize){
        double[] cumulativeMeanNormalizedDifference = new double[bufferSize];

        cumulativeMeanNormalizedDifference[0] = 1;
        for (int lag = 1; lag < bufferSize; lag++) {
            double cmndf = 0;
            for (int index = 1; index <= lag; index++) {
                cmndf += difference[index];
            }
            cumulativeMeanNormalizedDifference[lag] = difference[lag] / (cmndf / lag);
        }
        return cumulativeMeanNormalizedDifference;
    }

    private int AbsoluteThreshold(double[] cumulativeMeanNormalizedDifference, int bufferSize){

        double threshold = 0.45;
        int lag;

        for (  lag = 1; lag < bufferSize-1; lag++) {
            if(cumulativeMeanNormalizedDifference[lag-1]< threshold){
                while (lag+1<bufferSize && cumulativeMeanNormalizedDifference[lag+1] < cumulativeMeanNormalizedDifference[lag]) {
                    lag++;

                }
                break;
            }
        }
        lag = lag >= bufferSize ? bufferSize - 1 : lag;
        return lag;
    }

    private int OctaveThreshold(int bufferSize, int lag, double[] cumulativeMeanNormalizedDifference){
        int subOctaves = 3;
        int subOctaveSize = bufferSize / subOctaves;
        int subOctaveStart = (lag / subOctaveSize) * subOctaveSize;
        int subOctaveEnd = subOctaveStart + subOctaveSize;
        for (int i = subOctaveStart + 1; i < subOctaveEnd && i < cumulativeMeanNormalizedDifference.length; i++) {
            if (cumulativeMeanNormalizedDifference[i] < cumulativeMeanNormalizedDifference[lag]) {
                lag = i;
            }
        }
        return lag;
    }

    public double parabolicInterpolation(double[] cumulativeMeanNormalizedDifference , int lag  ) {

        int x0 = lag < 1 ? lag : lag - 1;
        int x2 = lag + 1 < cumulativeMeanNormalizedDifference.length ? lag + 1 : lag;


        double newLag;

        if (x0 == lag) {
            if (cumulativeMeanNormalizedDifference[lag] <= cumulativeMeanNormalizedDifference[x2]) {
                newLag = lag;
            } else {
                newLag = x2;
            }
        } else if (x2 == lag) {
            if (cumulativeMeanNormalizedDifference[lag] <= cumulativeMeanNormalizedDifference[x0]) {
                newLag = lag;
            } else {
                newLag = x0;
            }
        } else {

            double s0 = cumulativeMeanNormalizedDifference[x0];
            double s1 = cumulativeMeanNormalizedDifference[lag];
            double s2 = cumulativeMeanNormalizedDifference[x2];

            newLag = lag + (s2 - s0) / (2 * (2 * s1 - s2 - s0));
        }



        return newLag;
    }
}
