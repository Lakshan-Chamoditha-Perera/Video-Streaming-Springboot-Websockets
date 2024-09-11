package com.example.hosting.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;

@Slf4j
public class StreamHandler extends BinaryWebSocketHandler {

    private WebSocketSession activeClient = null;  // Keeps track of the connected client
    private boolean ffmpegInitialized = false;     // Tracks if FFmpeg has been initialized

    /**
     * Called when a new WebSocket connection is established.
     */
    @Override
    public synchronized void afterConnectionEstablished(WebSocketSession session) throws Exception {
       log.info("Connection established: {}", session.getId());
        try{
            if (activeClient != null) {
                log.warn("Connection rejected: Only one client is allowed.");
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }

            log.info("Client connected: {}", session.getId());
            activeClient = session;

            // Initialize FFmpeg process to handle video encoding and streaming
            startFFmpegProcess();
        } catch (RuntimeException e) {
            log.error("Error establishing connection: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Handles incoming binary messages from the WebSocket client (video stream data).
     */
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws IOException {
        log.info("--------------------------------------------------------------------");
        log.info("Received binary message from client: {}", session.getId());
        if (!ffmpegInitialized) {
            log.warn("FFmpeg is not initialized. Ignoring message.");
            return;
        }

        byte[] data = message.getPayload().array();  // Extract binary data from the message

        try {
            // Log the received message size for debugging purposes
            log.info("Received binary message from client, size: {} bytes", data.length);
            processVideoFrame(data);  // Placeholder method to handle video frame
        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage(), e);
        }
    }

    /**
     * Called when the WebSocket connection is closed.
     */
    @Override
    public synchronized void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
       try{
           if (activeClient != null && activeClient.getId().equals(session.getId())) {
               log.info("Client disconnected: {}", session.getId());

               // Cleanup FFmpeg resources when the connection is closed
               closeFFmpegProcess();
               activeClient = null;
           }
       } catch (RuntimeException e) {
           log.error("Error closing connection: {}", e.getMessage(), e);
           throw new RuntimeException(e);
       }
    }

    /**
     * Handles any transport error in the WebSocket connection.
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("Error in WebSocket session {}: {}", session.getId(), exception.getMessage(), exception);
        session.close(CloseStatus.SERVER_ERROR);

        // Cleanup FFmpeg resources on error
        closeFFmpegProcess();
        activeClient = null;
    }

    /**
     * Initializes FFmpeg and prepares the process for video encoding and streaming.
     */
    private void startFFmpegProcess() throws Exception {
        log.info("Initializing FFmpeg process...");

        // Placeholder: Initialize FFmpeg networking and codecs
        initializeFFmpeg();

        // Placeholder: Configure RTMP output and start the process
        configureRTMPStream();

        log.info("FFmpeg process initialized.");
        ffmpegInitialized = true;
    }

    /**
     * Processes incoming video frame data by passing it to FFmpeg.
     * Placeholder method for the actual FFmpeg logic.
     *
     * @param data The raw binary video data received from the client.
     */
    private void processVideoFrame(byte[] data) throws Exception {
        log.info("Processing video frame with size: {} bytes", data.length);

        // Placeholder: Send the raw frame to FFmpeg for encoding
        sendFrameToFFmpeg(data);
    }

    /**
     * Closes the FFmpeg process and cleans up resources.
     */
    private void closeFFmpegProcess() {
        if (ffmpegInitialized) {
            log.info("Closing FFmpeg process...");

            // Placeholder: Finalize the FFmpeg process and clean up
            finalizeFFmpeg();

            ffmpegInitialized = false;
            log.info("FFmpeg process closed successfully.");
        }
    }

    // Placeholder methods related to FFmpeg operations

    /**
     * Placeholder: Initializes FFmpeg networking, allocates resources, and configures codecs.
     */
    private void initializeFFmpeg() throws Exception {
        log.info("Initializing FFmpeg...");
        // FFmpeg initialization logic (e.g., avformat_network_init(), codec allocation) will go here.
    }

    /**
     * Placeholder: Configures the RTMP stream and sets up the output format and codecs.
     */
    private void configureRTMPStream() throws Exception {
        log.info("Configuring RTMP stream...");
        // RTMP stream configuration logic (e.g., avformat_alloc_context(), codec configuration) will go here.
    }

    /**
     * Placeholder: Sends the raw video frame to FFmpeg for encoding and streaming.
     *
     * @param data The raw video frame data.
     */
    private void sendFrameToFFmpeg(byte[] data) throws Exception {
        log.info("Sending video frame to FFmpeg...");
        // Logic to send the frame to FFmpeg for encoding (e.g., avcodec_send_frame()) will go here.
    }

    /**
     * Placeholder: Finalizes the FFmpeg process, writes the trailer, and closes resources.
     */
    private void finalizeFFmpeg() {
        log.info("Finalizing FFmpeg...");
        // FFmpeg process finalization logic (e.g., av_write_trailer(), avformat_free_context()) will go here.
    }
}
