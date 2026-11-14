package com.matt.quantumfalcon.skill;

import java.util.ArrayList;
import java.util.List;

/**
 * Node placement and connector routing for the skill tree screen, kept in common code so a server qftest can check it
 * (NS1: a connector must not pass through any node other than its two ends). Connectors are elbows: down from the
 * parent's bottom edge to the middle of the gap above the child's row, across, then down into the child's top edge.
 */
public record SkillTreeLayout(int treeLeft, int treeTop, int colW, int rowH, int node) {

    public int x(SkillTreeSystem.Skill s) { return treeLeft + s.gridX * colW + (colW - node) / 2; }
    public int y(SkillTreeSystem.Skill s) { return treeTop + s.gridY * rowH; }

    /** Axis-aligned segments {x1, y1, x2, y2} from parent to child. */
    public List<int[]> connector(SkillTreeSystem.Skill parent, SkillTreeSystem.Skill child) {
        int px = x(parent) + node / 2, py = y(parent) + node;
        int cx = x(child) + node / 2, cy = y(child);
        int midY = cy - (cy - py) / 2;
        List<int[]> out = new ArrayList<>();
        out.add(new int[]{px, py, px, midY});
        out.add(new int[]{Math.min(px, cx), midY, Math.max(px, cx), midY});
        out.add(new int[]{cx, midY, cx, cy});
        return out;
    }

    /** True if the segment touches the inside of the node's box. */
    public boolean crosses(int[] seg, SkillTreeSystem.Skill s) {
        int x0 = x(s), y0 = y(s), x1 = x0 + node, y1 = y0 + node;
        int sx0 = Math.min(seg[0], seg[2]), sx1 = Math.max(seg[0], seg[2]), sy0 = Math.min(seg[1], seg[3]), sy1 = Math.max(seg[1], seg[3]);
        return sx1 > x0 && sx0 < x1 && sy1 > y0 && sy0 < y1;
    }
}
