package cn.com.fiis.fine;

/** 配置项 */
public class FineProxyProp {
	/** 名称 */
	private String name;
	/** 拦截路径 */
	private String path;
	/** 目标路径 */
	private String targetUrl;
	/** 截取前缀 */
	private String truncatePrifix;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getTargetUrl() {
		return targetUrl;
	}

	public void setTargetUrl(String targetUrl) {
		this.targetUrl = targetUrl;
	}

	public String getTruncatePrifix() {
		return truncatePrifix;
	}

	public void setTruncatePrifix(String truncatePrifix) {
		this.truncatePrifix = truncatePrifix;
	}

}
