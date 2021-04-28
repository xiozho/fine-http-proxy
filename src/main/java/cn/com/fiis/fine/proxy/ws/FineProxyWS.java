package cn.com.fiis.fine.proxy.ws;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.websocket.Session;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import cn.com.fiis.fine.proxy.http.FineProxy;

/** 代理 */
@Component
public class FineProxyWS {
	private static final Logger logger = Logger.getLogger(FineProxy.class.getName());

	private static final ConcurrentHashMap<String, WebSocketSession> SERVER_SESSION_MAP = new ConcurrentHashMap<>();

	/** 是否记录日志 */
	private boolean enabledLog;
	private final StandardWebSocketClient wsClient;
	private final Random random;

	public FineProxyWS() {
		this(true);
	}

	public FineProxyWS(boolean enabledLog) {
		this.enabledLog = enabledLog;
		this.wsClient = new StandardWebSocketClient();
		this.random = new Random();
	}

	public WebSocketSession createProxySession(Session session, List<String> wsUrls) throws Exception {
		if (wsUrls == null) {
			throw new IllegalArgumentException("Argument[targetUrl] must be not null.");
		}
		wsUrls.removeAll(Collections.singleton(null));
		wsUrls.removeAll(Collections.singleton(""));
		if (wsUrls.isEmpty()) {
			throw new IllegalArgumentException("Argument[targetUrl] must be not empty.");
		}
		String wsUrl;
		if (wsUrls.size() > 1) {
			wsUrl = wsUrls.get(random.nextInt(wsUrls.size()));
		} else {
			wsUrl = wsUrls.get(0);
		}
		if (enabledLog) {
			logger.info("ProxyWSFor-> " + wsUrl);
		}
		try {
			WebSocketSession se = wsClient.doHandshake(new ProxyWsHandler(session), wsUrl).get();
			SERVER_SESSION_MAP.put(session.getId(), se);
			return se;
		} catch (Exception e) {
			throw e;
		}
	}

	public void closeProxySession(Session session) {
		WebSocketSession se = SERVER_SESSION_MAP.remove(session.getId());
		if (se != null && se.isOpen()) {
			try {
				se.close();
			} catch (IOException e) {
				// nothing
			}
		}
	}

	public void sendMessage(Session session, String message) throws IOException {
		WebSocketSession se = SERVER_SESSION_MAP.remove(session.getId());
		if (se != null && se.isOpen()) {
			se.sendMessage(new TextMessage(message));
		}
	}

	public void sendMessage(Session session, ByteBuffer message) throws IOException {
		WebSocketSession se = SERVER_SESSION_MAP.remove(session.getId());
		if (se != null && se.isOpen()) {
			se.sendMessage(new BinaryMessage(message));
		}
	}

	public void setEnabledLog(boolean enabledLog) {
		this.enabledLog = enabledLog;
	}

}
