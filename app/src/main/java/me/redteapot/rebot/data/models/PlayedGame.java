package me.redteapot.rebot.data.models;

import discord4j.common.util.Snowflake;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dizitart.no2.NitriteId;
import org.dizitart.no2.objects.Id;

@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PlayedGame {
    @Id
    private NitriteId id;

    private Snowflake member;
    private String link;

    public PlayedGame(Snowflake member, String link) {
        this.member = member;
        this.link = link;
    }
}
