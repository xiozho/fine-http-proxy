package cn.com.fiis.fine.proxy.http;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;

/** 基础代理Controller */
public abstract class FineController {
	private List<String> targetBaseUrl;
	private String prifix;

	public List<String> getTargetBaseUrl() {
		return targetBaseUrl;
	}

	public void setTargetBaseUrl(List<String> targetBaseUrl) {
		this.targetBaseUrl = targetBaseUrl;
	}

	public String getPrifix() {
		return prifix;
	}

	public void setPrifix(String prifix) {
		this.prifix = prifix;
	}

	@Autowired
	public FineProxy fineProxy;

	@RequestMapping(path = "/**")
	public ResponseEntity<?> proxy(HttpServletRequest request, HttpServletResponse response) {
		return fineProxy.doProxy(request, response, prifix, targetBaseUrl);
	}

}
