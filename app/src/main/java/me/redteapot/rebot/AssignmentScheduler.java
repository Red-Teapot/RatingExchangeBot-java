package me.redteapot.rebot;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import lombok.extern.slf4j.Slf4j;
import me.redteapot.rebot.data.models.Exchange;

import java.io.Closeable;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static me.redteapot.rebot.Checks.ensure;
import static me.redteapot.rebot.Markdown.md;
import static me.redteapot.rebot.formatting.Formatter.format;

@Slf4j
public class AssignmentScheduler implements Closeable, Runnable {
    private static final int SCHEDULE_OFFSET = 5;

    private final GatewayDiscordClient client;
    private final Config config;

    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> future;

    public AssignmentScheduler(GatewayDiscordClient client, Config config) {
        this.client = client;
        this.config = config;

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


        /*for (Exchange exchange : exchanges) {
            ensure(exchange.getNextInvokeTime().compareTo(now) <= 0, "Exchange next invoke time is greater than now");
            log.debug("Processing round {} of exchange {}", exchange.getRound(), exchange.getId());
            processExchangeRound(exchange);
        }*/

        schedule();
    }

    private void processExchangeRound(Exchange exchange) {
        Guild guild = client.getGuildById(exchange.getGuild()).block();
        ensure(guild != null, "Guild is null");
        GuildMessageChannel channel = (GuildMessageChannel) guild.getChannelById(exchange.getSubmissionChannel()).block();
        ensure(channel != null, "Channel is null");

        Markdown message;
        switch (exchange.getState()) {
            case BEFORE_SUBMISSIONS:
                message = md("Submissions for `{}` start now!", exchange.getName())
                    .line("You have {} to submit your games (submissions end at {}).",
                        exchange.getSubmissionDuration(),
                        exchange.getGraceStart())
                    .line("Use the following command: {}",
                        addPrefixTo(format(" submit {} <link to your game>", exchange.getName())))
                    .line("Please make sure to use the full link to the jam page of your game, otherwise it may not be recognized.");
                channel.createMessage(message.toString()).block();
                exchange.setState(Exchange.State.ACCEPTING_SUBMISSIONS);
                break;
            case ACCEPTING_SUBMISSIONS:
                message = md("Submissions period for `{}` has just ended.", exchange.getName())
                    .line("No further submissions will be accepted.")
                    .line("If you have missed this round, you can wait until another starts.");
                channel.createMessage(message.toString()).block();
                exchange.setState(Exchange.State.GRACE_PERIOD);
                break;
            case GRACE_PERIOD:
                message = md("Sending assignments for `{}`.", exchange.getName())
                    .line("If you have submitted a game, you should receive your assignments soon.")
                    .line("If this doesn't happen, please inform server administrators.");
                channel.createMessage(message.toString()).block();

                // TODO Assignments

                exchange.setState(Exchange.State.BEFORE_SUBMISSIONS);
                break;
        }

        //exchangeRepo.update(exchange);
    }

    private void schedule() {
        /*ZonedDateTime now = ZonedDateTime.now();
        Exchange next = exchangeRepo.find(
            and(not(eq("state", Exchange.State.FINISHED)),
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

        ZonedDateTime nextInvokeTime = next.getNextInvokeTime().plus(SCHEDULE_OFFSET, ChronoUnit.SECONDS);
        long delay = now.until(nextInvokeTime, ChronoUnit.SECONDS);
        future = executor.schedule(this, delay, TimeUnit.SECONDS);

        log.debug("Scheduled next invocation on {} ({} seconds from now)", next, delay);*/
    }

    private String addPrefixTo(String command) {
        if (config.getPrefix().isBlank()) {
            return format("<@{}>`{}`", client.getSelfId().asString(), command);
        } else {
            return format("`{}{}`", config.getPrefix(), command);
        }
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
