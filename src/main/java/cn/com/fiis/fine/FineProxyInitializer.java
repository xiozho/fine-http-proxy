package cn.com.fiis.fine;

import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cn.com.fiis.fine.proxy.FineManager;
import cn.com.fiis.fine.proxy.FineProxy;
import cn.com.fiis.fine.proxy.ProxyCompiler;
import cn.com.fiis.fine.proxy.ProxyController;
import cn.com.fiis.fine.proxy.ProxyProp;

@Configuration
@ConfigurationProperties(prefix = "fine")
public class FineProxyInitializer implements ServletContextInitializer {
	private static final Logger logger = Logger.getLogger(FineProxyInitializer.class.getName());

	private boolean enabledLog = true; // 跟踪日志
	private boolean enable = false; // 是否启动(默认否)
	private List<ProxyProp> proxys; // 代理信息

	@Bean
	@ConditionalOnMissingBean(FineManager.class)
	public FineManager fineManager() {
		return new FineManager();
	}

	@Bean
	@ConditionalOnMissingBean(FineProxy.class)
	public FineProxy FineProxy() {
		FineProxy fine = new FineProxy();
		fine.setEnabledLog(enabledLog);
		return fine;
	}

	@Autowired
	private DefaultListableBeanFactory factory;

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		if (!enable || proxys == null || proxys.isEmpty()) {
			// 未启用代理，或代理配置信息空
			return;
		}
		proxys.forEach(x -> {
			String name = x.getName();
			String path = x.getPath();
			String targetUrl = x.getTargetUrl();
			String truncatePrifix = x.getTruncatePrifix();

			ProxyController controller = ProxyCompiler.newProxyController(name, path);
			controller.setTargetBaseUrl(targetUrl);
			controller.setPrifix(truncatePrifix);

			factory.autowireBean(controller); // 自动注入
			String beanName = "FineProxyController$" + name;
			factory.registerSingleton(beanName, controller); // 注册单例Bean
			try {
				FineManager.registerController(beanName); // 注入Controller映射
			} catch (Exception e) {
				logger.warning("" + e);
			}
		});
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

}
