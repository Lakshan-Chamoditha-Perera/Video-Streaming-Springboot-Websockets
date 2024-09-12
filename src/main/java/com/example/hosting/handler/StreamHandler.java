package com.example.hosting.handler;

import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.*;
import java.util.concurrent.atomic.AtomicReference;

public class StreamHandler extends BinaryWebSocketHandler {

    private AtomicReference<WebSocketSession> activeClient = new AtomicReference<>(null);
    private Process ffmpegProcess;
    private OutputStream ffmpegInput;
    private boolean isFFmpegRunning = false;
    private long totalBytesTransferred = 0;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (activeClient.get() != null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            System.out.println("Connection rejected: Only one client is allowed.");
            return;
        }

        activeClient.set(session);
        System.out.println("Client connected");
        startFFmpegProcess();
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        if (isFFmpegRunning) {
            byte[] payload = message.getPayload().array();
            totalBytesTransferred += payload.length;
//            double mbTransferred = totalBytesTransferred / 1048576.0;
            System.out.println("Data received: " + payload.length + " bytes");
            try {
                ffmpegInput.write(payload);
                ffmpegInput.flush();
                System.out.println("Data written to FFmpeg process.");
            } catch (IOException e) {
                System.err.println("Error writing to FFmpeg process: " + e.getMessage());
                isFFmpegRunning = false;
                startFFmpegProcess();
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        if (activeClient.get() == session) {
            double totalMBTransferred = totalBytesTransferred / 1048576.0;
            System.out.println("Client disconnected. Total data transferred: " + totalMBTransferred + " MB");
            closeFFmpeg();
            activeClient.set(null);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket error: " + exception.getMessage());
        closeFFmpeg();
        activeClient.set(null);
    }

    private void startFFmpegProcess() throws IOException {
        if (isFFmpegRunning) {
            return;
        }

        // Spawn FFmpeg process to stream video to the RTMP server
        ProcessBuilder builder = new ProcessBuilder(
                "ffmpeg",
                "-i", "pipe:0",                // Input from stdin (WebSocket data)
                "-c:v", "libx264",             // Encode video with H.264 codec
                "-preset", "veryfast",         // Very fast preset for faster encoding
                "-tune", "zerolatency",        // Low-latency tuning for streaming
                "-pix_fmt", "yuv420p",         // Pixel format
                "-b:v", "1000k",               // Set video bitrate to 1000 Kbps
                "-maxrate", "1000k",           // Max bitrate
                "-bufsize", "2000k",           // Buffer size
                "-g", "60",                    // Set GOP (keyframe interval) to 60 frames
                "-keyint_min", "60",           // Minimum keyframe interval
                "-sc_threshold", "0",          // Disable scene change detection
                "-c:a", "aac",                 // Audio codec
                "-b:a", "128k",                // Audio bitrate
                "-f", "flv",                   // Output format (FLV for RTMP streaming)
                "rtmp://192.168.1.69:1935/live/stream" // RTMP destination
        );

        builder.redirectErrorStream(true);
        ffmpegProcess = builder.start();
        ffmpegInput = new BufferedOutputStream(ffmpegProcess.getOutputStream());
        isFFmpegRunning = true;

        // Capture FFmpeg output for debugging
        new Thread(() -> {
            try (var reader = new BufferedReader(new InputStreamReader(ffmpegProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("FFmpeg Log: " + line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void closeFFmpeg() throws IOException {
        if (ffmpegInput != null) {
            ffmpegInput.close();
        }
        if (ffmpegProcess != null) {
            ffmpegProcess.destroy();
        }
        isFFmpegRunning = false;
        System.out.println("FFmpeg process terminated.");
    }
}
