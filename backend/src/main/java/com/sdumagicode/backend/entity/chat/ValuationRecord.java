package com.sdumagicode.backend.entity.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Document("valuation_record")
public class ValuationRecord {
    @Id
    private String valuationId;

    private Long chatId;

    private List<ValuationRank> valuationRanks;
}
