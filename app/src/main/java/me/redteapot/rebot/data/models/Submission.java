package me.redteapot.rebot.data.models;

import discord4j.common.util.Snowflake;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.net.URL;
import java.time.ZonedDateTime;

@Entity
@Table(indexes = {
    @Index(name = "uniqueSubmissionByLink", columnList = "exchange_id, round, link", unique = true),
    @Index(name = "uniqueSubmissionByMember", columnList = "exchange_id, round, member", unique = true),
})
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Submission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    private Exchange exchange;
    private int round;
    private URL link;

    private Snowflake member;
    private ZonedDateTime submissionDatetime;

    public Submission(Exchange exchange, int round, URL link,
                      Snowflake member, ZonedDateTime submissionDatetime) {
        this.exchange = exchange;
        this.round = round;
        this.link = link;
        this.member = member;
        this.submissionDatetime = submissionDatetime;
    }
}
