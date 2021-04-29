package cn.com.fiis.fine.proxy.ws;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;

import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

/** Copies data from the client to the server session. */
public class ProxyWsHandler extends AbstractWebSocketHandler {
	private static final Logger logger = Logger.getLogger(ProxyWsHandler.class.getName());

	private boolean enabledLog; // 是否记录日志
	private final Session session; // 页面请求者Session

	public ProxyWsHandler(Session session) {
		this(session, true);
	}

	public ProxyWsHandler(Session session, boolean enabledLog) {
		this.session = session;
		this.enabledLog = enabledLog;
	}

	/** 客户端Session是否可用 */
	public boolean isCanuse() {
		return session != null && session.isOpen();
	}

	public Session getSession() {
		return session;
	}

	@Override
	public void handleMessage(WebSocketSession se, WebSocketMessage<?> message) throws Exception {
		if (isCanuse()) {
			if (enabledLog) {
				logger.info(String.format("ProxyWs[SERVER]MsgFor(%s -> %s):%s", se.getId(), session.getId(),
						message.getPayload()));
			}
			RemoteEndpoint.Async sender = session.getAsyncRemote();
			if (message instanceof TextMessage) {
				TextMessage m = (TextMessage) message;
				String msg = m.getPayload();
				sender.sendText(msg);
			} else if (message instanceof BinaryMessage) {
				BinaryMessage m = (BinaryMessage) message;
				ByteBuffer msg = m.getPayload();
				sender.sendBinary(msg);
			} else if (message instanceof PongMessage) {
				PongMessage m = (PongMessage) message;
				ByteBuffer msg = m.getPayload();
				sender.sendPong(msg);
			} else if (message instanceof PingMessage) {
				PingMessage m = (PingMessage) message;
				ByteBuffer msg = m.getPayload();
				sender.sendPing(msg);
			} else {
				Object msg = message.getPayload();
				sender.sendObject(msg);
			}
		} else {
			try {
				if (enabledLog) {
					logger.info(String.format("ProxyWs[CLIENT]CloseFor(-> %s)", se.getId()));
				}
				se.close(CloseStatus.NO_CLOSE_FRAME);
			} catch (Exception e) {
				logger.warning(String.format("ProxyWs[CLIENT]CloseFor(-> %s)Error: %s", se.getId(), e));
			}
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession se, CloseStatus status) throws Exception {
		super.afterConnectionClosed(se, status);
		if (isCanuse()) {
			if (enabledLog) {
				String sid = se == null ? "" : se.getId();
				logger.info(String.format("ProxyWs[SERVER]CloseFor(%s -> %s)", sid, session.getId()));
			}
			if (status != null) {
				CloseReason reason = new CloseReason(CloseCodes.getCloseCode(status.getCode()), status.getReason());
				session.close(reason);
			} else {
				session.close();
			}
		}
	}

	public void setEnabledLog(boolean enabledLog) {
		this.enabledLog = enabledLog;
	}

}
