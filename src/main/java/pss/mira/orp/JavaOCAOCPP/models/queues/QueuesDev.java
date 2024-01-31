package pss.mira.orp.JavaOCAOCPP.models.queues;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("dev")
public class QueuesDev implements Queues {
    @Override
    public String getDateBase() {
        return "bd";
    }

    @Override
    public String getChargePointLogic() {
        return "cp";
    }

    @Override
    public String getModBus() {
        return "ModBusFake";
    }

    @Override
    public String getOCPP() {
        return "ocpp";
    }

    @Override
    public String getOCPPCache() {
        return "ocppCache";
    }
}