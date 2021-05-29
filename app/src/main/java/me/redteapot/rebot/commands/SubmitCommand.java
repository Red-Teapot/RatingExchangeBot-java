package me.redteapot.rebot.commands;

import discord4j.common.util.Snowflake;
import lombok.extern.slf4j.Slf4j;
import me.redteapot.rebot.data.Database;
import me.redteapot.rebot.data.models.Exchange;
import me.redteapot.rebot.data.models.Submission;
import me.redteapot.rebot.frontend.Command;
import me.redteapot.rebot.frontend.annotations.BotCommand;
import me.redteapot.rebot.frontend.annotations.OrderedArgument;
import me.redteapot.rebot.frontend.annotations.Permissions;
import me.redteapot.rebot.frontend.arguments.Identifier;
import me.redteapot.rebot.frontend.arguments.QuotedString;
import org.dizitart.no2.objects.ObjectRepository;

import java.time.ZonedDateTime;

import static me.redteapot.rebot.Markdown.md;
import static org.dizitart.no2.objects.filters.ObjectFilters.and;
import static org.dizitart.no2.objects.filters.ObjectFilters.eq;

/**
 * The command to submit a game to rating exchange.
 */
@Slf4j
@BotCommand(name = "submit", permissions = Permissions.GENERAL)
public class SubmitCommand extends Command {
    /**
     * The name of an exchange to submit the game to.
     */
    @OrderedArgument(type = Identifier.class, order = 0)
    public String exchangeName;

    /**
     * Link to your game.
     */
    @OrderedArgument(type = QuotedString.class, order = 1)
    public String gameLink;

    @Override
    public void execute() {
        ObjectRepository<Exchange> exchangeRepo = Database.getRepository(Exchange.class);
        ObjectRepository<Submission> submissionRepo = Database.getRepository(Submission.class);

        Exchange exchange = exchangeRepo.find(eq("name", exchangeName)).firstOrDefault();

        if (exchange == null) {
            context.respond(md("Exchange with name `{}` does not exist.", exchangeName));
            return;
        }

        if (!context.getChannel().getId().equals(exchange.getSubmissionChannel())) {
            context.respond(md("Wrong channel. Please post your submissions for `{}` in <#{}>",
                exchange.getName(),
                exchange.getSubmissionChannel().asString()));
            return;
        }

        if (!exchange.getState().equals(Exchange.State.ACCEPTING_SUBMISSIONS)) {
            context.respond(md("Exchange `{}` is not accepting submissions right now.", exchangeName));
            return;
        }

        int round = exchange.getRound();
        ZonedDateTime submissionTime = ZonedDateTime.now();
        Snowflake member = context.getMessage().getAuthor().get().getId();

        Submission submission = submissionRepo.find(and(
            eq("exchangeID", exchange.getId()),
            eq("round", round),
            eq("member", member)
        )).firstOrDefault();

        // TODO Validate game link
        gameLink = gameLink.strip();

        if (submission == null) {
            submission = new Submission(
                exchange.getId(),
                round,
                gameLink,
                member,
                submissionTime);
            submissionRepo.insert(submission);
            context.respond(md("Successfully submitted!"));
        } else {
            String oldLink = submission.getLink();

            submission.setLink(gameLink);
            submission.setSubmissionDatetime(submissionTime);
            submissionRepo.update(submission);

            context.respond(md("Updated your submission.")
                .line("Previous one was: {}", oldLink));
        }
    }
}
