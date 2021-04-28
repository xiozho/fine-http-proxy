package cn.com.fiis.fine.proxy.ws;

import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

public class WebSocketConfigurator extends ServerEndpointConfig.Configurator {
	public static final String HTTP_SESSION_ID_ATTR_NAME = "http.session.id";
	public static final String HTTP_HEADERS_ATTR_NAME = "http.header";

	@Override
	public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
		Map<String, Object> attributes = sec.getUserProperties();
		HttpSession session = (HttpSession) request.getHttpSession();
		if (session != null) {
			attributes.put(HTTP_SESSION_ID_ATTR_NAME, session.getId());
			Enumeration<String> names = session.getAttributeNames();
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				attributes.put(name, session.getAttribute(name));
			}
		}
		Map<String, List<String>> headers = request.getHeaders();
		if (headers != null) {
			attributes.put(HTTP_HEADERS_ATTR_NAME, headers);
		}
	}
}