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

    /**
     * @param latestScanList current scan list of APs
     * @param proj           project details for current area
     * @param algorithm_choice algorithm choice
     * @return estimated user location with nearby places
     */
    public static LocationWithNearbyPlaces processingAlgorithms(List<WifiDataNetwork> latestScanList,
                                                                IndoorProject proj,
                                                                int algorithm_choice) {

        int i, j;
        List<AccessPoint> aps = proj.getAps();
        ArrayList<Float> observedRSSValues = new ArrayList<>();
        WifiDataNetwork temp_LR;
        int notFoundCounter = 0;

        // Check which radio-map MACs are currently observed
        for (i = 0; i < aps.size(); ++i) {
            for (j = 0; j < latestScanList.size(); ++j) {
                temp_LR = latestScanList.get(j);
                if (aps.get(i).getMac_address().compareTo(temp_LR.getBssid()) == 0) {
                    observedRSSValues.add((float) temp_LR.getLevel());
                    break;
                }
            }
            // Missing MAC -> place NaN small value
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

    /**
     * KNN / WKNN
     */
    private static LocationWithNearbyPlaces KNN_WKNN_Algorithm(IndoorProject proj,
                                                               ArrayList<Float> observedRSSValues,
                                                               String parameter,
                                                               boolean isWeighted) {

        List<AccessPoint> rssValues;
        float curResult;
        ArrayList<LocDistance> locDistanceResultsList = new ArrayList<>();
        String myLocation;
        int Kval;

        try {
            Kval = Integer.parseInt(parameter);
        } catch (Exception e) {
            return null;
        }

        // Build location-distance pairs
        for (ReferencePoint referencePoint : proj.getRps()) {
            rssValues = referencePoint.getReadings();
            curResult = calculateEuclideanDistance(rssValues, observedRSSValues);
            if (curResult == Float.NEGATIVE_INFINITY)
                return null;
            locDistanceResultsList.add(new LocDistance(curResult, referencePoint.getLocId(), referencePoint.getName()));
        }

        // Sort by ascending distance
        Collections.sort(locDistanceResultsList, Comparator.comparingDouble(LocDistance::getDistance));

        if (!isWeighted) {
            myLocation = calculateAverageKDistanceLocations(locDistanceResultsList, Kval);
        } else {
            myLocation = calculateWeightedAverageKDistanceLocations(locDistanceResultsList, Kval);
        }
        return new LocationWithNearbyPlaces(myLocation, locDistanceResultsList);
    }

    /**
     * MAP / MMSE
     */
    private static LocationWithNearbyPlaces MAP_MMSE_Algorithm(IndoorProject proj,
                                                               ArrayList<Float> observedRssValues,
                                                               String parameter,
                                                               boolean isWeighted) {
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

        // Find location with highest probability
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

    /**
     * Euclidean distance between stored RSS and observed RSS
     */
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

    /**
     * Probability for MAP/MMSE
     */
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

            // avoid underflow to zero if possible
            if (finalResult * temp != 0)
                finalResult *= temp;
        }
        return finalResult;
    }

    /**
     * Average of the K nearest locations
     */
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

    /**
     * Weighted average of the K nearest locations (weights 1/d)
     */
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
                LocationWeight = 100; // large weight for exact match
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

    /**
     * Weighted average over ALL locations using normalized probabilities
     */
    private static String calculateWeightedAverageProbabilityLocations(ArrayList<LocDistance> list) {
        double sumProbabilities = 0.0f;
        double WeightedSumX = 0.0f;
        double WeightedSumY = 0.0f;
        double NP;
        float x, y;
        String[] LocationArray;

        for (int i = 0; i < list.size(); ++i)
            sumProbabilities += list.get(i).getDistance();

        for (int i = 0; i < list.size(); ++i) {
            LocationArray = list.get(i).getLocation().split(" ");
            try {
                x = Float.parseFloat(LocationArray[0].trim());
                y = Float.parseFloat(LocationArray[1].trim());
            } catch (Exception e) {
                return null;
            }

            NP = list.get(i).getDistance() / sumProbabilities;
            WeightedSumX += (x * NP);
            WeightedSumY += (y * NP);
        }

        return WeightedSumX + " " + WeightedSumY;
    }

    /**
     * Read parameters from file (legacy). Kept for completeness.
     */
    private static String readParameter(File file, int algorithm_choice) {
        String line;
        BufferedReader reader = null;
        String parameter = null;

        try {
            FileReader fr = new FileReader(file.getAbsolutePath().replace(".txt", "-parameters2.txt"));
            reader = new BufferedReader(fr);

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.trim().equals("")) {
                    continue;
                }
                String[] temp = line.split(":");
                if (temp.length != 2) {
                    return null;
                }

                if (algorithm_choice == 0 && temp[0].equals("NaN")) {
                    parameter = temp[1];
                    break;
                } else if (algorithm_choice == 1 && temp[0].equals("KNN")) {
                    parameter = temp[1];
                    break;
                } else if (algorithm_choice == 2 && temp[0].equals("WKNN")) {
                    parameter = temp[1];
                    break;
                } else if (algorithm_choice == 3 && temp[0].equals("MAP")) {
                    parameter = temp[1];
                    break;
                } else if (algorithm_choice == 4 && temp[0].equals("MMSE")) {
                    parameter = temp[1];
                    break;
                }
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
        }
        return parameter;
    }

    /**
     * Read parameter for selected algorithm (in-app default)
     */
    private static String readParameter(int algorithm_choice) {
        String parameter = null;

        if (algorithm_choice == 1) {
            parameter = K; // KNN
        } else if (algorithm_choice == 2) {
            parameter = K; // WKNN
        } else if (algorithm_choice == 3) {
            parameter = K; // MAP (uses K here as sigma placeholder)
        } else if (algorithm_choice == 4) {
            parameter = K; // MMSE
        }
        return parameter;
    }
}
