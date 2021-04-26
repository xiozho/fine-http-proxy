package cn.com.fiis.fine.proxy;

import java.lang.reflect.Method;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Component
public class FineManager implements ApplicationContextAware {
	private static ApplicationContext applicationContext;
	private static RequestMappingHandlerMapping mapping;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		FineManager.applicationContext = applicationContext;
		FineManager.mapping = FineManager.getBean("requestMappingHandlerMapping"); // controller映射服务
	}

	/** 获取Bean */
	public static <T> T getBean(String name) {
		@SuppressWarnings("unchecked")
		T t = (T) applicationContext.getBean(name);
		return t;
	}

	/** 获取Bean */
	public static <T> T getBean(Class<T> clazz) {
		return applicationContext.getBean(clazz);
	}

	/** 注册Controller */
	public static void registerController(String controllerBeanName) throws Exception {
		if (mapping != null) {
			String handler = controllerBeanName;
			Object controller = FineManager.getBean(handler);
			if (controller == null) {
				return;
			}
			unregisterController(controllerBeanName); // 移除旧注册
			Method method = RequestMappingHandlerMapping.class.getSuperclass().getSuperclass()
					.getDeclaredMethod("detectHandlerMethods", Object.class);
			method.setAccessible(true);
			method.invoke(mapping, handler);
		}
	}

	/** 去掉Controller的Mapping */
	public static void unregisterController(String controllerBeanName) {
		if (mapping != null) {
			String handler = controllerBeanName;
			Object controller = FineManager.getBean(handler);
			if (controller == null) {
				return;
			}
			final Class<?> targetClass = controller.getClass();
			ReflectionUtils.doWithMethods(targetClass, new ReflectionUtils.MethodCallback() {
				public void doWith(Method method) {
					Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
					try {
						Method createMappingMethod = RequestMappingHandlerMapping.class
								.getDeclaredMethod("getMappingForMethod", Method.class, Class.class);
						createMappingMethod.setAccessible(true);
						RequestMappingInfo requestMappingInfo = (RequestMappingInfo) createMappingMethod.invoke(mapping,
								specificMethod, targetClass);
						if (requestMappingInfo != null) {
							mapping.unregisterMapping(requestMappingInfo);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}, ReflectionUtils.USER_DECLARED_METHODS);
		}
	}

}
