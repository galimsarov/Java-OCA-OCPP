package pss.mira.orp.JavaOCAOCPP.models.requests.rabbit;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
public class DBTablesGetRequest {
    private List<String> tables;
}