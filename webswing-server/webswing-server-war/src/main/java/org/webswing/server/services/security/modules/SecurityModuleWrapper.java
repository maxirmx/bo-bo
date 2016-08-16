package org.webswing.server.services.security.modules;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.Callable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webswing.server.common.util.ConfigUtil;
import org.webswing.server.common.util.CommonUtil;
import org.webswing.server.services.security.api.AbstractWebswingUser;
import org.webswing.server.services.security.api.BuiltInModules;
import org.webswing.server.services.security.api.SecurityContext;
import org.webswing.server.services.security.api.SecurityModuleClassLoader;
import org.webswing.server.services.security.api.WebswingSecurityConfig;
import org.webswing.server.services.security.api.WebswingSecurityModule;
import org.webswing.server.services.security.api.WebswingSecurityModuleConfig;
import org.webswing.toolkit.util.ClasspathUtil;

public class SecurityModuleWrapper implements WebswingSecurityModule {
	private static final Logger log = LoggerFactory.getLogger(SecurityModuleWrapper.class);

	private WebswingSecurityModule custom;
	private WebswingSecurityConfig config;
	private URLClassLoader customCL;
	private SecurityContext context;

	public SecurityModuleWrapper(SecurityContext context, WebswingSecurityConfig config) {
		this.context = context;
		this.config = config;
	}

	@Override
	public void init() {
		try {
			String classPath = CommonUtil.getClassPath(config.getClassPath());
			URL[] urls = ClasspathUtil.populateClassPath(classPath, context.resolveFile(".").getAbsolutePath());
			customCL = new SecurityModuleClassLoader(urls, SecurityModuleWrapper.class.getClassLoader());
			String securityModuleClassName = BuiltInModules.getSecurityModuleClassName(config.getModule());
			Class<?> moduleClass = customCL.loadClass(securityModuleClassName);

			Constructor<?> defaultConstructor = null;
			Constructor<?> configConstructor = null;
			for (Constructor<?> constructor : moduleClass.getConstructors()) {
				Class<?>[] parameterTypes = constructor.getParameterTypes();
				if (parameterTypes.length == 1) {
					if (WebswingSecurityModuleConfig.class.isAssignableFrom(parameterTypes[0])) {
						configConstructor = constructor;
						break;
					}
				} else if (parameterTypes.length == 0) {
					defaultConstructor = constructor;
				}
			}

			if (configConstructor != null) {
				Class<?> configClass = configConstructor.getParameterTypes()[0];
				try {
					custom = (WebswingSecurityModule) configConstructor.newInstance(ConfigUtil.instantiateConfig(config.getConfig(), configClass, context));
				} catch (Exception e) {
					log.error("Could not construct custom security module class (using WebswingSecurityModuleConfig constructor).", e);
				}
			}
			if (custom == null && defaultConstructor != null) {
				try {
					custom = (WebswingSecurityModule) defaultConstructor.newInstance();
				} catch (Exception e) {
					log.error("Could not construct custom security module class (using Default constructor).", e);
				}
			}
			if (custom != null) {
				runWithContextClassLoader(new Callable<Object>() {

					@Override
					public Object call() throws Exception {
						custom.init();
						return null;
					}
				});
			} else {
				log.error("Custom security module class should define a default or WebswingSecurityModuleConfig constructor!");
			}
		} catch (Exception e) {
			log.error("Failed to initialize security module. ", e);
			throw new RuntimeException("Failed to initialize Secuirty module.", e);
		}
	}

	@Override
	public AbstractWebswingUser doLogin(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		if (custom != null) {
			AbstractWebswingUser user;
			try {
				user = runWithContextClassLoader(new Callable<AbstractWebswingUser>() {

					@Override
					public AbstractWebswingUser call() throws Exception {
						return custom.doLogin(request, response);
					}
				});
			} catch (Exception e1) {
				if (e1 instanceof ServletException) {
					throw (ServletException) e1;
				} else if (e1 instanceof IOException) {
					throw (IOException) e1;
				} else {
					throw new IOException("Failed to get user. Unexpected exception", e1);
				}
			}
			return user;
		}
		return null;

	}

	public void doLogout(final HttpServletRequest req, final HttpServletResponse res) {
		if (custom != null) {
			try {
				runWithContextClassLoader(new Callable<Object>() {

					@Override
					public Object call() throws Exception {
						try {
							custom.doLogout(req, res);
						} catch (Throwable e) {
							throw new Exception(e);
						}
						return null;
					}
				});
			} catch (Exception e1) {
				log.error("Logout by SecurityModule failed.", e1);
			}
		}
	}

	@Override
	public void destroy() {
		if (custom != null) {
			try {
				runWithContextClassLoader(new Callable<Object>() {

					@Override
					public Object call() throws Exception {
						try {
							custom.destroy();
						} catch (Throwable e) {
							throw new Exception(e);
						}
						return null;
					}
				});
			} catch (Exception e1) {
				log.error("Destroying SecurityModule failed.", e1);
			}
		}
		if (customCL != null) {
			try {
				customCL.close();
			} catch (IOException e) {
				log.error("Closing Custom SecurityModule classloader failed.", e);
			}
		}
	}

	public WebswingSecurityModuleConfig getModuleConfig() {
		if (custom != null && custom instanceof AbstractSecurityModule) {
			return ((AbstractSecurityModule<?>) custom).getConfig();
		}
		return null;
	}

	private <T> T runWithContextClassLoader(Callable<T> contextCallback) throws Exception {
		ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(customCL);
			return contextCallback.call();
		} finally {
			Thread.currentThread().setContextClassLoader(original);
		}
	}

}
