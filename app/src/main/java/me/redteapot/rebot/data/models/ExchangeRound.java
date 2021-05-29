package me.redteapot.rebot.data.models;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dizitart.no2.NitriteId;
import org.dizitart.no2.objects.Id;

import java.time.Duration;
import java.time.ZonedDateTime;

import static me.redteapot.rebot.Checks.unreachable;

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

    private ZonedDateTime nextInvokeTime;

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

        updateNextInvokeTime();
    }

    private void updateNextInvokeTime() {
        if (state == null) {
            return;
        }

        switch (state) {
            case BEFORE_SUBMISSIONS:
            case ASSIGNMENTS_SENT:
                nextInvokeTime = startDateTime;
                break;
            case ACCEPTING_SUBMISSIONS:
                nextInvokeTime = startDateTime.plus(submissionDuration);
                break;
            case GRACE_PERIOD:
                nextInvokeTime = startDateTime.plus(submissionDuration).plus(graceDuration);
                break;
            default:
                unreachable("Unknown state: {}", state);
                break;
        }
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

    public void setState(State state) {
        this.state = state;
        updateNextInvokeTime();
    }

    public enum State {
        BEFORE_SUBMISSIONS,
        ACCEPTING_SUBMISSIONS,
        GRACE_PERIOD,
        ASSIGNMENTS_SENT,
    }
}
