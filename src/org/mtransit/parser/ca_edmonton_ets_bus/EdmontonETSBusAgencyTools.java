package org.mtransit.parser.ca_edmonton_ets_bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
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
		return Long.parseLong(gRoute.getRouteShortName()); // using route short name as route ID
	}

	private static final String SLASH = " / ";
	private static final String FORT = "Ft";
	private static final String _AVE = " Ave";
	private static final String _ST = " St";
	private static final String EDMONTON = "Edm";
	private static final String EDM_GARRISON = EDMONTON + " Garrison";
	private static final String WEST_EDM_MALL = "WEM"; // "West " + EDMONTON + " Mall";
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
	private static final String _82_ST_132_AVE = "82" + _ST + " / 132" + _AVE;
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
	private static final String RUTHERFORD_BLACKBURN = RUTHERFORD + SLASH + BLACKBURN;
	private static final String SOUTHWOOD = "Southwood";
	private static final String S_EDM_COMMON = "S " + EDMONTON + " Common";
	private static final String PARKALLEN = "Parkallen";
	private static final String WINDSOR_PK = "Windsor Pk";
	private static final String PARKALLEN_WINDSOR_PK = PARKALLEN + SLASH + WINDSOR_PK;
	private static final String KNOTTWOOD = "Knottwood";
	private static final String BELVEDERE = "Belvedere";
	private static final String BONNIE_DOON = "Bonnie Doon";
	private static final String LAUREL = "Laurel";
	private static final String PLYPOW = "Plypow";
	private static final String TAMARACK = "Tamarack";
	private static final String BRECKENRIDGE_GRNS = "Breckenridge Grns";
	private static final String WESTRIDGE = "Westridge";
	private static final String LESSARD = "Lessard";
	private static final String LESSARD_WEST_EDM_MALL = LESSARD + SLASH + WEST_EDM_MALL;
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
	private static final String _100_ST_160_AVE = "100" + _ST + " / 160" + _AVE;
	private static final String _95_ST_132_AVE = "95" + _ST + " / 132" + _AVE;
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
	private static final String HAWKS_RDG = "Hawks Rdg";
	private static final String WINTERBURN = "Winterburn";
	private static final String WINTERBURN_IND = WINTERBURN + " Ind";
	private static final String HOLYROOD = "Holyrood";
	private static final String STRATHCONA_IND = "Strathcona Ind";
	private static final String RITCHIE = "Ritchie";
	private static final String AMBLESIDE = "Ambleside";
	private static final String WINDERMERE = "Windermere";
	private static final String _104_ST_82_AVE = "104" + _ST + " / 82" + _AVE;
	private static final String BELGRAVIA = "Belgravia";
	private static final String ROSENTHAL = "Rosenthal";
	private static final String CHAPPELLE = "Chappelle";
	private static final String ORCHARDS = "Orchards";
	private static final String QUARRY_RDG = "Quarry Rdg";
	private static final String HOLLICK_KENYON = "Hollick Kenyon";
	private static final String EDM_WASTE_MGT_CTR = EDMONTON + " Waste Mgt Ctr";
	private static final String VLY_ZOO = "Vly Zoo";
	private static final String _84_ST_111_AVE = "84" + _ST + " / 111" + _AVE;
	private static final String VLY_ZOO_FT_EDM = VLY_ZOO + SLASH + FT_EDM;
	private static final String ALL_WEATHER_WINDOWS = "All Weather Windows";
	private static final String AIRPORT = "Int Airport";

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String gRouteLongName = gRoute.getRouteLongName();
		gRouteLongName = CleanUtils.cleanStreetTypes(gRouteLongName);
		return CleanUtils.cleanLabel(gRouteLongName);
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
		if (ts1.getTripId() == 3801l) { // 38 East to Southgate
			if ("4938".equals(ts1GStop.getStopCode()) && "4519".equals(ts2GStop.getStopCode())) {
				return -1;
			} else if ("4519".equals(ts1GStop.getStopCode()) && "4938".equals(ts2GStop.getStopCode())) {
				return +1;
			}
		} else if (ts1.getTripId() == 3802l) { // 38 West to Leger
			if ("4320".equals(ts1GStop.getStopCode()) && "4373".equals(ts2GStop.getStopCode())) {
				return -1;
			} else if ("4373".equals(ts1GStop.getStopCode()) && "4320".equals(ts2GStop.getStopCode())) {
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
			String firstStopId = SplitUtils.getFirstStopId(mRoute, gtfs, gTrip);
			String lastStopId = SplitUtils.getLastStopId(mRoute, gtfs, gTrip);
			if ("7496".equals(firstStopId) && "6447".equals(lastStopId)) {
				mTrip.setHeadsignString(_100_ST_160_AVE, MDirectionType.NORTH.intValue());
				return;
			} else if ("6039".equals(firstStopId) && "7060".equals(lastStopId)) {
				mTrip.setHeadsignString(_95_ST_132_AVE, MDirectionType.SOUTH.intValue());
				return;
			}
		} else if (mRoute.id == 597l) {
			String firstStopId = SplitUtils.getFirstStopId(mRoute, gtfs, gTrip);
			String lastStopId = SplitUtils.getLastStopId(mRoute, gtfs, gTrip);
			if ("5208".equals(firstStopId) && "8740".equals(lastStopId)) {
				mTrip.setHeadsignString(ALL_WEATHER_WINDOWS, MDirectionType.WEST.intValue());
				return;
			}
		} else if (mRoute.id == 697l) {
			String firstStopId = SplitUtils.getFirstStopId(mRoute, gtfs, gTrip);
			String lastStopId = SplitUtils.getLastStopId(mRoute, gtfs, gTrip);
			if ("4810".equals(firstStopId) && "2703".equals(lastStopId)) {
				mTrip.setHeadsignString(S_CAMPUS_FT_EDM, MDirectionType.NORTH.intValue());
				return;
			}
		} else if (mRoute.id == 725l) {
			String firstStopId = SplitUtils.getFirstStopId(mRoute, gtfs, gTrip);
			String lastStopId = SplitUtils.getLastStopId(mRoute, gtfs, gTrip);
			if ("1111".equals(firstStopId) && "2002".equals(lastStopId)) {
				mTrip.setHeadsignString(UNIVERSITY, MDirectionType.SOUTH.intValue());
				return;
			}
		} else if (mRoute.id == 738l) {
			String firstStopId = SplitUtils.getFirstStopId(mRoute, gtfs, gTrip);
			String lastStopId = SplitUtils.getLastStopId(mRoute, gtfs, gTrip);
			if ("4815".equals(firstStopId) && "2709".equals(lastStopId)) {
				mTrip.setHeadsignString(LEGER, MDirectionType.SOUTH.intValue());
				return;
			}
		} else if (mRoute.id == 739l) {
			String firstStopId = SplitUtils.getFirstStopId(mRoute, gtfs, gTrip);
			String lastStopId = SplitUtils.getLastStopId(mRoute, gtfs, gTrip);
			if ("2745".equals(firstStopId) && "2002".equals(lastStopId)) {
				mTrip.setHeadsignString(UNIVERSITY, MDirectionType.NORTH.intValue());
				return;
			}
		} else if (mRoute.id == 757l) {
			String firstStopId = SplitUtils.getFirstStopId(mRoute, gtfs, gTrip);
			String lastStopId = SplitUtils.getLastStopId(mRoute, gtfs, gTrip);
			if (("6369".equals(firstStopId) || "5201".equals(firstStopId)) && "2002".equals(lastStopId)) {
				mTrip.setHeadsignString(UNIVERSITY, MDirectionType.SOUTH.intValue());
				return;
			}
		} else if (mRoute.id == 837l) {
			String firstStopId = SplitUtils.getFirstStopId(mRoute, gtfs, gTrip);
			String lastStopId = SplitUtils.getLastStopId(mRoute, gtfs, gTrip);
			if ("1814".equals(firstStopId) && "1110".equals(lastStopId)) {
				mTrip.setHeadsignString(KINGSWAY_RAH, MDirectionType.WEST.intValue());
				return;
			}
		} else if (mRoute.id == 853l) {
			String firstStopId = SplitUtils.getFirstStopId(mRoute, gtfs, gTrip);
			String lastStopId = SplitUtils.getLastStopId(mRoute, gtfs, gTrip);
			if ("7496".equals(firstStopId) && "7008".equals(lastStopId)) {
				mTrip.setHeadsignString(NORTHGATE, MDirectionType.WEST.intValue());
				return;
			}
		} else if (mRoute.id == 926l) {
			String firstStopId = SplitUtils.getFirstStopId(mRoute, gtfs, gTrip);
			String lastStopId = SplitUtils.getLastStopId(mRoute, gtfs, gTrip);
			if ("2001".equals(firstStopId) && "5006".equals(lastStopId)) {
				mTrip.setHeadsignString(WEST_EDM_MALL, MDirectionType.WEST.intValue());
				return;
			}
		}
		System.out.printf("\n%s: Unexpected trip %s.\n", mRoute.id, gTrip);
		System.exit(-1);
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.id)) {
			return ALL_ROUTE_TRIPS2.get(mRoute.id).getAllTrips();
		}
		if (ALL_ROUTE_TRIPS.containsKey(mRoute.id)) {
			return ALL_ROUTE_TRIPS.get(mRoute.id).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS;
	static {
		HashMap<Long, RouteTripSpec> map = new HashMap<Long, RouteTripSpec>();
		map.put(10l, new RouteTripSpec(10l, //
				0, MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW, //
				1, MTrip.HEADSIGN_TYPE_STRING, COLISEUM) //
				.addALLFromTo(0, "1203", "7101") //
				.addALLFromTo(1, "7101", "1203") //
		);
		map.put(13l, new RouteTripSpec(13l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE) //
				.addALLFromTo(MDirectionType.NORTH.intValue(), "7011", "6005") //
				.addALLFromTo(MDirectionType.SOUTH.intValue(), "6005", "7011") //
		);
		map.put(17l, new RouteTripSpec(17l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addALLFromTo(MDirectionType.NORTH.intValue(), "4203", "2206") //
				.addALLFromTo(MDirectionType.SOUTH.intValue(), "2206", "4203") //
		);
		map.put(23l, new RouteTripSpec(23l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addALLFromTo(MDirectionType.WEST.intValue(), "3217", "5001") //
				.addALLFromTo(MDirectionType.EAST.intValue(), "5001", "3217") //
		);
		map.put(24l, new RouteTripSpec(24l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addALLFromTo(MDirectionType.WEST.intValue(), "4201", "4806") //
				.addALLFromTo(MDirectionType.EAST.intValue(), "4806", "4201") //
		);
		map.put(31l, new RouteTripSpec(31l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addALLFromTo(MDirectionType.NORTH.intValue(), "4813", "2208") //
				.addALLFromTo(MDirectionType.SOUTH.intValue(), "2208", "4813") //
		);
		map.put(32l, new RouteTripSpec(32l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BRANDER_GDNS, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, S_CAMPUS_FT_EDM) //
				.addALLFromTo(MDirectionType.WEST.intValue(), "2705", "4025") //
				.addALLFromTo(MDirectionType.EAST.intValue(), "4025", "2705") //
				.addBothFromTo(MDirectionType.WEST.intValue(), "2705", "2705") // 4025
		);
		map.put(34l, new RouteTripSpec(34l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addALLFromTo(MDirectionType.WEST.intValue(), "2209", "4809") //
				.addALLFromTo(MDirectionType.EAST.intValue(), "4809", "2209") //
		);
		map.put(35l, new RouteTripSpec(35l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addALLFromTo(MDirectionType.WEST.intValue(), "4215", "4812") //
				.addALLFromTo(MDirectionType.EAST.intValue(), "4812", "4215") //
		);
		map.put(36l, new RouteTripSpec(36l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, S_CAMPUS_FT_EDM, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addALLFromTo(MDirectionType.NORTH.intValue(), "4211", "2703") //
				.addALLFromTo(MDirectionType.SOUTH.intValue(), "2703", "4211") //
		);
		map.put(37l, new RouteTripSpec(37l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addALLFromTo(MDirectionType.WEST.intValue(), "4215", "4802") //
				.addALLFromTo(MDirectionType.EAST.intValue(), "4802", "4215") //
		);
		map.put(38l, new RouteTripSpec(38l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addALLFromTo(MDirectionType.WEST.intValue(), "2207", "4805") //
				.addALLFromTo(MDirectionType.EAST.intValue(), "4805", "2207") //
		);
		map.put(39l, new RouteTripSpec(39l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, RUTHERFORD) //
				.addALLFromTo(MDirectionType.NORTH.intValue(), "9242", "4213") //
				.addALLFromTo(MDirectionType.SOUTH.intValue(), "4213", "9242") //
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
		map2.put(2l, new RouteTripSpec(2l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LESSARD_WEST_EDM_MALL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5723", "5008", "5437", "1336",
						/* + */"1256"/* + */, //
								"1408", "1561", "1454", "7902" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7902", "1561", "1407",
						/* + */"1266"/* + */, //
								"1620", "5185", "5003", "5723" })) //
				.compileBothTripSort());
		map2.put(3l, new RouteTripSpec(3l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CROMDALE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5106", "5928", "1279", "1360", "1256", "1147" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "1147", "1346", "1775", "1846", "1669", "5389", "5106" })) //
				.compileBothTripSort());
		map2.put(4l, new RouteTripSpec(4l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEM_LEWIS_FARMS, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2306", "2159", "2891", "2001",
						/* + */"2660"/* + */, //
								"2702",
								/* + */"5006"/* + */, //
								"5003", "8601" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "8601", "5006", "2714", "2002",
						/* + */"2834"/* + */, //
								/* + */"2065"/* + */, //
								/* + */"2196"/* + */, //
								/* + */"2159"/* + */, //
								/* + */"2593"/* + */, //
								/* + */"2549"/* + */, //
								"2447", "2306" })) //
				.compileBothTripSort());
		map2.put(5l, new RouteTripSpec(5l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLISEUM, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5210", "1083", "1336", "1188",
						/* + */"1051"/* + */, //
								"1268", "1202" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "1202", "1328", "1620", "5210" })) //
				.compileBothTripSort());
		map2.put(6l, new RouteTripSpec(6l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2203", "2270", "2109", "2102", "3006", "3215" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3215", "3010", "2109", "2266", "2888",
						/* + */"2630"/* + */, // ?
								"2203" })) //
				.compileBothTripSort());
		map2.put(7l, new RouteTripSpec(7l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5108", "1881", "1829", "1542", "2659", "2891", "2002" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2002", "2860", "2824", "1457", "1989", "1808", "5108" })) //
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
		map2.put(11l, new RouteTripSpec(11l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7007", "7186", "7106" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7106", "7572", "7007" })) //
				.compileBothTripSort());
		map2.put(12l, new RouteTripSpec(12l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "1251", "1529", "1109", "1533", "1550", "1434", "1435", "1553", "1032", "7003" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7003", "6369", "1109", "1113", "1251" })) //
				.compileBothTripSort());
		map2.put(14l, new RouteTripSpec(14l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_JASPER_PLACE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5011", "5024",
						/*-"5103", -*/
						"5112",
						/* + */"5103"/* + */, //
								/* + */"5293"/* + */, //
								"1999" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "1123", "5103", "5855", "5011" })) //
				.compileBothTripSort());
		map2.put(15l, new RouteTripSpec(15l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3208", "2117", "2551", "1457", "1532", "1476", "6317" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "6317", "1532", "1542", "2188", "2118", "3208" })) //
				.compileBothTripSort());
		map2.put(16l, new RouteTripSpec(16l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, GOV_CTR) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "1310", "7011", "6314", "6075", "6576", "6009" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "6009", "6124", "6317",
						/* + */"7011"/* + */, //
								"7003", "1310" })) //
				.compileBothTripSort());
		map2.put(30l, new RouteTripSpec(30l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, S_CAMPUS_FT_EDM, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_CENTURY_PK) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3217", "4211", "4811", "4597", "4153", "2704" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2704", "4021", "4494", "4811", "4803", "4202", "3217" })) //
				.compileBothTripSort());
		map2.put(33l, new RouteTripSpec(33l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MEADOWS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5001", "4021", "4040", "2973", "2205", "2215", "2118", "3713" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3713", "2117", "2205", "2984", "4021", "4153", "5001" })) //
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
		map2.put(54l, new RouteTripSpec(54l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, S_CAMPUS_FT_EDM) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2710", "2001" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2001", "2710" })) //
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
		map2.put(310l, new RouteTripSpec(310l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, RIO_TERRACE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5174", "5302", "5383", "5105" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "5105", "5491", "5301", "5174" })) //
				.compileBothTripSort());
		map2.put(311l, new RouteTripSpec(311l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5011", "5222", "5836", "5105" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "5105", "5851", "5325", "5011" })) //
				.compileBothTripSort());
		map2.put(312l, new RouteTripSpec(312l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LONDONDERRY, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7008", "7754", "7944" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7944", "7754", "7008" })) //
				.compileBothTripSort());
		map2.put(313l, new RouteTripSpec(313l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, PARKALLEN_WINDSOR_PK) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2795", "2689", "2002" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2002", "2971", "2001", "2690", "2795" })) //
				.compileBothTripSort());
		map2.put(315l, new RouteTripSpec(315l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WINTERBURN_IND, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEWIS_FARMS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "8602", "8061", "8989", "8727" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "8727", "8989", "8066", "8602" })) //
				.compileBothTripSort());
		map2.put(316l, new RouteTripSpec(316l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HAWKS_RDG, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEWIS_FARMS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "8603", "6824", "6408", "6709" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "6709", "6617", "6825", "8603" })) //
				.compileBothTripSort());
		map2.put(317l, new RouteTripSpec(317l, // TODO better (same stops in both trips in different orders)
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WINTERBURN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"8989", "8967", "8943", "8975", "8927", "8163", "8846", "8975", "8945", //
								"8941", "5105"
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"5105", "8904", //
								"8694", "8927", "8163", "8846", "8975", "8927", "8163", "8955", "8938", "8989"
						})) //
				.compileBothTripSort());
		map2.put(318l, new RouteTripSpec(318l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ABBOTTSFIELD, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLISEUM) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "1208", "1070", "1001", "1491", "1002" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "1002", "1340", "1208" })) //
				.compileBothTripSort());
		map2.put(321l, new RouteTripSpec(321l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA_IND) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "3733", "3744", "2106" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2106",
						/* + */"3481"/* + */, //
								"3733" })) //
				.compileBothTripSort());
		map2.put(322l, new RouteTripSpec(322l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HOLYROOD, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BONNIE_DOON) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2808", "2585", "2841",
						/* + */"2246"/* + */, //
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] {
						/* + */"2246"/* + */, //
								"2613", "2808" })) //
				.compileBothTripSort());
		map2.put(323l, new RouteTripSpec(323l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BONNIE_DOON, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, RITCHIE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2419", "2313", "2808" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2808",
						/* + */"2294"/* + */, //
								"2419" })) //
				.compileBothTripSort());
		map2.put(324l, new RouteTripSpec(324l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, AMBLESIDE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "9092", "9630", "4201" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "4201", "9635", "9092" })) //
				.compileBothTripSort());
		map2.put(325l, new RouteTripSpec(325l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WINDERMERE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "9632", "9526", "4801" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "4801",
						/* + */"4938"/* + */, //
								"9632" })) //
				.compileBothTripSort());
		map2.put(327l, new RouteTripSpec(327l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, _104_ST_82_AVE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BELGRAVIA) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2765", "2680", "2821" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2821",
						/* + */"2648"/* + */, //
								"2765" })) //
				.compileBothTripSort());
		map2.put(330l, new RouteTripSpec(330l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, S_CAMPUS_FT_EDM, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "4811", "4597", "4153", "2704", })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2704", "4021", "4494", "4811" })) //
				.compileBothTripSort());
		map2.put(331l, new RouteTripSpec(331l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CHAPPELLE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "9285",
						/* + */"9270"/* + */, //
								/* + */"9271"/* + */, //
								/* + */"9272"/* + */, //
								/* + */"9366"/* + */, //
								/* + */"9281"/* + */, //
								/* + */"9382"/* + */, //
								"4216" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4216",
						/* + */"9044"/* + */, //
								/* + */"9187"/* + */, //
								/* + */"9273"/* + */, //
								/* + */"9274"/* + */, //
								/* + */"9368"/* + */, //
								/* + */"9263"/* + */, //
								/* + */"9264"/* + */, //
								/* + */"9265"/* + */, //
								"9285" })) //
				.compileBothTripSort());
		map2.put(333l, new RouteTripSpec(333l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEWIS_FARMS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ROSENTHAL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "8167",
						/* + */"8852"/* + */, //
								"8604" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "8604",
						/* + */"8168"/* + */, //
								"8167" })) //
				.compileBothTripSort());
		map2.put(334l, new RouteTripSpec(334l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "4809",
						/* + */"4626"/* + */, //
								"4215" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "4215",
						/* + */"4642"/* + */, //
								"4809" })) //
				.compileBothTripSort());
		map2.put(336l, new RouteTripSpec(336l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "4810", "4455", "4069", "2208", })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2208", "4167", "4129", "4810" })) //
				.compileBothTripSort());
		map2.put(337l, new RouteTripSpec(337l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "4802", "4117", "4110", "4215", })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "4215", "4941", "4856", "4802" })) //
				.compileBothTripSort());
		map2.put(338l, new RouteTripSpec(338l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BLACKBURN) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "9226", "4201", "4813", "4597", "4034", "2207", })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2207", "4034", "4042", "4805", "4204", "9226" })) //
				.compileBothTripSort());
		map2.put(339l, new RouteTripSpec(339l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, RUTHERFORD_BLACKBURN) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "9251", "9685", "4213", })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4213", "9756", "9251" })) //
				.compileBothTripSort());
		map2.put(340l, new RouteTripSpec(340l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3217", "3482", "2102", })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2102",
						/* + */"3448"/* + */, //
								"3217" })) //
				.compileBothTripSort());
		map2.put(347l, new RouteTripSpec(347l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ALLARD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "9717", "9685", "4213", })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4213", "9666", "9717" })) //
				.compileBothTripSort());
		map2.put(360l, new RouteTripSpec(360l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ORCHARDS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "9306",
						/* + */"9050"/* + */, //
								"4216", })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4216",
						/* + */"9051"/* + */, //
								"9306" })) //
				.compileBothTripSort());
		map2.put(361l, new RouteTripSpec(361l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3210", "3585", "2105", })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2105", "3529", "3210" })) //
				.compileBothTripSort());
		map2.put(362l, new RouteTripSpec(362l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHWOOD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3082",
						/* + */"3149"/* + */, //
								"3211", })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "3211", "3009", "3082" })) //
				.compileBothTripSort());
		map2.put(363l, new RouteTripSpec(363l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHWOOD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3066", "3003", "3215", })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "3215",
						/* + */"3174"/* + */, //
								"3066" })) //
				.compileBothTripSort());
		map2.put(370l, new RouteTripSpec(370l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3206", "3957", "3796", "2106", })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2106", "3748", "3950", "3206" })) //
				.compileBothTripSort());
		map2.put(380l, new RouteTripSpec(380l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, QUARRY_RDG, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7903",
						/* + */"7587"/* + */, //
								"7213" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7213",
						/* + */"77358"/* + */, //
								"7903" })) //
				.compileBothTripSort());
		map2.put(381l, new RouteTripSpec(381l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HOLLICK_KENYON, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "7905",
						/* + */"7982"/* + */, //
								"7151", })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7151",
						/* + */"7808"/* + */, //
								"7905" })) //
				.compileBothTripSort());
		map2.put(577l, new RouteTripSpec(577l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHLANDS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, _84_ST_111_AVE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "1408",
						/* + */"1094"/* + */, //
								"1371" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "1371",
						/* + */"1180"/* + */, //
								"1408" })) //
				.compileBothTripSort());
		map2.put(589l, new RouteTripSpec(589l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, EDM_WASTE_MGT_CTR, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLISEUM) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "1211", "7700",
						/* + */"7701"/* + */, //
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] {
						/* + */"7700"/* + */, //
								"7701", "1211" })) //
				.compileBothTripSort());
		map2.put(595l, new RouteTripSpec(595l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, S_CAMPUS_FT_EDM, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, FT_EDM) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "4476",
						/* + */"2978"/* + */, //
								"2706" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2706",
						/* + */"22160"/* + */, //
								"4476" })) //
				.compileBothTripSort());
		map2.put(596l, new RouteTripSpec(596l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, S_CAMPUS_FT_EDM, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, VLY_ZOO_FT_EDM) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5015", "4476", "2706" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2706", "4476", "5015" })) //
				.compileBothTripSort());
		map2.put(747l, new RouteTripSpec(747l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, AIRPORT) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "9747", "4216" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4216", "9747" })) //
				.compileBothTripSort() //
				.addBothFromTo(MDirectionType.SOUTH.intValue(), "4216", "4216") //
				.addBothFromTo(MDirectionType.NORTH.intValue(), "9747", "9747")); //
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.id)) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.id));
		}
		if (ALL_ROUTE_TRIPS.containsKey(mRoute.id)) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS.get(mRoute.id));
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern TRANSIT_CENTER = Pattern.compile("((^|\\W){1}(transit center|transit centre)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String TRANSIT_CENTER_REPLACEMENT = "$2TC$4";

	private static final Pattern INTERNATIONAL = Pattern.compile("((^|\\W){1}(international)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String INTERNATIONAL_REPLACEMENT = "$2Int$4";

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = TRANSIT_CENTER.matcher(gStopName).replaceAll(TRANSIT_CENTER_REPLACEMENT);
		gStopName = INTERNATIONAL.matcher(gStopName).replaceAll(INTERNATIONAL_REPLACEMENT);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

}
