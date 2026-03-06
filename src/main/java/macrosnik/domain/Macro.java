package macrosnik.domain;

import java.util.ArrayList;
import java.util.List;

public class Macro {
    public int version = 1;
    public String name = "Untitled";
    public List<Action> actions = new ArrayList<>();

    public Macro() { }

    public Macro(String name) {
        this.name = name;
    }
}
