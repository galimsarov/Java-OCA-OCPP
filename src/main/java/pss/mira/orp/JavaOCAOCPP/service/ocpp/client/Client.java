package pss.mira.orp.JavaOCAOCPP.service.ocpp.client;

import eu.chargetime.ocpp.JSONClient;
import pss.mira.orp.JavaOCAOCPP.models.info.ocpp.BootNotificationInfo;

import java.util.List;

public interface Client {
    void createClient(List<Object> parsedMessage, String source);

    BootNotificationInfo getBootNotificationInfo();

    JSONClient getClient();
}