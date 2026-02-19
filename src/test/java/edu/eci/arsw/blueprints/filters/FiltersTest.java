package edu.eci.arsw.blueprints.filters;

import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FiltersTest {

    @Test
    void testIdentityFilter() {
        BlueprintsFilter filter = new IdentityFilter();
        Blueprint bp = new Blueprint("john", "house", 
            List.of(new Point(0, 0), new Point(1, 1), new Point(2, 2)));

        Blueprint filtered = filter.apply(bp);

        assertEquals(3, filtered.getPoints().size());
        assertEquals(bp.getPoints(), filtered.getPoints());
    }

    @Test
    void testRedundancyFilter() {
        BlueprintsFilter filter = new RedundancyFilter();
        Blueprint bp = new Blueprint("john", "house", 
            List.of(
                new Point(0, 0),
                new Point(0, 0),  
                new Point(1, 1),
                new Point(1, 1),  
                new Point(2, 2)
            ));

        Blueprint filtered = filter.apply(bp);

        assertEquals(3, filtered.getPoints().size());
        assertEquals(new Point(0, 0), filtered.getPoints().get(0));
        assertEquals(new Point(1, 1), filtered.getPoints().get(1));
        assertEquals(new Point(2, 2), filtered.getPoints().get(2));
    }

    @Test
    void testRedundancyFilterNoConsecutiveDuplicates() {
        BlueprintsFilter filter = new RedundancyFilter();
        Blueprint bp = new Blueprint("john", "house", 
            List.of(new Point(0, 0), new Point(1, 1), new Point(0, 0)));

        Blueprint filtered = filter.apply(bp);

        assertEquals(3, filtered.getPoints().size()); 
    }

    @Test
    void testUndersamplingFilter() {
        BlueprintsFilter filter = new UndersamplingFilter();
        Blueprint bp = new Blueprint("john", "house", 
            List.of(
                new Point(0, 0),
                new Point(1, 1),
                new Point(2, 2),
                new Point(3, 3),
                new Point(4, 4)
            ));

        Blueprint filtered = filter.apply(bp);

        assertEquals(3, filtered.getPoints().size()); 
        assertEquals(new Point(0, 0), filtered.getPoints().get(0));
        assertEquals(new Point(2, 2), filtered.getPoints().get(1));
        assertEquals(new Point(4, 4), filtered.getPoints().get(2));
    }

    @Test
    void testUndersamplingFilterSmallBlueprint() {
        BlueprintsFilter filter = new UndersamplingFilter();
        Blueprint bp = new Blueprint("john", "house", 
            List.of(new Point(0, 0), new Point(1, 1)));

        Blueprint filtered = filter.apply(bp);

        assertEquals(2, filtered.getPoints().size()); 
    }
}
