package me.redteapot.rebot.data.models;

import discord4j.common.util.Snowflake;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(indexes = @Index(name = "uniqueExchange", columnList = "guild, name", unique = true))
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Exchange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private Snowflake guild;
    private String name;

    private Snowflake submissionChannel;

    private int totalRounds;

    private int gamesPerMember;

    private ZonedDateTime startDateTime;
    private String timezone;
    private Duration submissionDuration;
    private Duration graceDuration;

    private int round;
    private State state;

    private ZonedDateTime nextInvokeTime;

    public Exchange(Snowflake guild, String name, Snowflake submissionChannel, int totalRounds, State state,
                    int gamesPerMember, ZonedDateTime startDateTime, String timezone, Duration submissionDuration, Duration graceDuration) {
        this.guild = guild;
        this.name = name;
        this.submissionChannel = submissionChannel;
        this.totalRounds = totalRounds;
        this.gamesPerMember = gamesPerMember;
        this.startDateTime = startDateTime;
        this.timezone = timezone;
        this.submissionDuration = submissionDuration;
        this.graceDuration = graceDuration;

        this.round = 0;
        this.state = state;

        update();
    }

    public ZonedDateTime getStartDateTime() {
        startDateTime = startDateTime.withZoneSameInstant(ZoneId.of(timezone));
        return startDateTime;
    }

    public void setRound(int round) {
        this.round = round;
        update();
    }

    public void setState(State state) {
        this.state = state;
        update();
    }

    public void setStartDateTime(ZonedDateTime startDateTime) {
        this.startDateTime = startDateTime;
        update();
    }

    public void setSubmissionDuration(Duration submissionDuration) {
        this.submissionDuration = submissionDuration;
        update();
    }

    public void setGraceDuration(Duration graceDuration) {
        this.graceDuration = graceDuration;
        update();
    }

    public Duration getTotalRoundDuration() {
        return submissionDuration.plus(graceDuration);
    }

    public ZonedDateTime getRoundStart() {
        return getStartDateTime().plus(getTotalRoundDuration().multipliedBy(round));
    }

    public ZonedDateTime getGraceStart() {
        return getRoundStart().plus(submissionDuration);
    }

    public ZonedDateTime getRoundEnd() {
        return getGraceStart().plus(graceDuration);
    }

    private void update() {
        if (startDateTime == null || submissionDuration == null || graceDuration == null) {
            return;
        }

        final ZonedDateTime roundStartTime = getRoundStart();

        switch (state) {
            case BEFORE_SUBMISSIONS:
                nextInvokeTime = roundStartTime;
                break;
            case ACCEPTING_SUBMISSIONS:
                nextInvokeTime = roundStartTime.plus(submissionDuration);
                break;
            case GRACE_PERIOD:
                nextInvokeTime = roundStartTime.plus(submissionDuration).plus(graceDuration);
                break;
            case FINISHED:
                // Nothing to do
                break;
        }
    }

    public enum State {
        BEFORE_SUBMISSIONS,
        ACCEPTING_SUBMISSIONS,
        GRACE_PERIOD,
        FINISHED,
    }
}
