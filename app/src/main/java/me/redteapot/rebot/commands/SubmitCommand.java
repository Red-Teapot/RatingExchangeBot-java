package me.redteapot.rebot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;
import lombok.extern.slf4j.Slf4j;
import me.redteapot.rebot.data.Database;
import me.redteapot.rebot.data.models.Exchange;
import me.redteapot.rebot.data.models.Submission;
import me.redteapot.rebot.frontend.Command;
import me.redteapot.rebot.frontend.annotations.BotCommand;
import me.redteapot.rebot.frontend.annotations.OrderedArgument;
import me.redteapot.rebot.frontend.annotations.Permissions;
import me.redteapot.rebot.frontend.arguments.Identifier;
import me.redteapot.rebot.frontend.arguments.URLArg;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static me.redteapot.rebot.Checks.ensure;
import static me.redteapot.rebot.Markdown.md;

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
    @OrderedArgument(type = URLArg.class, order = 1)
    public URL gameLink;

    @Override
    public void execute() {
        ensure(context.getMessage().getGuildId().isPresent(), "Guild id not present");

        EntityManager exchangeManager = Database.getInstance().getEntityManager(Exchange.class);
        EntityManager submissionManager = Database.getInstance().getEntityManager(Submission.class);

        TypedQuery<Exchange> exchangeQ = exchangeManager.createQuery("SELECT e FROM Exchange e WHERE e.name = :name AND e.guild = :guild", Exchange.class);
        exchangeQ.setParameter("name", exchangeName);
        exchangeQ.setParameter("guild", context.getMessage().getGuildId().get());
        List<Exchange> exchanges = exchangeQ.getResultList();

        ensure(exchanges.size() <= 1, "Too many exchanges: {}", exchanges);

        if (exchanges.isEmpty()) {
            context.respond("Exchange `{}` does not exist.", exchangeName);
            return;
        }

        Exchange exchange = exchanges.get(0);

        if (!exchange.getSubmissionChannel().equals(context.getChannel().getId())) {
            context.respond("Please post your submissions in <#{}>", exchange.getSubmissionChannel().asString());
            return;
        }

        if (!context.getDispatcher().isGameLinkValid(gameLink)) {
            context.respond("Sorry, your link is invalid. Expected something like `{}`", context.getConfig().getGameLinkExample());
            return;
        }

        if (exchange.getState() != Exchange.State.ACCEPTING_SUBMISSIONS) {
            context.respond("Given exchange is not accepting submissions right now.");
            return;
        }

        Optional<User> author = context.getMessage().getAuthor();
        ensure(author.isPresent(), "Author is not present");
        Snowflake member = author.get().getId();
        ensure(member != null, "Member is null");

        EntityTransaction submissionTransaction = submissionManager.getTransaction();
        submissionTransaction.begin();
        TypedQuery<Submission> submissionQ = submissionManager.createQuery("SELECT s FROM Submission s WHERE s.exchange = :exchange AND s.round = :round AND s.member = :member", Submission.class);
        submissionQ.setParameter("exchange", exchange);
        submissionQ.setParameter("round", exchange.getRound());
        submissionQ.setParameter("member", member);
        List<Submission> submissions = submissionQ.getResultList();
        ensure(submissions.size() <= 1, "Too many submissions for {}", member);

        final Submission submission;
        try {
            if (submissions.isEmpty()) {
                submission = new Submission(exchange, exchange.getRound(), gameLink, member, ZonedDateTime.now());
                submissionManager.persist(submission);
                context.respond(md("Got it!"));
            } else {
                submission = submissions.get(0);
                context.respond(md("Updated your previous submission, which was <{}>", submission.getLink()));
                submission.setLink(gameLink);
                submissionManager.merge(submission);
            }
            submissionTransaction.commit();
        } catch (PersistenceException e) {
            submissionTransaction.rollback();
            context.respond(md("This game is already submitted: <{}>", gameLink));
            log.warn("Exception while executing submit command", e);
        } catch (Throwable e) {
            submissionTransaction.rollback();
            throw e;
        } finally {
            exchangeManager.close();
            submissionManager.close();
        }
    }
}
