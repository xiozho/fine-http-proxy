package cn.com.fiis.fine;

/** 配置项 */
public class FineProxyProp {
	/** 名称 */
	private String name;
	/** 拦截路径 */
	private String servletUrl;
	/** 目标路径 */
	private String targetUrl;
	/** 跟踪日志 */
	private boolean enabledLog = false;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getServletUrl() {
		return servletUrl;
	}

	public void setServletUrl(String servletUrl) {
		this.servletUrl = servletUrl;
	}

	public String getTargetUrl() {
		return targetUrl;
	}

	public void setTargetUrl(String targetUrl) {
		this.targetUrl = targetUrl;
	}

	public boolean isEnabledLog() {
		return enabledLog;
	}

	public void setEnabledLog(boolean enabledLog) {
		this.enabledLog = enabledLog;
	}

}
