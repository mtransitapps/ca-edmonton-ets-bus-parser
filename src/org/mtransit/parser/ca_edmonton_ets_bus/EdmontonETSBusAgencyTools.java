package org.mtransit.parser.ca_edmonton_ets_bus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Pair;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GStopTime;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MSpec;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

// http://www.edmonton.ca/transportation/ets/about_ets/ets-data-for-developers.aspx
// http://webdocs.edmonton.ca/transit/etsdatafeed/google_transit.zip
public class EdmontonETSBusAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-edmonton-ets-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new EdmontonETSBusAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("Generating ETS bus data...\n");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("Generating ETS bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public long getRouteId(GRoute gRoute) {
		return Long.parseLong(gRoute.route_short_name); // using route short name as route ID
	}

	private static final String WEST_EDM_MALL = "West Edm. Mall";
	private static final String WEM_LEWIS_FARMS = WEST_EDM_MALL + " / Lewis Farms";
	private static final String CAPILANO = "Capilano"; // "Capilano Transit Ctr"
	private static final String CLAREVIEW = "Clareview";
	private static final String CROMDALE = "Cromdale";
	private static final String JASPER_PLACE = "Jasper Pl";
	private static final String COLISEUM = "Coliseum";
	private static final String WESTMOUNT = "Westmount";
	private static final String UNIVERSITY = "University";
	private static final String MILL_WOODS = "Mill Woods";
	private static final String SOUTHGATE = "Southgate";
	private static final String NORTHGATE = "Northgate";
	private static final String ABBOTTSFIELD = "Abbottsfield";
	private static final String EAUX_CLAIRES = "Eaux Claires";
	private static final String DOWNTOWN = "Downtown";
	private static final String DOWNTOWN_JASPER_PLACE = DOWNTOWN + " / " + JASPER_PLACE;
	private static final String GOV_CTR = "Gov Ctr";
	private static final String CASTLE_DOWNS = "Castle Downs";
	private static final String CENTURY_PK = "Century Pk";
	private static final String YELLOWBIRD = "Yellowbird";
	private static final String YELLOWBIRD_CENTURY_PK = YELLOWBIRD + "/" + CENTURY_PK;
	private static final String MILL_WOODS_CENTURY_PK = MILL_WOODS + " / " + CENTURY_PK;
	private static final String S_CAMPUS = "S Campus";
	private static final String FORT_EDM = "Fort Edm.";
	private static final String S_CAMPUS_FORT_EDM = S_CAMPUS + " / " + FORT_EDM;
	private static final String LEGER = "Leger";
	private static final String BRANDER_GDNS = "Brander Gdns";
	private static final String MEADOWS_TC = "Meadows TC";
	private static final String BLACKMUD_CRK = "Blackmud Crk";
	private static final String BLACKBURN = "Blackburn";
	private static final String ALLARD = "Allard";
	private static final String HARRY_AINLAY_LP = "Harry Ainlay Lp";
	private static final String TWIN_BROOKS = "Twin Brooks";
	private static final String RUTHERFORD = "Rutherford";

	private static final String RLN_SPLIT = " - ";

	private static final String RLN_1 = WEST_EDM_MALL + RLN_SPLIT + CAPILANO;
	private static final String RLN_3 = JASPER_PLACE + RLN_SPLIT + CROMDALE;

	@Override
	public String getRouteLongName(GRoute gRoute) {
		int rsn = Integer.parseInt(gRoute.route_short_name);
		switch (rsn) {
		// @formatter:off
		case 1: return RLN_1;
		case 2: break; // keep original
		case 3: return RLN_3;
		// @formatter:on
		}
		String gRouteLongName = gRoute.route_long_name;
		gRouteLongName = MSpec.cleanStreetTypes(gRouteLongName);
		return MSpec.cleanLabel(gRouteLongName);
	}

	private static final String AGENCY_COLOR_BLUE = "2D3092"; // BLUE (from Wikipedia SVG)

	private static final String AGENCY_COLOR = AGENCY_COLOR_BLUE;

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public int compare(MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ts1.getTripId() == 3004l) { // 30 South to Mill Woods
			if (STOP_ID_4803.equals(ts1GStop.stop_code) && STOP_ID_2704.equals(ts2GStop.stop_code)) {
				return -1;
			} else if (STOP_ID_2704.equals(ts1GStop.stop_code) && STOP_ID_4803.equals(ts2GStop.stop_code)) {
				return +1;
			}
		}
		return super.compare(ts1, ts2, ts1GStop, ts2GStop);
	}


	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS.containsKey(mRoute.id)) {
			return; // split
		}
		System.out.println("Unexpected trip " + gTrip);
		System.exit(-1);
	}

	@Override
	public HashSet<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS.containsKey(mRoute.id)) {
			return ALL_ROUTE_TRIPS.get(mRoute.id).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}







	private static final String DASH = "-";
	private static final String ALL = "*";

	private static final String STOP_ID_1001 = "1001";
	private static final String STOP_ID_1075 = "1075";
	private static final String STOP_ID_1109 = "1109";
	private static final String STOP_ID_1123 = "1123";
	private static final String STOP_ID_1142 = "1142";
	private static final String STOP_ID_1147 = "1147";
	private static final String STOP_ID_1202 = "1202";
	private static final String STOP_ID_1203 = "1203";
	private static final String STOP_ID_1251 = "1251";
	private static final String STOP_ID_1256 = "1256";
	private static final String STOP_ID_1266 = "1266";
	private static final String STOP_ID_1310 = "1310";
	private static final String STOP_ID_1336 = "1336";
	private static final String STOP_ID_1407 = "1407";
	private static final String STOP_ID_1476 = "1476";
	private static final String STOP_ID_1532 = "1532";
	private static final String STOP_ID_1557 = "1557";
	private static final String STOP_ID_1561 = "1561";
	private static final String STOP_ID_1989 = "1989";
	private static final String STOP_ID_1999 = "1999";
	private static final String STOP_ID_2001 = "2001";
	private static final String STOP_ID_2002 = "2002";
	private static final String STOP_ID_2204 = "2204";
	private static final String STOP_ID_2205 = "2205";
	private static final String STOP_ID_2206 = "2206";
	private static final String STOP_ID_2208 = "2208";
	private static final String STOP_ID_2209 = "2209";
	private static final String STOP_ID_2214 = "2214";
	private static final String STOP_ID_2103 = "2103";
	private static final String STOP_ID_2109 = "2109";
	private static final String STOP_ID_2215 = "2215";
	private static final String STOP_ID_2117 = "2117";
	private static final String STOP_ID_2118 = "2118";
	private static final String STOP_ID_2159 = "2159";
	private static final String STOP_ID_2203 = "2203";
	private static final String STOP_ID_2207 = "2207";
	private static final String STOP_ID_2210 = "2210";
	private static final String STOP_ID_2211 = "2211";
	private static final String STOP_ID_2213 = "2213";
	private static final String STOP_ID_2217 = "2217";
	private static final String STOP_ID_2218 = "2218";
	private static final String STOP_ID_2301 = "2301";
	private static final String STOP_ID_2306 = "2306";
	private static final String STOP_ID_2447 = "2447";
	private static final String STOP_ID_2549 = "2549";
	private static final String STOP_ID_2703 = "2703";
	private static final String STOP_ID_2704 = "2704";
	private static final String STOP_ID_2705 = "2705";
	private static final String STOP_ID_2710 = "2710";
	private static final String STOP_ID_2711 = "2711";
	private static final String STOP_ID_3008 = "3008";
	private static final String STOP_ID_3207 = "3207";
	private static final String STOP_ID_3208 = "3208";
	private static final String STOP_ID_3215 = "3215";
	private static final String STOP_ID_3217 = "3217";
	private static final String STOP_ID_3713 = "3713";
	private static final String STOP_ID_4025 = "4025";
	private static final String STOP_ID_4201 = "4201";
	private static final String STOP_ID_4202 = "4202";
	private static final String STOP_ID_4203 = "4203";
	private static final String STOP_ID_4204 = "4204";
	private static final String STOP_ID_4206 = "4206";
	private static final String STOP_ID_4207 = "4207";
	private static final String STOP_ID_4208 = "4208";
	private static final String STOP_ID_4209 = "4209";
	private static final String STOP_ID_4210 = "4210";
	private static final String STOP_ID_4211 = "4211";
	private static final String STOP_ID_4213 = "4213";
	private static final String STOP_ID_4214 = "4214";
	private static final String STOP_ID_4215 = "4215";
	private static final String STOP_ID_4265 = "4265";
	private static final String STOP_ID_4290 = "4290";
	private static final String STOP_ID_4307 = "4307";
	private static final String STOP_ID_4490 = "4490";
	private static final String STOP_ID_4802 = "4802";
	private static final String STOP_ID_4803 = "4803";
	private static final String STOP_ID_4805 = "4805";
	private static final String STOP_ID_4806 = "4806";
	private static final String STOP_ID_4809 = "4809";
	private static final String STOP_ID_4811 = "4811";
	private static final String STOP_ID_4812 = "4812";
	private static final String STOP_ID_4813 = "4813";
	private static final String STOP_ID_5001 = "5001";
	private static final String STOP_ID_5003 = "5003";
	private static final String STOP_ID_5006 = "5006";
	private static final String STOP_ID_5008 = "5008";
	private static final String STOP_ID_5009 = "5009";
	private static final String STOP_ID_5011 = "5011";
	private static final String STOP_ID_5103 = "5103";
	private static final String STOP_ID_5106 = "5106";
	private static final String STOP_ID_5108 = "5108";
	private static final String STOP_ID_5210 = "5210";
	private static final String STOP_ID_5723 = "5723";
	private static final String STOP_ID_6005 = "6005";
	private static final String STOP_ID_6009 = "6009";
	private static final String STOP_ID_6124 = "6124";
	private static final String STOP_ID_6317 = "6317";
	private static final String STOP_ID_7003 = "7003";
	private static final String STOP_ID_7007 = "7007";
	private static final String STOP_ID_7011 = "7011";
	private static final String STOP_ID_7101 = "7101";
	private static final String STOP_ID_7106 = "7106";
	private static final String STOP_ID_7902 = "7902";
	private static final String STOP_ID_8601 = "8601";
	private static final String STOP_ID_9226 = "9226";
	private static final String STOP_ID_9242 = "9242";
	private static final String STOP_ID_9301 = "9301";
	private static final String STOP_ID_9756 = "9756";

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS;
	static {
		HashMap<Long, RouteTripSpec> map = new HashMap<Long, RouteTripSpec>();
		map.put(1l, new RouteTripSpec(1l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO) //
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_ID_2301, STOP_ID_5009) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_ID_5009, STOP_ID_2301) //
		);
		map.put(2l, new RouteTripSpec(2l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.id, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.id) //
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_ID_7902, STOP_ID_5723) //
				.addFromTo(MDirectionType.WEST.intValue(), STOP_ID_1266, STOP_ID_5003) //
				.addFromTo(MDirectionType.WEST.intValue(), STOP_ID_1407, STOP_ID_5003) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_ID_5723, STOP_ID_7902) //
				.addFromTo(MDirectionType.EAST.intValue(), STOP_ID_5008, STOP_ID_1256) //
				.addFromTo(MDirectionType.EAST.intValue(), STOP_ID_5008, STOP_ID_1336) //
				.addFromTo(MDirectionType.EAST.intValue(), STOP_ID_5008, STOP_ID_1561) //
				.addBothFromTo(MDirectionType.WEST.intValue(), STOP_ID_7902, STOP_ID_7902) // 5723
				.addBothFromTo(MDirectionType.WEST.intValue(), STOP_ID_1561, STOP_ID_1336) // 5723
				.addBothFromTo(MDirectionType.WEST.intValue(), STOP_ID_1561, STOP_ID_5008) // 5723
				.addBothFromTo(MDirectionType.WEST.intValue(), STOP_ID_5003, STOP_ID_1561) // 5723
		);
		map.put(3l, new RouteTripSpec(3l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CROMDALE) //
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_ID_1147, STOP_ID_5106) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_ID_5106, STOP_ID_1147) //
		);
		map.put(4l, new RouteTripSpec(4l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEM_LEWIS_FARMS, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO) //
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_ID_2306, STOP_ID_8601) //
				.addFromTo(MDirectionType.WEST.intValue(), STOP_ID_2159, STOP_ID_5006) //
				.addFromTo(MDirectionType.WEST.intValue(), STOP_ID_2002, STOP_ID_5006) //
				.addFromTo(MDirectionType.WEST.intValue(), STOP_ID_2159, STOP_ID_2001) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_ID_8601, STOP_ID_2306) //
				.addFromTo(MDirectionType.EAST.intValue(), STOP_ID_5006, STOP_ID_2447) //
				.addFromTo(MDirectionType.EAST.intValue(), STOP_ID_5006, STOP_ID_2002) //
				.addFromTo(MDirectionType.EAST.intValue(), STOP_ID_5006, STOP_ID_2549) //
				.addBothFromTo(MDirectionType.EAST.intValue(), STOP_ID_5006, STOP_ID_5006) //
				.addBothFromTo(MDirectionType.EAST.intValue(), STOP_ID_5006, STOP_ID_5003) //
		);
		map.put(5l, new RouteTripSpec(5l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLISEUM) //
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_ID_1202, STOP_ID_5210) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_ID_5210, STOP_ID_1202) //
		);
		map.put(6l, new RouteTripSpec(6l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_ID_3215, STOP_ID_2203) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_ID_2203, STOP_ID_3215) //
				.addBothFromTo(MDirectionType.WEST.intValue(), STOP_ID_2109, STOP_ID_2109) // 2203
		);
		map.put(7l, new RouteTripSpec(7l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY) //
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_ID_2002, STOP_ID_5108) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_ID_5108, STOP_ID_2002) //
		);
		map.put(8l, new RouteTripSpec(8l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ABBOTTSFIELD, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addALLFromTo(MDirectionType.NORTH.intValue(), STOP_ID_3207, STOP_ID_1001) //
				.addFromTo(MDirectionType.NORTH.intValue(), STOP_ID_3008, STOP_ID_1989) //
				.addALLFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_1001, STOP_ID_3207) //
				.addFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_1557, STOP_ID_2103) //
				.addFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_1075, STOP_ID_2103) //
		);
		map.put(9l, new RouteTripSpec(9l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addALLFromTo(MDirectionType.NORTH.intValue(), STOP_ID_2218, STOP_ID_6317) //
				.addALLFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_6317, STOP_ID_2218) //
				.addFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_1532, STOP_ID_1142) //
		);
		map.put(10l, new RouteTripSpec(10l, //
				0, MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW, //
				1, MTrip.HEADSIGN_TYPE_STRING, COLISEUM) //
				.addALLFromTo(0, STOP_ID_1203, STOP_ID_7101) //
				.addALLFromTo(1, STOP_ID_7101, STOP_ID_1203) //
		);
		map.put(11l, new RouteTripSpec(11l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW) //
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_ID_7106, STOP_ID_7007) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_ID_7007, STOP_ID_7106) //
		);
		map.put(12l, new RouteTripSpec(12l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addFromTo(MDirectionType.NORTH.intValue(), STOP_ID_1109, STOP_ID_7003) //
				.addFromTo(MDirectionType.NORTH.intValue(), STOP_ID_1476, STOP_ID_7003) //
				.addFromTo(MDirectionType.NORTH.intValue(), STOP_ID_1251, STOP_ID_7003) //
				.addFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_7003, STOP_ID_1109) //
				.addFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_7003, STOP_ID_1251) //
				.addFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_7003, STOP_ID_1476) //
				.addBothFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_7003, STOP_ID_7003) //
		);
		map.put(13l, new RouteTripSpec(13l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE) //
				.addALLFromTo(MDirectionType.NORTH.intValue(), STOP_ID_7011, STOP_ID_6005) //
				.addALLFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_6005, STOP_ID_7011) //
		);
		map.put(14l, new RouteTripSpec(14l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_JASPER_PLACE) //
				.addBothFromTo(MDirectionType.WEST.intValue(), STOP_ID_5103, STOP_ID_5103) // 5011
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_ID_1123, STOP_ID_5011) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_ID_5011, STOP_ID_1999) //
		);
		map.put(15l, new RouteTripSpec(15l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addALLFromTo(MDirectionType.NORTH.intValue(), STOP_ID_3208, STOP_ID_6317) //
				.addFromTo(MDirectionType.NORTH.intValue(), STOP_ID_2117, STOP_ID_1532) //
				.addALLFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_6317, STOP_ID_3208) //
				.addFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_1532, STOP_ID_2118) //
		);
		map.put(16l, new RouteTripSpec(16l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, GOV_CTR) //
				.addALLFromTo(MDirectionType.NORTH.intValue(), STOP_ID_1310, STOP_ID_6009) //
				.addALLFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_6009, STOP_ID_1310) //
				.addFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_6124, STOP_ID_7003) //
		);
		map.put(17l, new RouteTripSpec(17l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addALLFromTo(MDirectionType.NORTH.intValue(), STOP_ID_4203, STOP_ID_2206) //
				.addALLFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_2206, STOP_ID_4203) //
		);
		map.put(23l, new RouteTripSpec(23l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_ID_3217, STOP_ID_5001) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_ID_5001, STOP_ID_3217) //
		);
		map.put(24l, new RouteTripSpec(24l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_ID_4201, STOP_ID_4806) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_ID_4806, STOP_ID_4201) //
		);
		map.put(30l, new RouteTripSpec(30l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, S_CAMPUS_FORT_EDM, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_CENTURY_PK) //
				.addFromTo(MDirectionType.NORTH.intValue(), STOP_ID_4811, STOP_ID_2704) //
				.addFromTo(MDirectionType.NORTH.intValue(), STOP_ID_3217, STOP_ID_2704) //
				.addFromTo(MDirectionType.NORTH.intValue(), STOP_ID_3217, STOP_ID_4211) //
				.addFromTo(MDirectionType.NORTH.intValue(), STOP_ID_4211, STOP_ID_4811) //
				.addFromTo(MDirectionType.NORTH.intValue(), STOP_ID_4211, STOP_ID_2704) //
				.addFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_2704, STOP_ID_4811) //
				.addFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_4803, STOP_ID_3217) //
				.addFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_2704, STOP_ID_4803) //
				.addFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_2704, STOP_ID_3217) //
				.addFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_4803, STOP_ID_4202) //
				.addBothFromTo(MDirectionType.NORTH.intValue(), STOP_ID_4811, STOP_ID_4811) // 2704
				.addBothFromTo(MDirectionType.NORTH.intValue(), STOP_ID_4811, STOP_ID_4202) // 2704
				.addBothFromTo(MDirectionType.NORTH.intValue(), STOP_ID_3217, STOP_ID_4803) // 2704
		);
		map.put(31l, new RouteTripSpec(31l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addALLFromTo(MDirectionType.NORTH.intValue(), STOP_ID_4813, STOP_ID_2208) //
				.addALLFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_2208, STOP_ID_4813) //
		);
		map.put(32l, new RouteTripSpec(32l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BRANDER_GDNS, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, S_CAMPUS_FORT_EDM) //
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_ID_2705, STOP_ID_4025) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_ID_4025, STOP_ID_2705) //
				.addBothFromTo(MDirectionType.WEST.intValue(), STOP_ID_2705, STOP_ID_2705) // 4025
		);
		map.put(33l, new RouteTripSpec(33l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MEADOWS_TC) //
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_ID_3713, STOP_ID_5001) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_ID_5001, STOP_ID_3713) //
				.addBothFromTo(MDirectionType.WEST.intValue(), STOP_ID_2205, STOP_ID_2205) // 5001
				.addBothFromTo(MDirectionType.WEST.intValue(), STOP_ID_2205, STOP_ID_2215) // 5001
		);
		map.put(34l, new RouteTripSpec(34l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_ID_2209, STOP_ID_4809) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_ID_4809, STOP_ID_2209) //
		);
		map.put(35l, new RouteTripSpec(35l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_ID_4215, STOP_ID_4812) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_ID_4812, STOP_ID_4215) //
		);
		map.put(36l, new RouteTripSpec(36l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, S_CAMPUS_FORT_EDM, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addALLFromTo(MDirectionType.NORTH.intValue(), STOP_ID_4211, STOP_ID_2703) //
				.addALLFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_2703, STOP_ID_4211) //
		);
		map.put(37l, new RouteTripSpec(37l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_ID_4215, STOP_ID_4802) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_ID_4802, STOP_ID_4215) //
		);
		map.put(38l, new RouteTripSpec(38l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_ID_2207, STOP_ID_4805) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_ID_4805, STOP_ID_2207) //
		);
		map.put(39l, new RouteTripSpec(39l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, RUTHERFORD) //
				.addALLFromTo(MDirectionType.NORTH.intValue(), STOP_ID_9242, STOP_ID_4213) //
				.addALLFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_4213, STOP_ID_9242) //
		);
		map.put(40l, new RouteTripSpec(40l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, YELLOWBIRD_CENTURY_PK) //
				.addALLFromTo(MDirectionType.NORTH.intValue(), STOP_ID_4290, STOP_ID_2211) //
				.addALLFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_2211, STOP_ID_4290) //
		);
		map.put(41l, new RouteTripSpec(41l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addALLFromTo(MDirectionType.NORTH.intValue(), STOP_ID_4208, STOP_ID_2213) //
				.addALLFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_2213, STOP_ID_4208) //
		);
		map.put(42l, new RouteTripSpec(42l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addALLFromTo(MDirectionType.NORTH.intValue(), STOP_ID_4209, STOP_ID_2217) //
				.addALLFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_2217, STOP_ID_4209) //
		);
		map.put(43l, new RouteTripSpec(43l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, S_CAMPUS_FORT_EDM, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addALLFromTo(MDirectionType.NORTH.intValue(), STOP_ID_4214, STOP_ID_2711) //
				.addALLFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_2711, STOP_ID_4214) //
		);
		map.put(44l, new RouteTripSpec(44l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, TWIN_BROOKS) //
				.addALLFromTo(MDirectionType.NORTH.intValue(), STOP_ID_4265, STOP_ID_2204) //
				.addALLFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_2204, STOP_ID_4265) //
				.addFromTo(MDirectionType.NORTH.intValue(), STOP_ID_4265, STOP_ID_4204) //
				.addFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_4204, STOP_ID_4265) //
				.addBothFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_4204, STOP_ID_4204) // 4265
		);
		map.put(45l, new RouteTripSpec(45l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addALLFromTo(MDirectionType.NORTH.intValue(), STOP_ID_4207, STOP_ID_2214) //
				.addALLFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_2214, STOP_ID_4207) //
		);
		map.put(46l, new RouteTripSpec(46l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HARRY_AINLAY_LP, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, YELLOWBIRD) //
				.addFromTo(MDirectionType.NORTH.intValue(), STOP_ID_4290, STOP_ID_4307) //
				.addFromTo(MDirectionType.NORTH.intValue(), STOP_ID_4209, STOP_ID_4307) //
				.addFromTo(MDirectionType.NORTH.intValue(), STOP_ID_4290, STOP_ID_4209) //
				.addFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_4490, STOP_ID_4290) //
				.addFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_4208, STOP_ID_4290) //
				.addBothFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_4208, STOP_ID_4307) // 4290
				.addBothFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_4490, STOP_ID_4209) // 4290
		);
		map.put(47l, new RouteTripSpec(47l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ALLARD) //
				.addALLFromTo(MDirectionType.NORTH.intValue(), STOP_ID_9301, STOP_ID_4206) //
				.addALLFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_4206, STOP_ID_9301) //
		);
		map.put(48l, new RouteTripSpec(48l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BLACKBURN) //
				.addALLFromTo(MDirectionType.NORTH.intValue(), STOP_ID_9226, STOP_ID_4204) //
				.addALLFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_4204, STOP_ID_9226) //
		);
		map.put(49l, new RouteTripSpec(49l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BLACKMUD_CRK) //
				.addALLFromTo(MDirectionType.NORTH.intValue(), STOP_ID_9756, STOP_ID_4210) //
				.addALLFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_4210, STOP_ID_9756) //
		);
		map.put(50l, new RouteTripSpec(50l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, S_CAMPUS_FORT_EDM, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addALLFromTo(MDirectionType.NORTH.intValue(), STOP_ID_2210, STOP_ID_2710) //
				.addALLFromTo(MDirectionType.SOUTH.intValue(), STOP_ID_2710, STOP_ID_2210) //
		);
		ALL_ROUTE_TRIPS = map;
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, HashSet<MTrip> splitTrips, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS.containsKey(mRoute.id)) {
			RouteTripSpec rts = ALL_ROUTE_TRIPS.get(mRoute.id);
			return splitTripStop(gTrip, gTripStop, gtfs, //
					rts.getBeforeAfterStopIds(0), //
					rts.getBeforeAfterStopIds(1), //
					rts.getBeforeAfterBothStopIds(0), //
					rts.getBeforeAfterBothStopIds(1), //
					rts.getTripId(0), //
					rts.getTripId(1), //
					rts.getAllBeforeAfterStopIds());
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, gtfs);
	}

	private Pair<Long[], Integer[]> splitTripStop(GTrip gTrip, GTripStop gTripStop, GSpec gtfs, List<String> stopIdsTowards1, List<String> stopIdsTowards2,
			List<String> stopIdsTowardsBoth21, List<String> stopIdsTowardsBoth12, long tidTowardsStop1, long tidTowardsStop2, List<String> allBeforeAfterStopIds) {
		String beforeAfter = getBeforeAfterStopId(gtfs, gTrip, gTripStop, stopIdsTowards1, stopIdsTowards2, stopIdsTowardsBoth21, stopIdsTowardsBoth12,
				allBeforeAfterStopIds);
		if (stopIdsTowards1.contains(beforeAfter)) {
			return new Pair<Long[], Integer[]>(new Long[] { tidTowardsStop1 }, new Integer[] { gTripStop.stop_sequence });
		} else if (stopIdsTowards2.contains(beforeAfter)) {
			return new Pair<Long[], Integer[]>(new Long[] { tidTowardsStop2 }, new Integer[] { gTripStop.stop_sequence });
		} else if (stopIdsTowardsBoth21.contains(beforeAfter)) {
			return new Pair<Long[], Integer[]>(new Long[] { tidTowardsStop2, tidTowardsStop1 }, new Integer[] { 1, gTripStop.stop_sequence });
		} else if (stopIdsTowardsBoth12.contains(beforeAfter)) {
			return new Pair<Long[], Integer[]>(new Long[] { tidTowardsStop1, tidTowardsStop2 }, new Integer[] { 1, gTripStop.stop_sequence });
		}
		System.out.println("Unexptected trip stop to split " + gTripStop);
		System.exit(-1);
		return null;
	}

	private String getBeforeAfterStopId(GSpec gtfs, GTrip gTrip, GTripStop gTripStop, List<String> stopIdsTowards1, List<String> stopIdsTowards2,
			List<String> stopIdsTowardsBoth21, List<String> stopIdsTowardsBoth12, List<String> allBeforeAfterStopIds) {
		int gStopMaxSequence = -1;
		ArrayList<String> afterStopIds = new ArrayList<String>();
		ArrayList<Integer> afterStopSequence = new ArrayList<Integer>();
		ArrayList<String> beforeStopIds = new ArrayList<String>();
		ArrayList<Integer> beforeStopSequence = new ArrayList<Integer>();
		ArrayList<Pair<String, Integer>> gTripStops = new ArrayList<Pair<String, Integer>>(); // DEBUG
		for (GStopTime gStopTime : gtfs.stopTimes) {
			if (!gStopTime.trip_id.equals(gTrip.getTripId())) {
				continue;
			}
			gTripStops.add(new Pair<String, Integer>(gStopTime.stop_id, gStopTime.stop_sequence)); // DEBUG
			if (allBeforeAfterStopIds.contains(gStopTime.stop_id)) {
				if (gStopTime.stop_sequence < gTripStop.stop_sequence) {
					beforeStopIds.add(gStopTime.stop_id);
					beforeStopSequence.add(gStopTime.stop_sequence);
				}
				if (gStopTime.stop_sequence > gTripStop.stop_sequence) {
					afterStopIds.add(gStopTime.stop_id);
					afterStopSequence.add(gStopTime.stop_sequence);
				}
			}
			if (gStopTime.stop_sequence > gStopMaxSequence) {
				gStopMaxSequence = gStopTime.stop_sequence;
			}
		}
		if (allBeforeAfterStopIds.contains(gTripStop.stop_id)) {
			if (gTripStop.stop_sequence == 1) {
				beforeStopIds.add(gTripStop.stop_id);
				beforeStopSequence.add(gTripStop.stop_sequence);
			}
			if (gTripStop.stop_sequence == gStopMaxSequence) {
				afterStopIds.add(gTripStop.stop_id);
				afterStopSequence.add(gTripStop.stop_sequence);
			}
		}
		String beforeAfterStopIdCandidate = findBeforeAfterStopIdCandidate(gTripStop, stopIdsTowards1, stopIdsTowards2, stopIdsTowardsBoth21,
				stopIdsTowardsBoth12, afterStopIds, afterStopSequence, beforeStopIds, beforeStopSequence);
		if (beforeAfterStopIdCandidate != null) {
			return beforeAfterStopIdCandidate;
		}
		System.out.println("Unexpected trip (befores:" + beforeStopIds + "|afters:" + afterStopIds + ") " + gTrip);
		System.exit(-1);
		return null;
	}

	private String findBeforeAfterStopIdCandidate(GTripStop gTripStop, List<String> stopIdsTowards1, List<String> stopIdsTowards2,
			List<String> stopIdsTowardsBoth21, List<String> stopIdsTowardsBoth12, ArrayList<String> afterStopIds, ArrayList<Integer> afterStopSequence,
			ArrayList<String> beforeStopIds, ArrayList<Integer> beforeStopSequence) {
		String beforeAfterStopIdCurrent;
		Pair<Integer, String> beforeAfterStopIdCandidate = null;
		String beforeStopId, afterStopId;
		for (int b = 0; b < beforeStopIds.size(); b++) {
			beforeStopId = beforeStopIds.get(b);
			for (int a = 0; a < afterStopIds.size(); a++) {
				afterStopId = afterStopIds.get(a);
				beforeAfterStopIdCurrent = beforeStopId + DASH + afterStopId;
				if (stopIdsTowards1.contains(beforeAfterStopIdCurrent) || stopIdsTowards2.contains(beforeAfterStopIdCurrent)) {
					int size = Math.max(afterStopSequence.get(a) - gTripStop.stop_sequence, gTripStop.stop_sequence - beforeStopSequence.get(b));
					if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
						beforeAfterStopIdCandidate = new Pair<Integer, String>(size, beforeAfterStopIdCurrent);
					}
				}
			}
		}
		for (int b = 0; b < beforeStopIds.size(); b++) {
			beforeStopId = beforeStopIds.get(b);
			beforeAfterStopIdCurrent = beforeStopId + DASH + ALL;
			if (stopIdsTowards1.contains(beforeAfterStopIdCurrent) || stopIdsTowards2.contains(beforeAfterStopIdCurrent)) {
				int size = gTripStop.stop_sequence - beforeStopSequence.get(b);
				if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
					beforeAfterStopIdCandidate = new Pair<Integer, String>(size, beforeAfterStopIdCurrent);
				}
			}
		}
		for (int a = 0; a < afterStopIds.size(); a++) {
			afterStopId = afterStopIds.get(a);
			beforeAfterStopIdCurrent = ALL + DASH + afterStopId;
			if (stopIdsTowards1.contains(beforeAfterStopIdCurrent) || stopIdsTowards2.contains(beforeAfterStopIdCurrent)) {
				int size = afterStopSequence.get(a) - gTripStop.stop_sequence;
				if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
					beforeAfterStopIdCandidate = new Pair<Integer, String>(size, beforeAfterStopIdCurrent);
				}
			}
		}
		for (int b = 0; b < beforeStopIds.size(); b++) {
			beforeStopId = beforeStopIds.get(b);
			for (int a = 0; a < afterStopIds.size(); a++) {
				afterStopId = afterStopIds.get(a);
				if (gTripStop.stop_id.equals(beforeStopId) && gTripStop.stop_id.equals(afterStopId)) {
					continue;
				}
				beforeAfterStopIdCurrent = beforeStopId + DASH + afterStopId;
				if (stopIdsTowardsBoth21.contains(beforeAfterStopIdCurrent) || stopIdsTowardsBoth12.contains(beforeAfterStopIdCurrent)) {
					int size = Math.max(afterStopSequence.get(a) - gTripStop.stop_sequence, gTripStop.stop_sequence - beforeStopSequence.get(b));
					if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
						beforeAfterStopIdCandidate = new Pair<Integer, String>(size, beforeAfterStopIdCurrent);
					}
				}
			}
		}
		for (int b = 0; b < beforeStopIds.size(); b++) {
			beforeStopId = beforeStopIds.get(b);
			beforeAfterStopIdCurrent = beforeStopId + DASH + ALL;
			if (stopIdsTowardsBoth21.contains(beforeAfterStopIdCurrent) || stopIdsTowardsBoth12.contains(beforeAfterStopIdCurrent)) {
				int size = gTripStop.stop_sequence - beforeStopSequence.get(b);
				if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
					beforeAfterStopIdCandidate = new Pair<Integer, String>(size, beforeAfterStopIdCurrent);
				}
			}
		}
		for (int a = 0; a < afterStopIds.size(); a++) {
			afterStopId = afterStopIds.get(a);
			beforeAfterStopIdCurrent = ALL + DASH + afterStopId;
			if (stopIdsTowardsBoth21.contains(beforeAfterStopIdCurrent) || stopIdsTowardsBoth12.contains(beforeAfterStopIdCurrent)) {
				int size = afterStopSequence.get(a) - gTripStop.stop_sequence;
				if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
					beforeAfterStopIdCandidate = new Pair<Integer, String>(size, beforeAfterStopIdCurrent);
				}
			}
		}
		return beforeAfterStopIdCandidate == null ? null : beforeAfterStopIdCandidate.second;
	}



	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		return MSpec.cleanLabel(tripHeadsign);
	}

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = MSpec.cleanStreetTypes(gStopName);
		gStopName = MSpec.cleanNumbers(gStopName);
		return MSpec.cleanLabel(gStopName);
	}

	private static class RouteTripSpec {

		private long routeId;
		private int directionId0;
		private int headsignType0;
		private String headsignString0;
		private int directionId1;
		private int headsignType1;
		private String headsignString1;

		public RouteTripSpec(long routeId, int directionId0, int headsignType0, String headsignString0, int directionId1, int headsignType1,
				String headsignString1) {
			this.routeId = routeId;
			this.directionId0 = directionId0;
			this.headsignType0 = headsignType0;
			this.headsignString0 = headsignString0;
			this.directionId1 = directionId1;
			this.headsignType1 = headsignType1;
			this.headsignString1 = headsignString1;
		}

		private ArrayList<String> allBeforeAfterStopIds = new ArrayList<String>();

		public ArrayList<String> getAllBeforeAfterStopIds() {
			return this.allBeforeAfterStopIds;
		}

		public long getTripId(int directionIndex) {
			switch (directionIndex) {
			case 0:
				return MTrip.getNewId(this.routeId, this.directionId0);
			case 1:
				return MTrip.getNewId(this.routeId, this.directionId1);
			default:
				System.out.println("getTripId() > Unexpected direction index: " + directionIndex);
				System.exit(-1);
				return -1l;
			}
		}

		private HashMap<Integer, ArrayList<String>> beforeAfterStopIds = new HashMap<Integer, ArrayList<String>>();

		public ArrayList<String> getBeforeAfterStopIds(int directionIndex) {
			switch (directionIndex) {
			case 0:
				if (!this.beforeAfterStopIds.containsKey(this.directionId0)) {
					this.beforeAfterStopIds.put(this.directionId0, new ArrayList<String>());
				}
				return this.beforeAfterStopIds.get(this.directionId0);
			case 1:
				if (!this.beforeAfterStopIds.containsKey(this.directionId1)) {
					this.beforeAfterStopIds.put(this.directionId1, new ArrayList<String>());
				}
				return this.beforeAfterStopIds.get(this.directionId1);
			default:
				System.out.println("getBeforeAfterStopIds() > Unexpected direction index: " + directionIndex);
				System.exit(-1);
				return null;
			}
		}

		private HashMap<Integer, ArrayList<String>> beforeAfterBothStopIds = new HashMap<Integer, ArrayList<String>>();

		public ArrayList<String> getBeforeAfterBothStopIds(int directionIndex) {
			switch (directionIndex) {
			case 0:
				if (!this.beforeAfterBothStopIds.containsKey(this.directionId0)) {
					this.beforeAfterBothStopIds.put(this.directionId0, new ArrayList<String>());
				}
				return this.beforeAfterBothStopIds.get(this.directionId0);
			case 1:
				if (!this.beforeAfterBothStopIds.containsKey(this.directionId1)) {
					this.beforeAfterBothStopIds.put(this.directionId1, new ArrayList<String>());
				}
				return this.beforeAfterBothStopIds.get(this.directionId1);
			default:
				System.out.println("getBeforeAfterBothStopIds() > Unexpected direction index: " + directionIndex);
				System.exit(-1);
				return null;
			}
		}

		private HashSet<MTrip> allTrips = null;

		public HashSet<MTrip> getAllTrips() {
			if (this.allTrips == null) {
				initAllTrips();
			}
			return this.allTrips;
		}

		private void initAllTrips() {
			this.allTrips = new HashSet<MTrip>();
			if (this.headsignType0 == MTrip.HEADSIGN_TYPE_STRING) {
				this.allTrips.add(new MTrip(this.routeId).setHeadsignString(this.headsignString0, this.directionId0));
			} else if (this.headsignType0 == MTrip.HEADSIGN_TYPE_DIRECTION) {
				this.allTrips.add(new MTrip(this.routeId).setHeadsignDirection(MDirectionType.parse(this.headsignString0)));
			} else {
				System.out.println("Unexpected trip type " + this.headsignType0 + " for " + this.routeId);
				System.exit(-1);
			}
			if (this.headsignType1 == MTrip.HEADSIGN_TYPE_STRING) {
				this.allTrips.add(new MTrip(this.routeId).setHeadsignString(this.headsignString1, this.directionId1));
			} else if (this.headsignType1 == MTrip.HEADSIGN_TYPE_DIRECTION) {
				this.allTrips.add(new MTrip(this.routeId).setHeadsignDirection(MDirectionType.parse(this.headsignString1)));
			} else {
				System.out.println("Unexpected trip type " + this.headsignType1 + " for " + this.routeId);
				System.exit(-1);
			}
		}

		public RouteTripSpec addALLFromTo(int directionId, String stopIdFrom, String stopIdTo) {
			addBeforeAfter(directionId, stopIdFrom + DASH + ALL);
			addBeforeAfter(directionId, ALL + DASH + stopIdTo);
			addBeforeAfter(directionId, stopIdFrom + DASH + stopIdTo);
			this.allBeforeAfterStopIds.add(stopIdFrom);
			this.allBeforeAfterStopIds.add(stopIdTo);
			return this;
		}

		public RouteTripSpec addAllFrom(int directionId, String stopIdFrom) {
			addBeforeAfter(directionId, stopIdFrom + DASH + ALL);
			this.allBeforeAfterStopIds.add(stopIdFrom);
			return this;
		}

		public RouteTripSpec addAllTo(int directionId, String stopIdTo) {
			addBeforeAfter(directionId, ALL + DASH + stopIdTo);
			this.allBeforeAfterStopIds.add(stopIdTo);
			return this;
		}

		public RouteTripSpec addFromTo(int directionId, String stopIdFrom, String stopIdTo) {
			addBeforeAfter(directionId, stopIdFrom + DASH + stopIdTo);
			this.allBeforeAfterStopIds.add(stopIdFrom);
			this.allBeforeAfterStopIds.add(stopIdTo);
			return this;
		}

		private void addBeforeAfter(int directionId, String beforeAfterStopId) {
			if (!this.beforeAfterStopIds.containsKey(directionId)) {
				this.beforeAfterStopIds.put(directionId, new ArrayList<String>());
			}
			this.beforeAfterStopIds.get(directionId).add(beforeAfterStopId);
		}

		public RouteTripSpec addAllBothFrom(int directionId, String stopIdFrom) {
			addBeforeAfterBoth(directionId, stopIdFrom + DASH + ALL);
			this.allBeforeAfterStopIds.add(stopIdFrom);
			return this;
		}

		public RouteTripSpec addAllBothTo(int directionId, String stopIdTo) {
			addBeforeAfterBoth(directionId, ALL + DASH + stopIdTo);
			this.allBeforeAfterStopIds.add(stopIdTo);
			return this;
		}

		public RouteTripSpec addBothFromTo(int directionId, String stopIdFrom, String stopIdTo) {
			addBeforeAfterBoth(directionId, stopIdFrom + DASH + stopIdTo);
			this.allBeforeAfterStopIds.add(stopIdFrom);
			this.allBeforeAfterStopIds.add(stopIdTo);
			return this;
		}

		private void addBeforeAfterBoth(int directionId, String beforeAfterStopId) {
			if (!this.beforeAfterBothStopIds.containsKey(directionId)) {
				this.beforeAfterBothStopIds.put(directionId, new ArrayList<String>());
			}
			this.beforeAfterBothStopIds.get(directionId).add(beforeAfterStopId);
		}
	}
}
