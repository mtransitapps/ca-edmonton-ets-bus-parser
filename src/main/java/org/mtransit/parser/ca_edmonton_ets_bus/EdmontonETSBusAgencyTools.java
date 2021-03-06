package org.mtransit.parser.ca_edmonton_ets_bus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.StringUtils;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GIDs;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mtransit.parser.Constants.EMPTY;
import static org.mtransit.parser.Constants.SPACE_;

// https://data.edmonton.ca/
// http://www.edmonton.ca/ets/ets-data-for-developers.aspx
// https://data.edmonton.ca/Transit/ETS-Bus-Schedule-GTFS-Data-Schedules-zipped-files/urjq-fvmq
// https://gtfs.edmonton.ca/TMGTFSRealTimeWebService/GTFS/GTFS.zip
public class EdmontonETSBusAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new EdmontonETSBusAgencyTools().start(args);
	}

	@Override
	public boolean defaultExcludeEnabled() {
		return true;
	}


	@NotNull
	public String getAgencyName() {
		return "ETS";
	}

	private static final int AGENCY_ID_INT = GIDs.getInt("1"); // Edmonton Transit Service ONLY

	@Override
	public boolean excludeRoute(@NotNull GRoute gRoute) {
		if (gRoute.isDifferentAgency(AGENCY_ID_INT)) {
			return true; // exclude
		}
		return super.excludeRoute(gRoute);
	}

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		if ("Not In Service".equalsIgnoreCase(gTrip.getTripHeadsign())) {
			return true; // exclude
		}
		if ("Sorry Not In Service".equalsIgnoreCase(gTrip.getTripHeadsign())) {
			return true; // exclude
		}
		return super.excludeTrip(gTrip);
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	private static final String X = "X";

	private static final long RID_ENDS_WITH_X = 24_000L;

	@Override
	public long getRouteId(@NotNull GRoute gRoute) {
		if (!Utils.isDigitsOnly(gRoute.getRouteShortName())) {
			Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
			if (matcher.find()) {
				long digits = Long.parseLong(matcher.group());
				if (gRoute.getRouteShortName().endsWith(X)) {
					return digits + RID_ENDS_WITH_X;
				}
			}
			throw new MTLog.Fatal("Unexpected route ID for %s!", gRoute.toStringPlus());
		}
		return Long.parseLong(gRoute.getRouteShortName()); // using route short name as route ID
	}

	@NotNull
	@Override
	public String cleanRouteLongName(@NotNull String gRouteLongName) {
		gRouteLongName = CleanUtils.cleanStreetTypes(gRouteLongName);
		return CleanUtils.cleanLabel(gRouteLongName);
	}

	@Override
	public boolean mergeRouteLongName(@NotNull MRoute mRoute, @NotNull MRoute mRouteToMerge) {
		if (mRoute.simpleMergeLongName(mRouteToMerge)) {
			return super.mergeRouteLongName(mRoute, mRouteToMerge);
		}
		if (isGoodEnoughAccepted()) {
			return super.mergeRouteLongName(mRoute, mRouteToMerge);
		}
		throw new MTLog.Fatal("Unexpected routes to merge: %s & %s!", mRoute, mRouteToMerge);
	}

	private static final String AGENCY_COLOR_BLUE = "2D3092"; // BLUE (from Wikipedia SVG)

	private static final String AGENCY_COLOR = AGENCY_COLOR_BLUE;

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	private static final Pattern BAY_AZ09 = Pattern.compile("( bay [a-z0-9]+)", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanDirectionHeadsign(boolean fromStopName, @NotNull String directionHeadSign) {
		directionHeadSign = cleanTripHeadsign(fromStopName, directionHeadSign);
		directionHeadSign = BAY_AZ09.matcher(directionHeadSign).replaceAll(SPACE_);
		return directionHeadSign;
	}

	private static final String S_ = "S ";

	@Nullable
	@Override
	public String selectDirectionHeadSign(@Nullable String headSign1, @Nullable String headSign2) {
		if (StringUtils.equals(headSign1, headSign2)) {
			return null; // can NOT select
		}
		if (headSign1 != null && headSign1.startsWith(S_)) {
			if (headSign2 == null || !headSign2.startsWith(S_)) {
				return headSign2;
			}
		} else if (headSign2 != null && headSign2.startsWith(S_)) {
			return headSign1;
		}
		return null;
	}

	private static final String NAIT = "NAIT";
	private static final Pattern N_A_I_T = CleanUtils.cleanWords("n a i t");
	private static final String N_A_I_T_REPLACEMENT = CleanUtils.cleanWordsReplacement(NAIT);

	private static final Pattern SUPER_EXPRESS = CleanUtils.cleanWords("super express");

	private static final Pattern STARTS_WITH_RSN = Pattern.compile("(^[\\d]+( )?)", Pattern.CASE_INSENSITIVE);

	private static final Pattern CLOCKWISE_ = CleanUtils.cleanWords("clockwise");
	private static final String CLOCKWISE_REPLACEMENT = CleanUtils.cleanWordsReplacement("CW");

	private static final Pattern COUNTERCLOCKWISE_ = CleanUtils.cleanWords("counterclockwise");
	private static final String COUNTERCLOCKWISE_REPLACEMENT = CleanUtils.cleanWordsReplacement("CCW");

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		return cleanTripHeadsign(false, tripHeadsign);
	}

	@NotNull
	private String cleanTripHeadsign(boolean fromStopName, @NotNull String tripHeadsign) {
		tripHeadsign = CleanUtils.keepToAndRemoveVia(tripHeadsign);
		tripHeadsign = CLOCKWISE_.matcher(tripHeadsign).replaceAll(CLOCKWISE_REPLACEMENT);
		tripHeadsign = COUNTERCLOCKWISE_.matcher(tripHeadsign).replaceAll(COUNTERCLOCKWISE_REPLACEMENT);
		if (!fromStopName) {
			tripHeadsign = STARTS_WITH_RSN.matcher(tripHeadsign).replaceAll(EMPTY);
		}
		tripHeadsign = TRANSIT_CENTER.matcher(tripHeadsign).replaceAll(TRANSIT_CENTER_REPLACEMENT);
		tripHeadsign = TOWN_CENTER.matcher(tripHeadsign).replaceAll(TOWN_CENTER_REPLACEMENT);
		tripHeadsign = SUPER_EXPRESS.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = INTERNATIONAL.matcher(tripHeadsign).replaceAll(INTERNATIONAL_REPLACEMENT);
		tripHeadsign = GOVERNMENT_.matcher(tripHeadsign).replaceAll(GOVERNMENT_REPLACEMENT);
		tripHeadsign = BELVEDERE_.matcher(tripHeadsign).replaceAll(BELVEDERE_REPLACEMENT);
		tripHeadsign = INDUSTRIAL_.matcher(tripHeadsign).replaceAll(INDUSTRIAL_REPLACEMENT);
		tripHeadsign = EDMONTON.matcher(tripHeadsign).replaceAll(EDMONTON_REPLACEMENT);
		tripHeadsign = N_A_I_T.matcher(tripHeadsign).replaceAll(N_A_I_T_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanBounds(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		tripHeadsign = CleanUtils.removePoints(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final String TRANSIT_CENTER_SHORT = "TC";
	private static final Pattern TRANSIT_CENTER = CleanUtils.cleanWords("transit center", "transit centre");
	private static final String TRANSIT_CENTER_REPLACEMENT = CleanUtils.cleanWordsReplacement(TRANSIT_CENTER_SHORT);

	private static final String TOWN_CENTER_SHORT = "TC";
	private static final Pattern TOWN_CENTER = CleanUtils.cleanWords("town center", "town centre");
	private static final String TOWN_CENTER_REPLACEMENT = CleanUtils.cleanWordsReplacement(TOWN_CENTER_SHORT);

	private static final Pattern INTERNATIONAL = CleanUtils.cleanWords("international");
	private static final String INTERNATIONAL_REPLACEMENT = CleanUtils.cleanWordsReplacement("Int");

	private static final String GOVERNMENT_SHORT = "Gov";
	private static final Pattern GOVERNMENT_ = CleanUtils.cleanWords("government");
	private static final String GOVERNMENT_REPLACEMENT = CleanUtils.cleanWordsReplacement(GOVERNMENT_SHORT);

	private static final String BELVEDERE = "Belvedere";
	private static final Pattern BELVEDERE_ = CleanUtils.cleanWords("belevedere");
	private static final String BELVEDERE_REPLACEMENT = CleanUtils.cleanWordsReplacement(BELVEDERE);

	private static final String INDUSTRIAL_SHORT = "Ind";
	private static final Pattern INDUSTRIAL_ = CleanUtils.cleanWords("industrial");
	private static final String INDUSTRIAL_REPLACEMENT = CleanUtils.cleanWordsReplacement(INDUSTRIAL_SHORT);

	private static final String EDMONTON_SHORT = "Edm";
	private static final Pattern EDMONTON = CleanUtils.cleanWords("edmonton");
	private static final String EDMONTON_REPLACEMENT = CleanUtils.cleanWordsReplacement(EDMONTON_SHORT);

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = TRANSIT_CENTER.matcher(gStopName).replaceAll(TRANSIT_CENTER_REPLACEMENT);
		gStopName = TOWN_CENTER.matcher(gStopName).replaceAll(TOWN_CENTER_REPLACEMENT);
		gStopName = INTERNATIONAL.matcher(gStopName).replaceAll(INTERNATIONAL_REPLACEMENT);
		gStopName = INDUSTRIAL_.matcher(gStopName).replaceAll(INDUSTRIAL_REPLACEMENT);
		gStopName = GOVERNMENT_.matcher(gStopName).replaceAll(GOVERNMENT_REPLACEMENT);
		gStopName = BELVEDERE_.matcher(gStopName).replaceAll(BELVEDERE_REPLACEMENT);
		gStopName = EDMONTON.matcher(gStopName).replaceAll(EDMONTON_REPLACEMENT);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@Override
	public int getStopId(@NotNull GStop gStop) {
		return Math.abs(super.getStopId(gStop)); // remove negative stop IDs
	}

	private static final Pattern REMOVE_STARTING_DASH = Pattern.compile("(^-)", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String getStopCode(@NotNull GStop gStop) {
		String stopCode = super.getStopCode(gStop); // do not change, used by real-time API
		stopCode = REMOVE_STARTING_DASH.matcher(stopCode).replaceAll(EMPTY);
		return stopCode; // do not change, used by real-time API
	}
}
