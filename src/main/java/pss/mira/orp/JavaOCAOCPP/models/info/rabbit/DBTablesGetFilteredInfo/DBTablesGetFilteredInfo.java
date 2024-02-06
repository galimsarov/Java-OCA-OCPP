package pss.mira.orp.JavaOCAOCPP.models.info.rabbit.DBTablesGetFilteredInfo;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class DBTablesGetFilteredInfo {
    private String nameTable;
    private Params params;
}
