package me.redteapot.rebot.commands;

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
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.net.URL;
import java.util.List;

import static me.redteapot.rebot.Checks.ensure;

@BotCommand(name = "deleteSubmission", permissions = Permissions.SERVER_ADMIN)
public class DeleteSubmissionCommand extends Command {

    /**
     * The name of the exchange.
     */
    @OrderedArgument(type = Identifier.class, order = 0)
    public String exchangeName;

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

        EntityTransaction submissionTransaction = submissionManager.getTransaction();
        submissionTransaction.begin();
        try {
            Query submissionDeleteQuery = submissionManager.createQuery("DELETE FROM Submission s WHERE s.link = :link AND s.exchange = :exchange AND s.round = :round");
            submissionDeleteQuery.setParameter("link", gameLink);
            submissionDeleteQuery.setParameter("exchange", exchange);
            submissionDeleteQuery.setParameter("round", exchange.getRound());

            int rowsAffected = submissionDeleteQuery.executeUpdate();
            ensure(rowsAffected <= 1, "Too many submissions deleted for {}", exchange.getName());
            submissionTransaction.commit();

            if (rowsAffected == 0) {
                context.respond("Could not find the submission `{}` to exchange `{}`.", gameLink, exchangeName);
            } else {
                context.respond("Deleted the submission `{}` from exchange `{}`.", gameLink, exchangeName);
            }
        } catch (Throwable e) {
            submissionTransaction.rollback();
            throw e;
        }
    }
}
