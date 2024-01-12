package pss.mira.orp.JavaOCAOCPP.models.requests;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
public class DBTablesRequest {
    private List<String> tables;
}