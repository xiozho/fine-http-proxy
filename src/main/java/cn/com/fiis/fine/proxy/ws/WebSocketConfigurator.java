package cn.com.fiis.fine.proxy.ws;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

/** WS配置类 */
public class WebSocketConfigurator extends ServerEndpointConfig.Configurator {
	public static final String ATTR_NAME_HTTP_SESSION_ID = "http.session.id";
	public static final String ATTR_NAME_HTTP_PARAMETER = "http.parameter";
	public static final String ATTR_NAME_HTTP_SESSION = "http.session";
	public static final String ATTR_NAME_HTTP_HEADERS = "http.header";

	@Override
	public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
		Map<String, Object> attributes = sec.getUserProperties();
		HttpSession session = (HttpSession) request.getHttpSession();
		if (session != null) {
			attributes.put(ATTR_NAME_HTTP_SESSION_ID, session.getId());
			Enumeration<String> names = session.getAttributeNames();
			Map<String, Object> attrs = new HashMap<>();
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				attrs.put(name, session.getAttribute(name));
			}
			attributes.put(ATTR_NAME_HTTP_SESSION, attrs);
		}
		Map<String, List<String>> params = request.getParameterMap();
		if (params != null) {
			attributes.put(ATTR_NAME_HTTP_PARAMETER, params);
		}
		Map<String, List<String>> headers = request.getHeaders();
		if (headers != null) {
			attributes.put(ATTR_NAME_HTTP_HEADERS, headers);
		}
	}
}