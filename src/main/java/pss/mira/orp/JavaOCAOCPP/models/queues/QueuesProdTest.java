package pss.mira.orp.JavaOCAOCPP.models.queues;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("prodTest")
public class QueuesProdTest implements Queues {
    @Override
    public String getDateBase() {
        return "bd";
    }

    @Override
    public String getChargePointLogic() {
        return "mainChargePointLogic";
    }

    @Override
    public String getModBus() {
        return "ModBus";
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