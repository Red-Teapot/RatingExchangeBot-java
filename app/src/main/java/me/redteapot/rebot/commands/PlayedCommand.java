package me.redteapot.rebot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;
import me.redteapot.rebot.data.Database;
import me.redteapot.rebot.data.models.PlayedGame;
import me.redteapot.rebot.frontend.Command;
import me.redteapot.rebot.frontend.annotations.BotCommand;
import me.redteapot.rebot.frontend.annotations.OrderedArgument;
import me.redteapot.rebot.frontend.annotations.Permissions;
import me.redteapot.rebot.frontend.arguments.URLArg;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import java.net.URL;
import java.util.Optional;

import static me.redteapot.rebot.Checks.ensure;

@BotCommand(name = "played", permissions = Permissions.GENERAL, allowedInDM = true)
public class PlayedCommand extends Command {
    @OrderedArgument(type = URLArg.class, order = 0)
    public URL gameLink;

    @Override
    public void execute() {
        if (!context.getDispatcher().isGameLinkValid(gameLink)) {
            context.respond("Sorry, your link is invalid. Expected something like `{}`", context.getConfig().getGameLinkExample());
            return;
        }

        Optional<User> author = context.getMessage().getAuthor();
        ensure(author.isPresent(), "Author is not present");
        Snowflake member = author.get().getId();

        EntityManager playedGameManager = Database.getInstance().getEntityManager(PlayedGame.class);
        EntityTransaction transaction = playedGameManager.getTransaction();
        transaction.begin();
        try {
            playedGameManager.persist(new PlayedGame(
                member,
                gameLink
            ));
            transaction.commit();
            context.respond("Got it!");
        } catch (PersistenceException ignored) {
            transaction.rollback();
            context.respond("Already registered.");
        } catch (Throwable e) {
            transaction.rollback();
            throw e;
        } finally {
            playedGameManager.close();
        }
    }
}
