package me.redteapot.rebot.assigner;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

@PlanningEntity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Assignment {
    private Game game;
    @PlanningVariable(valueRangeProviderRefs = {"members"})
    private Member member;
}
