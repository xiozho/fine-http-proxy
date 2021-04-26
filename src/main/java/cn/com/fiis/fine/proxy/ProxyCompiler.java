package cn.com.fiis.fine.proxy;

import java.util.Map;
import java.util.logging.Logger;

import cn.com.fiis.fine.compiler.JavaStringCompiler;

/** 编译 */
public final class ProxyCompiler {
	private static final Logger logger = Logger.getLogger(ProxyCompiler.class.getName());

	private static final JavaStringCompiler COMPILER = new JavaStringCompiler();
	private static final String TEMP_CONTROLLER_TEMPLATE = "package cn.com.fiis.fine.temp;"
			+ "import org.springframework.stereotype.Controller;"
			+ "import org.springframework.web.bind.annotation.RequestMapping;"
			+ "import cn.com.fiis.fine.proxy.ProxyController;" + "@Controller @RequestMapping(path = \"%s\")"
			+ "public class %s extends ProxyController {}";

	public static ProxyController newProxyController(final String name, final String path) {
		String cName = name == null || name.isEmpty() ? "" : name.substring(0, 1).toUpperCase() + name.substring(1);
		String urlPath = path == null ? "" : path;
		String classShortName = String.format("Temp%sProxyController", cName);
		String classSource = String.format(TEMP_CONTROLLER_TEMPLATE, urlPath, classShortName);
		try {
			String fileName = classShortName + ".java";
			String className = "cn.com.fiis.fine.temp." + classShortName;
			Map<String, byte[]> results = COMPILER.compile(fileName, classSource);
			Class<?> clazz = COMPILER.loadClass(className, results);
			ProxyController controller = (ProxyController) clazz.newInstance();
			return controller;
		} catch (Exception e) {
			logger.warning("" + e);
			return null;
		}
	}

}
