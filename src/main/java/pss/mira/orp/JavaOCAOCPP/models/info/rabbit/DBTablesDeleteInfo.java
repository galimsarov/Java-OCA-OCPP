package pss.mira.orp.JavaOCAOCPP.models.info.rabbit;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class DBTablesDeleteInfo {
    private String nameTable;
    private String key;
    private Object value;
}