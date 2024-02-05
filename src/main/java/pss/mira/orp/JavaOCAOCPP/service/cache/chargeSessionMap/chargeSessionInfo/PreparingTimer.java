package pss.mira.orp.JavaOCAOCPP.service.cache.chargeSessionMap.chargeSessionInfo;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class PreparingTimer {
    private final int[] timer;
    private boolean stopReceived = false;
    private boolean canBeRemoved = false;

    public PreparingTimer(int[] timer) {
        this.timer = timer;
        Runnable timerTask = () -> {
            while (timer[0] > 0) {
                if (stopReceived) {
                    break;
                }
                if (timer[0] % 10 == 0) {
                    log.warn(timer[0] + " seconds for the connector to receive preparing status");
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error("An error while waiting to receive preparing status");
                }
                timer[0]--;
            }
            if (!stopReceived) {
                log.error("The time for the connector to receive preparing has expired. The start of the transaction " +
                        "is not sent to the central system");
                canBeRemoved = true;
            }
        };
        Thread timerThread = new Thread(timerTask);
        timerThread.start();
    }
}