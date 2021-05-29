package me.redteapot.rebot.data.models;

import discord4j.common.util.Snowflake;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dizitart.no2.NitriteId;
import org.dizitart.no2.objects.Id;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Submission {
    @Id
    private NitriteId id;

    private NitriteId exchangeID;
    private int round;
    private String link;
    private Snowflake member;
    private ZonedDateTime submissionDatetime;

    public Submission(NitriteId exchangeID, int round, String link,
                      Snowflake member, ZonedDateTime submissionDatetime) {
        this.exchangeID = exchangeID;
        this.round = round;
        this.link = link;
        this.member = member;
        this.submissionDatetime = submissionDatetime;
    }
}
