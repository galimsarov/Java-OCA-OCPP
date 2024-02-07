package pss.mira.orp.JavaOCAOCPP.service.pc.reStarter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.service.cache.chargeSessionMap.ChargeSessionMap;

import java.io.IOException;

@Service
@Slf4j
public class ReStarterImpl implements ReStarter {
    private final ChargeSessionMap chargeSessionMap;

    public ReStarterImpl(ChargeSessionMap chargeSessionMap) {
        this.chargeSessionMap = chargeSessionMap;
    }

    @Override
    public void restart() {
        Runnable restartTask = () -> {
            while (true) {
                if (chargeSessionMap.isNotEmpty()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        log.error("Аn error while waiting for a restart");
                    }
                } else {
                    break;
                }
            }
            if (SystemUtils.IS_OS_WINDOWS) {
                log.error("Application restart doesn't support in windows");
                return;
            }
            int restartTries = 10;
            for (int i = 0; i < restartTries; i++) {
                log.info("Try to restart #{}", i);
                try {
                    log.info("Restarting...");
                    Runtime.getRuntime().exec("sudo systemctl restart ocpp-service.service");
                    return; // По идее уже вырубится приложение, но на всякий return
                } catch (IOException e) {
                    log.error("Аn error while trying to restart");
                }
            }
            log.warn("System wasn't restarted in {} tries!", restartTries);
        };
        Thread restartThread = new Thread(restartTask);
        restartThread.start();
    }
}
