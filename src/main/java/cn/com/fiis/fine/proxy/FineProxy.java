package cn.com.fiis.fine.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

/*** 代理 */
public class FineProxy {
	private static final Logger logger = Logger.getLogger(FineProxy.class.getName());

	/** 是否记录日志 */
	private boolean enabledLog;
	/** 请求 */
	private final RestTemplate restTemplate;

	public FineProxy() {
		this(true);
	}

	public FineProxy(boolean enabledLog) {
		this.enabledLog = enabledLog;
		this.restTemplate = new RestTemplate();
	}

	/**
	 * 执行代理
	 * 
	 * @param request       HttpServletRequest
	 * @param response      HttpServletResponse
	 * @param targetBaseUrl 目标基础地址
	 * @param prifix        需移除的前缀
	 * @return
	 */
	public ResponseEntity<?> doProxy(final HttpServletRequest request, final HttpServletResponse response,
			final String targetBaseUrl, final String prifix) {
		String redirectUrl = createUrl(request, targetBaseUrl, prifix);
		try {
			HttpMethod httpMethod = HttpMethod.resolve(request.getMethod());
			HttpStatus httpStatus = restTemplate.execute(redirectUrl, httpMethod, new RequestCallback() {

				@Override
				public void doWithRequest(ClientHttpRequest req) throws IOException {
					// 转发请求头
					HttpHeaders headers = new HttpHeaders();
					List<String> headerNames = Collections.list(request.getHeaderNames());
					for (String headerName : headerNames) {
						List<String> headerValues = Collections.list(request.getHeaders(headerName));
						for (String headerValue : headerValues) {
							headers.add(headerName, headerValue);
						}
					}
					req.getHeaders().addAll(headers);
					// 转发请求体
					InputStream is = request.getInputStream();
					OutputStream os = req.getBody();
					StreamUtils.copy(is, os);
				}

			}, new ResponseExtractor<HttpStatus>() {

				@Override
				public HttpStatus extractData(ClientHttpResponse res) throws IOException {
					// 响应状态
					HttpStatus httpStatus = res.getStatusCode();
					response.setStatus(httpStatus.value());

					// 转发回应头
					HttpHeaders headers = res.getHeaders();
					if (headers != null) {
						headers.forEach((k, v) -> {
							v.forEach(x -> {
								response.addHeader(k, x);
							});
						});
					}
					// 转发回应体
					InputStream is = res.getBody();
					OutputStream os = response.getOutputStream();
					StreamUtils.copy(is, os);

					return httpStatus;
				}
			});
			if (enabledLog) {
				logger.info(String.format("ProxyFor:%s-> %s", httpStatus, redirectUrl));
			}
			return new ResponseEntity<>(httpStatus);
		} catch (Exception e) {
			logger.warning(String.format("Error.ProxyFor-> %s", redirectUrl));
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_GATEWAY);
		}
	}

	/** 创建URL */
	private String createUrl(HttpServletRequest request, String targetBaseUrl, String prifix) {
		if (targetBaseUrl == null || targetBaseUrl.isEmpty()) {
			throw new IllegalArgumentException("Argument[targetBaseUrl] must be not null.");
		}
		if (prifix == null) {
			prifix = "";
		} else if ("/".equals(prifix)) {
			prifix = "";
		} else if (prifix.length() > 0 && !prifix.startsWith("/")) {
			prifix = "/" + prifix;
		}
		String queryString = request.getQueryString();
		if (queryString != null && queryString.length() > 0) {
			queryString = "?" + queryString;
		} else {
			queryString = "";
		}
		String requestUrl = request.getRequestURI();
		if (prifix != null && prifix.length() > 0) {
			requestUrl = requestUrl.replace(prifix, "");
		}
		if (targetBaseUrl.endsWith("/") && requestUrl.startsWith("/")) {
			requestUrl = requestUrl.substring(1);
			return targetBaseUrl + requestUrl + queryString;
		} else if (!targetBaseUrl.endsWith("/") && !requestUrl.startsWith("/")) {
			return targetBaseUrl + "/" + requestUrl + queryString;
		} else {
			return targetBaseUrl + requestUrl + queryString;
		}
	}

	/** 记录日志 */
	public FineProxy setEnabledLog(boolean enabledLog) {
		this.enabledLog = enabledLog;
		return this;
	}

}