package cn.com.fiis.fine.proxy;

import java.util.logging.Logger;

import javax.websocket.server.ServerEndpoint;

import org.springframework.web.bind.annotation.RequestMapping;

import cn.com.fiis.fine.proxy.http.FineController;
import cn.com.fiis.fine.proxy.ws.FineWsServer;
import cn.com.fiis.fine.proxy.ws.WebSocketConfigurator;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.ClassMemberValue;
import javassist.bytecode.annotation.StringMemberValue;

/** 编译 */
public final class ProxyCompiler {
	private static final Logger logger = Logger.getLogger(ProxyCompiler.class.getName());

	/** 创建代理Controller */
	public static FineController newProxyController(final String name, final String path) {
		String cName = name == null || name.isEmpty() ? "" : name.substring(0, 1).toUpperCase() + name.substring(1);
		String urlPath = path == null ? "/" : path;
		String className = String.format("cn.com.fiis.fine.temp.Temp%sProxyController", cName);
		try {
			ClassPool cp = ClassPool.getDefault();
			cp.insertClassPath(new ClassClassPath(FineController.class));
			cp.insertClassPath(new ClassClassPath(RequestMapping.class));
			// 创建新类
			CtClass cc = cp.makeClass(className);
			ClassFile cf = cc.getClassFile();
			ConstPool cst = cf.getConstPool();
			// 添加父类
			cc.setSuperclass(cp.get(FineController.class.getName()));
			// 类注解
			AnnotationsAttribute annAttr = new AnnotationsAttribute(cst, AnnotationsAttribute.visibleTag);
			Annotation ann = new Annotation(RequestMapping.class.getName(), cst);
			ArrayMemberValue amv = new ArrayMemberValue(cst);
			StringMemberValue[] svs = { new StringMemberValue(urlPath, cst) };
			amv.setValue(svs);
			ann.addMemberValue("value", amv);
			annAttr.setAnnotation(ann);
			cf.addAttribute(annAttr);
			// 编译成类加载
			Class<?> clazz = cc.toClass();
			// 释放
			cc.detach();
			// 实例
			FineController controller = (FineController) clazz.newInstance();
			return controller;
		} catch (Exception e) {
			logger.warning("" + e);
			return null;
		}
	}

	/** 创建代理Websocket服务 */
	public static FineWsServer newProxyWS(final String name, final String path) {
		String cName = name == null || name.isEmpty() ? "" : name.substring(0, 1).toUpperCase() + name.substring(1);
		String urlPath = path == null ? "/" : path;
		String className = String.format("cn.com.fiis.fine.temp.Temp%sWsServer", cName);
		try {
			ClassPool cp = ClassPool.getDefault();
			cp.insertClassPath(new ClassClassPath(FineWsServer.class));
			cp.insertClassPath(new ClassClassPath(ServerEndpoint.class));
			cp.insertClassPath(new ClassClassPath(WebSocketConfigurator.class));
			// 创建新类
			CtClass cc = cp.makeClass(className);
			ClassFile cf = cc.getClassFile();
			ConstPool cst = cf.getConstPool();
			// 添加父类
			cc.setSuperclass(cp.get(FineWsServer.class.getName()));
			// 类注解
			AnnotationsAttribute annAttr = new AnnotationsAttribute(cst, AnnotationsAttribute.visibleTag);
			Annotation ann = new Annotation(ServerEndpoint.class.getName(), cst);
			ann.addMemberValue("value", new StringMemberValue(urlPath, cst));
			ann.addMemberValue("configurator", new ClassMemberValue(WebSocketConfigurator.class.getName(), cst));
			annAttr.setAnnotation(ann);
			cf.addAttribute(annAttr);
			// 编译成类加载
			Class<?> clazz = cc.toClass();
			// 释放
			cc.detach();
			// 实例
			FineWsServer wsServer = (FineWsServer) clazz.newInstance();
			return wsServer;
		} catch (Exception e) {
			logger.warning("" + e);
			return null;
		}
	}

}
