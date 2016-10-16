package com.andrew.systemservice;

import java.util.LinkedList;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;
import android.widget.Button;


public class SystemService3
{

    private static final String TAG = "RecordPlayActivity";
    /**
     * 按钮
     */
    private Button bt_exit;
    /**
     * AudioRecord 写入缓冲区大小
     */
    protected int m_in_buf_size;
    /**
     * 录制音频对象
     */
    private AudioRecord m_in_rec;
    /**
     * 录入的字节数组
     */
    private byte[] m_in_bytes;
    /**
     * 存放录入字节数组的大小
     */
    private LinkedList<byte[]> m_in_q;
    /**
     * AudioTrack 播放缓冲大小
     */
    private int m_out_buf_size;
    /**
     * 播放音频对象
     */
    private AudioTrack m_out_trk;
    /**
     * 播放的字节数组
     */
    private byte[] m_out_bytes;
    /**
     * 录制音频线程
     */
    private Thread record;
    /**
     * 播放音频线程
     */
    private Thread play;
    /**
     * 让线程停止的标志
     */
    private boolean flag = true;


    public void listen()
    {

        init();
        record = new Thread(new recordSound());
        play = new Thread(new playRecord());
        // 启动录制线程
        record.start();
        // 启动播放线程
        play.start();
    }

    private void init()
    {
        // AudioRecord 得到录制最小缓冲区的大小
        m_in_buf_size = AudioRecord.getMinBufferSize(8000,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        // 实例化播放音频对象
        m_in_rec = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT, m_in_buf_size);
        // 实例化一个字节数组，长度为最小缓冲区的长度
        m_in_bytes = new byte[m_in_buf_size];
        // 实例化一个链表，用来存放字节组数
        m_in_q = new LinkedList<byte[]>();

        // AudioTrack 得到播放最小缓冲区的大小
        m_out_buf_size = AudioTrack.getMinBufferSize(8000,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        // 实例化播放音频对象
        m_out_trk = new AudioTrack(AudioManager.STREAM_MUSIC, 8000,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT, m_out_buf_size,
                AudioTrack.MODE_STREAM);
        // 实例化一个长度为播放最小缓冲大小的字节数组
        m_out_bytes = new byte[m_out_buf_size];
    }


    private void finish() {
        if(!flag) {
            m_in_rec.stop();
            m_in_rec.release();
        }
    }

    class recordSound implements Runnable
    {
        @Override
        public void run()
        {
            Log.i(TAG, "........recordSound run()......");
            byte[] bytes_pkg;
            // 开始录音
            m_in_rec.startRecording();

            while (flag)
            {
                m_in_rec.read(m_in_bytes, 0, m_in_buf_size);
                bytes_pkg = m_in_bytes.clone();
                Log.i(TAG, "........recordSound bytes_pkg==" + bytes_pkg.length);
                if (m_in_q.size() >= 2)
                {
                    m_in_q.removeFirst();
                }
                m_in_q.add(bytes_pkg);
            }
        }

    }

    /**
     * <p>
     * 文件名称 ：RecordPlayActivity.java
     * <p>
     * 内容摘要 ：
     * <p>
     * 作者 ：吴辰彪 创建时间 ：2012-7-25 下午01:57:34 描述 :播放线程
     */
    class playRecord implements Runnable
    {
        @Override
        public void run()
        {
            // TODO Auto-generated method stub
            Log.i(TAG, "........playRecord run()......");
            byte[] bytes_pkg = null;
            // 开始播放
            m_out_trk.play();

            while (flag)
            {
                try
                {
                    m_out_bytes = m_in_q.getFirst();
                    bytes_pkg = m_out_bytes.clone();
                    m_out_trk.write(bytes_pkg, 0, bytes_pkg.length);
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

}