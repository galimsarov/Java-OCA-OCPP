package pss.mira.orp.JavaOCAOCPP.service.ocpp.handler.remoteTrigger;

import eu.chargetime.ocpp.feature.profile.ClientRemoteTriggerProfile;

public interface RemoteTriggerHandler {
    ClientRemoteTriggerProfile getRemoteTrigger();

    void setRemoteTriggerTaskFinished();

    void waitForRemoteTriggerTaskComplete();
}