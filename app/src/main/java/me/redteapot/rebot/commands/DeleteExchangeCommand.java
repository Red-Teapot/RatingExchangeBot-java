package me.redteapot.rebot.commands;

import discord4j.common.util.Snowflake;
import me.redteapot.rebot.data.Database;
import me.redteapot.rebot.data.models.Exchange;
import me.redteapot.rebot.data.models.Submission;
import me.redteapot.rebot.frontend.Command;
import me.redteapot.rebot.frontend.annotations.BotCommand;
import me.redteapot.rebot.frontend.annotations.OrderedArgument;
import me.redteapot.rebot.frontend.annotations.Permissions;
import me.redteapot.rebot.frontend.arguments.Identifier;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

import static me.redteapot.rebot.Checks.ensure;
import static me.redteapot.rebot.Checks.require;

@BotCommand(name = "deleteExchange", permissions = Permissions.SERVER_ADMIN)
public class DeleteExchangeCommand extends Command {
    @OrderedArgument(type = Identifier.class, order = 0)
    public String name;

    @Override
    public void execute() {
        Optional<Snowflake> guild = context.getMessage().getGuildId();
        ensure(guild.isPresent(), "Current guild ID not present");

        EntityManager exchangeManager = Database.getInstance().getEntityManager(Exchange.class);
        // FIXME
        EntityManager submissionManager = Database.getInstance().getEntityManager(Submission.class);

        TypedQuery<Exchange> exchanges = exchangeManager.createQuery("SELECT e FROM Exchange e WHERE e.name = :name AND e.guild = :guild", Exchange.class);
        exchanges.setParameter("name", name);
        exchanges.setParameter("guild", guild.get());
        List<Exchange> exchangeList = exchanges.getResultList();

        if (exchangeList.isEmpty()) {
            context.respond("There is no exchange named `{}`.", name);
            return;
        }

        require(exchangeList.size() == 1, "Too many exchanges");

        Exchange exchange = exchangeList.get(0);

        EntityTransaction submissionTransaction = submissionManager.getTransaction();
        submissionTransaction.begin();
        try {
            Query submissionDeleteQuery = submissionManager.createQuery("DELETE FROM Submission s WHERE s.exchange = :exchange");
            submissionDeleteQuery.setParameter("exchange", exchange);
            submissionDeleteQuery.executeUpdate();
            submissionTransaction.commit();
        } catch (Throwable e) {
            submissionTransaction.rollback();
            throw e;
        }

        EntityTransaction exchangeTransaction = exchangeManager.getTransaction();
        exchangeTransaction.begin();
        try {
            exchangeManager.remove(exchange);
            exchangeTransaction.commit();
        } catch (Throwable e) {
            exchangeTransaction.rollback();
            throw e;
        }

        context.getScheduler().reschedule();
        context.respond("Exchange `{}` deleted!", name);
    }
}
