package pss.mira.orp.JavaOCAOCPP.service.ocpp.auth;

import eu.chargetime.ocpp.JSONClient;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;
import eu.chargetime.ocpp.model.Request;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.bootstrap.OcppLoader;

@Service
@Slf4j
public class AuthorizeImpl implements Authorize {
    private final OcppLoader ocppLoader;

    public AuthorizeImpl(OcppLoader ocppLoader) {
        this.ocppLoader = ocppLoader;
    }

    /**
     * Отправляет запрос на авторизацию в ЦС
     *
     * @param idTag, который нужно проверить на авторизацию
     *               Формат ответа от steve:
     *               AuthorizeConfirmation{idTagInfo=IdTagInfo{expiryDate="2024-01-10T11:12:02.925Z", parentIdTag=null, status=Accepted}, isValid=true}
     */
    @Override
    public void sendAuthorize(String idTag) {
        ClientCoreProfile core = ocppLoader.getCore();
        JSONClient client = ocppLoader.getClient();

        // Use the feature profile to help create event
        Request request = core.createAuthorizeRequest(idTag);

        // Client returns a promise which will be filled once it receives a confirmation.
        try {
            client.send(request).whenComplete((s, ex) -> System.out.println(s));
        } catch (OccurenceConstraintException | UnsupportedFeatureException ignored) {
            log.warn("An error occurred while sending or processing auth request");
        }
    }
}