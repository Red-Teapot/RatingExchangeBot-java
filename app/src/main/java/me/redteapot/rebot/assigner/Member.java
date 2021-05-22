package me.redteapot.rebot.assigner;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class Member {
    private String name;
    private List<Game> playedGames;
}
