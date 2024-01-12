package pss.mira.orp.JavaOCAOCPP.service.pc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.ZonedDateTime;

import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;

@Service
@Slf4j
public class TimeSetterImpl implements TimeSetter {
    @Override
    public void setTime(ZonedDateTime time) {
        String TimeHearbeat = time.toString();
        int index1 = TimeHearbeat.indexOf('+');

        if (index1 != -1) {
            TimeHearbeat = TimeHearbeat.substring(0, TimeHearbeat.indexOf('+'));

            log.info(TimeHearbeat);
            log.info("Получение времени после редактирования");

            TimeHearbeat = TimeHearbeat.replace("T", " ");
            TimeHearbeat = TimeHearbeat.replace("Z", "");

            log.info(TimeHearbeat);

            if (IS_OS_WINDOWS) {
                try {
                    log.info("Попытка задать время");
                    Runtime.getRuntime().exec("cmd /C date " + "'" + TimeHearbeat + "'");
                    Runtime.getRuntime().exec("cmd /C time " + "'" + TimeHearbeat + "'");
                } catch (IOException e) {
                    log.error("Время задать не удалось");
                }
            } else {
                try {
                    String Command = "sudo timedatectl set-time " + "'" + TimeHearbeat + "'";
                    try {
                        Runtime.getRuntime().exec(new String[]{"bash", "-c", Command});
                        log.info("Текущее время системы откорректировано");
                    } catch (IOException e) {
                        log.error("Время задать не удалось");
                    }
                    Runtime.getRuntime().exec(Command);
                } catch (IOException ex) {
                    log.error("Выполнить команду не удалось");
                }
            }
        } else {
            log.info(TimeHearbeat);
            log.info("Получение времени после редактирования");
            TimeHearbeat = TimeHearbeat.replace("T", " ");
            TimeHearbeat = TimeHearbeat.replace("Z", "");
            log.info(TimeHearbeat);
            String Command = "sudo timedatectl set-time " + "'" + TimeHearbeat + "'";
            if (!IS_OS_WINDOWS) {
                try {
                    try {
                        Runtime.getRuntime().exec(new String[]{"bash", "-c", Command});
                        log.info("Текущее время системы откорректировано");
                    } catch (IOException e) {
                        log.error("Время задать не удалось");
                    }
                } catch (Exception ex) {
                    log.error("Ошибка при попытке задать время");
                }
            } else {
                try {
                    log.info("Попытка задать время");
                    Runtime.getRuntime().exec("cmd /C date " + "'" + TimeHearbeat + "'");
                    Runtime.getRuntime().exec("cmd /C time " + "'" + TimeHearbeat + "'");
                } catch (IOException e) {
                    log.error("Ошибка при попытке задать время");
                }
            }
        }
    }
}
