package solution.experiments;

import org.pi4.locutil.GeoPosition;
import org.pi4.locutil.MACAddress;
import org.pi4.locutil.io.TraceGenerator;
import solution.offline.FingerprintingStrategy;
import solution.offline.RadioMap;
import solution.utilities.Helpers;
import solution.Constants;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;


/**
 * Created by Nikolaj on 16-09-2015.
 */
public class DistanceToSignalStrengthExperiment implements ExperimentStrategy
{
    private FingerprintingStrategy fingerprintingStrategy;
    private Map<MACAddress, GeoPosition> accessPointPositions;

    public DistanceToSignalStrengthExperiment(FingerprintingStrategy fingerprintingStrategy, Map<MACAddress, GeoPosition> accessPointPositions) {
        this.fingerprintingStrategy = fingerprintingStrategy;
        this.accessPointPositions = accessPointPositions;
    }

    @Override
    public List<DoublePair> runExperiment() throws IOException {
        TraceGenerator traceGenerator = Helpers.loadTraces(Constants.OFFLINE_TRACES, Constants.ONLINE_TRACES, Constants.OFFLINE_SIZE, Constants.ONLINE_SIZE);
        RadioMap radioMap = Helpers.train(traceGenerator, fingerprintingStrategy);
        List<DoublePair> resultList = new ArrayList<>();

        for (GeoPosition position : radioMap.keySet()) {
            for (MACAddress ap : radioMap.get(position).keySet()) {
                GeoPosition apPosition = accessPointPositions.get(ap);
                if (apPosition == null) {
                    continue;
                }
                Double distance = position.distance(apPosition);
                Double signalStrength = radioMap.get(position).get(ap);
                resultList.add(new DoublePair(distance, signalStrength));
            }
        }

        return resultList;
    }


    @Override
    public List<DoublePair> aggregateResults(List<DoublePair> results)
    {
        Map<Double, List<Double>> collectedMap = results.stream()
                                                        .collect(groupingBy(DoublePair::getFirst, mapping(DoublePair::getSecond, toList())));

        List<DoublePair> resultList = collectedMap.entrySet().stream()
                                                             .map(entry -> DoublePair.from(entry.getKey(), entry.getValue().stream().mapToDouble(d -> d).average().getAsDouble()))
                                                             .collect(Collectors.toList());

        return resultList;
    }

    public void setFingerprintingStrategy(FingerprintingStrategy fingerprintingStrategy) {
        this.fingerprintingStrategy = fingerprintingStrategy;
    }

    public void setAccessPointPositions(Map<MACAddress, GeoPosition> accessPointPositions) {
        this.accessPointPositions = accessPointPositions;
    }
}
