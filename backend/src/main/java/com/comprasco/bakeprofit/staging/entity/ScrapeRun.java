package com.comprasco.bakeprofit.staging.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity 
@Getter 
@NoArgsConstructor 
@Table(name = "scrape_runs")
public class ScrapeRun {
    @Id 
    private java.util.UUID id;

    private java.time.OffsetDateTime startedAt;
    
    private java.time.OffsetDateTime completedAt;
}
