package pss.mira.orp.JavaOCAOCPP.service.ocpp.bootNotification;

import eu.chargetime.ocpp.JSONClient;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;
import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.model.core.BootNotificationConfirmation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.models.requests.ocpp.BootNotificationRequest;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handler.Handler;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.heartBeat.Heartbeat;
import pss.mira.orp.JavaOCAOCPP.service.pc.TimeSetter;

import java.util.List;

import static eu.chargetime.ocpp.model.core.RegistrationStatus.Accepted;

@Service
@Slf4j
public class BootNotificationImpl implements BootNotification {
    private final Handler handler;
    private final Heartbeat heartbeat;
    private final TimeSetter timeSetter;

    private JSONClient client;
    @Value("${vendor.name}")
    private String vendorName;

    public BootNotificationImpl(Handler handler, Heartbeat heartbeat, TimeSetter timeSetter) {
        this.handler = handler;
        this.heartbeat = heartbeat;
        this.timeSetter = timeSetter;
    }

    /**
     * Подключается к центральной системе:
     * addressCP - адрес ЦС,
     * chargePointID - id зарядной станции.
     * Отправляет запрос bootNotification:
     * vendor - информация о производителе,
     * model - информация о модели станции.
     * Формат ответа от steve:
     * BootNotificationConfirmation{currentTime="2024-01-10T10:09:17.743Z", interval=60, status=Accepted, isValid=true}
     * Тестовый сын Джейсона для steve:
     * ["db","809734a6-7a67-478a-a932-2cade981e8f1",{"tables":{"config_zs":[{"name":"Версия службы и контроллера","secure_level":false,"id":34,"type":"string","value":"Firmware Service - dc3125d_2023-10-09::CHAdeMO - 3.4_d.0.9_23-9-8::CCS - 3.4_d.0.9_23-9-8::GBT - 3.2_a.0.3_23-8-12","key":"version"},{"name":"Включение RFID считывателя","secure_level":true,"id":8,"type":"bool","value":"true","key":"EnableCardReader"},{"name":"Модель ЗС","secure_level":false,"id":1,"type":"string","value":"CPmodel","key":"ChargePointModel"},{"name":"Серийный номер","secure_level":false,"id":2,"type":"string","value":"23X000002","key":"ChargePointSerialNumber"},{"name":"Коэффициент счетчика type1","secure_level":true,"id":4,"type":"int","value":"200","key":"coefMode3Type1"},{"name":"Коэффициент счетчика type2","secure_level":true,"id":5,"type":"int","value":"400","key":"coefMode3Type2"},{"name":"Старт с RFID","secure_level":false,"id":6,"type":"bool","value":"false","key":"mode3rfidStart"},{"name":"Использование полного счетчика","secure_level":true,"id":7,"type":"string","value":"full","key":"provider"},{"name":"Время диагностической перезагрузки","secure_level":false,"id":9,"type":"string","value":"01:00","key":"restartTime"},{"name":"Включение кнопок","secure_level":true,"id":10,"type":"bool","value":"false","key":"statusButton"},{"name":"Параллельная зарядка","secure_level":true,"id":11,"type":"bool","value":"true","key":"parallelCharging"},{"name":"Type2","secure_level":false,"id":12,"type":"bool","value":"false","key":"slave1"},{"name":"CHAdeMO","secure_level":true,"id":13,"type":"bool","value":"true","key":"slave2"},{"name":"CCS","secure_level":true,"id":14,"type":"bool","value":"true","key":"slave3"},{"name":"Type1","secure_level":false,"id":15,"type":"bool","value":"false","key":"slave4"},{"name":"GB/T","secure_level":true,"id":16,"type":"bool","value":"true","key":"slave5"},{"name":"CCS2","secure_level":false,"id":17,"type":"bool","value":"false","key":"slave6"},{"name":"Порт общения с контроллером","secure_level":true,"id":18,"type":"string","value":"/dev/ttyUSB0","key":"port"},{"name":"Комбинация отображения коннекторов","secure_level":true,"id":19,"type":"string","value":"12345","key":"combination"},{"name":"Подмена изображения Mode3","secure_level":true,"id":20,"type":"bool","value":"false","key":"recomboMode3"},{"name":"Адрес ЦС","secure_level":false,"id":21,"type":"string","value":"ws://10.10.0.255:8080/steve/websocket/CentralSystemService/","key":"adresCS"},{"name":"Максимальная мощность Type2 в кВт","secure_level":true,"id":22,"type":"int","value":"22","key":"maxPowerSlave1"},{"name":"Максимальный ток Type2 в А","secure_level":true,"id":23,"type":"int","value":"32","key":"maxAmperSlave1"},{"name":"Максимальная мощность CHAdeMO в кВт","secure_level":true,"id":24,"type":"int","value":"60","key":"maxPowerSlave2"},{"name":"Максимальный ток CHAdeMO в А","secure_level":true,"id":25,"type":"int","value":"120","key":"maxAmperSlave2"},{"name":"Максимальная мощность CCS в кВт","secure_level":true,"id":26,"type":"int","value":"60","key":"maxPowerSlave3"},{"name":"Максимальный ток CCS в А","secure_level":true,"id":27,"type":"int","value":"120","key":"maxAmperSlave3"},{"name":"Максимальная мощность Type1 в кВт","secure_level":true,"id":28,"type":"int","value":"7","key":"maxPowerSlave4"},{"name":"Максимальный ток Type1 в А","secure_level":true,"id":29,"type":"int","value":"32","key":"maxAmperSlave4"},{"name":"Максимальная мощность GB/T в кВт","secure_level":true,"id":30,"type":"int","value":"60","key":"maxPowerSlave5"},{"name":"Максимальный ток GB/T в А","secure_level":true,"id":31,"type":"int","value":"120","key":"maxAmperSlave5"},{"name":"Максимальная мощность CCS2 в кВт","secure_level":true,"id":32,"type":"int","value":"60","key":"maxPowerSlave6"},{"name":"Максимальный ток CCS2 в А","secure_level":true,"id":33,"type":"int","value":"120","key":"maxAmperSlave6"},{"name":"Режим Админа","secure_level":true,"id":35,"type":"bool","value":"false","key":"statusAdmin"},{"name":"Локальный режим станции","secure_level":false,"id":36,"type":"bool","value":"false","key":"local"},{"name":"Перевод Чадемо в Preparing с контроллера","secure_level":false,"id":37,"type":"bool","value":"false","key":"ChademoPreparing"},{"name":"Тип ограничения станции true-\u003e current, false \u003d power","secure_level":false,"id":38,"type":"bool","value":"true","key":"LimitationType"},{"name":"Максимальная мощность станции кВт","secure_level":true,"id":39,"type":"int","value":"160","key":"maxPowerStation"},{"name":"Максимальный ток станции А","secure_level":true,"id":40,"type":"int","value":"160","key":"maxCurrentStation"},{"name":"Вывод информации по станции","secure_level":false,"id":41,"type":"bool","value":"true","key":"infoStation"},{"name":"Мощность силового модуля","secure_level":false,"id":42,"type":"int","value":"40","key":"modulesPower"},{"name":"Паркинг мод","secure_level":true,"id":43,"type":"bool","value":"false","key":"parkingMod"},{"name":"Настройка шунта","secure_level":false,"id":44,"type":"int","value":"8","key":"shunt"},{"name":"Режим работы с новой мирой","secure_level":false,"id":45,"type":"bool","value":"false","key":"newMira"},{"name":"Отображение мощности","secure_level":false,"id":46,"type":"bool","value":"true","key":"FrontPower"},{"name":"Отображении тока запроса","secure_level":false,"id":47,"type":"bool","value":"false","key":"FrontAReq"},{"name":"Отображение тока фактического","secure_level":false,"id":48,"type":"bool","value":"true","key":"FrontATarget"},{"name":"отображение напряжения запроса","secure_level":false,"id":49,"type":"bool","value":"false","key":"FrontVReq"},{"name":"Отображение напряжение фактического","secure_level":false,"id":50,"type":"bool","value":"true","key":"FrontVTarget"},{"name":"Отображение потребленной энергии за сессию","secure_level":false,"id":51,"type":"bool","value":"true","key":"FrontConsumer"},{"name":"Отображение потребленной энергии за все время","secure_level":false,"id":52,"type":"bool","value":"false","key":"FrontFullConsumer"},{"name":"Отображение ограничения по току за сессию","secure_level":false,"id":53,"type":"bool","value":"false","key":"FrontMaxASession"},{"name":"Отображение ограничения по мощности за сессию","secure_level":false,"id":54,"type":"bool","value":"false","key":"FrontMaxWSession"},{"name":"Отображение максимального тока коннектора","secure_level":false,"id":55,"type":"bool","value":"false","key":"FrontMaxA"},{"name":"Отображение максимальной мощности коннектора","secure_level":false,"id":56,"type":"bool","value":"false","key":"FrontMaxW"},{"name":"Отображение минимального тока коннектора","secure_level":false,"id":57,"type":"bool","value":"false","key":"FrontMinA"},{"name":"Отображение минимальной мощности коннектора","secure_level":false,"id":58,"type":"bool","value":"false","key":"FrontMinW"},{"name":"Отображение температуры АКБ (только ГБТ)","secure_level":false,"id":59,"type":"bool","value":"false","key":"FrontTemperature"},{"name":"Отображение времени зарядки","secure_level":false,"id":60,"type":"bool","value":"true","key":"FrontTimeCharge"},{"name":"Автоматический старт при обноружении Preparing","secure_level":false,"id":61,"type":"bool","value":"false","key":"startPreparing"},{"name":"Отключение ожидания ответа стопа транзакции (Для трассы)","secure_level":false,"id":62,"type":"bool","value":"false","key":"flagNoResponseStop"},{"name":"Таймер ожидания после вставки коннектора Mod4","secure_level":false,"id":63,"type":"int","value":"90","key":"connectorInsertedTime"},{"name":"Флаг поднятия ножки","secure_level":false,"id":64,"type":"bool","value":"false","key":"flaglegLift"},{"name":"Идентификатор ЗС","secure_level":false,"id":3,"type":"string","value":"23X000002","key":"ChargePointID"}]}}]
     */
    @Override
    public void sendBootNotification(List<Object> parsedMessage) {
        BootNotificationRequest bootNotificationRequest = new BootNotificationRequest(parsedMessage);

        if (bootNotificationRequest.getAddressCP() != null &&
                bootNotificationRequest.getChargePointID() != null &&
                bootNotificationRequest.getModel() != null) {
            log.info("OCPP is ready to connect with the central system and send the boot notification");

            if (bootNotificationRequest.getAddressCP().endsWith("/")) {
                bootNotificationRequest.setAddressCP(
                        bootNotificationRequest.getAddressCP().substring(0, bootNotificationRequest.getAddressCP().length() - 1)
                );
            }

            ClientCoreProfile core = handler.getCore();
            JSONClient jsonClient = new JSONClient(core, bootNotificationRequest.getChargePointID());
            jsonClient.connect(bootNotificationRequest.getAddressCP(), null);
            client = jsonClient;

            // Use the feature profile to help create event
            Request request = core.createBootNotificationRequest(vendorName, bootNotificationRequest.getModel());

            // Client returns a promise which will be filled once it receives a confirmation.
            try {
                client.send(request).whenComplete((confirmation, ex) -> {
                    log.info("Received from the central system: " + confirmation.toString());
                    handleResponse(confirmation);
                });
            } catch (OccurenceConstraintException | UnsupportedFeatureException e) {
                log.error("Аn error occurred while trying to send a boot notification");
            }
        } else {
            log.error("OCPP did not receive one of the parameters (adresCS, ChargePointID, ChargePointVendor, " +
                    "ChargePointModel) and cannot establish a connection the central system");
        }
    }

    private void handleResponse(Confirmation confirmation) {
        BootNotificationConfirmation bootNotificationConfirmation = (BootNotificationConfirmation) confirmation;
        // TODO Сделать другие варианты, кроме Accepted
        if (bootNotificationConfirmation.getStatus().equals(Accepted)) {
            timeSetter.setTime(bootNotificationConfirmation.getCurrentTime());

            Thread heartBeatThread = getHeartbeatThread(bootNotificationConfirmation);
            heartBeatThread.start();
        }
    }

    private Thread getHeartbeatThread(BootNotificationConfirmation bootNotificationConfirmation) {
        Runnable runHeartbeat = () -> {
            while (true) {
                try {
                    Thread.sleep(bootNotificationConfirmation.getInterval() * 1000);
                } catch (InterruptedException e) {
                    log.error("Аn error while waiting for a heartbeat to be sent");
                }
                heartbeat.sendHeartbeat(handler.getCore(), getClient());
            }
        };
        return new Thread(runHeartbeat);
    }

    @Override
    public JSONClient getClient() {
        return client;
    }
}
