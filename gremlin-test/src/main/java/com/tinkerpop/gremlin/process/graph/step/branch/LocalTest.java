package com.tinkerpop.gremlin.process.graph.step.branch;

import com.tinkerpop.gremlin.LoadGraphWith;
import com.tinkerpop.gremlin.process.AbstractGremlinProcessTest;
import com.tinkerpop.gremlin.process.T;
import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.structure.Order;
import com.tinkerpop.gremlin.structure.Vertex;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static com.tinkerpop.gremlin.LoadGraphWith.GraphData.CREW;
import static com.tinkerpop.gremlin.LoadGraphWith.GraphData.MODERN;
import static com.tinkerpop.gremlin.process.graph.AnonymousGraphTraversal.Tokens.__;
import static org.junit.Assert.*;


/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public abstract class LocalTest extends AbstractGremlinProcessTest {

    public abstract Traversal<Vertex, String> get_g_V_localXpropertiesXlocationX_order_byXvalueX_limitX2XX_value();

    public abstract Traversal<Vertex, Map<String, Object>> get_g_V_hasXlabel_personX_asXaX_localXoutXcreatedX_asXbXX_selectXa_bX_byXnameX_byXidX();

    public abstract Traversal<Vertex, Long> get_g_V_localXoutE_countX();

    @Test
    @LoadGraphWith(CREW)
    public void g_V_localXpropertiesXlocationX_orderByXvalueX_limitX2XX_value() {
        final Traversal<Vertex, String> traversal = get_g_V_localXpropertiesXlocationX_order_byXvalueX_limitX2XX_value();
        printTraversalForm(traversal);
        checkResults(Arrays.asList("brussels", "san diego", "centreville", "dulles", "baltimore", "bremen", "aachen", "kaiserslautern"), traversal);

    }

    @Test
    @LoadGraphWith(MODERN)
    public void g_V_hasXlabel_personX_asXaX_localXoutXcreatedX_asXbXX_selectXa_bX_byXnameX_by() {
        final Traversal<Vertex, Map<String, Object>> traversal = get_g_V_hasXlabel_personX_asXaX_localXoutXcreatedX_asXbXX_selectXa_bX_byXnameX_byXidX();
        printTraversalForm(traversal);
        int counter = 0;
        while (traversal.hasNext()) {
            final Map<String, Object> map = traversal.next();
            counter++;
            assertEquals(2, map.size());
            if (map.get("a").equals("marko")) {
                assertEquals(convertToVertexId("lop"), map.get("b"));
            } else if (map.get("a").equals("josh")) {
                assertTrue(convertToVertexId("lop").equals(map.get("b")) || convertToVertexId("ripple").equals(map.get("b")));
            } else if (map.get("a").equals("peter")) {
                assertEquals(convertToVertexId("lop"), map.get("b"));
            } else {
                fail("The following map should not have been returned: " + map);
            }
        }
        assertEquals(4, counter);
    }

    @Test
    @LoadGraphWith(MODERN)
    public void g_V_localXoutE_countX() {
        final Traversal<Vertex, Long> traversal = get_g_V_localXoutE_countX();
        printTraversalForm(traversal);
        checkResults(Arrays.asList(3l, 0l, 0l, 0l, 1l, 2l), traversal);

    }

    /*@Test
    @LoadGraphWith(MODERN)
    public void g_V_localXoutE_weight_groupCountX() {
        final Traversal<Vertex, Map<Double, Long>> traversal = get_g_V_localXoutE_weight_groupCountX();
        int counter = 0;
        int zeroCounter = 0;
        while (traversal.hasNext()) {
            counter++;
            final Map<Double, Long> map = traversal.next();
            if(0 == map.size()) zeroCounter++;
            System.out.println(map);
        }
        assertEquals(6, counter);
        assertEquals(3, zeroCounter);
    }*/


    public static class StandardTest extends LocalTest {
        public StandardTest() {
            requiresGraphComputer = false;
        }

        @Override
        public Traversal<Vertex, String> get_g_V_localXpropertiesXlocationX_order_byXvalueX_limitX2XX_value() {
            return g.V().local(__.properties("location").order().by(T.value, Order.incr).range(0, 2)).value();
        }

        @Override
        public Traversal<Vertex, Map<String, Object>> get_g_V_hasXlabel_personX_asXaX_localXoutXcreatedX_asXbXX_selectXa_bX_byXnameX_byXidX() {
            return g.V().has(T.label, "person").as("a").local(__.out("created").as("b")).select("a", "b").by("name").by(T.id);
        }

        @Override
        public Traversal<Vertex, Long> get_g_V_localXoutE_countX() {
            return g.V().local(__.outE().count());
        }

        /*@Override
        public Traversal<Vertex, Map<Double, Long>> get_g_V_localXoutE_weight_groupCountX() {
            return g.V().local((Traversal) __.outE().values("weight").groupCount());
        }*/

    }

    public static class ComputerTest extends LocalTest {
        public ComputerTest() {
            requiresGraphComputer = true;
        }

        @Override
        public Traversal<Vertex, String> get_g_V_localXpropertiesXlocationX_order_byXvalueX_limitX2XX_value() {
            return g.V().local(__.properties("location").order().by(T.value, Order.incr).range(0, 2)).<String>value().submit(g.compute());
        }

        @Override
        public Traversal<Vertex, Map<String, Object>> get_g_V_hasXlabel_personX_asXaX_localXoutXcreatedX_asXbXX_selectXa_bX_byXnameX_byXidX() {
            return g.V().has(T.label, "person").as("a").local(__.out("created").as("b")).select("a", "b").by("name").by(T.id).submit(g.compute());
        }

        @Override
        public Traversal<Vertex, Long> get_g_V_localXoutE_countX() {
            return g.V().local(__.outE().count()).submit(g.compute());
        }

        /*@Override
        public Traversal<Vertex, Map<Double, Long>> get_g_V_localXoutE_weight_groupCountX() {
            return g.V().local((Traversal) __.outE().values("weight").groupCount()).submit(g.compute());
        }*/

    }
}