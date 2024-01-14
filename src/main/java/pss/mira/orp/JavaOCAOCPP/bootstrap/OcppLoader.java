package pss.mira.orp.JavaOCAOCPP.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import pss.mira.orp.JavaOCAOCPP.models.requests.rabbit.DBTablesRequest;
import pss.mira.orp.JavaOCAOCPP.service.rabbit.sender.Sender;

import java.util.List;
import java.util.UUID;

import static pss.mira.orp.JavaOCAOCPP.models.enums.DBKeys.config_zs;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Services.bd;

@Component
public class OcppLoader implements CommandLineRunner {
    private final Sender sender;

    public OcppLoader(Sender sender) {
        this.sender = sender;
    }

    @Override
    public void run(String... args) {
        sender.sendRequestToQueue(bd.name(), UUID.randomUUID().toString(), "Get", new DBTablesRequest(List.of(config_zs.name())));
    }
}