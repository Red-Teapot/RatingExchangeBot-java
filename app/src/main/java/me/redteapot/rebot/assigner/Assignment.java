package me.redteapot.rebot.assigner;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

import java.net.URL;

@PlanningEntity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Assignment {
    private URL game;
    @PlanningVariable(valueRangeProviderRefs = {"members"})
    private Member member;
}
