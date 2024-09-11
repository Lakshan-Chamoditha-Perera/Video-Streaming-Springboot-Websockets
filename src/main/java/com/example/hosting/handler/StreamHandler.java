package com.example.hosting.handler;

import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

public class StreamHandler extends BinaryWebSocketHandler {

    private AtomicReference<WebSocketSession> activeClient = new AtomicReference<>(null);
    private Process ffmpegProcess;
    private OutputStream ffmpegInput;
    private long lastDataTimestamp;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (activeClient.get() != null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            System.out.println("Connection rejected: Only one client is allowed.");
            return;
        }

        activeClient.set(session);
        System.out.println("Client connected");

        // Start FFmpeg process
        startFFmpegProcess();

        // Log FFmpeg output for debugging
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(ffmpegProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("FFmpeg Log: " + line);  // Log FFmpeg output
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        if (ffmpegProcess.isAlive()) {
            byte[] payload = message.getPayload().array();
            ffmpegInput.write(payload);  // Write the WebSocket data to FFmpeg
            ffmpegInput.flush();  // Ensure immediate writing
            lastDataTimestamp = System.currentTimeMillis();
            System.out.println("Data transferred: " + payload.length + " bytes");
        } else {
            System.out.println("FFmpeg process is no longer alive, restarting...");
            restartFFmpegProcess();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        if (activeClient.get() == session) {
            System.out.println("Client disconnected");
            try {
                closeFFmpeg();
            } catch (IOException e) {
                e.printStackTrace();
            }
            activeClient.set(null);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket error: " + exception.getMessage());
        try {
            closeFFmpeg();
        } catch (IOException e) {
            e.printStackTrace();
        }
        activeClient.set(null);
    }

    private void startFFmpegProcess() throws IOException {
        ProcessBuilder builder = new ProcessBuilder(
                "ffmpeg",
                "-i",//
                "pipe:0" ,
                "-c:v", "libx264", "-preset", "veryfast", "-tune", "zerolatency", "-pix_fmt", "yuv420p",
                "-b:v", "1000k", "-maxrate", "1000k", "-bufsize", "2000k",
                "-g", "60", "-keyint_min", "60", "-sc_threshold", "0",
                "-c:a", "aac", "-b:a", "128k",
                "-f", "flv", "rtmp://192.168.1.69:1935/live/stream"
        );
        builder.redirectErrorStream(true); // Capture both stdout and stderr
        ffmpegProcess = builder.start();
        ffmpegInput = new BufferedOutputStream(ffmpegProcess.getOutputStream());
        lastDataTimestamp = System.currentTimeMillis();
    }

    private void restartFFmpegProcess() throws IOException {
        closeFFmpeg();
        startFFmpegProcess();
    }

    private void closeFFmpeg() throws IOException {
        if (ffmpegInput != null) {
            ffmpegInput.close();
        }
        if (ffmpegProcess != null) {
            ffmpegProcess.destroy();
        }
    }
}
