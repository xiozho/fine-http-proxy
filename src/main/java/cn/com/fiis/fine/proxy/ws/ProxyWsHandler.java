package cn.com.fiis.fine.proxy.ws;

import javax.websocket.Session;

import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

/** Copies data from the client to the server session. */
public class ProxyWsHandler extends AbstractWebSocketHandler {
	private final Session userSession;

	public ProxyWsHandler(Session session) {
		this.userSession = session;
	}

	@Override
	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
		userSession.getAsyncRemote().sendObject(message.getPayload());
	}

}
