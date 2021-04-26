package cn.com.fiis.fine.proxy;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;

/** 基础代理Controller */
public abstract class ProxyController {
	private String targetBaseUrl;
	private String prifix;

	public String getTargetBaseUrl() {
		return targetBaseUrl;
	}

	public void setTargetBaseUrl(String targetBaseUrl) {
		this.targetBaseUrl = targetBaseUrl;
	}

	public String getPrifix() {
		return prifix;
	}

	public void setPrifix(String prifix) {
		this.prifix = prifix;
	}

	@Autowired
	FineProxy fineProxy;

	@RequestMapping(path = "/**")
	public ResponseEntity<?> proxy(HttpServletRequest request, HttpServletResponse response) {
		return fineProxy.doProxy(request, response, targetBaseUrl, prifix);
	}

}
