package me.redteapot.rebot.commands;

import discord4j.common.util.Snowflake;
import lombok.extern.slf4j.Slf4j;
import me.redteapot.rebot.data.Database;
import me.redteapot.rebot.data.models.Exchange;
import me.redteapot.rebot.data.models.ExchangeRound;
import me.redteapot.rebot.frontend.Command;
import me.redteapot.rebot.frontend.annotations.BotCommand;
import me.redteapot.rebot.frontend.annotations.NamedArgument;
import me.redteapot.rebot.frontend.annotations.Permissions;
import me.redteapot.rebot.frontend.arguments.*;
import org.dizitart.no2.objects.ObjectRepository;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * The command to create rating exchanges.
 */
@Slf4j
@BotCommand(name = "createExchange", permissions = Permissions.SERVER_ADMIN)
public class CreateExchangeCommand extends Command {
    /**
     * The name of given exchange. Must be an identifier (i.e. contain only
     * ASCII alphanumeric characters and underscores).
     */
    @NamedArgument(type = Identifier.class, name = "name")
    public String name;

    /**
     * The channel to accept submissions in.
     */
    @NamedArgument(type = ChannelMention.class, name = "submissionChannel")
    public Snowflake submissionChannel;

    /**
     * The number of exchange rounds (i.e. how many times the bot will
     * collect submissions and send assignments).
     */
    @NamedArgument(type = SimpleInteger.class, name = "rounds", optional = true)
    public int rounds = 5;

    /**
     * The number of games to be assigned to each member.
     */
    @NamedArgument(type = SimpleInteger.class, name = "gamesPerMember", optional = true)
    public int gamesPerMember = 5;

    /**
     * The start of the first round.
     */
    @NamedArgument(type = ZonedDateTimeArg.class, name = "start", optional = true)
    public ZonedDateTime start = ZonedDateTime.now(ZoneId.of("UTC"));

    /**
     * Duration of the period when the bot is accepting submissions.
     */
    @NamedArgument(type = DurationArg.class, name = "submissionDuration", optional = true)
    public Duration submissionDuration = Duration.ofHours(24);

    /**
     * Duration of the period after stopping accepting submissions and before
     * sending assignments.
     */
    @NamedArgument(type = DurationArg.class, name = "graceDuration", optional = true)
    public Duration graceDuration = Duration.ofHours(0);

    @Override
    public void execute() {
        ObjectRepository<Exchange> exchangeRepo = Database.getRepository(Exchange.class);
        ObjectRepository<ExchangeRound> roundRepo = Database.getRepository(ExchangeRound.class);

        context.respond("Start date: {}", start);

        Exchange exchange = new Exchange(context.getMessage().getGuildId().get(), name, submissionChannel);
        exchangeRepo.insert(exchange);

        Duration totalRoundDuration = submissionDuration.plus(graceDuration);

        for (int i = 0; i < rounds; i++) {
            ZonedDateTime roundStart = start.plus(totalRoundDuration.multipliedBy(i));
            ExchangeRound round = new ExchangeRound(
                exchange.getId(),
                gamesPerMember,
                roundStart,
                submissionDuration,
                graceDuration,
                ExchangeRound.State.BEFORE_SUBMISSIONS);
            roundRepo.insert(round);
        }

        context.respond("Exchange with name {} created.", exchange.getName());
    }
}
