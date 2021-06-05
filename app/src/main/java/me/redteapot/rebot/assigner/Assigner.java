package me.redteapot.rebot.assigner;

import lombok.Data;
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.calculator.EasyScoreCalculator;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.score.director.ScoreDirectorFactoryConfig;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.termination.TerminationConfig;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@PlanningSolution
@Data
public class Assigner {
    @ValueRangeProvider(id = "games")
    @ProblemFactCollectionProperty
    private List<Game> games = new ArrayList<>();

    @ValueRangeProvider(id = "members")
    @ProblemFactCollectionProperty
    private List<Member> members = new ArrayList<>();

    @PlanningEntityCollectionProperty
    private List<Assignment> assignments = new ArrayList<>();

    @PlanningScore
    private HardSoftScore score;

    private int gamesPerMember;

    private int membersPerGame;

    public static class ScoreCalculator implements EasyScoreCalculator<Assigner, HardSoftScore> {
        @Override
        public HardSoftScore calculateScore(Assigner assigner) {
            int gpm = assigner.getGamesPerMember();
            int mpg = assigner.getMembersPerGame();

            Map<Game, Integer> gameAssignmentNum = new HashMap<>();
            Map<Member, Integer> memberAssignmentNum = new HashMap<>();

            AtomicInteger hardScore = new AtomicInteger(0);
            AtomicInteger softScore = new AtomicInteger(0);

            for (int i = 0; i < assigner.getAssignments().size(); i++) {
                Assignment assignment = assigner.getAssignments().get(i);
                Game game = assignment.getGame();
                Member member = assignment.getMember();

                if (member != null && member.getName().equals("DummyMem")) {
                    softScore.addAndGet(-10);
                    continue;
                }

                if (!gameAssignmentNum.containsKey(game)) {
                    gameAssignmentNum.put(game, 1);
                } else {
                    gameAssignmentNum.put(game, gameAssignmentNum.get(game) + 1);
                }

                if (!memberAssignmentNum.containsKey(member)) {
                    memberAssignmentNum.put(member, 1);
                } else {
                    memberAssignmentNum.put(member, memberAssignmentNum.get(member) + 1);
                }

                if (member != null && member.getPlayedGames().contains(game)) {
                    hardScore.addAndGet(-100);
                }

                int other = assigner.getAssignments().indexOf(assignment);
                if (other >= 0 && other != i) {
                    hardScore.addAndGet(-100);
                }
            }

            gameAssignmentNum.forEach((key, value) -> {
                if (value > mpg) {
                    hardScore.addAndGet(-10);
                } else {
                    softScore.addAndGet(value - mpg);
                }
            });
            memberAssignmentNum.forEach((key, value) -> {
                if (value > gpm) {
                    hardScore.addAndGet(-10);
                } else {
                    softScore.addAndGet(value - gpm);
                }
            });

            return HardSoftScore.of(hardScore.get(), softScore.get());
        }
    }

    public static void main(String[] args) {
        SolverFactory<Assigner> solverFactory = SolverFactory.create(
            new SolverConfig()
                .withSolutionClass(Assigner.class)
                .withEntityClasses(Assignment.class)
                .withScoreDirectorFactory(new ScoreDirectorFactoryConfig().withEasyScoreCalculatorClass(ScoreCalculator.class))
                .withTerminationConfig(new TerminationConfig()
                    .withSecondsSpentLimit(30L))
        );
        Solver<Assigner> solver = solverFactory.buildSolver();

        Assigner assigner = new Assigner();
        assigner.setGamesPerMember(5);
        assigner.setMembersPerGame(5);

        Game g1 = new Game("Game 1");
        Game g2 = new Game("Game 2");
        Game g3 = new Game("Game 3");
        Game g4 = new Game("Game 4");
        Game g5 = new Game("Game 5");
        Game g6 = new Game("Game 6");

        assigner.getGames().add(g1);
        assigner.getGames().add(g2);
        assigner.getGames().add(g3);
        assigner.getGames().add(g4);
        assigner.getGames().add(g5);
        assigner.getGames().add(g6);

        assigner.getMembers().add(new Member("Member 1", Arrays.asList(g1, g2)));
        assigner.getMembers().add(new Member("Member 2", Arrays.asList(g2)));
        assigner.getMembers().add(new Member("Member 3", Arrays.asList(g3)));
        assigner.getMembers().add(new Member("Member 4", Arrays.asList(g1, g4, g5)));
        assigner.getMembers().add(new Member("Member 5", Arrays.asList(g2, g5)));
        assigner.getMembers().add(new Member("Member 6", Arrays.asList(g6)));
        assigner.getMembers().add(new Member("DummyMem", Collections.emptyList()));

        for (Game game : assigner.getGames()) {
            for (int i = 0; i < assigner.getMembersPerGame(); i++) {
                assigner.getAssignments().add(new Assignment(game, null));
            }
        }

        Assigner solvedAssigner = solver.solve(assigner);

        System.out.print("         ");
        assigner.getGames().forEach(game -> System.out.print(game.getName() + " "));
        System.out.println();

        solvedAssigner.getMembers().forEach(member -> {
            System.out.print(member.getName() + " ");
            solvedAssigner.getGames().forEach(game -> {
                Assignment assignment = solvedAssigner.getAssignments()
                    .stream()
                    .filter(as -> as != null && as.getGame() != null && as.getMember() != null)
                    .filter(as -> as.getGame().equals(game) && as.getMember().equals(member))
                    .findAny().orElse(null);
                if (assignment != null) {
                    if (member.getPlayedGames().contains(game)) {
                        System.out.print("   P   ");
                    } else {
                        System.out.print("   1   ");
                    }
                } else {
                    System.out.print("       ");
                }
            });
            System.out.println();
        });
    }
}
