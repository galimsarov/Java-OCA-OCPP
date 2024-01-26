package pss.mira.orp.JavaOCAOCPP.models.requests.rabbit;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class DBTablesDeleteRequest {
    private String nameTable;
    private String key;
    private Object value;
}