package pss.mira.orp.JavaOCAOCPP.service.ocpp.handler.remoteTrigger;

import eu.chargetime.ocpp.feature.profile.ClientRemoteTriggerProfile;
import eu.chargetime.ocpp.model.core.MeterValuesRequest;

import java.util.Map;

public interface RemoteTriggerHandler {
    ClientRemoteTriggerProfile getRemoteTrigger();

    void setRemoteTriggerTaskFinished();

    void waitForRemoteTriggerTaskComplete();

    void setCachedMeterValuesRequestsMap(Map<Integer, MeterValuesRequest> cachedMeterValuesRequestsMap);
}