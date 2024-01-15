package pss.mira.orp.JavaOCAOCPP.service.ocpp.bootNotification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handler.Handler;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.heartBeat.Heartbeat;
import pss.mira.orp.JavaOCAOCPP.service.pc.TimeSetter;

import java.util.List;
import java.util.Map;

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
     * ["db","47fc19c0-9a0e-436d-a507-32052712a18c",{"tables":{"config_zs":"[{\"id\":34,\"key\":\"version\",\"value\":\"Firmware Service - dc3125d_2023-10-09::CHAdeMO - 3.4_d.0.9_23-9-8::CCS - 3.4_d.0.9_23-9-8::GBT - 3.2_a.0.3_23-8-12\",\"type\":\"string\",\"name\":\"Версия службы и контроллера\",\"secureLevel\":false},{\"id\":8,\"key\":\"EnableCardReader\",\"value\":\"true\",\"type\":\"bool\",\"name\":\"Включение RFID считывателя\",\"secureLevel\":true},{\"id\":4,\"key\":\"coefMode3Type1\",\"value\":\"3200\",\"type\":\"int\",\"name\":\"Коэффициент счетчика type1\",\"secureLevel\":true},{\"id\":1,\"key\":\"ChargePointModel\",\"value\":\"CPmodel\",\"type\":\"string\",\"name\":\"Модель ЗС\",\"secureLevel\":false},{\"id\":2,\"key\":\"ChargePointSerialNumber\",\"value\":\"23X000002\",\"type\":\"string\",\"name\":\"Серийный номер\",\"secureLevel\":false},{\"id\":5,\"key\":\"coefMode3Type2\",\"value\":\"400\",\"type\":\"int\",\"name\":\"Коэффициент счетчика type2\",\"secureLevel\":true},{\"id\":6,\"key\":\"mode3rfidStart\",\"value\":\"false\",\"type\":\"bool\",\"name\":\"Старт с RFID\",\"secureLevel\":false},{\"id\":7,\"key\":\"provider\",\"value\":\"full\",\"type\":\"string\",\"name\":\"Использование полного счетчика\",\"secureLevel\":true},{\"id\":9,\"key\":\"restartTime\",\"value\":\"01:00\",\"type\":\"string\",\"name\":\"Время диагностической перезагрузки\",\"secureLevel\":false},{\"id\":10,\"key\":\"statusButton\",\"value\":\"false\",\"type\":\"bool\",\"name\":\"Включение кнопок\",\"secureLevel\":true},{\"id\":11,\"key\":\"parallelCharging\",\"value\":\"true\",\"type\":\"bool\",\"name\":\"Параллельная зарядка\",\"secureLevel\":true},{\"id\":12,\"key\":\"slave1\",\"value\":\"false\",\"type\":\"bool\",\"name\":\"Type2\",\"secureLevel\":false},{\"id\":13,\"key\":\"slave2\",\"value\":\"true\",\"type\":\"bool\",\"name\":\"CHAdeMO\",\"secureLevel\":true},{\"id\":14,\"key\":\"slave3\",\"value\":\"true\",\"type\":\"bool\",\"name\":\"CCS\",\"secureLevel\":true},{\"id\":15,\"key\":\"slave4\",\"value\":\"false\",\"type\":\"bool\",\"name\":\"Type1\",\"secureLevel\":false},{\"id\":16,\"key\":\"slave5\",\"value\":\"true\",\"type\":\"bool\",\"name\":\"GB/T\",\"secureLevel\":true},{\"id\":17,\"key\":\"slave6\",\"value\":\"false\",\"type\":\"bool\",\"name\":\"CCS2\",\"secureLevel\":false},{\"id\":18,\"key\":\"port\",\"value\":\"/dev/ttyUSB0\",\"type\":\"string\",\"name\":\"Порт общения с контроллером\",\"secureLevel\":true},{\"id\":19,\"key\":\"combination\",\"value\":\"12345\",\"type\":\"string\",\"name\":\"Комбинация отображения коннекторов\",\"secureLevel\":true},{\"id\":20,\"key\":\"recomboMode3\",\"value\":\"false\",\"type\":\"bool\",\"name\":\"Подмена изображения Mode3\",\"secureLevel\":true},{\"id\":21,\"key\":\"adresCS\",\"value\":\"ws://10.10.0.255:8080/steve/websocket/CentralSystemService/\",\"type\":\"string\",\"name\":\"Адрес ЦС\",\"secureLevel\":false},{\"id\":22,\"key\":\"maxPowerSlave1\",\"value\":\"22\",\"type\":\"int\",\"name\":\"Максимальная мощность Type2 в кВт\",\"secureLevel\":true},{\"id\":23,\"key\":\"maxAmperSlave1\",\"value\":\"32\",\"type\":\"int\",\"name\":\"Максимальный ток Type2 в А\",\"secureLevel\":true},{\"id\":24,\"key\":\"maxPowerSlave2\",\"value\":\"60\",\"type\":\"int\",\"name\":\"Максимальная мощность CHAdeMO в кВт\",\"secureLevel\":true},{\"id\":25,\"key\":\"maxAmperSlave2\",\"value\":\"120\",\"type\":\"int\",\"name\":\"Максимальный ток CHAdeMO в А\",\"secureLevel\":true},{\"id\":26,\"key\":\"maxPowerSlave3\",\"value\":\"60\",\"type\":\"int\",\"name\":\"Максимальная мощность CCS в кВт\",\"secureLevel\":true},{\"id\":27,\"key\":\"maxAmperSlave3\",\"value\":\"120\",\"type\":\"int\",\"name\":\"Максимальный ток CCS в А\",\"secureLevel\":true},{\"id\":28,\"key\":\"maxPowerSlave4\",\"value\":\"7\",\"type\":\"int\",\"name\":\"Максимальная мощность Type1 в кВт\",\"secureLevel\":true},{\"id\":29,\"key\":\"maxAmperSlave4\",\"value\":\"32\",\"type\":\"int\",\"name\":\"Максимальный ток Type1 в А\",\"secureLevel\":true},{\"id\":30,\"key\":\"maxPowerSlave5\",\"value\":\"60\",\"type\":\"int\",\"name\":\"Максимальная мощность GB/T в кВт\",\"secureLevel\":true},{\"id\":31,\"key\":\"maxAmperSlave5\",\"value\":\"120\",\"type\":\"int\",\"name\":\"Максимальный ток GB/T в А\",\"secureLevel\":true},{\"id\":32,\"key\":\"maxPowerSlave6\",\"value\":\"60\",\"type\":\"int\",\"name\":\"Максимальная мощность CCS2 в кВт\",\"secureLevel\":true},{\"id\":33,\"key\":\"maxAmperSlave6\",\"value\":\"120\",\"type\":\"int\",\"name\":\"Максимальный ток CCS2 в А\",\"secureLevel\":true},{\"id\":35,\"key\":\"statusAdmin\",\"value\":\"false\",\"type\":\"bool\",\"name\":\"Режим Админа\",\"secureLevel\":true},{\"id\":36,\"key\":\"local\",\"value\":\"false\",\"type\":\"bool\",\"name\":\"Локальный режим станции\",\"secureLevel\":false},{\"id\":37,\"key\":\"ChademoPreparing\",\"value\":\"false\",\"type\":\"bool\",\"name\":\"Перевод Чадемо в Preparing с контроллера\",\"secureLevel\":false},{\"id\":38,\"key\":\"LimitationType\",\"value\":\"true\",\"type\":\"bool\",\"name\":\"Тип ограничения станции true-\u003e current, false \u003d power\",\"secureLevel\":false},{\"id\":39,\"key\":\"maxPowerStation\",\"value\":\"160\",\"type\":\"int\",\"name\":\"Максимальная мощность станции кВт\",\"secureLevel\":true},{\"id\":40,\"key\":\"maxCurrentStation\",\"value\":\"160\",\"type\":\"int\",\"name\":\"Максимальный ток станции А\",\"secureLevel\":true},{\"id\":41,\"key\":\"infoStation\",\"value\":\"true\",\"type\":\"bool\",\"name\":\"Вывод информации по станции\",\"secureLevel\":false},{\"id\":42,\"key\":\"modulesPower\",\"value\":\"40\",\"type\":\"int\",\"name\":\"Мощность силового модуля\",\"secureLevel\":false},{\"id\":43,\"key\":\"parkingMod\",\"value\":\"false\",\"type\":\"bool\",\"name\":\"Паркинг мод\",\"secureLevel\":true},{\"id\":44,\"key\":\"shunt\",\"value\":\"8\",\"type\":\"int\",\"name\":\"Настройка шунта\",\"secureLevel\":false},{\"id\":45,\"key\":\"newMira\",\"value\":\"false\",\"type\":\"bool\",\"name\":\"Режим работы с новой мирой\",\"secureLevel\":false},{\"id\":46,\"key\":\"FrontPower\",\"value\":\"true\",\"type\":\"bool\",\"name\":\"Отображение мощности\",\"secureLevel\":false},{\"id\":47,\"key\":\"FrontAReq\",\"value\":\"false\",\"type\":\"bool\",\"name\":\"Отображении тока запроса\",\"secureLevel\":false},{\"id\":48,\"key\":\"FrontATarget\",\"value\":\"true\",\"type\":\"bool\",\"name\":\"Отображение тока фактического\",\"secureLevel\":false},{\"id\":49,\"key\":\"FrontVReq\",\"value\":\"false\",\"type\":\"bool\",\"name\":\"отображение напряжения запроса\",\"secureLevel\":false},{\"id\":50,\"key\":\"FrontVTarget\",\"value\":\"true\",\"type\":\"bool\",\"name\":\"Отображение напряжение фактического\",\"secureLevel\":false},{\"id\":51,\"key\":\"FrontConsumer\",\"value\":\"true\",\"type\":\"bool\",\"name\":\"Отображение потребленной энергии за сессию\",\"secureLevel\":false},{\"id\":52,\"key\":\"FrontFullConsumer\",\"value\":\"false\",\"type\":\"bool\",\"name\":\"Отображение потребленной энергии за все время\",\"secureLevel\":false},{\"id\":53,\"key\":\"FrontMaxASession\",\"value\":\"false\",\"type\":\"bool\",\"name\":\"Отображение ограничения по току за сессию\",\"secureLevel\":false},{\"id\":54,\"key\":\"FrontMaxWSession\",\"value\":\"false\",\"type\":\"bool\",\"name\":\"Отображение ограничения по мощности за сессию\",\"secureLevel\":false},{\"id\":55,\"key\":\"FrontMaxA\",\"value\":\"false\",\"type\":\"bool\",\"name\":\"Отображение максимального тока коннектора\",\"secureLevel\":false},{\"id\":56,\"key\":\"FrontMaxW\",\"value\":\"false\",\"type\":\"bool\",\"name\":\"Отображение максимальной мощности коннектора\",\"secureLevel\":false},{\"id\":57,\"key\":\"FrontMinA\",\"value\":\"false\",\"type\":\"bool\",\"name\":\"Отображение минимального тока коннектора\",\"secureLevel\":false},{\"id\":58,\"key\":\"FrontMinW\",\"value\":\"false\",\"type\":\"bool\",\"name\":\"Отображение минимальной мощности коннектора\",\"secureLevel\":false},{\"id\":59,\"key\":\"FrontTemperature\",\"value\":\"false\",\"type\":\"bool\",\"name\":\"Отображение температуры АКБ (только ГБТ)\",\"secureLevel\":false},{\"id\":60,\"key\":\"FrontTimeCharge\",\"value\":\"true\",\"type\":\"bool\",\"name\":\"Отображение времени зарядки\",\"secureLevel\":false},{\"id\":61,\"key\":\"startPreparing\",\"value\":\"false\",\"type\":\"bool\",\"name\":\"Автоматический старт при обноружении Preparing\",\"secureLevel\":false},{\"id\":62,\"key\":\"flagNoResponseStop\",\"value\":\"false\",\"type\":\"bool\",\"name\":\"Отключение ожидания ответа стопа транзакции (Для трассы)\",\"secureLevel\":false},{\"id\":63,\"key\":\"connectorInsertedTime\",\"value\":\"90\",\"type\":\"int\",\"name\":\"Таймер ожидания после вставки коннектора Mod4\",\"secureLevel\":false},{\"id\":64,\"key\":\"flaglegLift\",\"value\":\"false\",\"type\":\"bool\",\"name\":\"Флаг поднятия ножки\",\"secureLevel\":false},{\"id\":3,\"key\":\"ChargePointID\",\"value\":\"23X000002\",\"type\":\"string\",\"name\":\"Идентификатор ЗС\",\"secureLevel\":false}]"}}]
     */
    @Override
    public void sendBootNotification(List<Object> parsedMessage) {
        Map<String, Map<String, String>> tablesMap = (Map<String, Map<String, String>>) parsedMessage.get(2);
        String fixedString = (tablesMap.get("tables")).get("config_zs")
                .replace("[*[", "[[")
                .replace("\\\"", "\"");

        try {
            List<Map<String, Object>> configZSList = (new ObjectMapper()).readValue(fixedString, List.class);
            if (configZSList != null) {
                String addressCP = null, chargePointID = null, model = null;

                for (Map<String, Object> map : configZSList) {
                    String key = map.get("key").toString();
                    switch (key) {
                        case ("adresCS"):
                            addressCP = map.get("value").toString();
                            break;
                        case ("ChargePointID"):
                            chargePointID = map.get("value").toString();
                            break;
                        case ("ChargePointModel"):
                            model = map.get("value").toString();
                    }
                }

                if (addressCP != null && chargePointID != null && model != null) {
                    log.info("OCPP is ready to connect with the central system and send the boot notification");

                    if (addressCP.endsWith("/")) {
                        addressCP = addressCP.substring(0, addressCP.length() - 1);
                    }

                    ClientCoreProfile core = handler.getCore();
                    JSONClient jsonClient = new JSONClient(core, chargePointID);
                    jsonClient.connect(addressCP, null);
                    client = jsonClient;

                    // Use the feature profile to help create event
                    Request request = core.createBootNotificationRequest(vendorName, model);

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
        } catch (JsonProcessingException e) {
            log.error("Error when parsing config_zs table");
        }
    }

    private void handleResponse(Confirmation confirmation) {
        BootNotificationConfirmation bootNotificationConfirmation = (BootNotificationConfirmation) confirmation;
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
