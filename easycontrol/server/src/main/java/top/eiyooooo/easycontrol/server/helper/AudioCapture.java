package top.eiyooooo.easycontrol.server.helper;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import top.eiyooooo.easycontrol.server.utils.L;
import top.eiyooooo.easycontrol.server.utils.Workarounds;

public final class AudioCapture {

    public static final int SAMPLE_RATE = 48000;
    public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;
    public static final int CHANNELS = 2;
    public static final int CHANNEL_MASK = AudioFormat.CHANNEL_IN_LEFT | AudioFormat.CHANNEL_IN_RIGHT;
    public static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    public static final int BYTES_PER_SAMPLE = 2;

    public static AudioRecord init() {
        AudioRecord recorder;
        try {
            recorder = createAudioRecord();
        } catch (Exception e) {
            L.w("Cannot create AudioRecord, try workaround");
            recorder = Workarounds.createAudioRecord(MediaRecorder.AudioSource.REMOTE_SUBMIX, SAMPLE_RATE,
                    CHANNEL_CONFIG, CHANNELS, CHANNEL_MASK, ENCODING);
        }
        recorder.startRecording();
        return recorder;
    }

    public static int millisToBytes(int millis) {
        return (SAMPLE_RATE * CHANNELS * BYTES_PER_SAMPLE / 1000) * millis;
    }

    @TargetApi(Build.VERSION_CODES.M)
    @SuppressLint({"WrongConstant", "MissingPermission"})
    private static AudioRecord createAudioRecord() {
        AudioRecord.Builder audioRecordBuilder = new AudioRecord.Builder();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) audioRecordBuilder.setContext(FakeContext.get());

        audioRecordBuilder.setAudioSource(MediaRecorder.AudioSource.REMOTE_SUBMIX);
        AudioFormat.Builder audioFormatBuilder = new AudioFormat.Builder();
        audioFormatBuilder.setEncoding(ENCODING);
        audioFormatBuilder.setSampleRate(SAMPLE_RATE);
        audioFormatBuilder.setChannelMask(CHANNEL_CONFIG);
        audioRecordBuilder.setAudioFormat(audioFormatBuilder.build());
        audioRecordBuilder.setBufferSizeInBytes(16 * AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, ENCODING));
        return audioRecordBuilder.build();
    }
}
