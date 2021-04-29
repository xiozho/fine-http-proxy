package cn.com.fiis.fine.proxy;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Websocket过滤器:转发协议 */
public class FineProxyFilter implements Filter {
	private static final Logger logger = Logger.getLogger(FineProxyFilter.class.getName());

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		try {
			HttpServletRequest request = (HttpServletRequest) servletRequest;
			HttpServletResponse response = (HttpServletResponse) servletResponse;
			String secWsProtocol = request.getHeader("Sec-WebSocket-Protocol");
			if (secWsProtocol != null) {
				response.setHeader("Sec-WebSocket-Protocol", secWsProtocol);
				request.getSession().setAttribute("ip", request.getRemoteHost());
			}
		} catch (Exception e) {
			logger.warning("" + e);
		}
		filterChain.doFilter(servletRequest, servletResponse);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void destroy() {
	}

}