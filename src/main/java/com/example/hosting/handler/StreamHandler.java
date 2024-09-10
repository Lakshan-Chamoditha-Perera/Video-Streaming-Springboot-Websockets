package com.example.hosting.handler;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class StreamHandler extends BinaryWebSocketHandler {

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        byte[] streamData = message.getPayload().array();
        log.info("Received binary message with size: {} KB", streamData.length/1024);

        // Forward the binary stream to FFmpeg or NGINX here
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established.");
    }

}