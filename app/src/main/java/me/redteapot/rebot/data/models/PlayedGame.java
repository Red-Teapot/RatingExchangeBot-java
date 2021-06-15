package me.redteapot.rebot.data.models;

import discord4j.common.util.Snowflake;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.net.URL;

@Entity
@Table(indexes = @Index(name = "uniquePlayedGame", columnList = "member, link", unique = true))
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayedGame {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private Snowflake member;
    private URL link;

    @Column(columnDefinition = "boolean default false")
    private boolean manual;

    public PlayedGame(Snowflake member, URL link, boolean manual) {
        this.member = member;
        this.link = link;
        this.manual = manual;
    }
}
