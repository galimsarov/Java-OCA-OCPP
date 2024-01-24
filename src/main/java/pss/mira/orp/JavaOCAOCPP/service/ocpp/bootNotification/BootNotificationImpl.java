package pss.mira.orp.JavaOCAOCPP.service.ocpp.bootNotification;

import eu.chargetime.ocpp.ClientEvents;
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
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handler.core.CoreHandler;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handler.reservation.ReservationHandler;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.heartBeat.Heartbeat;
import pss.mira.orp.JavaOCAOCPP.service.pc.TimeSetter;

import java.util.List;

import static eu.chargetime.ocpp.model.core.RegistrationStatus.Accepted;

@Service
@Slf4j
public class BootNotificationImpl implements BootNotification {
    private final CoreHandler coreHandler;
    private final ReservationHandler reservationHandler;
    private final Heartbeat heartbeat;
    private final TimeSetter timeSetter;

    private JSONClient client;
    @Value("${vendor.name}")
    private String vendorName;

    public BootNotificationImpl(
            CoreHandler coreHandler, ReservationHandler reservationHandler, Heartbeat heartbeat, TimeSetter timeSetter
    ) {
        this.coreHandler = coreHandler;
        this.reservationHandler = reservationHandler;
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
     * ["db","16322295-7cb6-45cc-8e83-38c6c9b27a98",{"tables":[{"nameTable":"config_zs","result":[{"name":"Порт общения с контроллером","secure_level":true,"id":18,"type":"string","value":"/dev/ttyS0","key":"port"},{"name":"Type2","secure_level":false,"id":12,"type":"bool","value":"false","key":"slave1"},{"name":"Максимальный ток CHAdeMO в А","secure_level":true,"id":29,"type":"int","value":"125","key":"maxAmperSlave2"},{"name":"Максимальная мощность CCS в кВт","secure_level":true,"id":30,"type":"int","value":"150","key":"maxPowerSlave3"},{"name":"Максимальный ток CCS в А","secure_level":true,"id":31,"type":"int","value":"200","key":"maxAmperSlave3"},{"name":"Максимальная мощность GB/T в кВт","secure_level":true,"id":34,"type":"int","value":"150","key":"maxPowerSlave5"},{"name":"Максимальный ток GB/T в А","secure_level":true,"id":35,"type":"int","value":"155","key":"maxAmperSlave5"},{"name":"Максимальная мощность CCS2 в кВт","secure_level":true,"id":36,"type":"int","value":"150","key":"maxPowerSlave6"},{"name":"Максимальный ток CCS2 в А","secure_level":true,"id":37,"type":"int","value":"200","key":"maxAmperSlave6"},{"name":"Локальный режим станции","secure_level":false,"id":40,"type":"bool","value":"false","key":"local"},{"name":"Максимальная мощность станции кВт","secure_level":true,"id":43,"type":"int","value":"150","key":"maxPowerStation"},{"name":"Максимальный ток станции А","secure_level":true,"id":44,"type":"int","value":"200","key":"maxCurrentStation"},{"name":"Отображение мощности","secure_level":false,"id":50,"type":"bool","value":"true","key":"FrontPower"},{"name":"Отображение тока фактического","secure_level":false,"id":52,"type":"bool","value":"true","key":"FrontATarget"},{"name":"Отображение напряжение фактического","secure_level":false,"id":54,"type":"bool","value":"true","key":"FrontVTarget"},{"name":"Отображение потребленной энергии за сессию","secure_level":false,"id":55,"type":"bool","value":"true","key":"FrontConsumer"},{"name":"Отображение времени зарядки","secure_level":false,"id":64,"type":"bool","value":"true","key":"FrontTimeCharge"},{"name":"Идентификатор ЗС","secure_level":false,"id":3,"type":"string","value":"22F123456","key":"ChargePointID"},{"name":"Модель ЗС","secure_level":false,"id":1,"type":"string","value":"CPmodel","key":"ChargePointModel"},{"name":"Коэффициент счетчика type1","secure_level":true,"id":4,"type":"int","value":"3200","key":"coefMode3Type1"},{"name":"Коэффициент счетчика type2","secure_level":true,"id":5,"type":"int","value":"400","key":"coefMode3Type2"},{"name":"Старт с RFID","secure_level":false,"id":6,"type":"bool","value":"false","key":"mode3rfidStart"},{"name":"Использование полного счетчика","secure_level":true,"id":7,"type":"string","value":"full","key":"provider"},{"name":"Включение RFID считывателя","secure_level":true,"id":8,"type":"bool","value":"false","key":"EnableCardReader"},{"name":"Время диагностической перезагрузки","secure_level":false,"id":9,"type":"string","value":"01:00","key":"restartTime"},{"name":"Включение кнопок","secure_level":true,"id":10,"type":"bool","value":"false","key":"statusButton"},{"name":"Параллельная зарядка","secure_level":true,"id":11,"type":"bool","value":"true","key":"parallelCharging"},{"name":"CHAdeMO","secure_level":true,"id":13,"type":"bool","value":"true","key":"slave2"},{"name":"CCS","secure_level":true,"id":14,"type":"bool","value":"true","key":"slave3"},{"name":"Type1","secure_level":false,"id":15,"type":"bool","value":"false","key":"slave4"},{"name":"GB/T","secure_level":true,"id":16,"type":"bool","value":"true","key":"slave5"},{"name":"CCS2","secure_level":false,"id":17,"type":"bool","value":"false","key":"slave6"},{"name":"Ширина дисплея","secure_level":true,"id":19,"type":"int","value":"1024","key":"screenWith"},{"name":"Высота дисплея","secure_level":true,"id":20,"type":"int","value":"768","key":"screenHeight"},{"name":"На весь экран фронт","secure_level":true,"id":21,"type":"bool","value":"true","key":"FullScreen"},{"name":"Подмена изображения Mode3","secure_level":true,"id":23,"type":"bool","value":"false","key":"recomboMode3"},{"name":"Полный путь до базы фронта","secure_level":true,"id":24,"type":"string","value":"sqlite:/home/pss/software/pss/ocpp/ocpp16.db","key":"jdbcFront"},{"name":"Адрес ЦС","secure_level":false,"id":25,"type":"string","value":"ws://10.10.0.255:8080/steve/websocket/CentralSystemService/","key":"adresCS"},{"name":"Максимальная мощность Type2 в кВт","secure_level":true,"id":26,"type":"int","value":"22","key":"maxPowerSlave1"},{"name":"Максимальный ток Type2 в А","secure_level":true,"id":27,"type":"int","value":"32","key":"maxAmperSlave1"},{"name":"Максимальная мощность CHAdeMO в кВт","secure_level":true,"id":28,"type":"int","value":"60","key":"maxPowerSlave2"},{"name":"Максимальная мощность Type1 в кВт","secure_level":true,"id":32,"type":"int","value":"7","key":"maxPowerSlave4"},{"name":"Максимальный ток Type1 в А","secure_level":true,"id":33,"type":"int","value":"32","key":"maxAmperSlave4"},{"name":"Режим Админа","secure_level":true,"id":39,"type":"bool","value":"false","key":"statusAdmin"},{"name":"Перевод Чадемо в Preparing с контроллера","secure_level":false,"id":41,"type":"bool","value":"false","key":"ChademoPreparing"},{"name":"Тип ограничения станции true-\u003e current, false \u003d power","secure_level":false,"id":42,"type":"bool","value":"true","key":"LimitationType"},{"name":"Вывод информации по станции","secure_level":false,"id":45,"type":"bool","value":"true","key":"infoStation"},{"name":"Мощность силового модуля","secure_level":false,"id":46,"type":"int","value":"40","key":"modulesPower"},{"name":"Паркинг мод","secure_level":true,"id":47,"type":"bool","value":"false","key":"parkingMod"},{"name":"Настройка шунта","secure_level":false,"id":48,"type":"int","value":"8","key":"shunt"},{"name":"Режим работы с новой мирой","secure_level":false,"id":49,"type":"bool","value":"false","key":"newMira"},{"name":"Отображении тока запроса","secure_level":false,"id":51,"type":"bool","value":"false","key":"FrontAReq"},{"name":"отображение напряжения запроса","secure_level":false,"id":53,"type":"bool","value":"false","key":"FrontVReq"},{"name":"Отображение ограничения по току за сессию","secure_level":false,"id":57,"type":"bool","value":"false","key":"FrontMaxASession"},{"name":"Отображение ограничения по мощности за сессию","secure_level":false,"id":58,"type":"bool","value":"false","key":"FrontMaxWSession"},{"name":"Отображение максимального тока коннектора","secure_level":false,"id":59,"type":"bool","value":"false","key":"FrontMaxA"},{"name":"Отображение максимальной мощности коннектора","secure_level":false,"id":60,"type":"bool","value":"false","key":"FrontMaxW"},{"name":"Отображение минимального тока коннектора","secure_level":false,"id":61,"type":"bool","value":"false","key":"FrontMinA"},{"name":"Отображение минимальной мощности коннектора","secure_level":false,"id":62,"type":"bool","value":"false","key":"FrontMinW"},{"name":"Автоматический старт при обноружении Preparing","secure_level":false,"id":65,"type":"bool","value":"false","key":"startPreparing"},{"name":"Отключение ожидания ответа стопа транзакции (Для трассы)","secure_level":false,"id":66,"type":"bool","value":"false","key":"flagNoResponseStop"},{"name":"Таймер ожидания после вставки коннектора Mod4","secure_level":false,"id":67,"type":"int","value":"90","key":"connectorInsertedTime"},{"name":"Флаг поднятия ножки","secure_level":false,"id":68,"type":"bool","value":"false","key":"flaglegLift"},{"name":"Комбинация отображения коннекторов","secure_level":true,"id":22,"type":"string","value":"12345","key":"combination"},{"name":"Серийный номер","secure_level":false,"id":2,"type":"string","value":"22F123456","key":"ChargePointSerialNumber"},{"name":"Версия службы и контроллера","secure_level":false,"id":38,"type":"string","value":"Firmware Service - 8e7a0a7_2023-09-12::Mode3_type2 - 0.100_0.0.0_22-6-18::CHAdeMO - 3.5_1.3.2_23-7-27::CCS - 3.5_0.0.0_23-7-27::GBT - 1.46_0.0.0_23-8-29","key":"version"},{"name":"Число зарядных точек","secure_level":true,"id":72,"type":"int","value":"3","key":"chargingPointsCount"},{"name":"Число силовых модулей","secure_level":false,"id":91,"type":"int","value":"4","key":"counterPowerModules"},{"name":"Флаг для отключение ожидания ответа на стоп","secure_level":false,"id":93,"type":"bool","value":"false","key":"FlagNoResponseStop"},{"name":"EVCCID?","secure_level":false,"id":94,"type":"bool","value":"false","key":"flagEVCCID"},{"name":"Установлен внешний счётчик","secure_level":false,"id":95,"type":"bool","value":"false","key":"externalMeter"},{"name":"Сдвиг по времени относительно UTC","secure_level":false,"id":121,"type":"string","value":"05:00","key":"utcUpdate"},{"name":"Экран сенсорный","secure_level":false,"id":122,"type":"bool","value":"false","key":"touchScreen"},{"name":"Включение Кнокнопки старта/стопа","secure_level":false,"id":126,"type":"bool","value":"false","key":"buttunStartStop"},{"name":"Отображение потребленной энергии за все время","secure_level":false,"id":56,"type":"bool","value":"false","key":"FrontFullConsumer"},{"name":"GB/T2","secure_level":false,"id":130,"type":"bool","value":"false","key":"slave8"},{"name":"Максимальная мощность GB/T2 в кВт","secure_level":false,"id":131,"type":"int","value":"60","key":"maxPowerSlave8"},{"name":"Максимальный ток GB/T2 в А","secure_level":false,"id":132,"type":"int","value":"120","key":"maxAmperSlave8"},{"name":"Настраиваемый порядок разъемов","secure_level":false,"id":133,"type":"bool","value":"false","key":"customizableOrderOfConnectors"},{"name":"Коэффициент трансформации по напряжению","secure_level":false,"id":134,"type":"string","value":"1","key":"voltageTransformationCoefficient"},{"name":"Коэффициент трансформации по току","secure_level":false,"id":135,"type":"string","value":"80","key":"currentTransformationCoefficient"},{"name":"Порт счётчика","secure_level":false,"id":136,"type":"string","value":"/dev/ttyUSB1","key":"meterPortName"},{"name":"Расширенный dataTransfer","secure_level":false,"id":137,"type":"bool","value":"false","key":"elMovement"},{"name":"Отображение температуры АКБ (только ГБТ)","secure_level":false,"id":63,"type":"bool","value":"false","key":"FrontTemperature"}]}]}]
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

            ClientCoreProfile core = coreHandler.getCore();
            JSONClient jsonClient = new JSONClient(core, bootNotificationRequest.getChargePointID());
            jsonClient.addFeatureProfile(reservationHandler.getReservation());

            final boolean[] connectionOpened = {false};
            while (true) {
                jsonClient.connect(bootNotificationRequest.getAddressCP(), new ClientEvents() {
                    @Override
                    public void connectionOpened() {
                        log.info("Connection to the central system is established");
                        connectionOpened[0] = true;
                    }

                    @Override
                    public void connectionClosed() {
                        // Реализован вариант с отсутствием связи, потом связь появляется, начинаем работать
                        // TODO Предусмотреть запуск станции, подключение к ЦС, обрыв связи, восстановление связи
                        log.warn("The connection to the central system has not been established. " +
                                "Another try will be made");
                        connectionOpened[0] = false;
                    }
                });
                if (connectionOpened[0]) {
                    break;
                } else {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        log.error("An error while waiting to connect to the central system");
                    }
                }
            }
            client = jsonClient;

            // Use the feature profile to help create event
            Request request = core.createBootNotificationRequest(vendorName, bootNotificationRequest.getModel());
            log.info("Sent to central system: " + request.toString());
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
                    log.error("Аn error while waiting for a ocpp heartbeat to be sent");
                }
                heartbeat.sendHeartbeat(coreHandler.getCore(), getClient());
            }
        };
        return new Thread(runHeartbeat);
    }

    @Override
    public JSONClient getClient() {
        return client;
    }
}
