package org.mtransit.parser.ca_edmonton_ets_bus;

import java.util.ArrayList;
import java.util.Arrays;
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
		System.out.printf("\nGenerating ETS bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("\nGenerating ETS bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
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

	private static final String SLASH = " / ";
	private static final String FORT = "Ft";
	private static final String EDMONTON = "Edm";
	private static final String EDM_GARRISON = EDMONTON + " Garrison";
	private static final String WEST_EDM_MALL = "West " + EDMONTON + " Mall";
	private static final String LEWIS_FARMS = "Lewis Farms";
	private static final String WEM_LEWIS_FARMS = WEST_EDM_MALL + SLASH + LEWIS_FARMS;
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
	private static final String _82_ST_132_AVE = "82 St / 132 Ave";
	private static final String _82_ST_132_AVE_EAUX_CLAIRES = _82_ST_132_AVE + SLASH + EAUX_CLAIRES;
	private static final String DOWNTOWN = "Downtown";
	private static final String DOWNTOWN_NORTHGATE = DOWNTOWN + SLASH + NORTHGATE;
	private static final String MILLGATE = "Millgate";
	private static final String MILLGATE_DOWNTOWN = MILLGATE + SLASH + DOWNTOWN;
	private static final String DOWNTOWN_JASPER_PLACE = DOWNTOWN + SLASH + JASPER_PLACE;
	private static final String GOV_CTR = "Gov Ctr";
	private static final String MAC_EWAN = "MacEwan";
	private static final String MAC_EWAN_GOV_CTR = MAC_EWAN + SLASH + GOV_CTR;
	private static final String CASTLE_DOWNS = "Castle Downs";
	private static final String CENTURY_PK = "Century Pk";
	private static final String YELLOWBIRD = "Yellowbird";
	private static final String YELLOWBIRD_CENTURY_PK = YELLOWBIRD + SLASH + CENTURY_PK;
	private static final String MILL_WOODS_CENTURY_PK = MILL_WOODS + SLASH + CENTURY_PK;
	private static final String S_CAMPUS = "S Campus";
	private static final String FT_EDM = FORT + " " + EDMONTON;
	private static final String S_CAMPUS_FT_EDM = S_CAMPUS + SLASH + FT_EDM;
	private static final String LEGER = "Leger";
	private static final String BRANDER_GDNS = "Brander Gdns";
	private static final String MEADOWS = "Meadows";
	private static final String BLACKMUD_CRK = "Blackmud Crk";
	private static final String BLACKBURN = "Blackburn";
	private static final String ALLARD = "Allard";
	private static final String HARRY_AINLAY_LP = "Harry Ainlay Lp";
	private static final String TWIN_BROOKS = "Twin Brooks";
	private static final String RUTHERFORD = "Rutherford";
	private static final String SOUTHWOOD = "Southwood";
	private static final String S_EDM_COMMON = "S " + EDMONTON + " Common";
	private static final String PARKALLEN = "Parkallen";
	private static final String KNOTTWOOD = "Knottwood";
	private static final String BELVEDERE = "Belvedere";
	private static final String BONNIE_DOON = "Bonnie Doon";
	private static final String LAUREL = "Laurel";
	private static final String PLYPOW = "Plypow";
	private static final String TAMARACK = "Tamarack";
	private static final String BRECKENRIDGE_GRNS = "Breckenridge Grns";
	private static final String WESTRIDGE = "Westridge";
	private static final String LESSARD = "Lessard";
	private static final String CAMERON_HTS = "Cameron Hts";
	private static final String LYMBURN = "Lymburn";
	private static final String WEDGEWOOD_HTS = "Wedgewood Hts";
	private static final String THE_GRANGE = "The Grange";
	private static final String RIO_TERRACE = "Rio Terrace";
	private static final String HAMPTONS = "Hamptons";
	private static final String WESTVIEW_VLG = "Westview Vlg";
	private static final String MISTATIM_IND = "Mistatim Ind";
	private static final String STADIUM = "Stadium";
	private static final String LAGO_LINDO = "Lago Lindo";
	private static final String MONTROSE = "Montrose";
	private static final String KINGSWAY_RAH = "Kingsway RAH";
	private static final String KING_EDWARD_PK = "King Edward Pk";
	private static final String RAPPERSWILL = "Rapperswill";
	private static final String OXFORD = "Oxford";
	private static final String _100_ST_160_AVE = "100 St / 160 Ave";
	private static final String _95_ST_132_AVE = "95 St / 132 Ave";
	private static final String CANOSSA = "Canossa";
	private static final String CHAMBERY = "Chambery";
	private static final String KERNOHAN = "Kernohan";
	private static final String LONDONDERRY = "Londonderry";
	private static final String EVERGREEN = "Evergreen";
	private static final String FRASER = "Fraser";
	private static final String FT_SASKATCHEWAN = FORT + " Saskatchewan";
	private static final String SPRUCE_GRV = "Spruce Grv";
	private static final String MC_CONACHIE = "McConachie";
	private static final String SCHONSEE = "Schonsee";
	private static final String BRINTNELL = "Brintnell";
	private static final String KLARVATTEN = "Klarvatten";
	private static final String RIVERDALE = "Riverdale";
	private static final String GOLD_BAR = "Gold Bar";
	private static final String JASPER_GATES = "Jasper Gates";
	private static final String SOUTH_PARK_CTR = "South Park Ctr";
	private static final String NORTHLANDS = "Northlands";

	@Override
	public String getRouteLongName(GRoute gRoute) {
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
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
		}
		if (ts1.getTripId() == 201l) { // 2 East
			if (STOP_1454.equals(ts1GStop.stop_code) && STOP_1561.equals(ts2GStop.stop_code)) {
				return -1;
			} else if (STOP_1561.equals(ts1GStop.stop_code) && STOP_1454.equals(ts2GStop.stop_code)) {
				return +1;
			}
		} else if (ts1.getTripId() == 401l) { // 2 East to Capilano
			if (STOP_2447.equals(ts1GStop.stop_code) && STOP_2549.equals(ts2GStop.stop_code)) {
				return -1;
			} else if (STOP_2549.equals(ts1GStop.stop_code) && STOP_2447.equals(ts2GStop.stop_code)) {
				return +1;
			}
		} else if (ts1.getTripId() == 402l) { // 2 West to Lewis
			if (STOP_5003.equals(ts1GStop.stop_code) && STOP_5006.equals(ts2GStop.stop_code)) {
				return -1;
			} else if (STOP_5006.equals(ts1GStop.stop_code) && STOP_5003.equals(ts2GStop.stop_code)) {
				return +1;
			}
		} else if (ts1.getTripId() == 601l) { // 6 East to Mill Woods
			if (STOP_2102.equals(ts1GStop.stop_code) && STOP_2109.equals(ts2GStop.stop_code)) {
				return -1;
			} else if (STOP_2109.equals(ts1GStop.stop_code) && STOP_2102.equals(ts2GStop.stop_code)) {
				return +1;
			}
		} else if (ts1.getTripId() == 602l) { // 6 West to Southgate
			if (STOP_2630.equals(ts1GStop.stop_code) && STOP_2888.equals(ts2GStop.stop_code)) {
				return -1;
			} else if (STOP_2888.equals(ts1GStop.stop_code) && STOP_2630.equals(ts2GStop.stop_code)) {
				return +1;
			}
		} else if (ts1.getTripId() == 1102l) { // 11 West to Northgate
			if (STOP_7007.equals(ts1GStop.stop_code) && STOP_7008.equals(ts2GStop.stop_code)) {
				return -1;
			} else if (STOP_7008.equals(ts1GStop.stop_code) && STOP_7007.equals(ts2GStop.stop_code)) {
				return +1;
			}
		} else if (ts1.getTripId() == 1203l) { // 12 North to Northgate
			if (STOP_1369.equals(ts1GStop.stop_code) && STOP_1128.equals(ts2GStop.stop_code)) {
				return -1;
			} else if (STOP_1128.equals(ts1GStop.stop_code) && STOP_1369.equals(ts2GStop.stop_code)) {
				return +1;
			}
			if (STOP_1059.equals(ts1GStop.stop_code) && STOP_1730.equals(ts2GStop.stop_code)) {
				return -1;
			} else if (STOP_1730.equals(ts1GStop.stop_code) && STOP_1059.equals(ts2GStop.stop_code)) {
				return +1;
			}
		} else if (ts1.getTripId() == 1204l) { // 12 South to Downtown
			if (STOP_1113.equals(ts1GStop.stop_code) && STOP_1109.equals(ts2GStop.stop_code)) {
				return -1;
			} else if (STOP_1109.equals(ts1GStop.stop_code) && STOP_1113.equals(ts2GStop.stop_code)) {
				return +1;
			}
		} else if (ts1.getTripId() == 1401l) { // 14 East to WEM
			if (STOP_5293.equals(ts1GStop.stop_code) && STOP_5103.equals(ts2GStop.stop_code)) {
				return -1;
			} else if (STOP_5103.equals(ts1GStop.stop_code) && STOP_5293.equals(ts2GStop.stop_code)) {
				return +1;
			}
		} else if (ts1.getTripId() == 1503l) { // 15 North to Eaux Claire
			if (STOP_1476.equals(ts1GStop.stop_code) && STOP_1532.equals(ts2GStop.stop_code)) {
				return -1;
			} else if (STOP_1532.equals(ts1GStop.stop_code) && STOP_1476.equals(ts2GStop.stop_code)) {
				return +1;
			}
		} else if (ts1.getTripId() == 1604l) { // 16 South to Gov Ctr
			if (STOP_7003.equals(ts1GStop.stop_code) && STOP_7011.equals(ts2GStop.stop_code)) {
				return -1;
			} else if (STOP_7011.equals(ts1GStop.stop_code) && STOP_7003.equals(ts2GStop.stop_code)) {
				return +1;
			}
		} else if (ts1.getTripId() == 3301l) { // 33 East to Meadows
			if (STOP_2215.equals(ts1GStop.stop_code) && STOP_2205.equals(ts2GStop.stop_code)) {
				return -1;
			} else if (STOP_2205.equals(ts1GStop.stop_code) && STOP_2215.equals(ts2GStop.stop_code)) {
				return +1;
			}
		} else if (ts1.getTripId() == 3801l) { // 38 East to Southgate
			if (STOP_4938.equals(ts1GStop.stop_code) && STOP_4519.equals(ts2GStop.stop_code)) {
				return -1;
			} else if (STOP_4519.equals(ts1GStop.stop_code) && STOP_4938.equals(ts2GStop.stop_code)) {
				return +1;
			}
		} else if (ts1.getTripId() == 3802l) { // 38 West to Leger
			if (STOP_4320.equals(ts1GStop.stop_code) && STOP_4373.equals(ts2GStop.stop_code)) {
				return -1;
			} else if (STOP_4373.equals(ts1GStop.stop_code) && STOP_4320.equals(ts2GStop.stop_code)) {
				return +1;
			}
		} else if (ts1.getTripId() == 4403l) { // 44 North to Southgate
			if (STOP_4210.equals(ts1GStop.stop_code) && STOP_4204.equals(ts2GStop.stop_code)) {
				return -1;
			} else if (STOP_4204.equals(ts1GStop.stop_code) && STOP_4210.equals(ts2GStop.stop_code)) {
				return +1;
			}
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public int compare(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		return super.compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}


	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.id)) {
			return; // split
		}
		if (ALL_ROUTE_TRIPS.containsKey(mRoute.id)) {
			return; // split
		}
		if (mRoute.id == 167l) {
			String firstStopId = getFirstStopId(gtfs, gTrip);
			String lastStopId = getLastStopId(gtfs, gTrip);
			if ("7496".equals(firstStopId) && "6447".equals(lastStopId)) {
				mTrip.setHeadsignString(_100_ST_160_AVE, MDirectionType.NORTH.intValue());
				return;
			} else if ("6039".equals(firstStopId) && "7060".equals(lastStopId)) {
				mTrip.setHeadsignString(_95_ST_132_AVE, MDirectionType.SOUTH.intValue());
				return;
			}
		}
		System.out.printf("\n%s: Unexpected trip %s.\n", mRoute.id, gTrip);
		System.exit(-1);
	}

	@Override
	public HashSet<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.id)) {
			return ALL_ROUTE_TRIPS2.get(mRoute.id).getAllTrips();
		}
		if (ALL_ROUTE_TRIPS.containsKey(mRoute.id)) {
			return ALL_ROUTE_TRIPS.get(mRoute.id).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}


	private static final String DASH = "-";
	private static final String ALL = "*";

	private static final String STOP_1059 = "1059";
	private static final String STOP_1109 = "1109";
	private static final String STOP_1113 = "1113";
	private static final String STOP_1123 = "1123";
	private static final String STOP_1128 = "1128";
	private static final String STOP_1147 = "1147";
	private static final String STOP_1202 = "1202";
	private static final String STOP_1203 = "1203";
	private static final String STOP_1256 = "1256";
	private static final String STOP_1266 = "1266";
	private static final String STOP_1310 = "1310";
	private static final String STOP_1336 = "1336";
	private static final String STOP_1369 = "1369";
	private static final String STOP_1407 = "1407";
	private static final String STOP_1476 = "1476";
	private static final String STOP_1532 = "1532";
	private static final String STOP_1454 = "1454";
	private static final String STOP_1561 = "1561";
	private static final String STOP_1730 = "1730";
	private static final String STOP_1999 = "1999";
	private static final String STOP_2001 = "2001";
	private static final String STOP_2002 = "2002";
	private static final String STOP_2205 = "2205";
	private static final String STOP_2206 = "2206";
	private static final String STOP_2208 = "2208";
	private static final String STOP_2209 = "2209";
	private static final String STOP_2102 = "2102";
	private static final String STOP_2109 = "2109";
	private static final String STOP_2215 = "2215";
	private static final String STOP_2117 = "2117";
	private static final String STOP_2118 = "2118";
	private static final String STOP_2159 = "2159";
	private static final String STOP_2203 = "2203";
	private static final String STOP_2207 = "2207";
	private static final String STOP_2306 = "2306";
	private static final String STOP_2447 = "2447";
	private static final String STOP_2549 = "2549";
	private static final String STOP_2630 = "2630";
	private static final String STOP_2703 = "2703";
	private static final String STOP_2705 = "2705";
	private static final String STOP_2888 = "2888";
	private static final String STOP_3208 = "3208";
	private static final String STOP_3215 = "3215";
	private static final String STOP_3217 = "3217";
	private static final String STOP_3713 = "3713";
	private static final String STOP_4025 = "4025";
	private static final String STOP_4201 = "4201";
	private static final String STOP_4203 = "4203";
	private static final String STOP_4204 = "4204";
	private static final String STOP_4210 = "4210";
	private static final String STOP_4211 = "4211";
	private static final String STOP_4213 = "4213";
	private static final String STOP_4215 = "4215";
	private static final String STOP_4320 = "4320";
	private static final String STOP_4373 = "4373";
	private static final String STOP_4519 = "4519";
	private static final String STOP_4802 = "4802";
	private static final String STOP_4805 = "4805";
	private static final String STOP_4806 = "4806";
	private static final String STOP_4809 = "4809";
	private static final String STOP_4812 = "4812";
	private static final String STOP_4813 = "4813";
	private static final String STOP_4938 = "4938";
	private static final String STOP_5001 = "5001";
	private static final String STOP_5003 = "5003";
	private static final String STOP_5006 = "5006";
	private static final String STOP_5008 = "5008";
	private static final String STOP_5011 = "5011";
	private static final String STOP_5103 = "5103";
	private static final String STOP_5106 = "5106";
	private static final String STOP_5108 = "5108";
	private static final String STOP_5210 = "5210";
	private static final String STOP_5293 = "5293";
	private static final String STOP_5723 = "5723";
	private static final String STOP_6005 = "6005";
	private static final String STOP_6009 = "6009";
	private static final String STOP_6124 = "6124";
	private static final String STOP_6314 = "6314";
	private static final String STOP_6317 = "6317";
	private static final String STOP_7003 = "7003";
	private static final String STOP_7007 = "7007";
	private static final String STOP_7008 = "7008";
	private static final String STOP_7011 = "7011";
	private static final String STOP_7101 = "7101";
	private static final String STOP_7106 = "7106";
	private static final String STOP_7902 = "7902";
	private static final String STOP_8601 = "8601";
	private static final String STOP_9242 = "9242";


	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS;
	static {
		HashMap<Long, RouteTripSpec> map = new HashMap<Long, RouteTripSpec>();
		map.put(2l, new RouteTripSpec(2l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.id, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.id) //
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_7902, STOP_5723) //
				.addFromTo(MDirectionType.WEST.intValue(), STOP_1266, STOP_5003) //
				.addFromTo(MDirectionType.WEST.intValue(), STOP_1407, STOP_5003) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_5723, STOP_7902) //
				.addFromTo(MDirectionType.EAST.intValue(), STOP_5008, STOP_1256) //
				.addFromTo(MDirectionType.EAST.intValue(), STOP_5008, STOP_1336) //
				.addFromTo(MDirectionType.EAST.intValue(), STOP_5008, STOP_1561) //
				.addBothFromTo(MDirectionType.WEST.intValue(), STOP_7902, STOP_7902) // 5723
				.addBothFromTo(MDirectionType.WEST.intValue(), STOP_1561, STOP_1336) // 5723
				.addBothFromTo(MDirectionType.WEST.intValue(), STOP_1561, STOP_5008) // 5723
				.addBothFromTo(MDirectionType.WEST.intValue(), STOP_5003, STOP_1561) // 5723
		);
		map.put(3l, new RouteTripSpec(3l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CROMDALE) //
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_1147, STOP_5106) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_5106, STOP_1147) //
		);
		map.put(4l, new RouteTripSpec(4l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEM_LEWIS_FARMS, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO) //
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_2306, STOP_8601) //
				.addFromTo(MDirectionType.WEST.intValue(), STOP_2159, STOP_5006) //
				.addFromTo(MDirectionType.WEST.intValue(), STOP_2002, STOP_5006) //
				.addFromTo(MDirectionType.WEST.intValue(), STOP_2159, STOP_2001) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_8601, STOP_2306) //
				.addFromTo(MDirectionType.EAST.intValue(), STOP_5006, STOP_2447) //
				.addFromTo(MDirectionType.EAST.intValue(), STOP_5006, STOP_2002) //
				.addFromTo(MDirectionType.EAST.intValue(), STOP_5006, STOP_2549) //
				.addBothFromTo(MDirectionType.EAST.intValue(), STOP_5006, STOP_5006) //
				.addBothFromTo(MDirectionType.EAST.intValue(), STOP_5006, STOP_5003) //
		);
		map.put(5l, new RouteTripSpec(5l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLISEUM) //
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_1202, STOP_5210) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_5210, STOP_1202) //
		);
		map.put(6l, new RouteTripSpec(6l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_3215, STOP_2203) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_2203, STOP_3215) //
				.addBothFromTo(MDirectionType.WEST.intValue(), STOP_2109, STOP_2109) // 2203
		);
		map.put(7l, new RouteTripSpec(7l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY) //
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_2002, STOP_5108) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_5108, STOP_2002) //
		);
		map.put(10l, new RouteTripSpec(10l, //
				0, MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW, //
				1, MTrip.HEADSIGN_TYPE_STRING, COLISEUM) //
				.addALLFromTo(0, STOP_1203, STOP_7101) //
				.addALLFromTo(1, STOP_7101, STOP_1203) //
		);
		map.put(11l, new RouteTripSpec(11l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW) //
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_7106, STOP_7007) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_7007, STOP_7106) //
		);
		map.put(13l, new RouteTripSpec(13l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE) //
				.addALLFromTo(MDirectionType.NORTH.intValue(), STOP_7011, STOP_6005) //
				.addALLFromTo(MDirectionType.SOUTH.intValue(), STOP_6005, STOP_7011) //
		);
		map.put(14l, new RouteTripSpec(14l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_JASPER_PLACE) //
				.addBothFromTo(MDirectionType.WEST.intValue(), STOP_5103, STOP_5103) // 5011
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_1123, STOP_5011) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_5011, STOP_1999) //
		);
		map.put(15l, new RouteTripSpec(15l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addALLFromTo(MDirectionType.NORTH.intValue(), STOP_3208, STOP_6317) //
				.addFromTo(MDirectionType.NORTH.intValue(), STOP_2117, STOP_1532) //
				.addALLFromTo(MDirectionType.SOUTH.intValue(), STOP_6317, STOP_3208) //
				.addFromTo(MDirectionType.SOUTH.intValue(), STOP_1532, STOP_2118) //
		);
		map.put(16l, new RouteTripSpec(16l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, GOV_CTR) //
				.addFromTo(MDirectionType.NORTH.intValue(), STOP_1310, STOP_7011) //
				.addFromTo(MDirectionType.NORTH.intValue(), STOP_6314, STOP_6009) //
				.addFromTo(MDirectionType.NORTH.intValue(), STOP_7011, STOP_6009) //
				.addFromTo(MDirectionType.NORTH.intValue(), STOP_1310, STOP_6009) // 7011
				.addFromTo(MDirectionType.SOUTH.intValue(), STOP_6124, STOP_7003) //
				.addFromTo(MDirectionType.SOUTH.intValue(), STOP_6009, STOP_7011) //
				.addFromTo(MDirectionType.SOUTH.intValue(), STOP_6009, STOP_6317) //
				.addFromTo(MDirectionType.SOUTH.intValue(), STOP_6009, STOP_7003) //
				.addFromTo(MDirectionType.SOUTH.intValue(), STOP_6009, STOP_6124) //
				.addFromTo(MDirectionType.SOUTH.intValue(), STOP_6124, STOP_7003) //
				.addFromTo(MDirectionType.SOUTH.intValue(), STOP_7003, STOP_1310) //
				.addFromTo(MDirectionType.SOUTH.intValue(), STOP_6009, STOP_1310) // 7003
				.addBothFromTo(MDirectionType.SOUTH.intValue(), STOP_6009, STOP_6009) // 7011
		);
		map.put(17l, new RouteTripSpec(17l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addALLFromTo(MDirectionType.NORTH.intValue(), STOP_4203, STOP_2206) //
				.addALLFromTo(MDirectionType.SOUTH.intValue(), STOP_2206, STOP_4203) //
		);
		map.put(23l, new RouteTripSpec(23l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_3217, STOP_5001) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_5001, STOP_3217) //
		);
		map.put(24l, new RouteTripSpec(24l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_4201, STOP_4806) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_4806, STOP_4201) //
		);
		map.put(31l, new RouteTripSpec(31l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addALLFromTo(MDirectionType.NORTH.intValue(), STOP_4813, STOP_2208) //
				.addALLFromTo(MDirectionType.SOUTH.intValue(), STOP_2208, STOP_4813) //
		);
		map.put(32l, new RouteTripSpec(32l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BRANDER_GDNS, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, S_CAMPUS_FT_EDM) //
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_2705, STOP_4025) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_4025, STOP_2705) //
				.addBothFromTo(MDirectionType.WEST.intValue(), STOP_2705, STOP_2705) // 4025
		);
		map.put(33l, new RouteTripSpec(33l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MEADOWS) //
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_3713, STOP_5001) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_5001, STOP_3713) //
				.addBothFromTo(MDirectionType.WEST.intValue(), STOP_2205, STOP_2205) // 5001
				.addBothFromTo(MDirectionType.WEST.intValue(), STOP_2205, STOP_2215) // 5001
		);
		map.put(34l, new RouteTripSpec(34l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_2209, STOP_4809) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_4809, STOP_2209) //
		);
		map.put(35l, new RouteTripSpec(35l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_4215, STOP_4812) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_4812, STOP_4215) //
		);
		map.put(36l, new RouteTripSpec(36l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, S_CAMPUS_FT_EDM, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addALLFromTo(MDirectionType.NORTH.intValue(), STOP_4211, STOP_2703) //
				.addALLFromTo(MDirectionType.SOUTH.intValue(), STOP_2703, STOP_4211) //
		);
		map.put(37l, new RouteTripSpec(37l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_4215, STOP_4802) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_4802, STOP_4215) //
		);
		map.put(38l, new RouteTripSpec(38l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addALLFromTo(MDirectionType.WEST.intValue(), STOP_2207, STOP_4805) //
				.addALLFromTo(MDirectionType.EAST.intValue(), STOP_4805, STOP_2207) //
		);
		map.put(39l, new RouteTripSpec(39l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, RUTHERFORD) //
				.addALLFromTo(MDirectionType.NORTH.intValue(), STOP_9242, STOP_4213) //
				.addALLFromTo(MDirectionType.SOUTH.intValue(), STOP_4213, STOP_9242) //
		);
		ALL_ROUTE_TRIPS = map;
	}

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(1l, new RouteTripSpec(1l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5009", "5302", "5110", "1346", "2591", "2301" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2301", "2267", "1620", "5101", "5301", "5009" })) //
				.compileBothTripSort());
		map2.put(5l, new RouteTripSpec(5l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLISEUM, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5210", "1083", "1336", "1188",
						/* + */"1051"/* + */, //
								"1268", "1202"
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "1202", "1328", "1620", "5210"
						})) //
				.compileBothTripSort());
		map2.put(8l, new RouteTripSpec(8l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ABBOTTSFIELD, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3207",
						/* + */"3008"/* + */, // ?
								"2108", "2551", "1457",
								/* + */"1989"/* + */, // ?
								"1106",
								/* + */"1476"/* + */, //
								"1201", "1001" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1001", "1208",
						/* + */"1075"/* + */, // ?
								"1112",
								/* + */"1557"/* + */, // ?
								"1542", "2549", "2103", "3207" })) //
				.compileBothTripSort());
		map2.put(9l, new RouteTripSpec(9l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2218", "2875", "1591", "1108", "1476",
						/* + */"7465"/* + */, //
								/* + */"7001"/* + */, //
								"7016",
								/* + */"7448"/* + */, //
								"6317" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "6317",
						/* + */"6536"/* + */, //
								"7001", "1532", "1104", "1142", "2690", "2218" })) //
				.compileBothTripSort());
		map2.put(12l, new RouteTripSpec(12l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"1251", "1529", "1109", "1533", "1550", "1434", "1435", "1553", "1032", "7003"
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"7003", "6369", "1109", "1113", "1251"
						})) //
				.compileBothTripSort());
		map2.put(30l, new RouteTripSpec(30l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, S_CAMPUS_FT_EDM, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_CENTURY_PK) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3217", "4211", "4811", "4597", "4153", "2704" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2704", "4021", "4494", "4811", "4803", "4202", "3217" })) //
				.compileBothTripSort());
		map2.put(40l, new RouteTripSpec(40l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, YELLOWBIRD_CENTURY_PK) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "4290", "4206", "", "4480", "4474", "2211" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2211", "4052", "4475", "4205", "4290" })) //
				.compileBothTripSort());
		map2.put(41l, new RouteTripSpec(41l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "4208", "4168", "2213" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2213", "4486", "4208" })) //
				.compileBothTripSort());
		map2.put(42l, new RouteTripSpec(42l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "4209", "4070", "2217" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2217", "4342", "4209" })) //
				.compileBothTripSort());
		map2.put(43l, new RouteTripSpec(43l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, S_CAMPUS_FT_EDM, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "4214", "4156", "2973", "2711" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2711", "2511", "4378", "4214" })) //
				.compileBothTripSort());
		map2.put(44l, new RouteTripSpec(44l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, TWIN_BROOKS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "4265",
						/* + */"4233"/* + */, //
								"4204", "4210", "4362", "2204" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2204", "4198", "4204",
						/* + */"4348"/* + */, //
								"4265" })) //
				.compileBothTripSort());
		map2.put(45l, new RouteTripSpec(45l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "4207", "4588", "2214" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2214", "2888", "4198", "4207" })) //
				.compileBothTripSort());
		map2.put(46l, new RouteTripSpec(46l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HARRY_AINLAY_LP, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, YELLOWBIRD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "4290", "4209", "4307" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4490", "4208", "4290" })) //
				.compileBothTripSort());
		map2.put(47l, new RouteTripSpec(47l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ALLARD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "9301", "9163", "4206" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4206", "9164", "9301" })) //
				.compileBothTripSort());
		map2.put(48l, new RouteTripSpec(48l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BLACKBURN) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "9226",
						/* + */"4002"/* + */, //
								"4204" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4204",
						/* + */"9551"/* + */, //
								"9226" })) //
				.compileBothTripSort());
		map2.put(49l, new RouteTripSpec(49l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BLACKMUD_CRK) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "9756", "9542", "4210" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4210",
						/* + */"4105"/* + */, //
								"9756" })) //
				.compileBothTripSort());
		map2.put(50l, new RouteTripSpec(50l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, S_CAMPUS_FT_EDM, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2210", "4277", "2517", "2957", "2710" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2710", "2510", "2924", "4474", "2210" })) //
				.compileBothTripSort());
		map2.put(51l, new RouteTripSpec(51l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, PARKALLEN) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2795", /* + */"2861"/* + */, "2001" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2001", /* + */"2889"/* + */, "2795" })) //
				.compileBothTripSort());
		map2.put(52l, new RouteTripSpec(52l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2212", //
								"2849", //
								/* + */"2632"/* + */, //
								/* + */"2290"/* + */, //
								"1425",
								/* + */"1728"/* + */, //
								/* + */"1991"/* + */, //
								"1308", "1777", "1262" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"1262", "1305", //
								/* + */"1792"/* + */, //
								/* + */"1629"/* + */, //
								/* + */"1993"/* + */, //
								/* + */"1425"/* + */, //
								/* + */"1567"/* + */, //
								/* + */"2768"/* + */, //
								"2821", //
								/* + */"2665"/* + */, //
								"2212" })) //
				.compileBothTripSort());
		map2.put(53l, new RouteTripSpec(53l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, S_CAMPUS_FT_EDM, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2216", "2973", "2712" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2712", "2511", "2216" })) //
				.compileBothTripSort());
		map2.put(55l, new RouteTripSpec(55l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, S_CAMPUS_FT_EDM, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2202", "2830", "2709" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2709", "2966", "2202" })) //
				.compileBothTripSort());
		map2.put(57l, new RouteTripSpec(57l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2002", "2860", "2824", "1383", "1358" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "1358", "1383", "2659", "2891", "2002" })) //
				.compileBothTripSort());
		map2.put(59l, new RouteTripSpec(59l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, S_EDM_COMMON) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "3440", "3003", "3209" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3209", "3007", "3440" })) //
				.compileBothTripSort());
		map2.put(60l, new RouteTripSpec(60l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3210", "3585", "2104", "2101", "1780", "1989" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1824", "1780", "2104", "3233", "3210" })) //
				.compileBothTripSort());
		map2.put(61l, new RouteTripSpec(61l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3211", "3585", "2105", "2104", "1780", "1989" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1824", "1780", "2105", "3529", "3211" })) //
				.compileBothTripSort());
		map2.put(62l, new RouteTripSpec(62l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHWOOD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3154", "3161", "3203", "3212", "1780", /* + */"1804"/* + */, "1989" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1824", "1804", "1780", "3203", "3139", "3154" })) //
				.compileBothTripSort());
		map2.put(63l, new RouteTripSpec(63l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHWOOD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3143", "3067", "3204", "3212", "1383", "1358" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1358", "1383", "3204", "3080", "3143" })) //
				.compileBothTripSort());
		map2.put(64l, new RouteTripSpec(64l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, KNOTTWOOD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3026", "3006",
						/* + */"3599"/* + */, //
								"3001", "3208", "2111", "1358", "1383" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1383", "1358", "2112", "3208", "3009", "3026" })) //
				.compileBothTripSort());
		map2.put(65l, new RouteTripSpec(65l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, KNOTTWOOD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3023", "3006", "3001", "3208", "2111", "1989" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1824", "2112", "3208", "3009", "3023" })) //
				.compileBothTripSort());
		map2.put(66l, new RouteTripSpec(66l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3213", "3011", /* + */"3224"/* + */, "2101", "2105", "1358" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1358", "2101", /* + */"3371"/* + */, "3011", "3003", "3213" })) //
				.compileBothTripSort());
		map2.put(67l, new RouteTripSpec(67l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MEADOWS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "3206", "3952", "3957", "3708" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3708", "3957", "3950", "3311", "3116", "3206" })) //
				.compileBothTripSort());
		map2.put(68l, new RouteTripSpec(68l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addTripSort(MDirectionType.NORTH.intValue(), Arrays.asList(new String[] { "3202", "3399", "3586", "2107", "2110", "1989" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), Arrays.asList(new String[] { "1824", "2107", "3230", "3584", "3202" })) //
				.compileBothTripSort());
		map2.put(69l, new RouteTripSpec(69l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addTripSort(MDirectionType.NORTH.intValue(), Arrays.asList(new String[] { "3214", "3702", "2110", "2107", "1989" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), Arrays.asList(new String[] { "1824", "2110", "3710", "3214" })) //
				.compileBothTripSort());
		map2.put(70l, new RouteTripSpec(70l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3201", "3012", "3093", "2685", /* + */"2840"/* + */, /* + */"2659"/* + */, "2824", "2659", "1780" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1780", "2840", "2245", "3497", "3004", "3201" })) //
				.compileBothTripSort());
		map2.put(71l, new RouteTripSpec(71l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, GOV_CTR, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3216", /* + */"3224"/* + */, "2111", /* + */"1153"/* + */, "1303" }))
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1303", /* + */"1993"/* + */, "2103", /* + */"3370"/* + */, "3216" }))
				.compileBothTripSort());
		map2.put(72l, new RouteTripSpec(72l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE_DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3206", "3255", "3796", "3491", "2106", "2106", "2110", "1989" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1989", "2110", "2106", "3355", "3748", "3185", "3206" })) //
				.compileBothTripSort());
		map2.put(73l, new RouteTripSpec(73l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2203", "2888", "2102", "3002", "3205" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3205", "3010", "2109", "2203" })) //
				.compileBothTripSort());
		map2.put(74l, new RouteTripSpec(74l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2204", "4202",/* + */"3671"/* + */, "3107", "3559", "3209" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3209", "3007", "3430", "3110", "4202", "4212", "2204" })) //
				.compileBothTripSort());
		map2.put(78l, new RouteTripSpec(78l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "4205", "3675", "9384", "9725", "3215" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3215", "9147", "9387", "3865", "4205" })) //
				.compileBothTripSort());
		map2.put(79l, new RouteTripSpec(79l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "4207", "3319", "9260", "9139", "3214" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3214", "9287", "9671", "3513", "4207" })) //
				.compileBothTripSort());
		map2.put(80l, new RouteTripSpec(80l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2218", "2769", "2826", "2551", "2599", "2223", "2305" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2305", "2152", "2264", "2188", "2622", "2837", "2888", /* + */"2630"/* + */, "2218" })) //
				.compileBothTripSort());
		map2.put(81l, new RouteTripSpec(81l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3216", "2106", "2338", //
								/* + */"2697"/* + */, "2659",/* + */"2824"/* + */, //
								/* + */"1246"/* + */, "1383", "1358" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1358", "1383", /* + */"2835"/* + */, //
								/* + */"2878"/* + */, /* ? */"2659"/* ? */, "2840", //
								"2385", "2106", "2104", "3216" })) //
				.compileBothTripSort());
		map2.put(82l, new RouteTripSpec(82l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3212", "2339", "2551", "1383", "1358" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1358", "1383", "2255", "2528", "3212" })) //
				.compileBothTripSort());
		map2.put(83l, new RouteTripSpec(83l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MEADOWS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "1358", "1950", "2196", "2188", "2805", //
								/* + */"2362"/* + */, /* + */"2536"/* + */, //
								/* + */"2943"/* + */, /* + */"2286"/* + */, //
								"2693", "3706" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3706", "2693", //
								/* + */"2357"/* + */, /* + */"2148"/* + */, //
								"2804", "2551", "2196",/* + */"1457"/* + */, "1763", "1358" })) //
				.compileBothTripSort());
		map2.put(84l, new RouteTripSpec(84l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2111", "2303" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2303", "2112" })) //
				.compileBothTripSort());
		map2.put(85l, new RouteTripSpec(85l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "1358", "2073", "2386", /* + */"2985"/* + */, "2550", /* + */"2059"/* + */, "2307" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2307", "2442", "1358" })) //
				.compileBothTripSort());
		map2.put(86l, new RouteTripSpec(86l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "1358", "2073", "2302" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2302", "2442", "1358" })) //
				.compileBothTripSort());
		map2.put(87l, new RouteTripSpec(87l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2106", "2338", "2824", "1383", "1358" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1358", "1383", "2285", "2385", "2106" })) //
				.compileBothTripSort());
		map2.put(88l, new RouteTripSpec(88l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "1680", "1336", "2274", "2449", "2307" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2307", "2298", "2267", "1718" })) //
				.compileBothTripSort());
		map2.put(89l, new RouteTripSpec(89l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MEADOWS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, TAMARACK) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3691",//
								/* + */"3608"/* + */, /* + */"3610"/* + */, /* + */"3192"/* + */, /* + */"3193"/* + */,//
								"3979", /* + */"3613"/* + */, "3711" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "3711", "3851", /* + */"3605"/* + */, "3691" })) //
				.compileBothTripSort());
		map2.put(90l, new RouteTripSpec(90l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MEADOWS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "1824", "2255", "3707" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3709", "2551", "1989" })) //
				.compileBothTripSort());
		map2.put(91l, new RouteTripSpec(91l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHLANDS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2307",
						/* + */"2425"/* + */, //
								"1371" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1371", "1131", "2307" })) //
				.compileBothTripSort());
		map2.put(92l, new RouteTripSpec(92l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, PLYPOW, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2118", "2876", /* + */"22330"/* + */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /*-"2876"-*//* + */"22330"/* + */, /* + */"22196"/* + */, "2118" })) //
				.compileBothTripSort());
		map2.put(94l, new RouteTripSpec(94l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2002", "2860", "2447", "2274", "2449", "2303" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2303", "2298", "2591", "2159", "2891", "2002" })) //
				.compileBothTripSort());
		map2.put(95l, new RouteTripSpec(95l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LAUREL, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "3213", "3303", /* + */"3761"/* + */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /*-"3303"-*//* + */"3761"/* + */, /* + */"3620"/* + */, "3213" })) //
				.compileBothTripSort());
		map2.put(96l, new RouteTripSpec(96l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BONNIE_DOON, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2110", /* + */"2433"/* + */, "2196" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2196", /* + */"2074"/* + */, "2110" })) //
				.compileBothTripSort());
		map2.put(99l, new RouteTripSpec(99l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BELVEDERE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2304", "1206", "7211" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7211", "1207", "2304" })) //
				.compileBothTripSort());
		map2.put(100l, new RouteTripSpec(100l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEWIS_FARMS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "1243", "1812", /* + */"5449"/* + */, /* + */"5001"/* + */, "5010", "8610" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "8610", "5001", /* + */"5054"/* + */, "1083", "1256", "1243" })) //
				.compileBothTripSort());
		map2.put(101l, new RouteTripSpec(101l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEDGEWOOD_HTS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* + */"5968"/* + */, "5908", "5821", "5002" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "5002", "5979", "5660", /* + */"5968"/* + */})) //
				.compileBothTripSort());
		map2.put(102l, new RouteTripSpec(102l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LYMBURN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5828", "5725", "5004" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5004", "5755", "5828" })) //
				.compileBothTripSort());
		map2.put(103l, new RouteTripSpec(103l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAMERON_HTS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5752", "5695", "5821", "5002" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "5002", "5979", "5623", "5752" })) //
				.compileBothTripSort());
		map2.put(104l, new RouteTripSpec(104l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, S_CAMPUS_FT_EDM, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LYMBURN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /*-"5755",-*/"5828", /* + */"5725"/* + */, "5821", "2706" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2706", /*-"5725"-,*//* + */"5755"/* + */,/* + */"5828"/* + */})) //
				.compileBothTripSort());
		map2.put(105l, new RouteTripSpec(105l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, S_CAMPUS_FT_EDM, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LESSARD) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* + */"5017"/* + */, /* + */"5932"/* + */, /* "-5634-", */"5733", "5821", "2706" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2706", /* "-5932-", *//* + */"5634"/* + */,/* + */"5017"/* + */})) //
				.compileBothTripSort());
		map2.put(106l, new RouteTripSpec(106l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LESSARD) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5733", /* + */"5722"/* + */, "5004", "5007", "2713", "2001" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2001", "2701", "5004",/* + */"5699"/* + */, "5733" })) //
				.compileBothTripSort());
		map2.put(107l, new RouteTripSpec(107l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTRIDGE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5747", "5657", "5005" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "5005", "5877", "5747" })) //
				.compileBothTripSort());
		map2.put(108l, new RouteTripSpec(108l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEWIS_FARMS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BRECKENRIDGE_GRNS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "8670", "8279", "8608" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "8608", "8999", "8670" })) //
				.compileBothTripSort());
		map2.put(109l, new RouteTripSpec(109l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5012", "5874", /* + */"5366"/* + */, "5111", /* + */"5250"/* + */, "5344", "1496" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "1571", "5132", "5111", "5903", "5012" })) //
				.compileBothTripSort());
		map2.put(110l, new RouteTripSpec(110l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTRIDGE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5005", "5877", "5747" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5747", "5811", "5811", "5005" })) //
				.compileBothTripSort());
		map2.put(111l, new RouteTripSpec(111l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_JASPER_PLACE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5001", "5795", "5109", "1620" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "1620", "5104", "5846", "5001" })) //
				.compileBothTripSort());
		map2.put(112l, new RouteTripSpec(112l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5013", "5433", "5344", "1910",
						/* + */"1824"/* + */, //
								"1542", "2122", "2302" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2302", "2497", "1457",
						/* + */"1989"/* + */, //
								"1878", "5132", "5038", "5013" })) //
				.compileBothTripSort());
		map2.put(113l, new RouteTripSpec(113l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5001", "5069", "5104" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "5104", "5151", "5001" })) //
				.compileBothTripSort());
		map2.put(114l, new RouteTripSpec(114l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTVIEW_VLG) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "8846", "8941", "5105" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5105", "8904", "8849", "8846" })) //
				.compileBothTripSort());
		map2.put(115l, new RouteTripSpec(115l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5013", "5433", "5344", "5209", "", "6333", "7011" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7011", "6369", "5203", "5132", "5038", "5013" })) //
				.compileBothTripSort());
		map2.put(117l, new RouteTripSpec(117l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HAMPTONS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEM_LEWIS_FARMS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5010", "8607", "8135", "8106" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "8106", "8033", /*-"8605",-*/"8607", /* + */"8605"/* + */, "5010" })) //
				.compileBothTripSort());
		map2.put(118l, new RouteTripSpec(118l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, RIO_TERRACE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5174", "5302", "5103" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "5103", "5301", "5174" })) //
				.compileBothTripSort());
		map2.put(119l, new RouteTripSpec(119l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEWIS_FARMS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, THE_GRANGE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "8583", "8097", "8033", "8607" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "8607", "8135", "8097", "8046", "8583" })) //
				.compileBothTripSort());
		map2.put(120l, new RouteTripSpec(120l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, STADIUM, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5110", "1242", "1083", "1336", "1407" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "1407", "1328", "1620", "1746", "5110" })) //
				.compileBothTripSort());
		map2.put(121l, new RouteTripSpec(121l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5205", "5215", "6345", "6646", "7011" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7011", "6183", "6371", "5404", "5205" })) //
				.compileBothTripSort());
		map2.put(122l, new RouteTripSpec(122l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5012", "8389", "5928", "5330", "5207" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5207", "5143", "5389", "8952", "5012" })) //
				.compileBothTripSort());
		map2.put(123l, new RouteTripSpec(123l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5105", "8691", "5648", "5374", "5205" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5205", "5692", "5635", "8684", "5105" })) //
				.compileBothTripSort());
		map2.put(124l, new RouteTripSpec(124l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MISTATIM_IND) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "6727", "6844", "5207" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5207", "6345", "6727" })) //
				.compileBothTripSort());
		map2.put(125l, new RouteTripSpec(125l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5101", "5448", "5202", "1113", "1113", "1107", "1251" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "1251", "1107", "5209", "5150", "5101" })) //
				.compileBothTripSort());
		map2.put(126l, new RouteTripSpec(126l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5013", "8882", "8590", "5928", "5208" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "5208", "5389", "8500", "8952", "5013" })) //
				.compileBothTripSort());
		map2.put(127l, new RouteTripSpec(127l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BELVEDERE, // 7205
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT) // 5204
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5204", "1110", "1401", "1209", "1205", "7205" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7205", "1209", "1402", "1110", "1105", "5204" })) //
				.compileBothTripSort());
		map2.put(128l, new RouteTripSpec(128l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2002",
						/* + */"2960"/* + */, //
								"5206", "6333",
								/* + */"6435"/* + */, //
								/* + */"6553"/* + */, //
								/* + */"6136"/* + */, //
								/* + */"6006"/* + */, //
								"6002",
								/* + */"6077"/* + */, //
								"6047"
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "6047",
						/* + */"6078"/* + */, //
								"6006",
								/* + */"6137"/* + */, //
								/* + */"6568"/* + */, //
								/* + */"6435"/* + */, //
								/* + */"6366"/* + */, //
								"6369", "5201",
								/* + */"2515"/* + */, //
								"2002"
						})) //
				.compileBothTripSort());
		map2.put(129l, new RouteTripSpec(129l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5012", "8740", "8740", "5960", "5208" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "5208", "5936", "8740", "5012" })) //
				.compileBothTripSort());
		map2.put(130l, new RouteTripSpec(130l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2002", "1700", "1107", "1532", "1476", "7002" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7002", "1532", "1111", "1855", "2002" })) //
				.compileBothTripSort());
		map2.put(133l, new RouteTripSpec(133l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, S_CAMPUS_FT_EDM, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEWIS_FARMS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "8606", "5001", "2701" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2701", "5010", "8606" })) //
				.compileBothTripSort());
		map2.put(134l, new RouteTripSpec(134l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "1251", "1237", "7002" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7002", "1372", "1251" })) //
				.compileBothTripSort());
		map2.put(136l, new RouteTripSpec(136l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HAMPTONS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "8583", "8089", "8033", "8602", "5010" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "5010", "8609", "8135", "8177", "8583" })) //
				.compileBothTripSort());
		map2.put(137l, new RouteTripSpec(137l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5010", "8882", "6850", /* + */"7011" /* + */, "7002", "7908" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7908", "7011", "6118", "8861", "5010" })) //
				.compileBothTripSort());
		map2.put(138l, new RouteTripSpec(138l, // TODO not exactly: same loop for the 2 trips
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, S_CAMPUS_FT_EDM, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEDGEWOOD_HTS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /*-"5627"-*//* + */"5968"/* + */, /* + */"5888"/* + */, /* + */"5789"/* + */, //
								"5983", "5747", "2707" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2707", "5747", "5719",//
								/* + */"5627"/* + */, /* + */"5858"/* + */, /* + */"5968"/* + *//*-"5789"-*/})) //
				.compileBothTripSort());
		map2.put(139l, new RouteTripSpec(139l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, S_CAMPUS_FT_EDM, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, HAMPTONS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "8457", "8106", "8033", "2707" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2707", "8135", "8457", "8460" })) //
				.compileBothTripSort());
		map2.put(140l, new RouteTripSpec(140l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LAGO_LINDO, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_NORTHGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "1251", "1040", "7003", "7010", "7377" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7377", "7003", "1380", "1251" })) //
				.compileBothTripSort());
		map2.put(141l, new RouteTripSpec(141l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ABBOTTSFIELD, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLISEUM) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "1204", "1561", "1003" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "1003", "1031", "1204" })) //
				.compileBothTripSort());
		map2.put(142l, new RouteTripSpec(142l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ABBOTTSFIELD, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLISEUM) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "1207", "1521", "1001" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "1001", "1367", "1207" })) //
				.compileBothTripSort());
		map2.put(143l, new RouteTripSpec(143l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MONTROSE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, KINGSWAY_RAH) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "1111", "1476", "1441", "1205", "1260" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "1260", "1213", "1278", "1075", "1111" })) //
				.compileBothTripSort());
		map2.put(145l, new RouteTripSpec(145l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LAGO_LINDO, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, _82_ST_132_AVE_EAUX_CLAIRES) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "7496", "6315", "7377", /* + */"7388"/* + */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /*-"7377",-*//* + */"7388"/* + */, /* + */"7483"/* + */, "6315", "6317", "7358", "7165" })) //
				.compileBothTripSort());
		map2.put(149l, new RouteTripSpec(149l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "6308", "7736", "7113", "7904" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7904", "7153", "7959", "6308" })) //
				.compileBothTripSort());
		map2.put(150l, new RouteTripSpec(150l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5007", "5107", "5207", "6333", "7011", "7010", "6303" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "6303", "7011", "6369", "5203", "5102", "5007" })) //
				.compileBothTripSort());
		map2.put(
				151l, // TODO not perfect but close enough
				new RouteTripSpec(151l, MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS, //
						MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, KING_EDWARD_PK)
						.addTripSort(
								MDirectionType.NORTH.intValue(), //
								Arrays.asList(new String[] { "2253", "2432", "1251", "1591", "1966", "1262", "1346", "1128", "1237", "1043", "6496", "6421",
										"6571", "6328", "6222", "6132", "6333", "6553", "6020", "6487", "6251", "6004" })) //
						.addTripSort(
								MDirectionType.SOUTH.intValue(), //
								Arrays.asList(new String[] { "6004", "6426", "6020", "6224", "6234", "6349", "6542", "6434", "6568", "6366", "6292", "6123",
										"6383", "6116", "6266", "6496", "6280", "1372", "1064", "1966", "1262", "1243", "1142", "1251", "2079", "2253" })) //
						.compileBothTripSort());
		map2.put(152l, new RouteTripSpec(152l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BELVEDERE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7003", "7074", "7208" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7208", "7221", "7003" })) //
				.compileBothTripSort());
		map2.put(153l, new RouteTripSpec(153l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BELVEDERE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7008", /* + */"7143"/* + */, "7204" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7204", /* + */"7043"/* + */, "7008" })) //
				.compileBothTripSort());
		map2.put(154l, new RouteTripSpec(154l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BELVEDERE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7009", "7592", "7202" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7202", "7123", "7009" })) //
				.compileBothTripSort());
		map2.put(155l, new RouteTripSpec(155l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, RAPPERSWILL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "6823", /* + */"6416"/* + */, "6313" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "6313", /* + */"6078"/* + */, "6823" })) //
				.compileBothTripSort());
		map2.put(160l, new RouteTripSpec(160l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, OXFORD, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, GOV_CTR) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "1304", "1820", "6348", "6243", "6835", "6676" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "6835", "6676", "6442", "6594", "1304" })) //
				.compileBothTripSort());
		map2.put(
				161l,
				new RouteTripSpec(161l, // like 162 // TODO not perfect, 2 different trip ending, different trip summer/winter
						MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS, //
						MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MAC_EWAN_GOV_CTR) //
						.addTripSort(
								MDirectionType.NORTH.intValue(), //
								Arrays.asList(new String[] { "1309", /* + */"1711"/* + */, /* + */"1035"/* + */, /* + */"1903"/* + */, /* + */"1871"/* + */,
										"1824", "1829", /* + */"1983"/* + */, /* + */"1680"/* + */, /* + */"1783"/* + */, "1820", /* + */"1707"/* + */, /* + */
										"1845"/* + */, /* + */"1271"/* + */, /* + */"1571"/* + */, /* + */"1253"/* + */, /* + */"1555"/* + */,//
										"7009", "6580", "6007" })) //
						.addTripSort(MDirectionType.SOUTH.intValue(), //
								Arrays.asList(new String[] { "6007", "6396", /* + */"7009"/* + */, "7003", //
										/* + */"1221"/* + */, /* + */"1280"/* + */, /* + */
										"1721"/* + */, /* + */"1496"/* + */, /* + */"1673"/* + */, /* + */"1622"/* + */, /* + */"1740"/* + */, /* + */
										"1756"/* + */, /* + */"1655"/* + */, "1868", /* + */"1837"/* + */, /* + */"1718"/* + */, /* + */"1626"/* + */, "1703", /* + */
										"1850"/* + */, "1989", /* + */"1643"/* + */, /* + */"1964"/* + */, "1309" })) //
						.compileBothTripSort());
		map2.put(162l, new RouteTripSpec(162l, // like 161 // TODO not perfect, 2 different trip ending, different trip summer/winter
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MAC_EWAN_GOV_CTR) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] {
								// "1309", "1829", "1820", //
								"1309", /* + */"1711"/* + */, /* + */"1035"/* + */, /* + */"1903"/* + */, /* + */"1871"/* + */, "1824", "1829", /* + */
								"1983"/* + */, /* + */"1680"/* + */, /* + */"1783"/* + */, "1820", /* + */"1707"/* + */, /* + */
								"1845"/* + */, /* + */"1271"/* + */, /* + */"1571"/* + */, /* + */"1253"/* + */, /* + */"1555"/* + */,//
								"6311", "6033", "6008" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "6008", "6340", /* + */"6311"/* + */, "6310",//
								/* + */"1221"/* + */, /* + */"1280"/* + */, /* + */
								"1721"/* + */, /* + */"1496"/* + */, /* + */"1673"/* + */, /* + */"1622"/* + */, /* + */"1740"/* + */, /* + */
								"1756"/* + */, /* + */"1655"/* + */, "1868", /* + */"1837"/* + */, /* + */"1718"/* + */, /* + */"1626"/* + */, "1703", /* + */
								"1850"/* + */, "1989", /* + */"1643"/* + */, /* + */"1964"/* + */, "1309"
						})) //
				.compileBothTripSort());
		map2.put(163l, new RouteTripSpec(163l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CHAMBERY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"6312", /* + */"7463"/* + */, /* + */"7748"/* + */, //
								/* + */"7381"/* + */, "6194", /* + */"6767"/* + */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						/* + */"6767"/* + */, "6598", /* + */"6854"/* + */, /* + */"6147"/* + */, /* + */"6362"/* + */, //
								/* + */"6074"/* + */, /* + */"6076"/* + */, "6236", /* + */"7482"/* + */, "6312" })) //
				.compileBothTripSort());
		map2.put(164l, new RouteTripSpec(164l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CANOSSA, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "7015", "6001", "6166" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "6166", /* + */"6582"/* + */, /* + */"6077"/* + */, "6236", /* + */"6021"/* + */,
						/* + */"6080"/* + */, /* + */"6225"/* + */, "6010", "7015" })) //
				.compileBothTripSort());
		map2.put(165l, new RouteTripSpec(165l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.id, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.id) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "7496", "6130", "6522", "6011", "6127" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "6074", "6010", "6396", "6579", "7299" })) //
				.compileBothTripSort());
		map2.put(168l, new RouteTripSpec(168l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "7011", "6243", "6619", "6835", //
								"6725",
								"6003", "6305" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "6305", "6011", //
								/* + */"6228"/* + */, //
								/* + */"6698"/* + */, //
								/* + */"6725"/* + */, //
								/* + */"6256"/* + */, //
								/* + */"6566"/* + */, //
								/* + */"6261"/* + */, //
								/* + */"6114"/* + */, //
								"6676", "6853", "6442", "7011" })) //
				.compileBothTripSort());
		map2.put(169l, new RouteTripSpec(169l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.id, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.id) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "7015", "6001", "6166", "6194" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "6194", "6456", "6010", "7015" })) //
				.compileBothTripSort());
		map2.put(180l, new RouteTripSpec(180l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ABBOTTSFIELD, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "1824", "6304", "7736", "7456", "7207", "7642", "1002"
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "1002", "7639", "7203", "7384", "7959",
						/* + */"6304"/* + */, //
								"6317",
								/* + */"6594"/* + */, //
								/* + */"1850"/* + */, //
								"1989"
						})) //
				.compileBothTripSort());
		map2.put(181l, new RouteTripSpec(181l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BELVEDERE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7206", //
								/* + */"7650"/* + */, //
								/* + */"7186"/* + */, //
								"7384", "7241", "7604", "7901" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7901", "7414", "7400", "7456", //
								/* + */"7164"/* + */, //
								/* + */"7479"/* + */, //
								/* + */"7650"/* + */, //
								/* + */"7265"/* + */, //
								/*-"7186",-*///
								"7206" })) //
				.compileBothTripSort());
		map2.put(182l, new RouteTripSpec(182l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, FRASER, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7003", "7186", "7104", "7470" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7470", "7105", "7572", "7003" })) //
				.compileBothTripSort());
		map2.put(183l, new RouteTripSpec(183l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ABBOTTSFIELD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "1002", "7668", "7885", "7102" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7102", "7983", "7729", "1002" })) //
				.compileBothTripSort());
		map2.put(184l, new RouteTripSpec(184l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EVERGREEN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "7903", "7262", "7128" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7128", "7262", "7903" })) //
				.compileBothTripSort());
		map2.put(185l, new RouteTripSpec(185l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ABBOTTSFIELD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "1002",
						/* + */"7954"/* + */, //
								"7102" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7102",
						/* + */"7744"/* + */, //
								"1002" })) //
				.compileBothTripSort());
		map2.put(186l, new RouteTripSpec(186l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, FRASER, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7358", "7286", "7206", "7104", "7470" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7470", "7105", "7205", "7120", "7011" })) //
				.compileBothTripSort());
		map2.put(187l, new RouteTripSpec(187l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, KERNOHAN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LONDONDERRY) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7230", "7103", "7756", "7943" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7943", "7103", "7102", "7185" })) //
				.compileBothTripSort());
		map2.put(188l, new RouteTripSpec(188l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LONDONDERRY, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "6309", "7230", "7186", "7907", "7729" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7668", "7904", "7549", "7185", "7188", "6309" })) //
				.compileBothTripSort());
		map2.put(190l, new RouteTripSpec(190l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "6306", "7763", "7803", "7054", "7906" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7906", "7384", "7815", "7674", "6306" })) //
				.compileBothTripSort());
		map2.put(191l, new RouteTripSpec(191l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, KLARVATTEN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "6307", //
								/* + */"7865"/* + */, //
								"7827" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						/* + */"7827"/* + */, //
								/* + */"7825"/* + */, //
								"7434", //
								/* + */"7795"/* + */, //
								"7779", "6307" })) //
				.compileBothTripSort());
		map2.put(192l, new RouteTripSpec(192l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BRINTNELL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "7909",
						/* + */"7512"/* + */, //
								"7984" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7984",
						/* + */"7603"/* + */, //
								"7909" })) //
				.compileBothTripSort());
		map2.put(193l, new RouteTripSpec(193l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BRINTNELL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "7910",
						/* + */"7992"/* + */, //
								"7414" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7414",
						/* + */"77280"/* + */, //
								"7910" })) //
				.compileBothTripSort());
		map2.put(194l, new RouteTripSpec(194l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SCHONSEE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "6308", "7677", "7919" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7919", "7674", "6308" })) //
				.compileBothTripSort());
		map2.put(195l, new RouteTripSpec(195l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_CONACHIE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "7907",
						/* + */"7879"/* + */, //
								"7308" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7308",
						/* + */"77436"/* + */, //
								"7907" })) //
				.compileBothTripSort());
		map2.put(197l, new RouteTripSpec(197l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SPRUCE_GRV) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "8737", "8785", "8761", "5415", //
								/* + */"1595"/* + */, //
								"1223", "1850", "1479" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "1570", "1679", "1227", //
								/* + */"1187"/* + */, //
								"5389", "8730", "8743", "8737" })) //
				.compileBothTripSort());
		map2.put(198l, new RouteTripSpec(198l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, FT_SASKATCHEWAN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7908",
						/* + */"77175"/* + */, //
								"7405" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7405", "7926", "7908" })) //
				.compileBothTripSort());
		map2.put(199l, new RouteTripSpec(199l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EDM_GARRISON, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "6316",
						/* + */"7873"/* + */, //
								"7895" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7895", "7873", "6316" })) //
				.compileBothTripSort());
		map2.put(301l, new RouteTripSpec(301l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "4204", "4065", "4547", "4186", "2211" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2211", "4275", "4543", "4443", "4204" })) //
				.compileBothTripSort());
		map2.put(302l, new RouteTripSpec(302l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EVERGREEN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "7103",
						/* + */"7689",/* + *///
								"7262", "7654", "7128" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7128", "7654", "7591",
						/* + */"7855",/* + *///
								"7103" })) //
				.compileBothTripSort());
		map2.put(303l, new RouteTripSpec(303l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MISTATIM_IND) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7011", "6688", "6732", "6680", "6183", "6345", "6727" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "6727", "6844", "6646", "6732", "6755",
						/* + */"6926",/* + *///
								/* + */"6563",/* + *///
								"6688", "7011" })) //
				.compileBothTripSort());
		map2.put(304l, new RouteTripSpec(304l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH_PARK_CTR) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "4569",
						/* + */"2076",/* + *///
								"2218" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2218", "2888",
						/* + */"4183",/* + *///
								"4569" })) //
				.compileBothTripSort());
		map2.put(305l, new RouteTripSpec(305l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_GATES) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5668", "5082", "5528", "", "5208", "5214" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "5214", "1481", "1861", "5205", "5055", "5335", "5668" })) //
				.compileBothTripSort());
		map2.put(306l, new RouteTripSpec(306l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MEADOWS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BONNIE_DOON) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2196", "2447", "2805", "2693", "3706" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3706", "2693", "2804", "2551", "2196" })) //
				.compileBothTripSort());
		map2.put(307l, new RouteTripSpec(307l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, GOLD_BAR, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BONNIE_DOON) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2196", "2304", "2012" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2012", "2305", "2196" })) //
				.compileBothTripSort());
		map2.put(308l, new RouteTripSpec(308l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, RIVERDALE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"1123",
						/* + */"1280"/* + */, //
								/* + */"1549"/* + */, //
								"1893"
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1893",
						/* + */"1510"/* + */, //
								/* + */"1953"/* + */, //
								/* + */"1914"/* + */, //
								"1254",
								/* + */"1498"/* + */, //
								/* + */"1120"/* + */, //
								"1262", "1123"
						})) //
				.compileBothTripSort());
		map2.put(309l, new RouteTripSpec(309l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, KINGSWAY_RAH, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, RIVERDALE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "1254", "1620", "1960", "1746", "1978", "1104", "1366" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1366", "1455", "1834", "1746", "1141", "1256", "1893", "1254" })) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, HashSet<MTrip> splitTrips, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.id)) {
			RouteTripSpec rts = ALL_ROUTE_TRIPS2.get(mRoute.id);
			return splitTripStop(gTrip, gTripStop, gtfs, //
					rts.getBeforeAfterStopIds(0), //
					rts.getBeforeAfterStopIds(1), //
					rts.getBeforeAfterBothStopIds(0), //
					rts.getBeforeAfterBothStopIds(1), //
					rts.getTripId(0), //
					rts.getTripId(1), //
					rts.getAllBeforeAfterStopIds());
		}
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
		System.out.printf("\n%s: Unexptected trip stop to split %s.\n", gTrip.getRouteId(), gTripStop);
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
		System.out.printf("\n%s: Unexpected trip (befores:%s|afters:%s) %s.\n", gTrip.getRouteId(), beforeStopIds, afterStopIds, gTrip);
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

	private String getFirstStopId(GSpec gtfs, GTrip gTrip) {
		int gStopMaxSequence = -1;
		String gStopId = null;
		for (GStopTime gStopTime : gtfs.stopTimes) {
			if (!gStopTime.trip_id.equals(gTrip.getTripId())) {
				continue;
			}
			if (gStopTime.stop_sequence > gStopMaxSequence) {
				gStopMaxSequence = gStopTime.stop_sequence;
			}
			if (gStopTime.stop_sequence != 1) {
				continue;
			}
			gStopId = gStopTime.stop_id;
		}
		if (StringUtils.isEmpty(gStopId)) {
			System.out.printf("\n%s: Unexpected trip (no 1st stop) %s.\n", gTrip.getRouteId(), gTrip);
			System.exit(-1);
		}
		return gStopId;
	}

	private String getLastStopId(GSpec gtfs, GTrip gTrip) {
		int gStopMaxSequence = -1;
		String gStopId = null;
		for (GStopTime gStopTime : gtfs.stopTimes) {
			if (!gStopTime.trip_id.equals(gTrip.getTripId())) {
				continue;
			}
			if (gStopTime.stop_sequence < gStopMaxSequence) {
				continue;
			}
			gStopMaxSequence = gStopTime.stop_sequence;
			gStopId = gStopTime.stop_id;
		}
		if (StringUtils.isEmpty(gStopId)) {
			System.out.printf("\n%s: Unexpected trip (no last stop) %s.\n", gTrip.getRouteId(), gTrip);
			System.exit(-1);
		}
		return gStopId;
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
				System.out.printf("\n%s: getTripId() > Unexpected direction index: %s.\n", this.routeId, directionIndex);
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
				System.out.printf("\n%s: getBeforeAfterStopIds() > Unexpected direction index: %s.\n", this.routeId, directionIndex);
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
				System.out.printf("\n%s: getBeforeAfterBothStopIds() > Unexpected direction index: %s.\n", this.routeId, directionIndex);
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
				System.out.printf("\n%s: Unexpected trip type %s for %s.\n", this.routeId, this.headsignType0, this.routeId);
				System.exit(-1);
			}
			if (this.headsignType1 == MTrip.HEADSIGN_TYPE_STRING) {
				this.allTrips.add(new MTrip(this.routeId).setHeadsignString(this.headsignString1, this.directionId1));
			} else if (this.headsignType1 == MTrip.HEADSIGN_TYPE_DIRECTION) {
				this.allTrips.add(new MTrip(this.routeId).setHeadsignDirection(MDirectionType.parse(this.headsignString1)));
			} else {
				System.out.printf("\n%s: Unexpected trip type %s for %s\n.", this.routeId, this.headsignType1, this.routeId);
				System.exit(-1);
			}
		}

		public RouteTripSpec addTripSort(int directionId, List<String> sortedStopIds) {
			this.allSortedStopIds.put(directionId, sortedStopIds);
			ArrayList<String> beforeStopIds = new ArrayList<String>();
			String currentStopId = null;
			for (int i = 0; i < sortedStopIds.size(); i++) {
				currentStopId = sortedStopIds.get(i);
				for (int b = beforeStopIds.size() - 1; b >= 0; b--) {
					addFromTo(directionId, beforeStopIds.get(b), currentStopId);
				}
				beforeStopIds.add(currentStopId);
			}
			return this;
		}

		private HashMap<Integer, List<String>> allSortedStopIds = new HashMap<Integer, List<String>>();

		public RouteTripSpec compileBothTripSort() {
			List<String> sortedStopIds0 = this.allSortedStopIds.get(this.directionId0);
			List<String> sortedStopIds1 = this.allSortedStopIds.get(this.directionId1);
			for (int i0 = 0; i0 < sortedStopIds0.size(); i0++) {
				String stopId0 = sortedStopIds0.get(i0);
				for (int i1 = 0; i1 < sortedStopIds1.size(); i1++) {
					String stopId1 = sortedStopIds1.get(i1);
					if (stopId0.equals(stopId1) || //
							sortedStopIds0.contains(stopId1) || sortedStopIds1.contains(stopId0)) {
						continue;
					}
					addBothFromTo(this.directionId0, stopId0, stopId1);
					addBothFromTo(this.directionId1, stopId1, stopId0);
				}
			}
			return this;
		}

		public int compare(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
			int directionId;
			if (MTrip.getNewId(this.routeId, this.directionId0) == ts1.getTripId()) {
				directionId = this.directionId0;
			} else if (MTrip.getNewId(this.routeId, this.directionId1) == ts1.getTripId()) {
				directionId = this.directionId1;
			} else {
				System.out.printf("\n%s: Unexpected trip ID %s", routeId, ts1.getTripId());
				System.exit(-1);
				return 0;
			}
			List<String> sortedStopIds = this.allSortedStopIds.get(directionId);
			if (!sortedStopIds.contains(ts1GStop.stop_code) || !sortedStopIds.contains(ts2GStop.stop_code)) {
				System.out.printf("\n%s: Unexpected stop IDs %s AND/OR %s", routeId, ts1GStop.stop_code, ts2GStop.stop_code);
				System.out.printf("\n%s: Not in sorted list: %s", routeId, sortedStopIds);
				System.exit(-1);
				return 0;
			}
			int ts1StopIndex = sortedStopIds.indexOf(ts1GStop.stop_code);
			int ts2StopIndex = sortedStopIds.indexOf(ts2GStop.stop_code);
			return ts2StopIndex - ts1StopIndex;
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
