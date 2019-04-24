package org.webswing.server;

import org.atmosphere.cpr.AtmosphereResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webswing.model.MsgIn;
import org.webswing.model.c2s.ConnectionHandshakeMsgIn;
import org.webswing.model.s2c.SimpleEventMsgOut;
import org.webswing.model.server.SwingDescriptor;
import org.webswing.model.server.admin.Sessions;
import org.webswing.model.server.admin.SwingSession;
import org.webswing.server.util.ServerUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SwingInstanceManager {

	private static SwingInstanceManager instance = new SwingInstanceManager();
	private static final Logger log = LoggerFactory.getLogger(SwingInstanceManager.class);

	//private List<SwingInstance> closedInstances = new ArrayList<SwingInstance>();
	private Map<String, SwingInstance> swingInstances = new ConcurrentHashMap<String, SwingInstance>();
	private Map<String, String> startingInstances = new ConcurrentHashMap<String, String>();
	private SwingInstanceChangeListener changeListener;

	private SwingInstanceManager() {
	}

	public static SwingInstanceManager getInstance() {
		return instance;
	}

	public synchronized Set<SwingInstance> getSwingInstanceSet() {
		Set<SwingInstance> set;
		synchronized (this) {
			set = new HashSet<SwingInstance>(swingInstances.values());
		}
		return set;
	}

	public void connectSwingInstance(AtmosphereResource r, ConnectionHandshakeMsgIn h) {
		SwingInstance swingInstance = findInstance(r, h);
		if (swingInstance == null) {// start new swing app
			if(h.isContinueSession()){
				ServerUtil.broadcastMessage(r, SimpleEventMsgOut.shutDownNotification.buildMsgOut());
				return;
			}

			SwingDescriptor app;
			if (h.isApplet()) {
				app = ConfigurationManager.getInstance().getApplet(h.getApplicationName());
			} else {
				app = ConfigurationManager.getInstance().getApplication(h.getApplicationName());
			}
			if (app == null) {
				throw new RuntimeException((h.isApplet() ? "Applet " : "Application ") + h.getApplicationName() + " is not configured.");
			}
			if (ServerUtil.isUserAuthorized(r, app, h)) {
				if (!h.isMirrored()) {
					if (!reachedMaxConnections(app)) {
						try {
							if(startingInstances.containsKey(h.getClientId())){
								log.error("find same client starting " + h.getClientId());
								return;
							}
							startingInstances.put(h.getClientId(), h.getClientId());
							swingInstance = new SwingInstance(resolveInstanceIdForMode(r, h, app), h, app, r);
							synchronized (this) {
								swingInstances.put(swingInstance.getInstanceId(), swingInstance);
								startingInstances.remove(h.getClientId());
							}
						} catch (Exception e) {
							log.error("Failed to create Swing instance.",e);
							if(h != null){
								startingInstances.remove(h.getClientId());
							}
						}
						notifySwingInstanceChanged();
					} else {
						ServerUtil.broadcastMessage(r, SimpleEventMsgOut.tooManyClientsNotification.buildMsgOut());
					}
				} else {
					ServerUtil.broadcastMessage(r, SimpleEventMsgOut.configurationError.buildMsgOut());
				}
			} else {
				log.error("Authorization error: User " + ServerUtil.getUserName(r) + " is not authorized to connect to application " + app.getName() + (h.isMirrored() ? " [Mirrored view only available for admin role]" : ""));
			}
		} else {
			if(startingInstances.containsKey(h.getClientId())){
				startingInstances.remove(h.getClientId());
			}
			if (h.isMirrored()) {// connect as mirror viewer
				if (ServerUtil.isUserAuthorized(r, swingInstance.getAppConfig(), h)) {
					notifySessionDisconnected(r.uuid());// disconnect possible running mirror sessions
					swingInstance.connectMirroredWebSession(r);
				} else {
					log.error("Authorization error: User " + ServerUtil.getUserName(r) + " is not authorized. [Mirrored view only available for admin role]");
				}
			} else {// continue old session?
				if (h.getSessionId() != null && h.getSessionId().equals(swingInstance.getSessionId())) {
					swingInstance.sendToSwing(r, h);
				} else {
					boolean result = swingInstance.connectPrimaryWebSession(r);
					if (result) {
						ServerUtil.broadcastMessage(r, SimpleEventMsgOut.continueOldSessionAutomatic.buildMsgOut());
						notifySwingInstanceChanged();
					} else {
						ServerUtil.broadcastMessage(r, SimpleEventMsgOut.applicationAlreadyRunning.buildMsgOut());
					}
				}
			}
		}
	}

	private SwingInstance findInstance(AtmosphereResource r, ConnectionHandshakeMsgIn h) {
		synchronized (this) {
			for (String instanceId : swingInstances.keySet()) {
				SwingInstance si = swingInstances.get(instanceId);
				String idForMode = resolveInstanceIdForMode(r, h, si.getAppConfig());
				if (idForMode.equals(instanceId)) {
					return si;
				}
			}
		}
		return null;
	}

	private boolean reachedMaxConnections(SwingDescriptor app) {
		if (app.getMaxClients() < 0) {
			return false;
		} else if (app.getMaxClients() == 0) {
			return true;
		} else {
			int count = 0;
			for (SwingInstance si : getSwingInstanceSet()) {
				if (app.getName().equals(si.getAppConfig().getName()) && si.isRunning()) {
					count++;
				}
			}
			if (count < app.getMaxClients()) {
				return false;
			} else {
				return true;
			}
		}
	}

	public void notifySwingClose(SwingInstance swingInstance) {
		synchronized (this) {
//			if(!closedInstances.contains(swingInstance)){
//				closedInstances.add(swingInstance);
//			}
			swingInstances.remove(swingInstance.getInstanceId());
			if(startingInstances.containsKey(swingInstance.getInstanceId())){
				startingInstances.remove(swingInstance.getInstanceId());
			}
		}
		notifySwingInstanceChanged();
	}

	public synchronized Sessions getSessions() {
		Sessions result = new Sessions();
		for (SwingInstance si : getSwingInstanceSet()) {
			result.getSessions().add(si.toSwingSession());
		}
//		for (SwingInstance si : closedInstances) {
//			result.getClosedSessions().add(si.toSwingSession());
//		}
		return result;
	}

	public synchronized SwingSession getSession(String id) {
		for (SwingInstance si : getSwingInstanceSet()) {
			if (si.getClientId().equals(id)) {
				return si.toSwingSession();
			}
		}
		return null;
	}

	public void shutdown(String id, boolean force) {
		for (SwingInstance si : getSwingInstanceSet()) {
			if (si.getClientId().equals(id)) {
				si.shutdown(force);
			}
		}
	}

	public void notifySessionDisconnected(String uuid) {
		Set<SwingInstance> set = getSwingInstanceSet();
		for (SwingInstance i : set) {
			if (i.getSessionId() != null && i.getSessionId().equals(uuid)) {
				i.disconnectPrimaryWebSession();
			} else if (i.getMirroredSessionId() != null && i.getMirroredSessionId().equals(uuid)) {
				i.disconnectMirroredWebSession();
			}
		}
	}

	public void notifySwingInstanceChanged() {
		if (changeListener != null) {
			changeListener.swingInstancesChanged();
		}
	}

	public void notifySwingInstanceStatsChanged() {
		if (changeListener != null) {
			changeListener.swingInstancesChangedStats();
		}
	}

	public void setChangeListener(SwingInstanceChangeListener changeListener) {
		this.changeListener = changeListener;
	}

	public interface SwingInstanceChangeListener {

		void swingInstancesChanged();

		void swingInstancesChangedStats();
	}

	public boolean sendMessageToSwing(AtmosphereResource r, String clientId, MsgIn o) {
		if (clientId != null) {
			SwingInstance client = findInstance(clientId);
			if (client != null) {
				return client.sendToSwing(r, o);
			}
		}
		return false;
	}

	public SwingInstance findInstance(String clientId) {
		if (clientId != null) {
			SwingInstance client = swingInstances.get(clientId);
			if (client == null) {
				for (SwingInstance si : getSwingInstanceSet()) {
					if (si.getClientId().equals(clientId)) {
						client = si;
						break;
					}
				}
			}
			return client;
		}
		return null;
	}

	public String resolveInstanceID(AtmosphereResource r, ConnectionHandshakeMsgIn h) {
		SwingDescriptor app;
		if (h.isApplet()) {
			app = ConfigurationManager.getInstance().getApplet(h.getApplicationName());
		} else {
			app = ConfigurationManager.getInstance().getApplication(h.getApplicationName());
		}
		if (app == null) {
			throw new RuntimeException((h.isApplet() ? "Applet " : "Application ") + h.getApplicationName() + " is not configured.");
		}
		return resolveInstanceIdForMode(r, h, app);
	}

	private String resolveInstanceIdForMode(AtmosphereResource r, ConnectionHandshakeMsgIn h, SwingDescriptor app) {
		if(h.isMirrored()){
			return h.getClientId();
		}else{
			switch (app.getSessionMode()) {
			case ALWAYS_NEW_SESSION:
				return h.getClientId() + h.getViewId();
			case CONTINUE_FOR_BROWSER:
				return h.getClientId();
			case CONTINUE_FOR_USER:
				return app.getName() + ServerUtil.getUserName(r);
			default:
				return h.getClientId();
			}
		}
	}

}
