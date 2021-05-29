package me.redteapot.rebot;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import lombok.extern.slf4j.Slf4j;
import me.redteapot.rebot.data.Database;
import me.redteapot.rebot.data.models.Exchange;
import me.redteapot.rebot.data.models.ExchangeRound;
import org.dizitart.no2.FindOptions;
import org.dizitart.no2.SortOrder;
import org.dizitart.no2.objects.Cursor;
import org.dizitart.no2.objects.ObjectRepository;

import java.io.Closeable;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static me.redteapot.rebot.Checks.ensure;
import static me.redteapot.rebot.Markdown.md;
import static org.dizitart.no2.objects.filters.ObjectFilters.*;

@Slf4j
public class AssignmentScheduler implements Closeable, Runnable {
    private final GatewayDiscordClient client;

    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> future;

    private final ObjectRepository<Exchange> exchangeRepo = Database.getRepository(Exchange.class);
    private final ObjectRepository<ExchangeRound> roundRepo = Database.getRepository(ExchangeRound.class);

    public AssignmentScheduler(GatewayDiscordClient client) {
        this.client = client;

        executor = Executors.newSingleThreadScheduledExecutor();

        log.debug("Created assignment scheduler");

        ensure(future == null, "Future is not null in the constructor");
        reschedule();
    }

    public void reschedule() {
        if (future != null) {
            future.cancel(false);
        }

        future = executor.schedule(this, 1, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        ZonedDateTime now = ZonedDateTime.now();

        Cursor<ExchangeRound> rounds = roundRepo.find(
            and(not(eq("state", ExchangeRound.State.ASSIGNMENTS_SENT)),
                lte("nextInvokeTime", now)));

        for (ExchangeRound round : rounds) {
            log.debug("Processing round {}", round.getId());
            processRound(round);
        }

        schedule();
    }

    private void processRound(ExchangeRound round) {
        Exchange exchange = exchangeRepo.getById(round.getExchangeID());
        Guild guild = client.getGuildById(exchange.getGuild()).block();
        ensure(guild != null, "Guild is null");
        GuildMessageChannel channel = (GuildMessageChannel) guild.getChannelById(exchange.getSubmissionChannel()).block();
        ensure(channel != null, "Channel is null");

        switch (round.getState()) {
            case BEFORE_SUBMISSIONS:
                Markdown message = md("Submissions for **{}** start now!", exchange.getName())
                    .line("You have {} to submit your games (submissions end at {}).", round.getSubmissionDuration(), round.getStartDateTime().plus(round.getSubmissionDuration()))
                    .line("Use the following command: `<prefix> submit {} <your game link>`", exchange.getName());
                channel.createMessage(message.toString()).block();
                round.setState(ExchangeRound.State.ACCEPTING_SUBMISSIONS);
                break;
        }

        roundRepo.update(round);
    }

    private void schedule() {
        ZonedDateTime now = ZonedDateTime.now();
        ExchangeRound next = roundRepo.find(
            and(not(eq("state", ExchangeRound.State.ASSIGNMENTS_SENT)),
                gte("nextInvokeTime", now)),
            FindOptions.sort("nextInvokeTime", SortOrder.Ascending))
            .firstOrDefault();

        if (next == null) {
            log.debug("No invocation scheduled");
            return;
        }

        if (future != null) {
            future.cancel(false);
        }

        long delay = now.until(next.getNextInvokeTime(), ChronoUnit.SECONDS);
        future = executor.schedule(this, delay, TimeUnit.SECONDS);

        log.debug("Scheduled next invocation on {} ({} seconds from now)", next.getNextInvokeTime(), delay);
    }

    @Override
    public void close() {
        if (future != null) {
            future.cancel(false);
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.MINUTES)) {
                log.warn("Executor didn't shut down in time, terminating");
            }
        } catch (InterruptedException ignored) {
        }
    }
}
