package pss.mira.orp.JavaOCAOCPP.service.ocpp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.service.cache.connectorsInfoCache.ConnectorsInfoCache;
import pss.mira.orp.JavaOCAOCPP.service.pc.TimeSetter;

import java.time.ZonedDateTime;

@Service
@Slf4j
public class Utils {
    private final ConnectorsInfoCache connectorsInfoCache;
    private final TimeSetter timeSetter;

    public Utils(ConnectorsInfoCache connectorsInfoCache, TimeSetter timeSetter) {
        this.connectorsInfoCache = connectorsInfoCache;
        this.timeSetter = timeSetter;
    }

    public Thread getEndOfChargingThread(ZonedDateTime currentTime) {
        Runnable task = () -> {
            while (true) {
                if (connectorsInfoCache.stationIsCharging()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        log.error("An error while waiting for the end of charging");
                    }
                } else {
                    break;
                }
            }
            timeSetter.setTime(currentTime);
        };
        return new Thread(task);
    }
}
