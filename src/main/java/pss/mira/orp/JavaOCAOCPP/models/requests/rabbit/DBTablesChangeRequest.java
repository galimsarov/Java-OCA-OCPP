package pss.mira.orp.JavaOCAOCPP.models.requests.rabbit;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Data
public class DBTablesChangeRequest {
    private String nameDB;
    private String updateKey;
    private List<Map<String, String>> values;
}