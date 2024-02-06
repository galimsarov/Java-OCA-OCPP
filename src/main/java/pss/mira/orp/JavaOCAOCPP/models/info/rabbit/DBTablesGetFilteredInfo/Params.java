package pss.mira.orp.JavaOCAOCPP.models.info.rabbit.DBTablesGetFilteredInfo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
public class Params {
    private List<Search> search;
}
