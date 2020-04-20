package org.webswing.services.impl;

import java.io.IOException;
import java.io.Serializable;
import java.lang.IllegalStateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import javax.jms.*;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.webswing.Constants;
import org.webswing.ext.services.ServerConnectionService;
import org.webswing.model.MsgIn;
import org.webswing.model.MsgOut;
import org.webswing.model.internal.JvmStatsMsgInternal;
import org.webswing.model.jslink.JavaEvalRequestMsgIn;
import org.webswing.model.jslink.JsResultMsg;
import org.webswing.toolkit.jslink.WebJSObject;
import org.webswing.toolkit.util.DeamonThreadFactory;
import org.webswing.toolkit.util.Logger;
import org.webswing.toolkit.util.Util;

/**
 * @author Viktor_Meszaros This class is needed to achieve classpath isolation for swing application, all functionality dependent on external libs is implemented here.
 */
public class ServerConnectionServiceImpl implements MessageListener, ServerConnectionService {

	private static ServerConnectionServiceImpl impl;
	private static ActiveMQConnectionFactory connectionFactory;
	private static long syncTimeout = Long.getLong(Constants.SWING_START_SYS_PROP_SYNC_TIMEOUT, 3000);
	private static final String WORK_IN_BACKGROUND = "WORK_IN_BACKGROUND";
	private Connection connection;
	private Session session;
	private MessageProducer producer;
	private MessageConsumer consumer;
	private long lastMessageTimestamp = System.currentTimeMillis();
	private Runnable watchdog;
	private ScheduledExecutorService exitScheduler = Executors.newSingleThreadScheduledExecutor(DeamonThreadFactory.getInstance());
	private ExecutorService jmsSender = Executors.newSingleThreadExecutor(DeamonThreadFactory.getInstance());

	private Map<String, Object> syncCallResposeMap = new HashMap<String, Object>();

	public static ServerConnectionServiceImpl getInstance() {
		if (impl == null) {
			impl = new ServerConnectionServiceImpl();
		}
		return impl;
	}

	public ServerConnectionServiceImpl() {
		connectionFactory = new ActiveMQConnectionFactory(System.getProperty(Constants.JMS_URL));
		connectionFactory.setAlwaysSessionAsync(false);
		connectionFactory.setTrustAllPackages(true);
		;
		watchdog = new Runnable() {
			private boolean terminated = false;

			@Override
			public void run() {
				int timeoutSec = Integer.parseInt(System.getProperty(Constants.SWING_SESSION_TIMEOUT_SEC, "300"));
				if (timeoutSec >= 0) {
					long diff = System.currentTimeMillis() - lastMessageTimestamp - 10000; /*-10000 is to compensate for 10s js heartbeat interval*/
					int timeoutMs = timeoutSec * 1000;
					timeoutMs = timeoutMs < 1000 ? 1000 : timeoutMs;
					if ((diff / 1000 > 10) && ((diff / 1000) % 10 == 0)) {
						Logger.warn("Inactive for " + diff / 1000 + " seconds." + (terminated ? "[waiting for application to stop]" : ""));
					}
					if (diff > timeoutMs && !isWorkingInBackground()) {
						if (!terminated) {//only call once
							terminated = true;
							Logger.warn("Exiting swing application due to inactivity for " + diff / 1000 + " seconds.");
							Util.getWebToolkit().exitSwing(1);
						}
					}
				}
				if (!terminated) {
					sendObject(getStats());
				}
			}
		};

	}

	private boolean isWorkingInBackground() {
		boolean workInBackground = Boolean.valueOf(System.getProperty(WORK_IN_BACKGROUND));
		Logger.warn("isWorkingInBackground = " + workInBackground);
		return workInBackground;
	}

	public void initialize() {
		try {
			String clientId = System.getProperty(Constants.SWING_START_SYS_PROP_CLIENT_ID);
			connection = connectionFactory.createConnection();
			connection.start();
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Queue consumerDest = session.createQueue(clientId + Constants.SERVER2SWING);
			Queue producerDest = session.createQueue(clientId + Constants.SWING2SERVER);
			producer = session.createProducer(producerDest);
			consumer = session.createConsumer(consumerDest);
			consumer.setMessageListener(this);
			connection.setExceptionListener(new ExceptionListener() {

				@Override
				public void onException(JMSException paramJMSException) {
                    Logger.warn("JMS clien connection error: " + paramJMSException.getMessage());
                    Logger.error("Exiting swing application because could not connect to JMS:"
                            + paramJMSException.getMessage(), paramJMSException);
                    System.exit(1);
				}
			});
		} catch (JMSException e) {
			Logger.error("Exiting swing application because could not connect to JMS:" + e.getMessage(), e);
			System.exit(1);
		}

		Runtime.getRuntime().addShutdownHook(new Thread() {

			public void run() {
				ServerConnectionServiceImpl.this.disconnect();
			}

		});

		exitScheduler.scheduleWithFixedDelay(watchdog, 1, 1, TimeUnit.SECONDS);
	}

