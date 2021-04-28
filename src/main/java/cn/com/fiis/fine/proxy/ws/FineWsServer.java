package cn.com.fiis.fine.proxy.ws;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

/** 基础WS代理类 */
public abstract class FineWsServer {
	private static final Logger logger = Logger.getLogger(FineWsServer.class.getName());

	/** 目标 */
	private static final ConcurrentHashMap<Class<?>, List<String>> targetBaseUrls = new ConcurrentHashMap<>();

	/** 添加路径 */
	public final void addTargetUrls(List<String> targetBaseUrl) {
		targetBaseUrls.put(getClass(), targetBaseUrl);
	}

	/** 代理 */
	private static FineProxyWS fineProxy;

	public static final void setFineProxy(FineProxyWS fineProxy) {
		if (FineWsServer.fineProxy == null) {
			FineWsServer.fineProxy = fineProxy;
		}
	}

	@OnOpen
	public void onOpen(Session session) {
		try {
			List<String> targetBaseUrl = targetBaseUrls.get(getClass());
			fineProxy.createProxySession(session, targetBaseUrl);
		} catch (Exception e) {
			throw new RuntimeException("CreateProxySession Error.", e);
		}
	}

	/** 客户端关闭 */
	@OnClose
	public void onClose(Session session, CloseReason reason) {
		fineProxy.closeProxySession(session);
	}

	/** 错误 */
	@OnError
	public void onError(Session session, Throwable throwable) {
		logger.fine(throwable.getMessage());
	}

	/** 收到客户端发来消息 */
	@OnMessage
	public void onMessage(Session session, ByteBuffer messages) {
		try {
			fineProxy.sendMessage(session, messages);
		} catch (IOException e) {
			throw new RuntimeException("sendMessage Error.", e);
		}
	}

	/** 收到客户端发来消息 */
	@OnMessage
	public void onMessage(Session session, String message) {
		try {
			fineProxy.sendMessage(session, message);
		} catch (IOException e) {
			throw new RuntimeException("sendMessage Error.", e);
		}
	}

}