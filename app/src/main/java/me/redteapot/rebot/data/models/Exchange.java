package me.redteapot.rebot.data.models;

import discord4j.common.util.Snowflake;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.Duration;
import java.time.ZonedDateTime;

@Entity
@Table(indexes = @Index(name = "unique", columnList = "guild, name", unique = true))
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Exchange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private Snowflake guild;
    private String name;

    private Snowflake submissionChannel;

    private int round;
    private State state;

    private int gamesPerMember;

    private ZonedDateTime startDateTime;
    private Duration submissionDuration;
    private Duration graceDuration;

    private ZonedDateTime nextInvokeTime;

    public Exchange(Snowflake guild, String name, Snowflake submissionChannel, int round, State state,
                    int gamesPerMember, ZonedDateTime startDateTime, Duration submissionDuration, Duration graceDuration) {
        this.guild = guild;
        this.name = name;
        this.submissionChannel = submissionChannel;
        this.round = round;
        this.state = state;
        this.gamesPerMember = gamesPerMember;
        this.startDateTime = startDateTime;
        this.submissionDuration = submissionDuration;
        this.graceDuration = graceDuration;

        updateNextInvokeTime();
    }

    public void setRound(int round) {
        this.round = round;
        updateNextInvokeTime();
    }

    public void setState(State state) {
        this.state = state;
        updateNextInvokeTime();
    }

    public void setStartDateTime(ZonedDateTime startDateTime) {
        this.startDateTime = startDateTime;
        updateNextInvokeTime();
    }

    public void setSubmissionDuration(Duration submissionDuration) {
        this.submissionDuration = submissionDuration;
        updateNextInvokeTime();
    }

    public void setGraceDuration(Duration graceDuration) {
        this.graceDuration = graceDuration;
        updateNextInvokeTime();
    }

    public Duration getTotalRoundDuration() {
        return submissionDuration.plus(graceDuration);
    }

    public ZonedDateTime getRoundStart() {
        return startDateTime.plus(getTotalRoundDuration().multipliedBy(round));
    }

    public ZonedDateTime getGraceStart() {
        return getRoundStart().plus(submissionDuration);
    }

    public ZonedDateTime getRoundEnd() {
        return getGraceStart().plus(graceDuration);
    }

    private void updateNextInvokeTime() {
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
