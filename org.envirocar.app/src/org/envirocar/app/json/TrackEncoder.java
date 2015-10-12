///*
// * enviroCar 2013
// * Copyright (C) 2013
// * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
// *
// * This program is free software; you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation; either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software Foundation,
// * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
// *
// */
//package org.envirocar.app.json;
//
//import org.envirocar.core.entity.Measurement;
//import org.envirocar.core.entity.Track;
//import org.envirocar.core.exception.NoMeasurementsException;
//import org.envirocar.core.logging.Logger;
//import org.envirocar.core.util.Util;
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//
//public class TrackEncoder {
//
//	private static final Logger logger = Logger.getLogger(TrackEncoder.class);
//
//	public static final Set<Measurement.PropertyKey> supportedPhenomenons = new HashSet<Measurement.PropertyKey>();
//
//	static {
//		supportedPhenomenons.add(Measurement.PropertyKey.CALCULATED_MAF);
//		supportedPhenomenons.add(Measurement.PropertyKey.MAF);
//		supportedPhenomenons.add(Measurement.PropertyKey.CO2);
//		supportedPhenomenons.add(Measurement.PropertyKey.SPEED);
//		supportedPhenomenons.add(Measurement.PropertyKey.RPM);
//		supportedPhenomenons.add(Measurement.PropertyKey.INTAKE_PRESSURE);
//		supportedPhenomenons.add(Measurement.PropertyKey.INTAKE_TEMPERATURE);
//		supportedPhenomenons.add(Measurement.PropertyKey.CONSUMPTION);
//		supportedPhenomenons.add(Measurement.PropertyKey.ENGINE_LOAD);
//		supportedPhenomenons.add(Measurement.PropertyKey.THROTTLE_POSITON);
//		supportedPhenomenons.add(Measurement.PropertyKey.GPS_ACCURACY);
//		supportedPhenomenons.add(Measurement.PropertyKey.GPS_ALTITUDE);
//		supportedPhenomenons.add(Measurement.PropertyKey.GPS_BEARING);
//		supportedPhenomenons.add(Measurement.PropertyKey.GPS_HDOP);
//		supportedPhenomenons.add(Measurement.PropertyKey.GPS_PDOP);
//		supportedPhenomenons.add(Measurement.PropertyKey.GPS_VDOP);
//		supportedPhenomenons.add(Measurement.PropertyKey.GPS_SPEED);
//		supportedPhenomenons.add(Measurement.PropertyKey.SHORT_TERM_TRIM_1);
//		supportedPhenomenons.add(Measurement.PropertyKey.LONG_TERM_TRIM_1);
//		supportedPhenomenons.add(Measurement.PropertyKey.LAMBDA_CURRENT);
//		supportedPhenomenons.add(Measurement.PropertyKey.LAMBDA_CURRENT_ER);
//		supportedPhenomenons.add(Measurement.PropertyKey.LAMBDA_VOLTAGE);
//		supportedPhenomenons.add(Measurement.PropertyKey.LAMBDA_VOLTAGE_ER);
//		supportedPhenomenons.add(Measurement.PropertyKey.FUEL_SYSTEM_LOOP);
//		supportedPhenomenons.add(Measurement.PropertyKey.FUEL_SYSTEM_STATUS_CODE);
//	}
//
//	/**
//	 * Converts Track Object into track.create.json string
//	 *
//	 * @return
//	 * @throws JSONException
//	 * @throws TrackWithoutMeasurementsException
//	 */
//	public JSONObject createTrackJson(Track track, boolean obfuscate) throws JSONException, TrackWithoutMeasurementsException {
//		JSONObject result = new JSONObject();
//
//		String trackSensorName = track.getCar().getId();
//
//		ArrayList<JSONObject> measurementElements = new ArrayList<JSONObject>();
//
//		List<Measurement> measurements = getNonObfuscatedMeasurements(track, obfuscate);
//
//		if (measurements == null || measurements.isEmpty()) {
//			throw new TrackWithoutMeasurementsException("Track did not contain any non obfuscated measurements.");
//		}
//
//
//		for (Measurement measurement : measurements) {
//			JSONObject measurementJson = createMeasurementJson(track, trackSensorName, measurement);
//			measurementElements.add(measurementJson);
//		}
//
//		result.put("type", "FeatureCollection");
//		result.put("features", new JSONArray(measurementElements));
//		result.put("properties", createTrackProperties(track, trackSensorName));
//
//		return result;
//	}
//
//	/**
//	 * resolve all not obfuscated measurements of a track.
//	 *
//	 * This returns all measurements, if obfuscation is disabled. Otherwise
//	 * measurements within the first and last minute and those within the start/end
//	 * radius of 250 m are ignored (only if they are in the beginning/end of the track).
//	 *
//	 * @param track
//	 * @return
//	 */
//	public List<Measurement> getNonObfuscatedMeasurements(Track track, boolean obfuscate) {
//		List<Measurement> measurements = track.getMeasurements();
//
//
//		if (obfuscate) {
//			boolean wasAtLeastOneTimeNotObfuscated = false;
//			ArrayList<Measurement> privateCandidates = new ArrayList<Measurement>();
//			ArrayList<Measurement> nonPrivateMeasurements = new ArrayList<Measurement>();
//			for (Measurement measurement : measurements) {
//				try {
//					/*
//					 * ignore early and late
//					 */
//					if (isTemporalObfuscationCandidate(measurement, track)) {
//						continue;
//					}
//
//					/*
//					 * ignore distance
//					 */
//					if (isSpatialObfuscationCandidate(measurement, track)) {
//						if (wasAtLeastOneTimeNotObfuscated) {
//							privateCandidates.add(measurement);
//							nonPrivateMeasurements.add(measurement);
//						}
//						continue;
//					}
//
//					/*
//					 * we may have found obfuscation candidates in the middle of the track
//					 * (may cross start or end point) in a PRIOR iteration
//					 * of this loop. these candidates can be removed now as we are again
//					 * out of obfuscation scope
//					 */
//					if (wasAtLeastOneTimeNotObfuscated) {
//						privateCandidates.clear();
//					}
//					else {
//						wasAtLeastOneTimeNotObfuscated = true;
//					}
//
//					nonPrivateMeasurements.add(measurement);
//				} catch (NoMeasurementsException e) {
//					logger.warn(e.getMessage(), e);
//				}
//
//			}
//			/*
//			 * the private candidates which have made it until here
//			 * shall be ignored
//			 */
//			nonPrivateMeasurements.removeAll(privateCandidates);
//			return nonPrivateMeasurements;
//		}
//
//		return measurements;
//	}
//
//	private boolean isSpatialObfuscationCandidate(Measurement measurement,
//			Track track) throws NoMeasurementsException {
//		return (Util.getDistance(track.getFirstMeasurement(), measurement) <= 0.25)
//				|| (Util.getDistance(track.getLastMeasurement(), measurement) <= 0.25);
//	}
//
//	private boolean isTemporalObfuscationCandidate(Measurement measurement,
//			Track track) throws NoMeasurementsException {
//		return (measurement.getTime() - track.getStartTime() <= 60000 ||
//				track.getEndTime() - measurement.getTime() <= 60000);
//	}
//
//
//	private JSONObject createTrackProperties(Track track, String trackSensorName) throws JSONException {
//		JSONObject result = new JSONObject();
//
//		result.put("sensor", trackSensorName);
//		result.put("description", track.getDescription());
//		result.put("name", track.getName());
//
//		if (track.getMetadata() != null) {
//			JSONObject json = track.getMetadata().toJson();
//			JSONArray names = json.names();
//			for (int i = 0; i < names.length(); i++) {
//				result.put(names.get(i).toString(), json.getString(names.get(i).toString()));
//			}
//		}
//
//		return result;
//	}
//
//	private JSONObject createMeasurementJson(Track track, String trackSensorName, Measurement measurement) throws JSONException {
//		JSONObject result = new JSONObject();
//		result.put("type", "Feature");
//
//		result.put("geometry", createGeometry(measurement));
//		result.put("properties", createMeasurementProperties(measurement, trackSensorName));
//
//		return result;
//	}
//
//	private JSONObject createGeometry(Measurement measurement) throws JSONException {
//		JSONObject result = new JSONObject();
//		result.put("type", "Point");
//
//		ArrayList<Double> coords = new ArrayList<Double>(2);
//		coords.add(measurement.getLongitude());
//		coords.add(measurement.getLatitude());
//
//		result.put("coordinates", new JSONArray(coords));
//		return result;
//	}
//
//	private JSONObject createMeasurementProperties(Measurement measurement, String trackSensorName) throws JSONException {
//		JSONObject result = new JSONObject();
//		result.put("sensor", trackSensorName);
//		JSONObject phens = createPhenomenons(measurement);
//		if (phens != null && phens.length() > 0) {
//			result.put("phenomenons", phens);
//		}
//		result.put("time", Util.longToIsoDate(measurement.getTime()));
//		return result;
//	}
//
//	private JSONObject createPhenomenons(Measurement measurement) throws JSONException {
//		JSONObject result = new JSONObject();
//		Map<Measurement.PropertyKey, Double> props = measurement.getAllProperties();
//		for (Measurement.PropertyKey key : props.keySet()) {
//			if (supportedPhenomenons.contains(key)) {
//				result.put(key.toString(), createValue(props.get(key)));
//			}
//		}
//		return result;
//	}
//
//	private JSONObject createValue(Double double1) throws JSONException {
//		JSONObject result = new JSONObject();
//		result.put("value", double1);
//		return result;
//	}
//}
