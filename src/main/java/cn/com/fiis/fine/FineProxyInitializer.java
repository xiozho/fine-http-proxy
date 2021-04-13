package cn.com.fiis.fine;

import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.mitre.dsmiley.httpproxy.ProxyServlet;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.ServletContextInitializer;

@ConfigurationProperties(prefix = "fine")
public class FineProxyInitializer implements ServletContextInitializer {

	private boolean enable = false; // 是否启动(默认否)
	private List<FineProxyProp> proxys; // 代理路径

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public List<FineProxyProp> getProxys() {
		return proxys;
	}

	public void setProxys(List<FineProxyProp> proxys) {
		this.proxys = proxys;
	}

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		if (!enable) {
			return; // 未启用
		}
		if (proxys == null || proxys.isEmpty()) {
			return;
		}
		proxys.forEach(prop -> {
			if (prop == null) {
				return;
			}
			if (prop.getServletUrl() == null || prop.getServletUrl().trim().length() == 0) {
				return;
			}
			ServletRegistration initServlet = servletContext.addServlet("Proxy-" + prop.getName(), ProxyServlet.class);
			initServlet.addMapping(prop.getServletUrl().split(","));
			initServlet.setInitParameter(ProxyServlet.P_TARGET_URI, prop.getTargetUrl());
			initServlet.setInitParameter(ProxyServlet.P_LOG, String.valueOf(prop.isEnabledLog()));
		});

	}

}
