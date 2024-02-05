package pss.mira.orp.JavaOCAOCPP.models.info.rabbit;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Data
public class DBTablesCreateInfo {
    private String nameDB;
    private List<Map<String, String>> values;
}