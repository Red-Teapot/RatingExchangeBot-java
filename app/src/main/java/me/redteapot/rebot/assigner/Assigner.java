package me.redteapot.rebot.assigner;

import discord4j.common.util.Snowflake;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.redteapot.rebot.data.models.Submission;
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

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static me.redteapot.rebot.Checks.ensure;

@PlanningSolution
@Data
@NoArgsConstructor
@Slf4j
public class Assigner {
    @ValueRangeProvider(id = "games")
    @ProblemFactCollectionProperty
    private List<Submission> submissions;

    @ValueRangeProvider(id = "members")
    @ProblemFactCollectionProperty
    private List<Member> members;

    @PlanningEntityCollectionProperty
    private List<Assignment> assignments = new ArrayList<>();

    @PlanningScore
    private HardSoftScore score;

    private int gamesPerMember;

    private int membersPerGame;

    private Member dummy;

    public Assigner(List<Submission> submissions, List<Member> members, int gamesPerMember, int membersPerGame) {
        this.submissions = submissions;
        this.members = members;
        this.gamesPerMember = gamesPerMember;
        this.membersPerGame = membersPerGame;

        this.dummy = new Member(null, new ArrayList<>());
        this.members.add(dummy);
    }

    public static Map<Snowflake, List<URL>> solve(Assigner assigner) {
        SolverFactory<Assigner> solverFactory = SolverFactory.create(
            new SolverConfig()
                .withSolutionClass(Assigner.class)
                .withEntityClasses(Assignment.class)
                .withScoreDirectorFactory(new ScoreDirectorFactoryConfig().withEasyScoreCalculatorClass(ScoreCalculator.class))
                .withTerminationConfig(new TerminationConfig()
                    .withSecondsSpentLimit(5 * 60L)
                    .withUnimprovedSecondsSpentLimit(30L))
        );

        Solver<Assigner> solver = solverFactory.buildSolver();
        for (Submission submission : assigner.getSubmissions()) {
            for (int i = 0; i < assigner.getMembersPerGame(); i++) {
                assigner.getAssignments().add(new Assignment(submission.getLink(), assigner.getDummy()));
            }
        }

        Assigner solvedAssigner = solver.solve(assigner);

        log.debug("Raw assignments: {}", solvedAssigner.getAssignments());

        Map<Snowflake, List<URL>> assignments = new HashMap<>();
        for (Submission submission : solvedAssigner.getSubmissions()) {
            Snowflake member = submission.getMember();
            ensure(!assignments.containsKey(member), "Duplicate member in assignments: {}", member);
            assignments.put(member, new ArrayList<>());
        }

        for (Assignment assignment : solvedAssigner.getAssignments()) {
            if (assignment.getMember().isDummy()) {
                continue;
            }
            Snowflake member = assignment.getMember().getId();

            ensure(assignments.containsKey(member), "No member in assignment list: {}", member);

            assignments.get(member).add(assignment.getGame());
        }

        log.debug("Assignments: {}", assignments);

        return assignments;
    }

    public static class ScoreCalculator implements EasyScoreCalculator<Assigner, HardSoftScore> {
        @Override
        public HardSoftScore calculateScore(Assigner assigner) {
            int gpm = assigner.getGamesPerMember();
            int mpg = assigner.getMembersPerGame();

            Map<URL, Integer> gameAssignmentNum = new HashMap<>();
            Map<Member, Integer> memberAssignmentNum = new HashMap<>();

            AtomicInteger hardScore = new AtomicInteger(0);
            AtomicInteger softScore = new AtomicInteger(0);

            for (int i = 0; i < assigner.getAssignments().size(); i++) {
                Assignment assignment = assigner.getAssignments().get(i);
                URL game = assignment.getGame();
                Member member = assignment.getMember();

                if (member != null && member.isDummy()) {
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
}
