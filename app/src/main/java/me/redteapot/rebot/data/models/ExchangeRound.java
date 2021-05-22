package me.redteapot.rebot.data.models;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dizitart.no2.NitriteId;
import org.dizitart.no2.objects.Id;

import java.time.Duration;
import java.time.ZonedDateTime;

@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExchangeRound {
    @Id
    private NitriteId id;

    private NitriteId exchangeID;

    private int gamesPerMember;

    private ZonedDateTime startDateTime;
    private Duration submissionDuration;
    private Duration graceDuration;

    private State state;

    public ExchangeRound(NitriteId exchangeID,
                         int gamesPerMember,
                         ZonedDateTime startDateTime,
                         Duration submissionDuration,
                         Duration graceDuration,
                         State state) {
        this.exchangeID = exchangeID;
        this.gamesPerMember = gamesPerMember;
        this.startDateTime = startDateTime;
        this.submissionDuration = submissionDuration;
        this.graceDuration = graceDuration;
        this.state = state;
    }

    public enum State {
        BEFORE_SUBMISSIONS,
        ACCEPTING_SUBMISSIONS,
        GRACE_PERIOD,
        ASSIGNMENTS_SENT,
    }
}
