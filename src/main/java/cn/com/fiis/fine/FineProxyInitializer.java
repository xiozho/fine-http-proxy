package cn.com.fiis.fine;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import cn.com.fiis.fine.proxy.FineManager;
import cn.com.fiis.fine.proxy.FineProxyFilter;
import cn.com.fiis.fine.proxy.ProxyCompiler;
import cn.com.fiis.fine.proxy.ProxyProp;
import cn.com.fiis.fine.proxy.http.FineController;
import cn.com.fiis.fine.proxy.http.FineProxy;
import cn.com.fiis.fine.proxy.ws.FineProxyWS;
import cn.com.fiis.fine.proxy.ws.FineWsServer;

@Configuration
@ConfigurationProperties(prefix = "fine")
public class FineProxyInitializer implements ServletContextInitializer {
	private static final Logger logger = Logger.getLogger(FineProxyInitializer.class.getName());

	private boolean enabledLog = true; // 跟踪日志
	private boolean enable = false; // 是否启动(默认否)
	private List<ProxyProp> proxys; // 代理信息
	private List<ProxyProp> proxysWs; // WebSocket代理信息

	@Bean
	@ConditionalOnMissingBean(FineManager.class)
	public FineManager fineManager() {
		return new FineManager();
	}

	@Bean
	@ConditionalOnMissingBean(FineProxy.class)
	public FineProxy fineProxy() {
		FineProxy fine = new FineProxy();
		fine.setEnabledLog(enabledLog);
		return fine;
	}

	@Bean
	@ConditionalOnMissingBean(FineProxyWS.class)
	public FineProxyWS fineProxyWS() {
		FineProxyWS fine = new FineProxyWS();
		fine.setEnabledLog(enabledLog);
		return fine;
	}

	@Bean
	@ConditionalOnBean(FineProxyWS.class)
	public FilterRegistrationBean<FineProxyFilter> fineWsFilter() {
		FilterRegistrationBean<FineProxyFilter> filter = new FilterRegistrationBean<>();
		filter.setName("FineProxyFilter");
		filter.setFilter(new FineProxyFilter());
		filter.addUrlPatterns("/*");
		return filter;
	}

	@Autowired
	private DefaultListableBeanFactory factory;

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		if (!enable) {
			return;// 未启用代理
		}
		AtomicInteger nameIndex = new AtomicInteger(1);
		if (proxys != null && !proxys.isEmpty()) {
			proxys.forEach(x -> {
				String name = x.getName();
				if (name == null || name.isEmpty()) {
					name = String.format("%04d", nameIndex.getAndAdd(1));
				}
				String path = x.getPath();
				List<String> targetUrl = x.getTargetUrl();
				String truncatePrifix = x.getTruncatePrifix();
				try {
					FineController controller = ProxyCompiler.newProxyController(name, path);
					controller.setTargetBaseUrl(targetUrl);
					controller.setPrifix(truncatePrifix);
					factory.autowireBean(controller); // 自动注入
					String beanName = "FineProxyController$" + name;
					factory.registerSingleton(beanName, controller); // 注册单例Bean
					FineManager.registerController(beanName); // 注入Controller映射
					if (enabledLog) {
						logger.info(String.format("Inject:%s -> %s", beanName, controller.getClass()));
					}
				} catch (Exception e) {
					logger.fine("" + e);
				}
			});
		}
		if (proxysWs != null && !proxysWs.isEmpty()) {
			FineWsServer.setFineProxy(fineProxyWS());
			proxysWs.forEach(x -> {
				String name = x.getName();
				if (name == null || name.isEmpty()) {
					name = String.format("%04d", nameIndex.getAndAdd(1));
				}
				String path = x.getPath();
				List<String> targetUrl = x.getTargetUrl();
				try {
					FineWsServer wsServer = ProxyCompiler.newProxyWS(name, path);
					wsServer.addTargetUrls(targetUrl);
					String wsBeanName = "FineWsServer$" + name;
					factory.registerSingleton(wsBeanName, wsServer); // 注册Bean
					if (enabledLog) {
						logger.info(String.format("Inject:%s -> %s", wsBeanName, wsServer.getClass()));
					}
				} catch (Exception e) {
					logger.fine("" + e);
				}
			});
		}
	}

	@Bean
	public ServerEndpointExporter serverEndpointExporter() {
		return new ServerEndpointExporter();
	}

	public boolean isEnabledLog() {
		return enabledLog;
	}

	public void setEnabledLog(boolean enabledLog) {
		this.enabledLog = enabledLog;
	}

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public List<ProxyProp> getProxys() {
		return proxys;
	}

	public void setProxys(List<ProxyProp> proxys) {
		this.proxys = proxys;
	}

	public List<ProxyProp> getProxysWs() {
		return proxysWs;
	}

	public void setProxysWs(List<ProxyProp> proxysWs) {
		this.proxysWs = proxysWs;
	}

}
