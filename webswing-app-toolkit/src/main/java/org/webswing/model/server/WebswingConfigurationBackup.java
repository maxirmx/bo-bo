package org.webswing.model.server;

import java.util.Map;
import java.util.TreeMap;

public class WebswingConfigurationBackup {

    private Map<String, WebswingConfiguration> backupMap = new TreeMap<String, WebswingConfiguration>();

    public Map<String, WebswingConfiguration> getBackupMap() {
        return backupMap;
    }

    public void setBackupMap(Map<String, WebswingConfiguration> backupMap) {
        this.backupMap = backupMap;
    }
}
