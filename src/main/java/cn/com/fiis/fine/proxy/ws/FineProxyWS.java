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

	// 参数Key
	public static final String ATTR_NAME_HTTP_SESSION_ID = "http.session.id";
	public static final String ATTR_NAME_HTTP_PARAMETER = "http.parameter";
	public static final String ATTR_NAME_HTTP_SESSION = "http.session";
	public static final String ATTR_NAME_HTTP_HEADERS = "http.header";

	/** 服务器端Session */
	private static final ConcurrentHashMap<String, WebSocketSession> SERVER_SESSION_MAP = new ConcurrentHashMap<>();
	/** 服务器端处理类 */
	private static final ConcurrentHashMap<String, FineProxyWsHandler> SERVER_HANDLER_MAP = new ConcurrentHashMap<>();

	/** 是否记录日志 */
	private boolean enabledLog;
	private final Random random;
	private final StandardWebSocketClient wsClient;

	public FineProxyWS() {
		this(true);
	}

	public FineProxyWS(boolean enabledLog) {
		this.enabledLog = enabledLog;
		this.wsClient = new StandardWebSocketClient();
		this.random = new Random();
		Thread heartbeatThread = new Thread(() -> {
			while (!Thread.currentThread().isInterrupted()) {
				// 定时清理数据
				SERVER_HANDLER_MAP.entrySet().removeIf(x -> {
					String sid = x.getKey();
					FineProxyWsHandler handler = x.getValue();
					WebSocketSession wss = SERVER_SESSION_MAP.get(sid);
					if (wss == null || !wss.isOpen()) {
						// 服务端已下线，通知客户端关闭后移除
						try {
							handler.afterConnectionClosed(wss, CloseStatus.SERVICE_OVERLOAD);
							return true;
						} catch (Exception e) {
							logger.warning("" + e);
							return false;
						}
					} else if (handler != null && handler.isCanuse()) {
						// 可用，不移除
						return false;
					} else {
						return true;
					}
				});
				SERVER_SESSION_MAP.entrySet().removeIf(x -> {
					String sid = x.getKey();
					WebSocketSession wss = x.getValue();
					FineProxyWsHandler handler = SERVER_HANDLER_MAP.get(sid);
					if (handler == null || !handler.isCanuse()) {
						// 客户端已下线，通知服务端关闭后移除
						try {
							wss.close(CloseStatus.GOING_AWAY);
							return true;
						} catch (Exception e) {
							logger.warning("" + e);
							return false;
						}
					} else if (wss != null && wss.isOpen()) {
						// 可用，不移除
						return false;
					} else {
						return true;
					}
				});
				try {
					Thread.sleep(1111);
				} catch (InterruptedException e) {
				}
			}
		});
		heartbeatThread.setDaemon(true);
		heartbeatThread.start();
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
			logger.info(String.format("ProxyWS[CONNECT]For(-> %s)", wsUrl));
		}
		try {
			WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
			if (props != null) {
				try {
					@SuppressWarnings("unchecked")
					Map<String, List<String>> httpHeaders = (Map<String, List<String>>) props
							.get(ATTR_NAME_HTTP_HEADERS);
					if (httpHeaders != null) {
						httpHeaders.forEach((k, values) -> {
							values.forEach(x -> {
								headers.add(k, x);
							});
						});
					}
				} catch (Exception e) {
					logger.warning(String.format("ProxyWS[CONNECT]For(-> %s)[Header]Error: %s", wsUrl, e));
				}
			}
			URI wsUri = new URI(wsUrl);
			FineProxyWsHandler handler = new FineProxyWsHandler(session, enabledLog);
			WebSocketSession se = wsClient.doHandshake(handler, headers, wsUri).get();
			SERVER_SESSION_MAP.put(session.getId(), se);
			SERVER_HANDLER_MAP.put(session.getId(), handler);
			return se;
		} catch (Exception e) {
			logger.warning(String.format("ProxyWS[CONNECT]For(-> %s)Error: %s", wsUrl, e));
			throw e;
		}
	}

	/** 关闭代理Session */
	public void closeSession(Session session, CloseReason reason) {
		WebSocketSession se = SERVER_SESSION_MAP.remove(session.getId());
		if (se != null && se.isOpen()) {
			if (enabledLog) {
				String sid = session == null ? "" : session.getId();
				logger.info(String.format("ProxyWs[CLIENT]CloseFor(%s -> %s)", sid, se.getId()));
			}
			try {
				if (reason != null) {
					CloseStatus status = new CloseStatus(reason.getCloseCode().getCode(), reason.getReasonPhrase());
					se.close(status);
				} else {
					se.close();
				}
			} catch (IOException e) {
				logger.warning(String.format("ProxyWs[CLIENT]CloseFor(-> %s)Error: %s", session.getId(), e));
			}
		}
	}

	/** 发送消息 */
	public void sendMessage(Session session, String message) throws IOException {
		WebSocketSession se = SERVER_SESSION_MAP.remove(session.getId());
		if (se != null && se.isOpen()) {
			if (enabledLog) {
				logger.info(String.format("ProxyWs[CLIENT]MsgFor(%s -> %s):%s", session.getId(), se.getId(), message));
			}
			se.sendMessage(new TextMessage(message));
		} else {
			try {
				if (enabledLog) {
					logger.info(String.format("ProxyWs[SERVER]CloseFor(-> %s)", session.getId()));
				}
				session.close(new CloseReason(CloseReason.CloseCodes.TRY_AGAIN_LATER, null));
			} catch (Exception e) {
				logger.warning(String.format("ProxyWs[SERVER]CloseFor(-> %s)Error: %s", session.getId(), e));
			}
		}
	}

	/** 发送消息 */
	public void sendMessage(Session session, ByteBuffer message) throws IOException {
		WebSocketSession se = SERVER_SESSION_MAP.remove(session.getId());
		if (se != null && se.isOpen()) {
			if (enabledLog) {
				logger.info(String.format("ProxyWs[CLIENT]MsgFor(%s -> %s):%s", session.getId(), se.getId(), message));
			}
			se.sendMessage(new BinaryMessage(message));
		} else {
			try {
				if (enabledLog) {
					logger.info(String.format("ProxyWs[SERVER]CloseFor(-> %s)", session.getId()));
				}
				session.close(new CloseReason(CloseReason.CloseCodes.TRY_AGAIN_LATER, null));
			} catch (Exception e) {
				logger.warning(String.format("ProxyWs[SERVER]CloseFor(-> %s)Error: %s", session.getId(), e));
			}
		}
	}

	public void setEnabledLog(boolean enabledLog) {
		this.enabledLog = enabledLog;
	}

}
