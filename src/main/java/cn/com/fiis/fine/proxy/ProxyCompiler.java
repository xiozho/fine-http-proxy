package cn.com.fiis.fine.proxy;

import java.util.logging.Logger;

import javax.websocket.server.ServerEndpoint;

import org.springframework.web.bind.annotation.RequestMapping;

import cn.com.fiis.fine.proxy.http.FineController;
import cn.com.fiis.fine.proxy.ws.FineWsServer;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.StringMemberValue;

/** 编译 */
public final class ProxyCompiler {
	private static final Logger logger = Logger.getLogger(ProxyCompiler.class.getName());

	public static FineController newProxyController(final String name, final String path) {
		String cName = name == null || name.isEmpty() ? "" : name.substring(0, 1).toUpperCase() + name.substring(1);
		String urlPath = path == null ? "/" : path;
		String className = String.format("cn.com.fiis.fine.temp.Temp%sProxyController", cName);
		try {
			ClassPool cp = ClassPool.getDefault();
			CtClass cc = cp.makeClass(className);
			ClassFile cf = cc.getClassFile();
			ConstPool cst = cf.getConstPool();
			// 添加父类
			cp.insertClassPath(new ClassClassPath(FineController.class));
			cc.setSuperclass(cp.get(FineController.class.getName()));
			// 类注解
			cp.insertClassPath(new ClassClassPath(RequestMapping.class));
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
			FineController controller = (FineController) clazz.newInstance();
			return controller;
		} catch (Exception e) {
			logger.warning("" + e);
			return null;
		}
	}

	public static FineWsServer newProxyWS(final String name, final String path) {
		String cName = name == null || name.isEmpty() ? "" : name.substring(0, 1).toUpperCase() + name.substring(1);
		String urlPath = path == null ? "/" : path;
		String className = String.format("cn.com.fiis.fine.temp.Temp%sWsServer", cName);
		try {
			ClassPool cp = ClassPool.getDefault();
			CtClass cc = cp.makeClass(className);
			ClassFile cf = cc.getClassFile();
			ConstPool cst = cf.getConstPool();
			// 添加父类
			cp.insertClassPath(new ClassClassPath(FineWsServer.class));
			cc.setSuperclass(cp.get(FineWsServer.class.getName()));
			// 类注解
			cp.insertClassPath(new ClassClassPath(ServerEndpoint.class));
			AnnotationsAttribute annAttr = new AnnotationsAttribute(cst, AnnotationsAttribute.visibleTag);
			Annotation ann = new Annotation(ServerEndpoint.class.getName(), cst);
			ann.addMemberValue("value", new StringMemberValue(urlPath, cst));
			annAttr.setAnnotation(ann);
			cf.addAttribute(annAttr);
			// 编译成类加载
			Class<?> clazz = cc.toClass();
			FineWsServer wsServer = (FineWsServer) clazz.newInstance();
			return wsServer;
		} catch (Exception e) {
			logger.warning("" + e);
			return null;
		}
	}

}
