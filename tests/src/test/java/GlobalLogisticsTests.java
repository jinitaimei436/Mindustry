import mindustry.game.strategy.core.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class GlobalLogisticsTests{

    @Test
    public void testMultiHopCrossSectorProductionChain(){
        StrategySimulationEngine engine = new StrategySimulationEngine();

        // Sector 1: mines titanium (10/s)
        SectorStrategyProfile s1 = new SectorStrategyProfile(1);
        s1.hasBase = true;
        s1.storageCapacity = 4000;
        s1.nodes.add(new ProductionNode("laser-drill").addOutput("titanium", 10f));
        s1.exportCapacities.put("*", 10f);

        // Sector 2: receives titanium (consumes 4/s) -> produces plastanium (2/s)
        SectorStrategyProfile s2 = new SectorStrategyProfile(2);
        s2.hasBase = true;
        s2.storageCapacity = 4000;
        s2.nodes.add(new ProductionNode("plastanium-compressor")
            .addInput("titanium", 4f)
            .addOutput("plastanium", 2f)
            .withPower(0f, 120f));
        s2.nodes.add(new ProductionNode("solar-panel").withPower(200f, 0f));
        s2.importDemands.put("titanium", 4f);
        s2.exportCapacities.put("*", 10f);

        // Sector 3: front-line consumer receiving plastanium
        SectorStrategyProfile s3 = new SectorStrategyProfile(3);
        s3.hasBase = true;
        s3.storageCapacity = 4000;
        s3.importDemands.put("plastanium", 5f);

        engine.putSector(s1);
        engine.putSector(s2);
        engine.putSector(s3);
        engine.addRoute(new LogisticsRoute(1, 2, "titanium", 10f));
        engine.addRoute(new LogisticsRoute(2, 3, "plastanium", 10f));

        engine.step(10f);

        assertEquals(6f, s1.getNetRate("titanium"), 0.1f);
        assertEquals(2f, s2.getExportRate("plastanium"), 0.1f);
        assertEquals(20f, s3.getStored("plastanium"), 0.5f);
    }

    @Test
    public void testPriorityAndProportionalAllocation(){
        StrategySimulationEngine engine = new StrategySimulationEngine();

        SectorStrategyProfile src = new SectorStrategyProfile(1);
        src.hasBase = true;
        src.nodes.add(new ProductionNode("drill").addOutput("titanium", 10f));
        src.exportCapacities.put("*", 20f);

        SectorStrategyProfile high = new SectorStrategyProfile(2);
        high.hasBase = true;
        high.importDemands.put("titanium", 6f);

        SectorStrategyProfile lowA = new SectorStrategyProfile(3);
        lowA.hasBase = true;
        lowA.importDemands.put("titanium", 4f);

        SectorStrategyProfile lowB = new SectorStrategyProfile(4);
        lowB.hasBase = true;
        lowB.importDemands.put("titanium", 4f);

        engine.putSector(src);
        engine.putSector(high);
        engine.putSector(lowA);
        engine.putSector(lowB);

        LogisticsRoute rHigh = new LogisticsRoute(1, 2, "titanium", 10f);
        rHigh.priority = 10;
        LogisticsRoute rLowA = new LogisticsRoute(1, 3, "titanium", 10f);
        rLowA.priority = 0;
        LogisticsRoute rLowB = new LogisticsRoute(1, 4, "titanium", 10f);
        rLowB.priority = 0;

        engine.addRoute(rHigh);
        engine.addRoute(rLowA);
        engine.addRoute(rLowB);

        engine.step(1f);

        assertEquals(6f, rHigh.lastFlowPerSec, 0.05f);
        assertEquals(2f, rLowA.lastFlowPerSec, 0.05f);
        assertEquals(2f, rLowB.lastFlowPerSec, 0.05f);
    }

    @Test
    public void testStorageFullBackpressureAllowsExports(){
        StrategySimulationEngine engine = new StrategySimulationEngine();

        SectorStrategyProfile src = new SectorStrategyProfile(1);
        src.hasBase = true;
        src.storageCapacity = 1000;
        src.setStored("copper", 1000f); // Already full
        src.nodes.add(new ProductionNode("mechanical-drill").addOutput("copper", 8f));
        src.exportCapacities.put("*", 10f);

        engine.putSector(src);

        // Case A: Full storage and NO outgoing route -> drill throttles down to 0
        engine.step(1f);
        assertEquals(0f, src.solvedGrossProduction.getOrDefault("copper", 0f), 0.05f);

        // Case B: Add downstream demand of 5/s -> drill resumes to satisfy export
        SectorStrategyProfile dst = new SectorStrategyProfile(2);
        dst.hasBase = true;
        dst.importDemands.put("copper", 5f);
        engine.putSector(dst);
        engine.addRoute(new LogisticsRoute(1, 2, "copper", 5f));

        engine.step(2f);
        assertTrue(dst.getStored("copper") >= 9.5f);
        assertTrue(src.getStored("copper") >= 995f);
    }

    @Test
    public void testTransitLatencyQueue(){
        StrategySimulationEngine engine = new StrategySimulationEngine();

        SectorStrategyProfile src = new SectorStrategyProfile(1);
        src.hasBase = true;
        src.nodes.add(new ProductionNode("drill").addOutput("lead", 4f));
        src.exportCapacities.put("*", 10f);

        SectorStrategyProfile dst = new SectorStrategyProfile(2);
        dst.hasBase = true;
        dst.importDemands.put("lead", 4f);

        LogisticsRoute delayed = new LogisticsRoute(1, 2, "lead", 4f);
        delayed.transitSeconds = 5f;

        engine.putSector(src);
        engine.putSector(dst);
        engine.addRoute(delayed);

        // After 3 seconds, nothing has arrived yet
        engine.step(3f);
        assertEquals(0f, dst.getStored("lead"), 0.01f);

        // After 4 more seconds (total 7s), earlier shipments have arrived
        engine.step(4f);
        assertTrue(dst.getStored("lead") >= 7.5f);
    }

    @Test
    public void testStrategicAIAndTroopDispatchHook(){
        StrategySimulationEngine engine = new StrategySimulationEngine();

        SectorStrategyProfile enemy = new SectorStrategyProfile(10);
        enemy.enemyBase = true;
        enemy.adjacentSectorIds.add(11);

        SectorStrategyProfile friendly = new SectorStrategyProfile(11);
        friendly.hasBase = true;
        friendly.adjacentSectorIds.add(10);

        engine.putSector(enemy);
        engine.putSector(friendly);

        final boolean[] fired = {false};
        engine.setAIController((elapsed, dt, sectors, out) -> {
            if(!fired[0] && elapsed >= 1.0){
                fired[0] = true;
                out.add(new TroopDispatchOrder(10, 11, 2, true, 3f).addUnits("dagger", 12));
            }
        });

        engine.step(1f);
        assertEquals(1, engine.activeDispatches.size());
        assertEquals(0, engine.arrivedDispatches.size());

        engine.step(4f);
        assertEquals(0, engine.activeDispatches.size());
        assertEquals(1, engine.arrivedDispatches.size());
        assertEquals(12, engine.arrivedDispatches.get(0).totalUnits());
    }
}
