package me.redteapot.rebot;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import lombok.extern.slf4j.Slf4j;
import me.redteapot.rebot.assigner.Assigner;
import me.redteapot.rebot.assigner.Member;
import me.redteapot.rebot.data.Database;
import me.redteapot.rebot.data.models.Exchange;
import me.redteapot.rebot.data.models.PlayedGame;
import me.redteapot.rebot.data.models.Submission;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.io.Closeable;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
        log.debug("Running assignment scheduler");

        try {
            Runner runner = new Runner();
            runner.fillPendingExchanges();
            runner.performAssignments();
            runner.scheduleNextInvocation();
        } catch (Throwable e) {
            log.error("Exception while doing assignments", e);
        }
    }

    private void schedule(ZonedDateTime nextInvokeTime) {
        ZonedDateTime now = ZonedDateTime.now();

        if (future != null) {
            future.cancel(false);
        }

        long delay = now.until(nextInvokeTime, ChronoUnit.SECONDS);
        future = executor.schedule(this, delay, TimeUnit.SECONDS);

        log.debug("Scheduled next invocation at {} ({} seconds from now)", nextInvokeTime, delay);
    }

    private String addPrefixTo(String command) {
        if (config.getPrefix().isBlank()) {
            return format("<@{}>`{}`", client.getSelfId().asString(), command);
        } else {
            return format("`{}{}`", config.getPrefix(), command);
        }
    }

    private class Runner {
        private final EntityManager exchangeManager;
        private final EntityManager submissionManager;
        private final EntityManager playedGameManager;

        private final Queue<Exchange> pendingExchanges = new LinkedList<>();

        private final ZonedDateTime start = ZonedDateTime.now();

        Runner() {
            exchangeManager = Database.getInstance().getEntityManager(Exchange.class);
            submissionManager = Database.getInstance().getEntityManager(Submission.class);
            playedGameManager = Database.getInstance().getEntityManager(PlayedGame.class);
        }

        void fillPendingExchanges() {
            Query exchangeQuery = exchangeManager.createQuery("SELECT e FROM Exchange e WHERE e.nextInvokeTime <= :timeNow AND e.state != :stateFinished");
            exchangeQuery.setParameter("timeNow", start);
            exchangeQuery.setParameter("stateFinished", Exchange.State.FINISHED);
            pendingExchanges.addAll(exchangeQuery.getResultList());
        }

        void performAssignments() {
            while (!pendingExchanges.isEmpty()) {
                Exchange exchange = pendingExchanges.remove();
                ensure(exchange.getNextInvokeTime().compareTo(start) <= 0, "Exchange is from the future");

                processExchangeRound(exchange);

                if (exchange.getState() != Exchange.State.FINISHED && exchange.getNextInvokeTime().compareTo(start) <= 0) {
                    pendingExchanges.add(exchange);
                }
            }
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
                    log.debug("Preparing data for the assigner for {}", exchange.getName());
                    List<Member> members = new ArrayList<>();
                    TypedQuery<Submission> submissions = submissionManager.createQuery("SELECT s FROM Submission s WHERE s.exchange = :exchange AND s.round = :round ", Submission.class);
                    submissions.setParameter("exchange", exchange);
                    submissions.setParameter("round", exchange.getRound());
                    TypedQuery<PlayedGame> playedGames = playedGameManager.createQuery("SELECT pg FROM PlayedGame pg WHERE pg.member = :member", PlayedGame.class);
                    for (Submission submission : submissions.getResultList()) {
                        playedGames.setParameter("member", submission.getMember());

                        List<URL> playedGameList = playedGames.getResultList()
                            .stream().map(PlayedGame::getLink)
                            .collect(Collectors.toList());
                        playedGameList.add(submission.getLink());

                        members.add(new Member(submission.getMember(), playedGameList));
                    }

                    log.debug("Running assigner for {}", exchange.getName());
                    Assigner assigner = new Assigner(submissions.getResultList(), members, exchange.getGamesPerMember(), exchange.getGamesPerMember());
                    Map<Snowflake, List<URL>> assignments = Assigner.solve(assigner);

                    message = md("Sending assignments for `{}`.", exchange.getName())
                        .line("If you have submitted a game, you should receive your assignments soon.")
                        .line("If this doesn't happen, please inform server administrators.");
                    channel.createMessage(message.toString()).block();

                    log.debug("Sending assignments to members for {}", exchange.getName());
                    for (Map.Entry<Snowflake, List<URL>> e : assignments.entrySet()) {
                        try {
                            sendAssignments(e.getKey(), e.getValue());
                        } catch (Throwable exception) {
                            log.warn("Couldn't send assignments to " + e.getKey().asString(), exception);
                        }
                    }

                    log.debug("Registering assigned games as played for {}", exchange.getName());
                    EntityTransaction submissionsTransaction = submissionManager.getTransaction();
                    submissionsTransaction.begin();
                    for (Map.Entry<Snowflake, List<URL>> e : assignments.entrySet()) {
                        for (URL game : e.getValue()) {
                            submissionManager.merge(new PlayedGame(e.getKey(), game, false));
                        }
                    }
                    submissionsTransaction.commit();

                    log.debug("Updating exchange state for {}", exchange.getName());
                    exchange.setRound(exchange.getRound() + 1);
                    if (exchange.getRound() >= exchange.getTotalRounds()) {
                        exchange.setState(Exchange.State.FINISHED);
                    } else {
                        exchange.setState(Exchange.State.BEFORE_SUBMISSIONS);
                    }
                    break;
            }

            EntityTransaction transaction = exchangeManager.getTransaction();
            transaction.begin();
            exchangeManager.merge(exchange);
            transaction.commit();
        }

        private void sendAssignments(Snowflake member, List<URL> games) {
            final Markdown message;

            if (games.isEmpty()) {
                message = md("Hi there! You've participated in a review exchange, but I couldn't assign you any games. " +
                    "Perhaps you've already played all of them, or my algorithms are not perfect. Sorry!");
            } else {
                message = md("Hi there! You've participated in a review exchange, so here are your assignments:");
                for (int i = 0; i < games.size(); i++) {
                    message.line("{}. {}", i + 1, games.get(i));
                }
                message.line("Feel free to play the games, rate and comment them as you see fit. Thanks for participating!");
                message.line("Note: if you decide to play some games on your own, you can register them using {} to avoid these being assigned to you. No need to register your assignments though - these are processed automatically.",
                    addPrefixTo(" played <link to the game>"));
            }

            client.getUserById(member).block().getPrivateChannel().block().createMessage(message.toString()).block();
        }

        void scheduleNextInvocation() {
            Query query = exchangeManager.createQuery("SELECT e FROM Exchange e WHERE e.state != :stateFinished AND e.nextInvokeTime >= :time ORDER BY e.nextInvokeTime ASC");
            query.setParameter("stateFinished", Exchange.State.FINISHED);
            query.setParameter("time", start);
            List<Exchange> exchanges = query.getResultList();

            if (exchanges.isEmpty()) {
                log.debug("No invocation scheduled");
                return;
            }

            Exchange firstExchange = exchanges.get(0);
            AssignmentScheduler.this.schedule(firstExchange.getNextInvokeTime().plus(SCHEDULE_OFFSET, ChronoUnit.SECONDS));
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
