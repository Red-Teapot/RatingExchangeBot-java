package me.redteapot.rebot;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Markdown {
    private final StringBuilder compiled = new StringBuilder();

    public Markdown(String text, Object... args) {
        line(text, args);
    }

    public Markdown line() {
        compiled.append('\n');
        return this;
    }

    public Markdown line(String text, Object... args) {
        compiled.append(Strings.format(text, args)).append('\n');
        return this;
    }

    public Markdown code(String contents, String language) {
        compiled.append("```").append(language).append('\n')
            .append(contents).append("\n```\n");
        return this;
    }

    public Markdown code(String contents) {
        return code(contents, "");
    }

    public Markdown concat(Markdown other) {
        compiled.append(other.compiled);
        return this;
    }

    @Override
    public String toString() {
        return compiled.toString();
    }

    public static Markdown md(String contents, Object... args) {
        return new Markdown(contents, args);
    }
}
