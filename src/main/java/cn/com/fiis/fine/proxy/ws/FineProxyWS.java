package cn.com.fiis.fine.proxy.ws;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.Session;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

/** 代理 */
@Component
public class FineProxyWS {
	private static final Logger logger = Logger.getLogger(FineProxyWS.class.getName());

	public static final String HTTP_SESSION_ID_ATTR_NAME = "http.session.id";
	public static final String HTTP_HEADERS_ATTR_NAME = "http.header";

	/** 服务器端Session */
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

	/** 创建代理Session */
	public WebSocketSession createSession(Session session, List<String> urls, Map<String, Object> props)
			throws Exception {
		if (urls == null) {
			throw new IllegalArgumentException("Argument[targetUrl] must be not null.");
		}
		urls.removeAll(Collections.singleton(null));
		urls.removeAll(Collections.singleton(""));
		if (urls.isEmpty()) {
			throw new IllegalArgumentException("Argument[targetUrl] must be not empty.");
		}
		String wsUrl;
		if (urls.size() > 1) {
			wsUrl = urls.get(random.nextInt(urls.size()));
		} else {
			wsUrl = urls.get(0);
		}
		if (enabledLog) {
			logger.info("ProxyWSFor-> " + wsUrl);
		}
		try {
			WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
			if (props != null) {
				try {
					@SuppressWarnings("unchecked")
					Map<String, List<String>> httpHeaders = (Map<String, List<String>>) props
							.get(HTTP_HEADERS_ATTR_NAME);
					if (httpHeaders != null) {
						httpHeaders.forEach((k, values) -> {
							values.forEach(x -> {
								headers.add(k, x);
							});
						});
					}
				} catch (Exception e) {
					logger.info(String.format("ProxyWSFor-> ( %s) [Header]Error: %s", wsUrl, e));
				}
			}
			URI wsUri = new URI(wsUrl);
			WebSocketSession se = wsClient.doHandshake(new ProxyWsHandler(session, enabledLog), headers, wsUri).get();
			SERVER_SESSION_MAP.put(session.getId(), se);
			return se;
		} catch (Exception e) {
			throw e;
		}
	}

	/** 关闭代理Session */
	public void closeSession(Session session, CloseReason reason) {
		WebSocketSession se = SERVER_SESSION_MAP.remove(session.getId());
		if (se != null && se.isOpen()) {
			try {
				if (reason != null) {
					CloseStatus status = new CloseStatus(reason.getCloseCode().getCode(), reason.getReasonPhrase());
					se.close(status);
				} else {
					se.close();
				}
			} catch (IOException e) {
				// nothing
			}
		}
	}

	/** 发送消息 */
	public void sendMessage(Session session, String message) throws IOException {
		WebSocketSession se = SERVER_SESSION_MAP.remove(session.getId());
		if (enabledLog) {
			logger.info(String.format("ProxyWs[REC]MsgFor(%s -> %s):%s", session.getId(), se.getId(), message));
		}
		if (se != null && se.isOpen()) {
			se.sendMessage(new TextMessage(message));
		} else {
			try {
				session.close(new CloseReason(CloseReason.CloseCodes.TRY_AGAIN_LATER, null));
			} catch (Exception e) {
				logger.info(String.format("ProxyWs[SEND]CloseFor-> %s", session.getId()));
			}
		}
	}

	/** 发送消息 */
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
