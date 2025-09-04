package com.talentica.wifiindoorpositioning.wifiindoorpositioning.core;

import com.talentica.wifiindoorpositioning.wifiindoorpositioning.model.AccessPoint;
import com.talentica.wifiindoorpositioning.wifiindoorpositioning.model.IndoorProject;
import com.talentica.wifiindoorpositioning.wifiindoorpositioning.model.LocDistance;
import com.talentica.wifiindoorpositioning.wifiindoorpositioning.model.LocationWithNearbyPlaces;
import com.talentica.wifiindoorpositioning.wifiindoorpositioning.model.ReferencePoint;
import com.talentica.wifiindoorpositioning.wifiindoorpositioning.model.WifiDataNetwork;
import com.talentica.wifiindoorpositioning.wifiindoorpositioning.utils.AppContants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Algorithms {

    final static String K = "4";

    public static LocationWithNearbyPlaces processingAlgorithms(List<WifiDataNetwork> latestScanList, IndoorProject proj, int algorithm_choice) {

        int i, j;
        List<AccessPoint> aps = proj.getAps();
        ArrayList<Float> observedRSSValues = new ArrayList<>();
        WifiDataNetwork temp_LR;
        int notFoundCounter = 0;

        for (i = 0; i < aps.size(); ++i) {
            for (j = 0; j < latestScanList.size(); ++j) {
                temp_LR = latestScanList.get(j);
                if (aps.get(i).getMac_address().compareTo(temp_LR.getBssid()) == 0) {
                    observedRSSValues.add((float) temp_LR.getLevel());
                    break;
                }
            }
            if (j == latestScanList.size()) {
                observedRSSValues.add(AppContants.NaN);
                ++notFoundCounter;
            }
        }

        if (notFoundCounter == aps.size())
            return null;

        String parameter = readParameter(algorithm_choice);
        if (parameter == null)
            return null;

        switch (algorithm_choice) {
            case 1:
                return KNN_WKNN_Algorithm(proj, observedRSSValues, parameter, false);
            case 2:
                return KNN_WKNN_Algorithm(proj, observedRSSValues, parameter, true);
            case 3:
                return MAP_MMSE_Algorithm(proj, observedRSSValues, parameter, false);
            case 4:
                return MAP_MMSE_Algorithm(proj, observedRSSValues, parameter, true);
        }
        return null;
    }

    private static LocationWithNearbyPlaces KNN_WKNN_Algorithm(IndoorProject proj, ArrayList<Float> observedRSSValues,
                                                                String parameter, boolean isWeighted) {

        List<AccessPoint> rssValues;
        float curResult;
        ArrayList<LocDistance> locDistanceResultsList = new ArrayList<>();
        String myLocation;
        int K;

        try {
            K = Integer.parseInt(parameter);
        } catch (Exception e) {
            return null;
        }

        for (ReferencePoint referencePoint : proj.getRps()) {
            rssValues = referencePoint.getReadings();
            curResult = calculateEuclideanDistance(rssValues, observedRSSValues);

            if (curResult == Float.NEGATIVE_INFINITY)
                return null;

            locDistanceResultsList.add(new LocDistance(curResult, referencePoint.getLocId(), referencePoint.getName()));
        }

        Collections.sort(locDistanceResultsList, Comparator.comparingDouble(LocDistance::getDistance));

        if (!isWeighted) {
            myLocation = calculateAverageKDistanceLocations(locDistanceResultsList, K);
        } else {
            myLocation = calculateWeightedAverageKDistanceLocations(locDistanceResultsList, K);
        }
        return new LocationWithNearbyPlaces(myLocation, locDistanceResultsList);
    }

    private static LocationWithNearbyPlaces MAP_MMSE_Algorithm(IndoorProject proj, ArrayList<Float> observedRssValues,
                                                                String parameter, boolean isWeighted) {
        List<AccessPoint> rssValues;
        double curResult;
        String myLocation = null;
        double highestProbability = Double.NEGATIVE_INFINITY;
        ArrayList<LocDistance> locDistanceResultsList = new ArrayList<>();
        float sGreek;

        try {
            sGreek = Float.parseFloat(parameter);
        } catch (Exception e) {
            return null;
        }

        for (ReferencePoint referencePoint : proj.getRps()) {
            rssValues = referencePoint.getReadings();
            curResult = calculateProbability(rssValues, observedRssValues, sGreek);

            if (curResult == Double.NEGATIVE_INFINITY)
                return null;
            else if (curResult > highestProbability) {
                highestProbability = curResult;
                myLocation = referencePoint.getLocId();
            }

            if (isWeighted)
                locDistanceResultsList.add(new LocDistance(curResult, referencePoint.getLocId(), referencePoint.getName()));
        }

        if (isWeighted)
            myLocation = calculateWeightedAverageProbabilityLocations(locDistanceResultsList);
        return new LocationWithNearbyPlaces(myLocation, locDistanceResultsList);
    }

    private static float calculateEuclideanDistance(List<AccessPoint> l1, ArrayList<Float> l2) {
        float finalResult = 0;
        float v1;
        float v2;
        float temp;

        for (int i = 0; i < l1.size(); ++i) {
            try {
                v1 = (float) l1.get(i).getMeanRss();
                v2 = l2.get(i);
            } catch (Exception e) {
                return Float.NEGATIVE_INFINITY;
            }
            temp = v1 - v2;
            temp *= temp;
            finalResult += temp;
        }
        return (float) Math.sqrt(finalResult);
    }

    private static double calculateProbability(List<AccessPoint> l1, ArrayList<Float> l2, float sGreek) {
        double finalResult = 1;
        float v1;
        float v2;
        double temp;

        for (int i = 0; i < l1.size(); ++i) {
            try {
                v1 = (float) l1.get(i).getMeanRss();
                v2 = l2.get(i);
            } catch (Exception e) {
                return Double.NEGATIVE_INFINITY;
            }
            temp = v1 - v2;
            temp *= temp;
            temp = -temp;
            temp /= (double) (sGreek * sGreek);
            temp = Math.exp(temp);

            if (finalResult * temp != 0)
                finalResult *= temp;
        }
        return finalResult;
    }

    private static String calculateAverageKDistanceLocations(ArrayList<LocDistance> list, int K) {
        float sumX = 0.0f;
        float sumY = 0.0f;
        String[] LocationArray;
        float x, y;

        int K_Min = Math.min(K, list.size());

        for (int i = 0; i < K_Min; ++i) {
            LocationArray = list.get(i).getLocation().split(" ");
            try {
                x = Float.parseFloat(LocationArray[0].trim());
                y = Float.parseFloat(LocationArray[1].trim());
            } catch (Exception e) {
                return null;
            }
            sumX += x;
            sumY += y;
        }
        sumX /= K_Min;
        sumY /= K_Min;
        return sumX + " " + sumY;
    }

    private static String calculateWeightedAverageKDistanceLocations(ArrayList<LocDistance> list, int K) {
        double LocationWeight;
        double sumWeights = 0.0f;
        double WeightedSumX = 0.0f;
        double WeightedSumY = 0.0f;
        String[] LocationArray;
        float x, y;

        int K_Min = Math.min(K, list.size());

        for (int i = 0; i < K_Min; ++i) {
            if (list.get(i).getDistance() != 0.0) {
                LocationWeight = 1 / list.get(i).getDistance();
            } else {
                LocationWeight = 100;
            }
            LocationArray = list.get(i).getLocation().split(" ");
            try {
                x = Float.parseFloat(LocationArray[0].trim());
                y = Float.parseFloat(LocationArray[1].trim());
            } catch (Exception e) {
                return null;
            }
            sumWeights += LocationWeight;
            WeightedSumX += LocationWeight * x;
            WeightedSumY += LocationWeight * y;
        }
        WeightedSumX /= sumWeights;
        WeightedSumY /= sumWeights;
        return WeightedSumX + " " + WeightedSumY;
    }

    private static String calculateWeightedAverageProbabilityLocations(ArrayList<LocDistance> list) {
