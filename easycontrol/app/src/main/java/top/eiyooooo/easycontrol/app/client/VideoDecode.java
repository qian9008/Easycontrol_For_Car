package top.eiyooooo.easycontrol.app.client;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.util.Pair;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

public class VideoDecode {
  private MediaCodec decodec;
  private final MediaCodec.Callback callback = new MediaCodec.Callback() {
    @Override
    public void onInputBufferAvailable(MediaCodec mediaCodec, int inIndex) {
      intputBufferQueue.offer(inIndex);
      checkDecode();
    }

    @Override
    public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int outIndex, @NonNull MediaCodec.BufferInfo bufferInfo) {
      // 修复巨大的延迟Bug：
      // 服务端传来的 bufferInfo.presentationTimeUs 是微秒级的，且基于服务端的开机时间。
      // releaseOutputBuffer(int, long) 期望的是基于客户端开机时间的纳秒（nanoseconds）。
      // 错误的时间戳会导致 SurfaceFlinger 将画面挂起很久甚至丢弃。改为 true 表示立即渲染。
      mediaCodec.releaseOutputBuffer(outIndex, true);
    }

    @Override
    public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
    }

    @Override
    public void onOutputFormatChanged(MediaCodec mediaCodec, MediaFormat format) {
    }
  };

  public VideoDecode(Pair<Integer, Integer> videoSize, Surface surface, Pair<byte[], Long> csd0, Pair<byte[], Long> csd1, Handler handler) throws IOException {
    setVideoDecodec(videoSize, surface, csd0, csd1, handler);
  }

  public void release() {
    try {
      decodec.stop();
      decodec.release();
    } catch (Exception ignored) {
    }
  }

  private final LinkedBlockingQueue<Pair<byte[], Long>> intputDataQueue = new LinkedBlockingQueue<>();
  private final LinkedBlockingQueue<Integer> intputBufferQueue = new LinkedBlockingQueue<>();

  public void decodeIn(byte[] data, long pts) {
    intputDataQueue.offer(new Pair<>(data, pts));
    checkDecode();
  }

  private synchronized void checkDecode() {
    if (intputDataQueue.isEmpty() || intputBufferQueue.isEmpty()) return;
    Integer inIndex = intputBufferQueue.poll();
    Pair<byte[], Long> data = intputDataQueue.poll();
    decodec.getInputBuffer(inIndex).put(data.first);
    decodec.queueInputBuffer(inIndex, 0, data.first.length, data.second, 0);
    checkDecode();
  }

  // 创建Codec
  private void setVideoDecodec(Pair<Integer, Integer> videoSize, Surface surface, Pair<byte[], Long> csd0, Pair<byte[], Long> csd1, Handler handler) throws IOException {
    boolean isH265Support = csd1 == null;
    // 创建解码器
    String codecMime = isH265Support ? MediaFormat.MIMETYPE_VIDEO_HEVC : MediaFormat.MIMETYPE_VIDEO_AVC;
    decodec = MediaCodec.createDecoderByType(codecMime);
    MediaFormat decodecFormat = MediaFormat.createVideoFormat(codecMime, videoSize.first, videoSize.second);
    // 获取视频标识头
    decodecFormat.setByteBuffer("csd-0", ByteBuffer.wrap(csd0.first));
    if (!isH265Support) decodecFormat.setByteBuffer("csd-1", ByteBuffer.wrap(csd1.first));
    // 开启低延迟解码 (Android 11+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        decodecFormat.setInteger(MediaFormat.KEY_LOW_LATENCY, 1);
    }
    // 异步解码
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      decodec.setCallback(callback, handler);
    } else decodec.setCallback(callback);
    // 配置解码器
    decodec.configure(decodecFormat, surface, null, 0);
    // 启动解码器
    decodec.start();
    // 解析首帧，解决开始黑屏问题
    decodeIn(csd0.first, csd0.second);
    if (!isH265Support) decodeIn(csd1.first, csd1.second);
  }

}
