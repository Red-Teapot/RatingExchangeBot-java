package me.redteapot.rebot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;
import lombok.extern.slf4j.Slf4j;
import me.redteapot.rebot.data.Database;
import me.redteapot.rebot.data.models.PlayedGame;
import me.redteapot.rebot.frontend.Command;
import me.redteapot.rebot.frontend.annotations.BotCommand;
import me.redteapot.rebot.frontend.annotations.Permissions;
import me.redteapot.rebot.frontend.annotations.VariadicArgument;
import me.redteapot.rebot.frontend.arguments.URLArg;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import static me.redteapot.rebot.Checks.ensure;

@Slf4j
@BotCommand(name = "played", permissions = Permissions.GENERAL, allowedInDM = true)
public class PlayedCommand extends Command {
    @VariadicArgument(type = URLArg.class)
    public List<URL> gameLinks;

    @Override
    public void execute() {
        if (gameLinks.isEmpty()) {
            context.respond("No game links provided!");
            return;
        }

        for (URL gameLink : gameLinks) {
            if (!context.getDispatcher().isGameLinkValid(gameLink)) {
                context.respond("Sorry, your link is invalid. Expected something like `{}`", context.getConfig().getGameLinkExample());
                return;
            }
        }

        Optional<User> author = context.getMessage().getAuthor();
        ensure(author.isPresent(), "Author is not present");
        Snowflake member = author.get().getId();

        EntityManager playedGameManager = Database.getInstance().getEntityManager(PlayedGame.class);
        EntityTransaction transaction = playedGameManager.getTransaction();
        transaction.begin();
        try {
            gameLinks.stream()
                .unordered()
                .distinct()
                .forEach(gameLink -> {
                    PlayedGame playedGame = new PlayedGame(member, gameLink, true);

                    // FIXME Don't do this because this is slow
                    Query findExistingQuery = playedGameManager.createQuery("SELECT g FROM PlayedGame g WHERE g.member = :member AND g.link = :link");
                    findExistingQuery.setParameter("member", playedGame.getMember());
                    findExistingQuery.setParameter("link", playedGame.getLink());

                    if (findExistingQuery.getResultList().isEmpty()) {
                        playedGameManager.persist(playedGame);
                    }
                });

            transaction.commit();
            context.respond("Got it!");
        } catch (Throwable e) {
            transaction.rollback();
            throw e;
        } finally {
            playedGameManager.close();
        }
    }
}
