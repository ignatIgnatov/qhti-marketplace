package com.platform.ads.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("boat_console_types")
public class BoatConsoleType {

    @Id
    private Long id;

    @Column("boat_spec_id")
    private Long boatSpecId;

    @Column("console_type")
    private String consoleType;
}