	public void disconnect() {
		try {
			producer.close();
			consumer.close();
			session.close();
			connection.close();
		} catch (JMSException e) {
			Logger.info("Disconnecting from JMS server failed.", e.getMessage());
		}
	}

	private void sendJmsMessage(final Serializable o) throws JMSException {
		try {
			jmsSender.submit(new Callable<Object>() {

				@Override
				public Object call() throws Exception {
					producer.send(session.createObjectMessage(o), DeliveryMode.NON_PERSISTENT, 4, 3000);
					return null;
				}
			}).get(4, TimeUnit.SECONDS);
		} catch (IllegalStateException e) {
			Logger.warn("ServerConnectionService.sendJmsMessage: " + e.getMessage());
		} catch (InterruptedException e) {
			Logger.error("ServerConnectionService.sendJmsMessage: Sending frame interrupted.", e);
		} catch (ExecutionException e) {
			if (e.getCause() instanceof JMSException) {
				throw (JMSException) e.getCause();
			} else {
				Logger.error("ServerConnectionService.sendJmsMessage", e);
				throw new JMSException(e.getMessage());
			}
		} catch (TimeoutException e) {
			Logger.error("ServerConnectionService.sendJmsMessage: Sending frame timed out.", e);
		}
	}

	@Override
	public void sendObject(Serializable o) {
		try {
			sendJmsMessage(o);
		} catch (JMSException e) {
			Logger.error("ServerConnectionService.sendJsonObject", e);
		}
	}

	@Override
	public Object sendObjectSync(MsgOut o, String correlationId) throws TimeoutException, IOException {
		try {
			Object syncObject = new Object();
			syncCallResposeMap.put(correlationId, syncObject);
			sendJmsMessage(o);
			Object result = null;
			try {
				synchronized (syncObject) {
					syncObject.wait(syncTimeout);
				}
			} catch (InterruptedException e) {
			}

			result = syncCallResposeMap.get(correlationId);
			syncCallResposeMap.remove(correlationId);
			if (result == syncObject) {
				throw new TimeoutException("Call timed out after " + syncTimeout + " ms");
			}
			return result;

		} catch (JMSException e) {
			Logger.error("ServerConnectionService.sendJsonObject", e);
			throw new IOException(e.getMessage());
		}
	}

	public void onMessage(Message msg) {
		try {
			lastMessageTimestamp = System.currentTimeMillis();
			if (msg instanceof ObjectMessage) {
				ObjectMessage omsg = (ObjectMessage) msg;
				if (omsg.getObject() instanceof JsResultMsg) {
					JsResultMsg syncmsg = (JsResultMsg) omsg.getObject();
					String correlationId = syncmsg.getCorrelationId();
					if (syncCallResposeMap.containsKey(correlationId)) {
						Object syncObject = syncCallResposeMap.get(correlationId);
						syncCallResposeMap.put(correlationId, omsg.getObject());
						synchronized (syncObject) {
							syncObject.notifyAll();
						}
					} else {
						Logger.warn("No thread waiting for sync-ed message with id ", correlationId);
					}
				} else if (omsg.getObject() instanceof JavaEvalRequestMsgIn) {
					JavaEvalRequestMsgIn javaReq = (JavaEvalRequestMsgIn) omsg.getObject();
					WebJSObject.evaluateJava(javaReq);
				} else if (omsg.getObject() instanceof MsgIn) {
					Util.getWebToolkit().getEventDispatcher().dispatchEvent((MsgIn) omsg.getObject());
				}
			}
		} catch (Exception e) {
			Logger.error("ServerConnectionService.onMessage", e);
		}
	}

	private JvmStatsMsgInternal getStats() {
		JvmStatsMsgInternal result = new JvmStatsMsgInternal();
		int mb = 1024 * 1024;
		Runtime runtime = Runtime.getRuntime();
		result.setHeapSize(runtime.totalMemory() / mb);
		result.setHeapSizeUsed((runtime.totalMemory() - runtime.freeMemory()) / mb);
		return result;
	}

}
