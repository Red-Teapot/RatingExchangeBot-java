package me.redteapot.rebot.data.models;

import discord4j.common.util.Snowflake;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dizitart.no2.NitriteId;
import org.dizitart.no2.objects.Id;

@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Exchange {
    @Id
    private NitriteId id;

    private Snowflake guild;
    private String name;
    private Snowflake submissionChannel;

    public Exchange(Snowflake guild, String name, Snowflake submissionChannel) {
        this.guild = guild;
        this.name = name;
        this.submissionChannel = submissionChannel;
    }
}
