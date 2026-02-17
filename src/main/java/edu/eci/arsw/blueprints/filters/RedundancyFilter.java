package edu.eci.arsw.blueprints.filters;

import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Removes consecutive duplicate points (x,y) to reduce redundancy.
 * Profile: "redundancy"
 */
@Component
@Profile("redundancy")
public class RedundancyFilter implements BlueprintsFilter {
    @Override
    public Blueprint apply(Blueprint bp) {
        List<Point> in = bp.getPoints();
        if (in.isEmpty()) return bp;
        List<Point> out = new ArrayList<>();
        Point prev = null;
        for (Point p : in) {
            if (prev == null || !(prev.x()==p.x() && prev.y()==p.y())) {
                out.add(p);
                prev = p;
            }
        }
        return new Blueprint(bp.getAuthor(), bp.getName(), out);
    }
}
