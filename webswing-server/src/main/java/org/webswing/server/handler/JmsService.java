package org.webswing.server.handler;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.region.policy.ConstantPendingMessageLimitStrategy;
import org.apache.activemq.broker.region.policy.PolicyEntry;
import org.apache.activemq.broker.region.policy.PolicyMap;
import org.apache.activemq.usage.MemoryUsage;
import org.apache.activemq.usage.SystemUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webswing.Constants;

public class JmsService implements ServletContextListener {

	private static final Logger log = LoggerFactory.getLogger(JmsService.class);

	private final static long defaultOveralMemLimit = 80 * 1024 * 1024;
	private final static long defaultDestMemLimit = 5 * 1024 * 1024;

	private BrokerService broker;

	public void contextInitialized(ServletContextEvent event) {
		try {
			broker = startService();
		} catch (Exception e) {
			log.error("Failed to start JMS service.", e);
			System.exit(1);
		}
	}

	public void contextDestroyed(ServletContextEvent event) {
		try {
			broker.stop();
		} catch (Exception e) {
			log.error("Failed to stop JMS service.", e);
		}
	}

	public BrokerService startService() throws Exception {
		System.setProperty("org.apache.activemq.SERIALIZABLE_PACKAGES",Constants.JMS_SERIALIZABLE_PACKAGES);
		BrokerService broker = new BrokerService();
		broker.setUseJmx(false);
		broker.setPersistent(false);

		PolicyMap policyMap = new PolicyMap();
		PolicyEntry defaultEntry = new PolicyEntry();

		ConstantPendingMessageLimitStrategy pendingMessageLimitStrategy = new ConstantPendingMessageLimitStrategy();
		pendingMessageLimitStrategy.setLimit(10);
		defaultEntry.setPendingMessageLimitStrategy(pendingMessageLimitStrategy);
		defaultEntry.setMemoryLimit(5 * 1024 * 1024);
		defaultEntry.setMemoryLimit(getDestinationMemoryLimit());

		policyMap.setDefaultEntry(defaultEntry);
		broker.setDestinationPolicy(policyMap);

		SystemUsage memoryManager = new SystemUsage();
		MemoryUsage memoryLimit = new MemoryUsage();
		memoryLimit.setLimit(getOveralMemoryLimit());

		memoryManager.setMemoryUsage(memoryLimit);
		broker.setSystemUsage(memoryManager);
		// configure the broker
		broker.addConnector(getUrl());

		broker.start();
		return broker;

	}

	private long getOveralMemoryLimit() {
		long result = defaultOveralMemLimit;
		if (System.getProperty(Constants.JMS_OVERAL_MEM_LIMIT) != null) {
			try {
				result = Long.parseLong(System.getProperty(Constants.JMS_OVERAL_MEM_LIMIT));
			} catch (NumberFormatException e) {
				log.error("System property " + Constants.JMS_OVERAL_MEM_LIMIT + " is not valid. Number value is expected (number of bytes).", e);
			}
		}
		return result;
	}

	private long getDestinationMemoryLimit() {
		long result = defaultDestMemLimit;
		if (System.getProperty(Constants.JMS_DEST_MEM_LIMIT) != null) {
			try {
				result = Long.parseLong(System.getProperty(Constants.JMS_DEST_MEM_LIMIT));
			} catch (NumberFormatException e) {
				log.error("System property " + Constants.JMS_DEST_MEM_LIMIT + " is not valid. Number value is expected (number of bytes).", e);
			}
		}
		return result;
	}

	public static String getUrl() {
		return System.getProperty(Constants.JMS_URL, Constants.JMS_URL_DEFAULT);
	}
}
