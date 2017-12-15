package org.mtransit.parser.ca_edmonton_ets_bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
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

// https://data.edmonton.ca/
// https://data.edmonton.ca/Transit/ETS-Bus-Schedule-GTFS-Data-Feed-zipped-files/gzhc-5ss6
// https://data.edmonton.ca/download/gzhc-5ss6/application/zip
// http://www.edmonton.ca/ets/ets-data-for-developers.aspx
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
		this.serviceIds = extractUsefulServiceIds(args, this, true);
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
	public boolean excludeRoute(GRoute gRoute) {
		return super.excludeRoute(gRoute);
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
		return Long.parseLong(gRoute.getRouteId()); // using route short name as route ID
	}

	private static final String DASH = " - ";
	private static final String SLASH = " / ";
	private static final String FORT = "Fort";
	private static final String _AVE = " Ave";
	private static final String _ST = " St";
	private static final String TRANSIT_CENTER_SHORT = "TC";
	private static final String EDM = "Edm";
	private static final String EDM_GARRISON = EDM + " Garrison";
	private static final String WEST_EDM_MALL = "WEM"; // "West " + EDM + " Mall";
	private static final String WEST_EDM_MALL_TC = "WEM"; // "WEM TC"
	private static final String LEWIS_FARMS = "Lewis Farms";
	private static final String LEWIS_FARMS_TC = "Lewis Farms"; // "Lewis Farms TC"
	private static final String CAPILANO = "Capilano"; //
	private static final String CAPILANO_TC = "Capilano"; // "Capilano TC"
	private static final String CLAREVIEW = "Clareview";
	private static final String CLAREVIEW_EAST_TC = "Clareview"; // "East Clareview TC"
	private static final String CLAREVIEW_WEST_TC = "Clareview"; // "West Clareview TC"
	private static final String CROMDALE = "Cromdale";
	private static final String JASPER_PLACE = "Jasper Pl";
	private static final String CONCORDIA = "Concordia";
	private static final String COLISEUM = "Coliseum";
	private static final String COLISEUM_TC = COLISEUM; // "Coliseum TC";
	private static final String WESTMOUNT = "Westmount";
	private static final String WESTMOUNT_TC = WESTMOUNT; // "Westmount TC"
	private static final String UNIVERSITY = "University";
	private static final String UNIVERSITY_TC = UNIVERSITY; // "University TC";
	private static final String MILL_WOODS = "Mill Woods";
	private static final String MILL_WOODS_ = "Mill Woods TC";
	private static final String MILL_WOODS_TC = "Mill Woods TC";
	private static final String DAN_KNOTT = "Dan Knott";
	private static final String NAIT = "NAIT";
	private static final String SOUTHGATE = "Southgate";
	private static final String SOUTHGATE_TC = "Southgate"; // "Southgate TC"
	private static final String NORTHGATE = "Northgate";
	private static final String NORTHGATE_TC = "Northgate"; // "Northgate TC"
	private static final String ABBOTTSFIELD = "Abbottsfield";
	private static final String AMISKWACIY = "amiskwaciy";
	private static final String EAUX_CLAIRES = "Eaux Claires";
	private static final String DOWNTOWN = "Downtown";
	private static final String MILLGATE = "Millgate";
	private static final String MILLGATE_TC = "Millgate"; // "Millgate TC"
	private static final String GOV_CTR = "Gov Ctr";
	private static final String MAC_EWAN = "MacEwan";
	private static final String MAC_EWAN_GOV_CTR = MAC_EWAN + SLASH + GOV_CTR;
	private static final String CASTLE_DOWNS = "Castle Downs";
	private static final String CASTLE_DOWNS_TC = "Castle Downs"; // "Castle Downs TC"
	private static final String CENTURY_PK = "Century Pk";
	private static final String CENTURY_PK_TC = CENTURY_PK; // "Century Pk TC";
	private static final String YELLOWBIRD = "Yellowbird";
	private static final String SOUTH_CAMPUS = "South Campus";
	private static final String SOUTH_CAMPUS_TC = SOUTH_CAMPUS; // "South Campus TC";
	private static final String FT_EDM = FORT + " " + EDM;
	private static final String LEGER = "Leger";
	private static final String LEGER_TC = LEGER; // "Leger TC"
	private static final String BRANDER_GDNS = "Brander Gdns";
	private static final String MEADOWS = "Mdws"; // "Meadows";
	private static final String BLACKMUD_CRK = "Blackmud Crk";
	private static final String BLACKBURNE = "Blackburne";
	private static final String ALLARD = "Allard";
	private static final String HARRY_AINLAY = "Harry Ainlay";
	private static final String TWIN_BROOKS = "Twin Brooks";
	private static final String RUTHERFORD = "Rutherford";
	private static final String SOUTHWOOD = "Southwood";
	private static final String SOUTH_EDM_COMMON = "South " + EDM + " Common";
	private static final String PARKALLEN = "Parkallen";
	private static final String KNOTTWOOD = "Knottwood";
	private static final String BELVEDERE = "Belvedere";
	private static final String BELVEDERE_TC = "Belvedere"; // "Belvedere TC"
	private static final String BONNIE_DOON = "Bonnie Doon";
	private static final String LAUREL = "Laurel";
	private static final String PLYPOW = "Plypow";
	private static final String TAMARACK = "Tamarack";
	private static final String BRECKENRIDGE_GRNS = "Breckenridge Grns";
	private static final String WESTRIDGE = "Westridge";
	private static final String LESSARD = "Lessard";
	private static final String CAMERON_HTS = "Cameron Hts";
	private static final String LYMBURN = "Lymburn";
	private static final String ARCH_MAC = "Arch Mac"; // Donald
	private static final String ROSS_SHEPPARD = "Ross Shep"; // "Ross Sheppard";
	private static final String ORMSBY_PL = "Ormsby Pl";
	private static final String LYMBURN_ORMSBY_PL = LYMBURN + SLASH + ORMSBY_PL;
	private static final String BERIAULT = "Beriault";
	private static final String CRESTWOOD = "Crestwood";
	private static final String ST_FRANCIS_XAVIER = "St Francis Xavier";
	private static final String LA_PERLE = "LaPerle";
	private static final String LA_ZERTE = "LaZerte";
	private static final String MARY_BUTTERWORTH = "Mary Butterworth";
	private static final String HILLCREST = "Hillcrest";
	private static final String CARLTON = "Carlton";
	private static final String WEDGEWOOD = "Wedgewood";
	private static final String THE_GRANGE = "The Grange";
	private static final String RIO_TERRACE = "Rio Ter";
	private static final String THE_HAMPTONS = "The Hamptons";
	private static final String WESTVIEW_VLG = "Westview Vlg";
	private static final String MISTATIM_IND = "Mistatim Ind";
	private static final String STADIUM = "Stadium";
	private static final String STADIUM_TC = "Stadium"; // "Stadium TC"
	private static final String LAGO_LINDO = "Lago Lindo";
	private static final String MONTROSE = "Montrose";
	private static final String KINGSWAY = "Kingsway";
	private static final String KING_EDWARD_PK = "King Edward Pk";
	private static final String RAPPERSWILL = "Rapperswill";
	private static final String OXFORD = "Oxford";
	private static final String _34_ST_35A_AVE = "34" + _ST + SLASH + "35A" + _AVE;
	private static final String _82_ST = "82" + _ST;
	private static final String _82_ST_132_AVE = "82" + _ST + SLASH + "132" + _AVE;
	private static final String _84_ST_105_AVE = "84" + _ST + SLASH + "105" + _AVE;
	private static final String _84_ST_111_AVE = "84" + _ST + SLASH + " 111" + _AVE;
	private static final String _85_ST_132_AVE = "85" + _ST + DASH + "132" + _AVE;
	private static final String _88_ST_132_AVE = "88" + _ST + SLASH + "132" + _AVE;
	private static final String _95_ST_132_AVE = "95" + _ST + SLASH + "132" + _AVE;
	private static final String _127_ST_129_AVE = "127" + _ST + SLASH + "129" + _AVE;
	private static final String _142_ST_109_AVE = "142" + _ST + SLASH + "109" + _AVE;
	private static final String WHITEMUD_DR_53_AVE = "Whitemud Dr" + SLASH + "53 " + _AVE;
	private static final String JOSEPH_MC_NEIL = "Joseph McNeil";
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
	private static final String JASPER_GATES = "Jasper Gts";
	private static final String SOUTHPARK = "Southpark";
	private static final String NORTHLANDS = "Northlands";
	private static final String HAWKS_RDG = "Hawks Rdg";
	private static final String WINTERBURN = "Winterburn";
	private static final String WINTERBURN_IND = WINTERBURN + " Ind";
	private static final String HOLYROOD = "Holyrood";
	private static final String STRATHCONA = "Strathcona";
	private static final String STRATHCONA_IND = STRATHCONA + " Ind";
	private static final String WINDSOR_PARK = "Windsor Pk";
	private static final String RITCHIE = "Ritchie";
	private static final String AMBLESIDE = "Ambleside";
	private static final String WINDERMERE = "Windermere";
	private static final String BELGRAVIA = "Belgravia";
	private static final String ROSENTHAL = "Rosenthal";
	private static final String CHAPPELLE = "Chappelle";
	private static final String ORCHARDS = "Orchards";
	private static final String QUARRY_RDG = "Quarry Rdg";
	private static final String HOLLICK_KENYON = "Hollick Kenyon";
	private static final String MC_LEOD = "McLeod";
	private static final String EDM_WASTE_MGT_CTR = EDM + " Waste Mgt Ctr";
	private static final String VLY_ZOO = "Vly Zoo";
	private static final String VLY_ZOO_FT_EDM = VLY_ZOO + SLASH + FT_EDM;
	private static final String EDM_INT_AIRPORT = "Edm Int Airport";
	private static final String GRIESBACH = "Griesbach";
	private static final String REMAND_CTR = "Remand Ctr";
	private static final String ARCH_O_LEARY = "Arch O'Leary";
	private static final String OTTEWELL = "Ottewell";
	private static final String AOB = "AOB";
	private static final String OTTEWELL_AOB = OTTEWELL + SLASH + AOB;
	private static final String BURNEWOOD = "Burnewood";
	private static final String MC_PHERSON = "McPherson";
	private static final String ST_ROSE = "St Rose";
	private static final String OSCAR_ROMERO = "Oscar Romero";
	private static final String BRAEMAR = "Braemar";
	private static final String PARKVIEW = "Parkview";
	private static final String QUEEN_ELIZABETH = "Queen Elizabeth";
	private static final String HADDOW = "Haddow";
	private static final String FR_TROY = "Fr Troy";
	private static final String JACKSON_HTS = "Jackson Hts";
	private static final String BATURYN = "Baturyn";
	private static final String EASTGLEN = "Eastglen";
	private static final String MINCHAU = "Minchau";
	private static final String HOLY_FAMILY = "Holy Family";
	private static final String MC_NALLY = "McNally";
	private static final String SILVERBERRY = "SilverBerry";
	private static final String VICTORIA = "Victoria";
	private static final String MEADOWLARK = "Meadowlark";
	private static final String WESTLAWN = "Westlawn";
	private static final String BELMEAD = "Belmead";
	private static final String MATT_BERRY = "Matt Berry";
	private static final String JJ_BOWLEN = "JJ Bowlen";
	private static final String CARDINAL_LEGER = "Cardinal Leger";
	private static final String DUNLUCE = "Dunluce";
	private static final String BEAUMARIS = "Beaumaris";
	private static final String ELSINORE = "Elsinore";
	private static final String RIVERBEND = "Riverbend";
	private static final String BEARSPAW = "Bearspaw";
	private static final String AVALON = "Avalon";
	private static final String WILDROSE = "Wildrose";
	private static final String GREENVIEW = "Greenview";
	private static final String KENILWORTH = "Kenilworth";
	private static final String HARDISTY = "Hardisty";
	private static final String CRAWFORD_PLAINS = "Crawford Plains";
	private static final String RHATIGAN_RIDGE = "Rhatigan Rdg";
	private static final String AVONMORE = "Avonmore";
	private static final String LARKSPUR = "Larkspur";
	private static final String MAYLIEWAN = "Mayliewan";
	private static final String WP_WAGNER = "WP Wagner";
	private static final String BROOKSIDE = "Brookside";
	private static final String MAGRATH = "Magrath";
	private static final String LY_CAIRNS = "LY Cairns";
	private static final String BRUCE_SMITH = "Bruce Smith";
	private static final String JH_PICARD = "JH Picard";
	private static final String TD_BAKER = "TD Baker";
	private static final String ST_KEVIN = "St Kevin";
	private static final String LAKEWOOD = "Lakewood";
	private static final String WOODVALE = "Woodvale";
	private static final String VERNON_BARFORD = "Vernon Barford";
	private static final String BELLE_RIVE = "Belle Rive";
	private static final String LENDRUM = "Lendrum";

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String gRouteLongName = gRoute.getRouteLongName();
		gRouteLongName = CleanUtils.cleanStreetTypes(gRouteLongName);
		return CleanUtils.cleanLabel(gRouteLongName);
	}

	@Override
	public String getRouteShortName(GRoute gRoute) {
		return super.getRouteShortName(gRoute); // do not change, used by real-time API
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
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public int compare(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		System.out.printf("\n%s: compare() > ts1: %s | ts2: %s", routeId, ts1, ts2);
		System.out.printf("\n%s: compare() > ts1GStop: %s | ts2GStop: %s", ts2GStop, routeId, ts1GStop, ts2GStop);
		System.out.printf("\n%s: COMPARE COMPARE COMPARE COMPARE COMPARE COMPARE COMPARE COMPARE COMPARE COMPARE", routeId);
		System.out.printf("\n%s: 1: %s", routeId, list1); // DEBUG
		System.out.printf("\n%s: 2: %s", routeId, list2); // DEBUG
		System.out.printf("\n");
		System.exit(-1);
		return -1;
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		String tripHeadsign = gTrip.getTripHeadsign();
		if ("1".equals(tripHeadsign)) {
			tripHeadsign = null;
		}
		if (StringUtils.isEmpty(tripHeadsign)) {
			System.out.printf("\nUnexpected trip to split %s\n", gTrip);
			System.exit(-1);
		}
		mTrip.setHeadsignString(tripHeadsign, gTrip.getDirectionId()); // cleanTripHeadsign() currently used for stop head sign
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		System.out.printf("\nUnexpected trips to merge: %s & %s!\n", mTrip, mTripToMerge);
		System.exit(-1);
		return false;
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(1l, new RouteTripSpec(1l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5009", // West Edmonton Mall Transit Centre
								"5302", // Meadowlark Transit Centre
								"5110", // Jasper Place Transit Centre
								"5169", // == 142 Street & Stony Plain Road
								"5432", "1047", // !=
								"5440", "1917", // !=
								"1242", // == 124 Street & 102 Avenue
								"1322", // == 103 Street & Jasper Avenue
								"1336", // != 101 Street & Jasper Avenue
								"1346", // != 101 Street & 101A Avenue
								"1346", // 101 Street & 101A Avenue
								"2591", // 79 Street & 106 Avenue
								"2301" // Capilano Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2301", // Capilano Transit Centre
								"2267", // 79 Street & 106 Avenue
								"1620", // 101 Street & Jasper Avenue
								"1746", // == 122 Street & 102 Avenue
								"1971", "5087", // !=
								"1828", "5564", // !=
								"5157", // == 140 Street & Stony Plain Road
								"5101", // Jasper Place Transit Centre
								"5301", // Meadowlark Transit Centre
								"5009" // West Edmonton Mall Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(2l, new RouteTripSpec(2l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LESSARD) //
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
						Arrays.asList(new String[] { "5106", "5928", "1279", "1360", //
								"1243", // ==
								"1142", // !=
								"1336", // !=
								"1256", "1147" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "1147", "1346", "1775", "1846", "1669", "5389", "5106" })) //
				.compileBothTripSort());
		map2.put(4l, new RouteTripSpec(4l, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEWIS_FARMS, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"8601", // Lewis Farms Transit Centre
								"5006", // West Edmonton Mall Transit Centre
								"2702", // South Campus Transit Centre Fort Edmonton Park // LAST
								"2714", // South Campus Transit Centre Fort Edmonton Park // CONTINUE
								"2748", // ==
								"22354", // != <>
								"2982", // != <>
								"2638", // == <>
								"2625", // != <>
								"2890", // == <> 114 Street & 89 Avenue
								"2002", // University Transit Centre
								"2065", // == 87 Street & 82 Avenue
								"2593", // != 85 Street & 82 Avenue
								"2196", // != 83 Street & 90 Avenue
								"2952", // != 83 Street & 84 Avenue
								"2159", // <> 83 Street & 82 Avenue // LAST
								"2549", // 83 Street & 82 Avenue // LAST
								"2447", // 83 Street & 82 Avenue // CONTINUE
								"2222", // !=
								"2372", // !=
								"2306", // Capilano Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2306", // Capilano Transit Centre
								"2532", // !=
								"2532", "2476", "2568", "2050", "2462", "2161", "2287", "2288", "2494", "2376", "2231", "2015", "2615", "2608", "2167", "2193", //
								"2037", // !=
								"2159", // <> 83 Street & 82 Avenue // CONTINUE
								"2590", // !=
								"2340", "2087", "2131", "2294", "2236", "2033", "2659", "2853", "2723", "2891", "2845", "2683", "2893", "2788", "2689", //
								"2733", // !=
								"2752", // ==
								"22354", // != <>
								"2982", // != <>
								"2638", // == <>
								"2625", // != <>
								"2890", // == <> 114 Street & 89 Avenue
								"2001", // != University Transit Centre
								"2702", // South Campus Transit Centre Fort Edmonton Park
								"5006", // West Edmonton Mall Transit Centre // LAST
								"5003", // West Edmonton Mall Transit Centre // CONTINUE
								"8601" // Lewis Farms Transit Centre
						})) //
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
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2203", // Southgate Transit Centre
								"2211", // Southgate Transit Centre
								"2085", // ++
								"2024", // ++
								"2109", // Millgate Transit Centre
								"2102", // Millgate Transit Centre
								"3281", // ++
								"3121", // ++
								"3215", // Mill Woods Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"3215", // Mill Woods Transit Centre
								"3127", // ++
								"3347", // ++
								"2109", // Millgate Transit Centre
								"2273", // ++
								"2179", // ++
								"2211", // Southgate Transit Centre
								"2203", // Southgate Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(7l, new RouteTripSpec(7l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5108", // Jasper Place Transit Centre
								"1881", // 124 Street & 107 Avenue
								"1829", // 105 Street & 105 Avenue
								"1542", // ++
								"2659", // ++
								"2891", // ++
								"2890", // 114 Street & 89 Avenue
								"2002", // University Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2002", // University Transit Centre
								"2860", // ++
								"2824", // ++
								"1457", // ++
								"1989", // 108 Street & 104 Avenue
								"1808", // ++
								"5108", // Jasper Place Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(8l, new RouteTripSpec(8l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ABBOTTSFIELD, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"3207", // Mill Woods Transit Centre
								"3122", // ==
								"3244", // !=
								"3338", // !=
								// "3462", // !=
								// "3498", // !=
								"3264", // ==
								"2108", // Millgate Transit Centre
								"1457", // 100 Street & Jasper Avenue
								"1989", // 108 Street & 104 Avenue
								"1106", // Kingsway RAH Transit
								"1476", // 106 Street & 118 Avenue
								"1201", // Coliseum Transit Centre
								"1001", // Abbottsfield Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"1001", // Abbottsfield Transit Centre
								"1208", // Coliseum Transit Centre
								"1112", // Kingsway RAH Transit Centre
								"1557", // 109 Street & 105 Avenue
								"1542", // 100 Street & Jasper Avenue
								"2103", // Millgate Transit Centre
								"3599", // ==
								// "3676", // !=
								// "3360", // !=
								"3394", // !=
								"3121", // ==
								"3207", // Mill Woods Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(9l, new RouteTripSpec(9l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) // SOUTHGATE
				.addTripSort(MDirectionType.NORTH.intValue(), // CENTURY_PK / SOUTHGATE => EAUX_CLAIRES
						Arrays.asList(new String[] { //
						"4216", // Century Park Transit Centre
								"2218", // == Southgate Transit Centre
								"2623", // ==
								"2658", // !=
								"2830", "2657", // !=
								"2852", // ==
								"1591", // 101 Street & MacDonald Drive
								"1108", // 101 Street & MacDonald Drive
								"1476", // 106 Street & 118 Avenue
								"7016", // Northgate Transit Centre
								"6317" // Eaux Claires Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), // EAUX_CLAIRES => CENTURY_PK / SOUTHGATE
						Arrays.asList(new String[] { //
						"6317", // Eaux Claires Transit Centre
								"7001", // Northgate Transit Centre
								"1532", // 106 Street & 118 Avenue Loop
								"1142", // 101 Street & MacDonald Drive nearside
								"2631",// ==
								"2895", "2833", // !=
								"-22352", // !=
								"2773", // ==
								"2639", // ==
								"-22223", // !=
								"2218", // == Southgate Transit Centre
								"2206", // Southgate Transit Centre
								"4216" // Century Park Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(10l, new RouteTripSpec(10l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW_EAST_TC, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLISEUM_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"1203", // Coliseum Transit Centre
								"7186", // 69 Street & 144 Avenue
								"7209", // Belvedere Transit Centre
								"7101", // East Clareview Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"7101", // East Clareview Transit Centre
								"7884", // Victoria Trail & Hooke Road
								"7201", // Belvedere Transit Centre
								"7572", // 66 Street & 144 Avenue
								"1203", // Coliseum Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(11l, new RouteTripSpec(11l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"7007", "7186", "7106" //
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"7106", "7572", "7008", "7496", "7007" //
						})) //
				.compileBothTripSort());
		map2.put(12L, new RouteTripSpec(12L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"1251", // 102 Street & MacDonald Drive
								"1110", // Kingsway RAH Transit Centre
								"7003", // Northgate Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"7003", // Northgate Transit Centre
								"11326", // ==
								"1110", // != Kingsway RAH Transit Centre => NORTHGATE
								"1113", // != Kingsway RAH Transit Centre
								"1243", // !=
								"1251", // 102 Street & MacDonald Drive
						})) //
				.compileBothTripSort());
		map2.put(13l, new RouteTripSpec(13l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE_TC, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"6005", // Castle Downs Transit Centre
								"7011", // Northgate Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"7011", // Northgate Transit Centre
								"6005", // Castle Downs Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(14l, new RouteTripSpec(14l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5011", // West Edmonton Mall Transit Centre
								"5024", // 180 Street & 98 Avenue
								"5153", // == 159 Street & Stony Plain Road
								"5112", // != 157 Street & Stony Plain Road nearside
								"5103",// != Jasper Place Transit Centre
								"5293", // != 143 Street & Stony Plain Road
								"1999" // != 100 Street & 103A Avenue nearside
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1123", // 99 Street & 103A Avenue nearside
								"1812", // == 111 Street & Jasper Avenue Nearside
								"1828", // != 124 Street & 102 Avenue
								"1971", // != 124 Street & 102 Avenue
								"5185", // == 142 Street & Stony Plain Road
								"5103", // Jasper Place Transit Centre
								"5855", // 182 Street & 97A Avenue
								"5011" // West Edmonton Mall Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(15l, new RouteTripSpec(15l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"3208", // Mill Woods Transit Centre
								"2117", // Millgate Transit Centre
								"1457", // 100 Street & Jasper Avenue
								"1989", // 108 Street & 104 Avenue
								"1227", // ++
								"1532", // ++
								"1476", // 106 Street & 118 Avenue
								"6317", // Eaux Claires Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"6317", // Eaux Claires Transit Centre
								"1532", // 106 Street & 118 Avenue Loop
								"1557", // 109 Street & 105 Avenue
								"1542", // 100 Street & Jasper Avenue
								"2118", // Millgate Transit Centre
								"3208", // Mill Woods Transit Centre
						})) //
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
		map2.put(17l, new RouteTripSpec(17l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE_TC, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"4203", // Century Park Transit Centre
								"2206", // Southgate Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2206", // Southgate Transit Centre
								"4203", // Century Park Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(23l, new RouteTripSpec(23l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5001", // West Edmonton Mall Transit Centre
								"4202", // Century Park Transit Centre
								"3217", // Mill Woods Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"3217", // Mill Woods Transit Centre
								"4211", // Century Park Transit Centre
								"5001", // West Edmonton Mall Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(24l, new RouteTripSpec(24l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"4806", // Leger Transit Centre
								"9093", // Anderson Crescent W Ent & Anderson Way SW
								"9095", // ++
								"9096", // ++
								"9097", // ++
								"9098", // ++
								"9241", // ++
								"9244", // ++
								"9245", // ++
								"9246", // ++
								"9673", // ++
								"9405", // ++
								"9633", // ++
								"9815", // !=
								"9057", // ==
								"9630", // == Rabbit Hill Road & Ellerslie Road
								"9071", // !=
								"9072", // !=
								"9631", // ==
								"4106", // !=
								"4864", // ++
								"4548", // ++
								"4201", // Century Park Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"4201", // Century Park Transit Centre
								"4456", // ++
								"4105", // ++
								"4790", // !=
								"9057", // ==
								"9630", // == Rabbit Hill Road & Ellerslie Road
								"9071", // !=
								"9072", // !=
								"9631", // ==
								"9635", // !=
								"9634", // ++
								"9770", // ++
								"9092", // 170 Street & Anderson Way SW
								"4806", // Leger Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(25l, new RouteTripSpec(25l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "4801",
						/* + */"4938"/* + */, //
								/* + */"9415"/* + */, //
								/* + */"9486"/* + */, //
								/* + */"9557"/* + */, //
								/* + */"9176"/* + */, //
								/* + */"9632"/* + */, //
								/* + */"9713"/* + */, //
								/* + */"9094"/* + */, //
								/* + */"9446"/* + */, //
								/* + */"4106"/* + */, //
								"4212" //
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "4212",
						/* + */"9324"/* + */, //
								"9632",
								/* + */"9409"/* + */, //
								/* + */"9553"/* + */, //
								/* + */"9555"/* + */, //
								/* + */"9412"/* + */, //
								/* + */"9486"/* + */, //
								/* + */"9415"/* + */, //
								/* + */"9486"/* + */, //
								"9526", "4801" //
						})) //
				.compileBothTripSort());
		map2.put(26l, new RouteTripSpec(26l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WINDERMERE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "9460",
						/* + */"9632"/* + */, //
								"4808" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4808",
						/* + */"9710"/* + */,//
								"9460" })) //
				.compileBothTripSort());
		map2.put(30l, new RouteTripSpec(30l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH_CAMPUS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3217", "4211", "4811", "4597", "4153", "2704" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2704", "4021", "4494", "4811", "4803", "4202", "3217" })) //
				.compileBothTripSort());
		map2.put(31l, new RouteTripSpec(31l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE_TC, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"4813", // Leger Transit Centre
								"4308", // Hodgson Boulevard & Hilliard Green
								"4329", // Carter Crest Road West & Rabbit Hill Road
								"2208", // Southgate Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2208", // Southgate Transit Centre
								"4439", // Terwillegar Drive & 40 Avenue
								"4834", // Hodgson Boulevard & Hilliard Green
								"4813", // Leger Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(32l, new RouteTripSpec(32l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH_CAMPUS_TC, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BRANDER_GDNS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"4025", // 148 Street & Riverbend Road nearside
								"4153", // Whitemud Drive NB & 53 Avenue
								"2705", // South Campus Transit Centre Fort Edmonton Park
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2705", // South Campus Transit Centre Fort Edmonton Park
								"4021", // Whitemud Drive SB & 53 Avenue
								"4025", // 148 Street & Riverbend Road nearside
						})) //
				.compileBothTripSort());
		map2.put(33l, new RouteTripSpec(33l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MEADOWS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5001", "4021", "4040", "2973", "2205", "2215", "2118", "3713" //
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"3713", "2117", "2205", "2984", "4021", "4153", "5001" //
						})) //
				.compileBothTripSort());
		map2.put(34l, new RouteTripSpec(34l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE_TC, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"4809", // Leger Transit Centre
								"4069", // Bulyea Road & Burton Road S
								"2209", // Southgate Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2209", // Southgate Transit Centre
								"4167", // Bulyea Road & Terwillegar Drive
								"4809", // Leger Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(35l, new RouteTripSpec(35l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"4812", // Leger Transit Centre
								"4935", // 156 Street & South Terwillegar Boulevard
								"4367", // Rabbit Hill Road & 23 Avenue
								"4215", // Century Park Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"4215", // Century Park Transit Centre
								"4114", // Rabbit Hill Road & 23 Avenue
								"4936", // 156 Street & 9 Avenue
								"4812", // Leger Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(36l, new RouteTripSpec(36l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH_CAMPUS_TC, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"4211", // Century Park Transit Centre
								"4749", // !=
								"4810", // <> Leger Transit Centre
								"4530", // !=
								"4455", // Falconer Road & Riverbend Square
								"4158", // Whitemud Drive SB & 53 Avenue
								"2703", // South Campus Transit Centre Fort Edmonton Park
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2703", // South Campus Transit Centre Fort Edmonton Park
								"4021", // Whitemud Drive SB & 53 Avenue
								"4129", // Falconer Road & Riverbend Square
								"4483", // !=
								"4810", // <> Leger Transit Centre => SOUTH_CAMPUS_TC
								"4804", // Leger Transit Centre
								"4211", // Century Park Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(37l, new RouteTripSpec(37l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"4802", // Leger Transit Centre
								"4117", // Towne Centre Boulevard & Terwillegar Boulevard
								"4754", // McLay Crescent W & MacTaggart Drive
								"4215", // Century Park Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"4215", // Century Park Transit Centre
								"4643", // Rabbit Hill Road & Terwillegar Boulevard
								"4856", // Towne Centre Boulevard & Terwillegar Boulevard
								"4802", // Leger Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(38l, new RouteTripSpec(38l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"4805", // Leger Transit Centre
								"4519", // !=
								"4122", // !=
								"4938", // !=
								"4455", // !=
								"4427", // ==
								"4288", // ++
								"4469", // ==
								"4597", // != Riverbend Road & Rabbit Hill Road
								"4191", // !=
								"4041", // ==
								"4037", // 143 Street & 53 Avenue
								"4038", // ++
								"4031", // ++
								"4034", // 144 Street & 60 Avenue
								"4279", // ==
								"4040", // != Whitemud Drive SB & 53 Avenue
								"2207", // Southgate Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2207", // Southgate Transit Centre
								"4020", // !=
								"4041", // ==
								"4037", // 143 Street & 53 Avenue
								"4038", // ++
								"4031", // ++
								"4034", // 144 Street & 60 Avenue
								"4279", // ==
								"4021", // !=
								"4126", // !=
								"4427", // ==
								"4288", // ++
								"4469", // ==
								"4042", // == Riverbend Road & Rabbit Hill Road
								"4373", // !=
								"4262", // !=
								"4320", // !=
								"4749", // !=
								"4805", // Leger Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(39l, new RouteTripSpec(39l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, RUTHERFORD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"9242", // 117 Street & Rutherford Road SW
								"9685", // McMullen Green & MacEwan Road SW
								"4213", // Century Park Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"4213", // Century Park Transit Centre
								"9666", // 111 Street & MacEwan Road SW
								"9242", // 117 Street & Rutherford Road SW
						})) //
				.compileBothTripSort());
		map2.put(40l, new RouteTripSpec(40l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, YELLOWBIRD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"4290", // 105 Street & 21 Avenue #Yellowbird
								"4118", // ++
								"4206", // Century Park Transit Centre
								"4224", // ++
								"4054", // ++
								"2203", // Southgate Transit Centre
								"2211" // Southgate Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2203", // Southgate Transit Centre
								"2211", // Southgate Transit Centre
								"4490", // ++
								"4164", // ++
								"4205", // Century Park Transit Centre
								"4467", // ++
								"4290" // 105 Street & 21 Avenue #Yellowbird
						})) //
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
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH_CAMPUS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"4214", // Century Park Transit Centre
								"4151", // == 111 Street & Saddleback Road N Ent
								"4543", // != 112 Street & Saddleback Road North Ent
								"4156", // != Saddleback Road & 27 Avenue
								"4547", // != 112 Street & Saddleback Road North Ent
								"4493", // != 116 Street & 30 Avenue
								"4154", // == 117 Street & 28 Avenue
								"2711", // South Campus Transit Centre Fort Edmonton Park
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2711", // South Campus Transit Centre Fort Edmonton Park
								"4337", // == 117 Street & 28 Avenue
								"4096", // != Saddleback Road & 27 Avenue
								"4166", // != 113 Street & Saddleback Road N Ent
								"4566", // != 116 Street & 30 Avenue
								"4245", // != 112 Street & 29A Avenue
								"4088", // == 112 Street & Saddleback Road North Ent
								"4214", // Century Park Transit Centre
						})) //
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
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HARRY_AINLAY, //
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
						Arrays.asList(new String[] { //
						"9301", // Allard Boulevard & Alexander Way SW
								"9163", // Callaghan Drive & Callaghan Point
								"4548", // == 111 Street & 23 Avenue
								"4214", // != Century Park Transit Centre
								"4206" // != Century Park Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"4206", // != Century Park Transit Centre
								"4214", // != Century Park Transit Centre
								"4456", // == 111 Street & 23 Avenue
								"9164", // Callaghan Drive & Callaghan Close
								"9301" // Allard Boulevard & Alexander Way SW
						})) //
				.compileBothTripSort());
		map2.put(48l, new RouteTripSpec(48l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BLACKBURNE) //
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
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH_CAMPUS, //
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
						Arrays.asList(new String[] { //
						"2795", // 112 Street & 65 Avenue nearside
								"2752", // ==
								"2982", // !=
								"22354", // !=
								"2638", // ==
								"2890", // 114 Street & 89 Avenue
								"2001", // University Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2001", // University Transit Centre
								/* + */"2889"/* + */, //
								"2795", // 112 Street & 65 Avenue nearside
						})) //
				.compileBothTripSort());
		map2.put(52l, new RouteTripSpec(52l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2212", // Southgate Transit Centre
								"2887", //
								"2849", // 104 Street & 81 Avenue
								"2632", //
								"2162", // ==
								"1425", // >>>>>>
								"-1425", // !=
								"1728", //
								"1991", //
								"1308", // Government Transit Centre
								"1794", // ==
								"1769", // !=
								"1693", // !=
								"1711", // !=
								"1271", // !=
								"1777",// ==
								"1777", // 103 Street & Jasper Avenue
								"11321", //
								"1292", // 100 Street & 102A Avenue
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"1292", // 100 Street & 102A Avenue
								"1262", //
								"1620", // ==
								"1673", // !=
								"1964", // !=
								"1949", // !=
								"1708", // !=
								"1941", // ==
								"1305", // Government Transit Centre
								"1792", //
								"1629", //
								"1993", //
								"-1425", // !=
								"1425", // <<<<<<<
								"1567", // ==
								"2768", //
								"2899", //
								"2821", // 104 Street & 82 Avenue
								"2665", //
								"2212" // Southgate Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(53l, new RouteTripSpec(53l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH_CAMPUS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2216", "2973", "2712" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2712", "2511", "2216" })) //
				.compileBothTripSort());
		map2.put(54L, new RouteTripSpec(54L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH_CAMPUS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2710", // South Campus Transit Centre Fort Edmonton Park
								"2890", // 114 Street & 89 Avenue
								"2001", // University Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2001", // University Transit Centre
								"2821", // ++
								"2710", // South Campus Transit Centre Fort Edmonton Park
						})) //
				.compileBothTripSort());
		map2.put(55l, new RouteTripSpec(55l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BELGRAVIA, // SOUTH_CAMPUS
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2202", // Southgate Transit Centre
								"2706", // South Campus Transit Centre Fort Edmonton Park
								"2765", // 118 Street & 73 Avenue
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2765", // 118 Street & 73 Avenue
								"2709", // South Campus Transit Centre Fort Edmonton Park
								"2202", // Southgate Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(57l, new RouteTripSpec(57l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2002", // University Transit Centre
								"2860", // ++
								"2824", // ++
								"1246", // ++
								"1364", // ++
								"1358", // 99 Street & 104 Avenue
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1358", // 99 Street & 104 Avenue
								"1608", // ++
								"2659", // ++
								"2891", // ++
								"2890", // 114 Street & 89 Avenue
								"2002", // University Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(59l, new RouteTripSpec(59l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH_EDM_COMMON) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "3440", "3003", "3209" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3209", "3007", "3440" })) //
				.compileBothTripSort());
		map2.put(60l, new RouteTripSpec(60l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3210", "3585", "2104", "2101", "1780", "1989" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1824", "1780", "2104", "3233", "3210" })) //
				.compileBothTripSort());
		map2.put(61l, new RouteTripSpec(61l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3211", "3585", "2105", "2104", "1780", "1989" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1824", "1780", "2105", "3529", "3211" })) //
				.compileBothTripSort());
		map2.put(62l, new RouteTripSpec(62l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHWOOD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"3154", // Mill Woods Road E & 20 Avenue
								"3128", // !=
								"3126", // ==
								"3212", // Mill Woods Transit Centre
								"3127", // ==
								"3087", // !=
								"1989", // 108 Street & 104 Avenue
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"1824", // 108 Street & 104 Avenue
								"3090", // !=
								"3126", // ==
								"-33219", // !=
								"3203", // Mill Woods Transit Centre
								"3127", // ==
								"3129", // !=
								"3154", // Mill Woods Road E & 20 Avenue
						})) //
				.compileBothTripSort());
		map2.put(63l, new RouteTripSpec(63l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHWOOD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"3143", // 48 Street & Mill Woods Road S
								"3165", // ++
								"3167", // ???
								"3169", // ???
								"3171", // ???
								"3173", // ???
								"3254", // ???
								"3148", // ???
								"3146", // ???
								"3144", // ???
								"3142", // ???
								"3140", // ???
								"3065", // ???
								"3067", // ???
								"3069", // ???
								"3071", // ???
								"3073", // ???
								"3075", // ???
								"3077", // ???
								"3079", // ???
								"3081", // ???
								"3083", // ???
								"3085", // ???
								"3130", // ???
								"3128", // !=
								"3126", // ==
								"3204", // == Mill Woods Transit Centre
								"3212", // != Mill Woods Transit Centre
								"3127", // ==
								"3087", // !=
								"1358", // 99 Street & 104 Avenue
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"1358", // 99 Street & 104 Avenue
								"3090", // !=
								"3126", // ==
								"3204", // == Mill Woods Transit Centre
								"3127", // ==
								"3129", // !=
								"3141", // !=
								"3143", // 48 Street & Mill Woods Road S
						})) //
				.compileBothTripSort());
		map2.put(64l, new RouteTripSpec(64l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, KNOTTWOOD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3026", "3006", "3001", "3208", "2111", //
								"1246", "1609", "1364", //
								"1358" //
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1358", //
								"1609", "1570", "1608", //
								"2112", "3208", "3009", "3026" //
						})) //
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
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3213", "3011", "2101", "2105", //
								"1246", "1609", "1364", //
								"1358" //
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1358", //
								"1609", "1570", "1608", //
								"2101", "3011", "3003", "3213" //
						})) //
				.compileBothTripSort());
		map2.put(67l, new RouteTripSpec(67l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MEADOWS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "3206", "3952", "3957", "3708" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3708", "3957", "3950", "3311", "3116", "3206" })) //
				.compileBothTripSort());
		map2.put(68l, new RouteTripSpec(68l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), Arrays.asList(new String[] { "3202", "3399", "3586", "2107", "2110", "1989" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), Arrays.asList(new String[] { "1824", "2107", "3230", "3584", "3202" })) //
				.compileBothTripSort());
		map2.put(69l, new RouteTripSpec(69l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"3214", // Mill Woods Transit Centre
								"3695", // ==
								"3400", //
								"3506", // !=
								"3702", // == Meadows Transit Centre
								"3705", // == Meadows Transit Centre
								"3124", //
								"3722", // !=
								"2024", // !=
								"2110", // Millgate Transit Centre => MILL_WOODS_TC
								"2107", // Millgate Transit Centre => DOWNTOWN
								"2026", // ++
								"1989" // 108 Street & 104 Avenue
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2110", // Millgate Transit Centre
								"2371", // !=
								"3953", // !=
								"3710", // == Meadows Transit Centre
								"3611", //
								"3653", // !=
								"3411", // ==
								"3214" // Mill Woods Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(70l, new RouteTripSpec(70l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"3201", // Mill Woods Transit Centre
								"2697", // == 99 Street & 82 Avenue
								"2659", // != 99 Street & 82 Avenue STOP
								"2824", // != 99 Street & 83 Avenue CONTINUE
								"1190", // == McDougall Hill & Grierson Hill
								"1262", // != 100 Street & Jasper Avenue
								"1292", // != 100 Street & 102A Avenue
								"1457", // != 100 Street & Jasper Avenue
								"1780", // != 103 Street & 102 Avenue
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"1292", // != 100 Street & 102A Avenue
								"1780", // != 103 Street & 102 Avenue
								"1322", // != 103 Street & Jasper Avenue
								"1336", // != 101 Street & Jasper Avenue
								"1542", // == 100 Street & Jasper Avenue
								"2878", // != 99 Street & 85 Avenue
								"2659", // != 99 Street & 82 Avenue
								"2840", // == 99 Street & 81 Avenue
								"3201" // Mill Woods Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(71l, new RouteTripSpec(71l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, GOV_CTR, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"3216", // Mill Woods Transit Centre
								"3337", // ++
								"1153", // 106 Street & 97 Avenue
								"1614", // 109 Street & 97 Avenue
								"1303", // Government Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"1303", // Government Transit Centre
								"1993", // 106 Street & 97 Avenue
								"3543", // ++
								"3216", // Mill Woods Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(72l, new RouteTripSpec(72l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, // MILLGATE
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3206", "3255", "3796", "3491", "2106", "2106", "2110", "1989" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1989", "2110", "2106", "3355", "3748", "3185", "3206" })) //
				.compileBothTripSort());
		map2.put(73l, new RouteTripSpec(73l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2203", // Southgate Transit Centre
								"2211", // Southgate Transit Centre
								"2888", "2102", "3002", //
								"3205" // Mill Woods Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"3205", // Mill Woods Transit Centre
								"3010", "2109", //
								"2203", // Southgate Transit Centre
								"2211" // Southgate Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(74l, new RouteTripSpec(74l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2204", "4202",
						/* + */"3671"/* + */, //
								"3107", "3559", "3209" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3209", "3007", "3430", "3110", "4202", "4212", "2204" })) //
				.compileBothTripSort());
		map2.put(77l, new RouteTripSpec(77l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "4210", "9850", "9111", "3205" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3205", "9761", "9361", "4210" })) //
				.compileBothTripSort());
		map2.put(78l, new RouteTripSpec(78l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "4205", "3675", "9384", "9725", "3215" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3215", "9147", "9387", "3865", "4205" })) //
				.compileBothTripSort());
		map2.put(79l, new RouteTripSpec(79l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_, //
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
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3216", "2106", "2338", //
								/* + */"2697"/* + */, "2659",/* + */"2824"/* + */, //
								/* + */"1246"/* + */, "1383", //
								"1246", "1609", "1364", //
								"1358" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1358", //
								"1609", "1570", "1608", //
								"1383", /* + */"2835"/* + */, //
								/* + */"2878"/* + */, /* ? */"2659"/* ? */, "2840", //
								"2385", "2106", "2104", "3216" })) //
				.compileBothTripSort());
		map2.put(82l, new RouteTripSpec(82l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3212", "2339", "2551", "1383", //
								"1246", "1609", "1364", //
								"1358" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1358", //
								"1609", "1570", "1608", //
								"1383", "2255", "2528", "3212" })) //
				.compileBothTripSort());
		map2.put(83l, new RouteTripSpec(83l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MEADOWS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"1383", //
								"1542", // !=
								"2196", // ==
								"2393", // ==
								"2952", // !=
								"2188", // ==
								"2572", // !=
								"2805", //
								"2911", // ==
								"2536", "2235", // !=
								"2362", "2136", // !=
								"2078", // ==
								"2034", // !=
								"2143", // ==
								"2286", // !=
								"2943", "2813", // !=
								"2431", // ==
								"2468", // ==
								"2415", // !=
								"2693", "2259", // ==
								"22189", // !=
								"3706" //
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"3706", //
								"22349", // !=
								"22188", // ==
								"2693", "2259", // ==
								"22178", // !=
								"2389", // ==
								"2148", "2913", // !=
								"2357", "2598", // !=
								"2802", // ==
								"2804", "2551", //
								"2329", // !=
								"2196", // ==
								"1457", // !=
								"1383" //
						})) //
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
						Arrays.asList(new String[] { //
						"1383", //
								"2434", // ==
								"2059", // !=
								"2985", "2560", // 1=
								"2379", // ==
								"2307" //
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2307", "1383", //
						})) //
				.compileBothTripSort());
		map2.put(86l, new RouteTripSpec(86l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "1358", //
								"1609", "1570", "1608", //
								"2073", "2302" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2302", "2442", //
								"1246", "1609", "1364", //
								"1358" })) //
				.compileBothTripSort());
		map2.put(87l, new RouteTripSpec(87l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2106", "2338", "2824", "1383", //
								"1246", "1609", "1364", //
								"1358" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1358", //
								"1609", "1570", "1608", //
								"1383", "2285", "2385", "2106" })) //
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
						Arrays.asList(new String[] { //
						"3691", // Tamarack Green & 35 Ave
								"3608", // ++
								"3610", // ++
								"3192", // ++
								"3193", // !=
								"3505", // !=
								"3193", // ++
								"3979", // Maple Rd & Loop
								"3773", // ++
								"3781", // !=
								"3613", // Tamarack Way & 38 Ave
								"3711", // Meadows TC
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] {//
						"3711", // Meadows TC
								"3851", // 19 St & 35 Ave
								"3605", // ++
								"3691" // Tamarack Green & 35 Ave
						})) //
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
						Arrays.asList(new String[] { "2101", "2118", "2876", /* + */"22330"/* + */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /*-"2876"-*//* + */"22330"/* + */, /* + */"22196"/* + */, "2118", "2101" })) //
				.compileBothTripSort());
		map2.put(94l, new RouteTripSpec(94l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2002", // University Transit Centre
								"2860", // ++
								"2447", // ++
								"2274", // ++
								"2449", // ++
								"2303", // Capilano Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2303", // Capilano Transit Centre
								"2298", // ++
								"2591", // ++
								"2159", // ++
								"2891", // ++
								"2752", // ==
								"2982", // != 114 Street & 83 Avenue
								"22354", // !=
								"2638", // ==
								"2002", // University Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(95l, new RouteTripSpec(95l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LAUREL, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"3213",
						/* + */"3189"/* + */, //
								/* + */"3952"/* + */, //
								/* + */"3618"/* + */, //
								/* + */"3303"/* + */, //
								"3305", "3703" //
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"3703", "3303",
						/* + */"3761"/* + */, //
								/* + */"3620"/* + */, //
								"3213" //
						})) //
				.compileBothTripSort());
		map2.put(96l, new RouteTripSpec(96l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BONNIE_DOON, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2110", /* + */"2433"/* + */, "2196" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2196", /* + */"2074"/* + */, "2110" })) //
				.compileBothTripSort());
		map2.put(97l, new RouteTripSpec(97l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NAIT, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3217", "3008", "2111", "1702", "1059" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1093", "1824", "2112", "3002", "3217" })) //
				.compileBothTripSort());
		map2.put(98l, new RouteTripSpec(98l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NAIT, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5219", "1059" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1093", "5003" })) //
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
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEDGEWOOD) //
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
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH_CAMPUS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LYMBURN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /*-"5755",-*/"5828", /* + */"5725"/* + */, "5821", "2706" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2706", /*-"5725"-,*//* + */"5755"/* + */,/* + */"5828"/* + */})) //
				.compileBothTripSort());
		map2.put(105l, new RouteTripSpec(105l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH_CAMPUS, //
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
						Arrays.asList(new String[] { //
						"5733", // 172 Street & Callingwood Road
								"5650", //
								"5900", "5757", "5722", "5638", "5671", "5974", "5821", "5749", "5923", "5750", //
								"5463", //
								"5004", // West Edmonton Mall Transit Centre END
								"5007", // West Edmonton Mall Transit Centre CONTINUE
								"5054", //
								"5186", "5486", "5566", "5578", "5359", "5281", "5197", "5332", "5451", "5499", "5298", "4425", "22162", "2978", //
								"22159", //
								"2713", // South Campus Transit Centre Fort Edmonton Park
								"2885", //
								"22157", "2959", "2944", "2505", "2516", //
								"2748", // ==
								"2982", // !=
								"22354", // !=
								"2638", // ==
								"2625", // ++
								"2890", // 114 Street & 89 Avenue
								"2001", // University Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2001", // University Transit Centre
								"2641", // ++
								"5004", // West Edmonton Mall Transit Centre
								"5733", // 172 Street & Callingwood Road
						})) //
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
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
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
						Arrays.asList(new String[] { //
						"5013", "5433", "5344", "5209", //
								"5549", // ==
								"1759", // !=
								"1867", // !=
								"1665", // !=
								"6122", // ==
								"6333", "7011" //
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"7011", //
								"-77862", //
								"6348", //
								"6369", "6289", // ==
								"5173", // !=
								"6372", // !=
								"1932", // !=
								"5090", // ==
								"5203", "5132", "5038", "5013"//
						})) //
				.compileBothTripSort());
		map2.put(117l, new RouteTripSpec(117l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, THE_HAMPTONS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"5010", // West Edmonton Mall Transit Centre
								"5819", // != 189 Street & 87 Avenue
								"8607", // <> Lewis Farms Transit Centre
								"8536", // != West Henday Promenade Access & Webber Greens Drive
								"8135", // ++ Guardian Road & Whitemud Drive
								"8106", // 199 Street & 62 Avenue
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"8106", // 199 Street & 62 Avenue
								"8390", // == 199 Street & Pipeline Compressor Station
								"8034", // ?? 199 Street & Christian Assembly Driveway
								"8430", // ?? 199 Street & Fieldstone Estates Driveway
								"8361", // == 199 Street & 69 Avenue
								"8033", // ++ Guardian Road & Whitemud Drive
								"8406", // == != Suder Greens Drive & Webber Greens Drive
								"8607", // != <> Lewis Farms Transit Centre => THE_HAMPTONS
								"8605", // !=Lewis Farms Transit Centre => WEST_EDM_MALL
								"5783", // == 187 Street & 87 Avenue
								"5010", // West Edmonton Mall Transit Centre
						})) //
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
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, THE_HAMPTONS) //
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
		map2.put(124L, new RouteTripSpec(124L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE) // MISTATIM_IND
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5106", // 157 Street & 100A Avenue
								"6231", //
								"5206", // Westmount Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"5206", // Westmount Transit Centre
								"6781", //
								"5106", // 157 Street & 100A Avenue
						})) //
				.compileBothTripSort());
		map2.put(125l, new RouteTripSpec(125l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BELVEDERE, // DOWNTOWN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5101", // Jasper Place Transit Centre
								"5469", // !=
								"5448",// 161 Street & 109 Avenue
								"5127", // !=
								"5204", // Westmount Transit Centre
								"5098", // !=
								"11326", // ==
								"1105", // == Kingsway RAH Transit Centre LAST
								"1107", // Kingsway RAH Transit Centre
								"1401", // Stadium Transit Centre
								"1044", // ==
								"1209", // == Coliseum Transit Centre LAST
								"1205", // Coliseum Transit Centre
								"7205", // Belvedere Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"7205", // Belvedere Transit Centre
								"1357", // !=
								"1209", // Coliseum Transit Centre
								"1148", // !=
								"1402", // Stadium Transit Centre
								"1032", // !=
								"1105", // == Kingsway RAH Transit Centre
								"1053", // !=
								"5077", // ==
								"5204", // == Westmount Transit Centre LAST
								"5209", // Westmount Transit Centre
								"5112", // !=
								"5101", // Jasper Place Transit Centre
						})) //
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
				.addTripSort(MDirectionType.NORTH.intValue(), // CASTLE_DOWNS
						Arrays.asList(new String[] { //
						"2002", // University Transit Centre
								"2638", // 114 Street & 85 Avenue
								"5127", // !=
								"5206", // <> Westmount Transit Centre => END
								"5207", // Westmount Transit Centre
								"6191", // !=
								"6333", // <> 127 Street & 129 Avenue
								"6553", // !=
								"6458", // !=
								"6006", // Castle Downs Transit Centre END >> UNIVERSITY
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), // UNIVERSITY
						Arrays.asList(new String[] { //
						"6006", // Castle Downs Transit Centre
								"6137", // !=
								"6366", // ++ 127 Street & 131 Avenue
								"6369", // 127 Street & 129 Avenue
								"6289", // ++
								"5051", // !=
								"5206", // <> Westmount Transit Centre
								"5329", // !=
								"2890", // 114 Street & 89 Avenue
								"2002", // University Transit Centre
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
						Arrays.asList(new String[] { //
						"2002", // University Transit Centre
								"1700", // ++
								"1532", // 106 Street & 118 Avenue Loop
								"1476", // ++
								"7002", // Northgate Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"7002", // Northgate Transit Centre
								"1532", // 106 Street & 118 Avenue Loop
								"1855", // ++
								"2890", // 114 Street & 89 Avenue
								"2002", // University Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(133L, new RouteTripSpec(133L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY, // S_CAMPUS_FT_EDM
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEWIS_FARMS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"8606", // Lewis Farms Transit Centre
								"8602", // Lewis Farms Transit Centre
								"5001", // West Edmonton Mall Transit Centre
								"2748", // ==
								"2982", // !=
								"22354", // !=
								"2638", // ==
								"2890", // 114 Street & 89 Avenue
								"2002", // University Transit Centre
								"2890", // 114 Street & 89 Avenue
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2890", // 114 Street & 89 Avenue
								"2002", // University Transit Centre
								"5010", // West Edmonton Mall Transit Centre
								"8602", // Lewis Farms Transit Centre
								"8606", // Lewis Farms Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(134l, new RouteTripSpec(134l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "1251", "1237", "7002" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7002", "1372", "1251" })) //
				.compileBothTripSort());
		map2.put(136L, new RouteTripSpec(136L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, THE_HAMPTONS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"8583", // 215 Street & Hope Road
								"8089", // Glastonbury Boulevard & 69 Avenue
								"8033", // ++
								"8602", // ++
								"5010", // West Edmonton Mall Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"5010", // West Edmonton Mall Transit Centre
								"8609", // ++
								"8135", // ++
								"8177", // ++
								"8046", // 199 Street & 62 Avenue
								"8583", // 215 Street & Hope Road
						})) //
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
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH_CAMPUS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEDGEWOOD) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /*-"5627"-*//* + */"5968"/* + */, /* + */"5888"/* + */, /* + */"5789"/* + */, //
								"5983", "5747", "2707" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2707", "5747", "5719",//
								/* + */"5627"/* + */, /* + */"5858"/* + */, /* + */"5968"/* + *//*-"5789"-*/})) //
				.compileBothTripSort());
		map2.put(139l, new RouteTripSpec(139l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH_CAMPUS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, THE_HAMPTONS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "8457", "8106", "8033", "2707" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2707", "8135", "8457", "8460" })) //
				.compileBothTripSort());
		map2.put(140l, new RouteTripSpec(140l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LAGO_LINDO, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "1251", "1040", "7003", "7010",
						/* + */"7748"/* + */, //
								"7377" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7377",
						/* + */"7042"/* + */, //
								"7003", "1380", "1251" })) //
				.compileBothTripSort());
		map2.put(141l, new RouteTripSpec(141l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ABBOTTSFIELD, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLISEUM) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "1204", "1561", "1002", "1003" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "1002", "1003", "1031", "1204" })) //
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
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, KINGSWAY) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "1111", "1476", "1441", "1205", "1260" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "1260", "1213", "1278", "1075", "1111" })) //
				.compileBothTripSort());
		map2.put(145l, new RouteTripSpec(145l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LAGO_LINDO, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, _82_ST_132_AVE) // EAUX_CLAIRES
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"6315", "7377",
						/* + */"7388"/* + */
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						/* + */"7388"/* + */, //
								/* + */"7483"/* + */, //
								"6315", "6317", "7358", "7165" //
						})) //
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
						Arrays.asList(new String[] { //
						"5007", // West Edmonton Mall Transit Centre
								"5107", // Jasper Place Transit Centre
								"5208", // Westmount Transit Centre
								"5549", // ==
								"1867", // !=
								"1665", // !=
								"6122", // ==
								"6333", //
								"7011", // Northgate Transit Centre
								"7010", //
								"6303", // Eaux Claires Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"6303", // Eaux Claires Transit Centre
								"7011", // Northgate Transit Centre
								"6369", // ++
								"6289", // ==
								"6372", // !=
								"1932", // !=
								"5090", // ==
								"5203", // Westmount Transit Centre
								"5102", // Jasper Place Transit Centre
								"5007", // West Edmonton Mall Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(151l, new RouteTripSpec(151l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, KING_EDWARD_PK) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { // CASTLE_DOWNS
						"2253", // 71 Street & 77 Avenue
								"2432", // 91 Street & 82 Avenue
								"1251", // == 102 Street & MacDonald Drive
								"1346", // 101 Street & 101A Avenue
								"1237", // 101 Street & 117 Avenue
								"1043", // != 97 St & Yellowhead Tr Nearside
								"6496", // == 97 Street & 128 Avenue
								"6421", // != 102 Street & 127 Avenue
								"6571", // ==
								"6333", // !=
								"6553", // !=
								"6020", // !=
								"6434", // !=
								"6292", // != 127 Street & 129 Avenue LAST
								"6328", // !=
								"6132", // !=
								"6487", // ==
								"6333", // 127 Street & 129 Avenue
								"6004", // Castle Downs Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { // KING_EDWARD_PK
						"6004", // Castle Downs Transit Centre
								"6366", // !=
								"6292", // 127 Street & 129 Avenue
								"6123", // !=
								"6116", // == 103 Street & 127 Avenue
								"6496", // == 97 Street & 128 Avenue LAST
								"6266", // 101 Street & 128 Avenue
								"1372", // 101 Street & 117 Avenue
								"1243", // == 101 Street & 101A Avenue
								"1251", // == 102 Street & MacDonald Drive LAST
								"1142", // 101 Street & MacDonald Drive nearside CONTINUE
								"2079", // 91 Street & 83 Avenue
								"2253", // 71 Street & 77 Avenue
						})) //
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
		map2.put(157l, new RouteTripSpec(157l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, REMAND_CTR) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "6379",
						/* + */"6077"/* + */, //
								"6302" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "6302",
						/* + */"6720"/* + */, //
								"6379" })) //
				.compileBothTripSort());
		map2.put(160l, new RouteTripSpec(160l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, OXFORD, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, GOV_CTR) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "1304", "1820", "6348", "6243", "6835", "6676" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "6835", "6676", "6442", "6594", "1304" })) //
				.compileBothTripSort());
		map2.put(161l, new RouteTripSpec(161l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MAC_EWAN_GOV_CTR) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"1309", // != Government Transit Centre START
								"1035", // !=
								"1824", // != 108 Street & 104 Avenue START
								"1845", // !=
								"1271", // ==
								"7579", // !=
								"7009", // <> Northgate Transit Centre
								"66112", // !=
								"6580", // ++
								"6007", // Castle Downs Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"6007", // Castle Downs Transit Centre
								"6396", // ++
								"6141", // == !=
								"7009", // != <> Northgate Transit Centre => CASTLE_DOWNS
								"7003", // != Northgate Transit Centre
								"1673", // ==
								"1740", // !=
								"1989", // != 108 Street & 104 Avenue END
								"1622", // !=
								"1309", // !≃ Government Transit Centre END
						})) //
				.compileBothTripSort());
		map2.put(162l, new RouteTripSpec(162l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MAC_EWAN_GOV_CTR) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"1309", // != Government Transit Centre START
								"1035", // !=
								"1824", // != 108 Street & 104 Avenue START
								"1845", // !=
								"1271", // ==
								"7579", // !=
								"6311", // <> Eaux Claires Transit Centre
								"6033", // ++
								"6008", // Castle Downs Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"6008", // Castle Downs Transit Centre
								"6340", // ++
								"6362", // ==
								"6311", // != <> Eaux Claires Transit Centre => CASTLE_DOWNS
								"6310", // != Eaux Claires Transit Centre
								"1622", // ==
								"1740", // !=
								"1989", // != 108 Street & 104 Avenue END
								"1964", // !=
								"1309", // != Government Transit Centre END
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
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, RAPPERSWILL, // CANOSSA, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"7015", // == Northgate Transit Centre
								"6001", // Castle Downs Transit Centre
								"6202" // 127 Street & 167 Avenue
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"6202", // 127 Street & 167 Avenue
								"6010", // Castle Downs Transit Centre
								"7015", // == Northgate Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(165l, new RouteTripSpec(165l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, _85_ST_132_AVE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "7496", "6130", "6522", "6011", "6127" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "6074", "6010", "6396", "6579", "7299" })) //
				.compileBothTripSort());
		map2.put(166l, new RouteTripSpec(166l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, GRIESBACH) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"6112", // Pegasus Boulevard & Stan Walters Avenue
								/* + */"6612"/* + */, //
								"7015" // Northgate Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"7015", // Northgate Transit Centre
								/* + */"6260"/* + */, //
								"6112" // Pegasus Boulevard & Stan Walters Avenue
						})) //
				.compileBothTripSort());
		map2.put(167l, new RouteTripSpec(167l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS + SLASH + _82_ST, // Castle Downs-82 St
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, _95_ST_132_AVE) // 95A Street & 132 Avenue
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						/* no stops */
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"6039", // 100 Street & 158 Avenue
								"6317", // Eaux Claires Transit Centre
								"7353", // 87 Street & 144 Avenue
								"7060", // 95A Street & 132 Avenue
						})) //
				.compileBothTripSort());
		map2.put(168l, new RouteTripSpec(168l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "7011", "6243", "6619", "6835", //
								"6725", //
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
		map2.put(169L, new RouteTripSpec(169L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CANOSSA, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"7015", // == Northgate Transit Centre
								"6148", "6468", // !=
								"6356", // ==
								"6001", //
								"6205", // 115 Street & 175 Avenue
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"6205", // 115 Street & 175 Avenue
								"6010", //
								"6101", // ==
								"6478", "6343", // !=
								"6361", // ==
								"7015", // Northgate Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(180l, new RouteTripSpec(180l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ABBOTTSFIELD, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"1824", "6304", "7736", "7456", "7207", "7642", "1002" //
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "1002", "7639", "7203", "7384", "7959",
						/* + */"6304"/* + */, //
								"6317",
								/* + */"6594"/* + */, //
								/* + */"1850"/* + */, //
								"1989" //
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
						Arrays.asList(new String[] { //
						"7003", // Northgate Transit Centre
								"7186", // ++
								"7104", // East Clareview Transit Centre
								"7470", // 26 Street & 151 Avenue
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"7470", // 26 Street & 151 Avenue
								"7158", // == 23 Street & 146 Avenue
								"7437", // != 21 Street & 147 Avenue
								"7864", // != 21 Street & 147 Avenue
								"77159", // != 17 Street & Fraser Way
								"7093", // == 22 Street & 151 Avenue
								"7105", // East Clareview Transit Centre
								"7003", // Northgate Transit Centre
						})) //
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
						Arrays.asList(new String[] { //
						"7907", // West Clareview Transit Centre
								"7879", // ++
								"7308" // 59A Street & McConachie Way
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"7308", // 59A Street & McConachie Way
								"77335", // ==
								"77428", // !=
								"7018", // McConachie Boulevard & 176 Avenue
								"77607", // !=
								"77424", // ==
								"77436", // ==
								"7907", // West Clareview Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(197l, new RouteTripSpec(197l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, EDM, //
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
		map2.put(211l, new RouteTripSpec(211l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW_WEST_TC, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "1643", "1321", "7903" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7903", "1620", "1643" })) //
				.compileBothTripSort());
		map2.put(301l, new RouteTripSpec(301l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"4204", // Century Park Transit Centre
								"4065", "4547", "4186", //
								"2203", // Southgate Transit Centre
								"2211" // Southgate Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2203", // Southgate Transit Centre
								"2211", // Southgate Transit Centre
								"4275", "4543", "4443", //
								"4204", // Century Park Transit Centre
						})) //
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
						Arrays.asList(new String[] { //
						"7011", // Northgate Transit Centre
								"6348", // ==
								"6472", // ><
								"6930", // ><
								"6484", // ==
								"6233", // ==
								"6183", // 142 Street & 134 Avenue
								"6727" // 159 Street & 131 Avenue Nearside
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"6727", // 159 Street & 131 Avenue Nearside
								"6677", // ==
								"66139", // !=
								"6178", // ==
								"6524", // ++
								"6472", // ><
								"6930", // ><
								"7579", // ++
								"7011" // Northgate Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(304l, new RouteTripSpec(304l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHPARK) //
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
						Arrays.asList(new String[] { //
						"1481", // 124 Street & 111 Avenue
								"5668", // Jasper Gate
								"5082", // ++
								"5528", // ++
								"5208", // Westmount Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"5208", // Westmount Transit Centre
								"5214", // Westmount East Entrance
								"1481", // 124 Street & 111 Avenue
						})) //
				.compileBothTripSort());
		map2.put(306l, new RouteTripSpec(306l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MEADOWS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BONNIE_DOON) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2808", // Bonnie Doon Safeway
								"2805", // ++ Girard Road & 76 Avenue
								"2415", // !=
								"2693", // == 17 Street & Oak Ridge Drive
								"2259", // ==
								"22189", // !=
								"3706" // Meadows Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"3706", // Meadows Transit Centre
								"22188", // !=
								"2693", // == 17 Street & Oak Ridge Drive
								"2259", // ==
								"22178", // !=
								"2804", // ++
								"2159", // ++
								"2808", // Bonnie Doon Safeway
						})) //
				.compileBothTripSort());
		map2.put(307l, new RouteTripSpec(307l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, GOLD_BAR, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BONNIE_DOON) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2196", "2304", "2012",
						/* + */"2068"/* + */, //
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] {
						/* + */"2068"/* + */, //
								"2475", "2305", "2196" //
						})) //
				.compileBothTripSort());
		map2.put(308l, new RouteTripSpec(308l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, RIVERDALE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"1123",
						/* + */"1280"/* + */, //
								/* + */"1549"/* + */, //
								"1893" //
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
								"1262", "1123" //
						})) //
				.compileBothTripSort());
		map2.put(309l, new RouteTripSpec(309l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, KINGSWAY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, RIVERDALE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"1254", // 92 Street & 101A Avenue
								"1620", // == 101 Street & Jasper Avenue
								"1673", // != 103 Street & Jasper Avenue
								"1964", // != 107 Street & Jasper Avenue
								"1949", // != 103 Street & 100 Avenue
								"1708", // != 105 Street & 100 Avenue
								"1941", // == 107 Street & 100 Avenue
								"1705", // !=
								"1293", // <> 110 Street & 100 Avenue
								"1961", // 1=
								"1942", // ++
								"1960", // ++
								"1978", // ++
								"1104", // ++
								"1366" // 101 Street & 111 Avenue
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"1366", // 101 Street & 111 Avenue
								"1455", // Kingsway Mall
								"1834", // ++
								"1141", // ++
								"1856", // !=
								"1293", // == <> 110 Street & 100 Avenue
								"1711", // != 107 Street & 100 Avenue
								"1271", // != 105 Street & Jasper Avenue
								"1769", // != 107 Street & 100 Avenue
								"1299", // != 103 Street & Jasper Avenue
								"1322", // == 103 Street & Jasper Avenue
								"1256", // Thornton Court & Jasper Avenue
								"1893", // ++
								"1254", // 92 Street & 101A Avenue
						})) //
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
		map2.put(313L, new RouteTripSpec(313L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, PARKALLEN) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2795", // 112 Street & 65 Avenue nearside
								"2689", // ++
								"2002", // University Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2002", // University Transit Centre
								"2971", // 117 Street & University Avenue
								"2001", // University Transit Centre
								"2690", // ++
								"2795", // 112 Street & 65 Avenue nearside
						})) //
				.compileBothTripSort());
		map2.put(315L, new RouteTripSpec(315L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WINTERBURN_IND, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEWIS_FARMS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"8609", // Lewis Farms Transit Centre
								"8536", // ==
								"8087", // !=
								"8175", // <> 215 Street & Secord Boulevard
								"8146", // <>
								"8123", // <>
								"8066", // <> 217 Street & 94B Avenue
								"8080", // <> Secord Drive & Secord Boulevard
								"8061", // <> 218 Street & Secord Blvd
								"8078", // !=
								"8694", // !=
								"8163", // <> 215 St & Westview Blvd
								"8955", // <>
								"8938", // <>
								"8989", // <> Lakeview Drive & Westview Boulevard
								"8975", // <> Westview Village & Lakeview Drive
								"8144", // !=
								"8727", // 220 Street & 115 Avenue
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"8727", // 220 Street & 115 Avenue
								"8369", // !=
								"8163", // <> 215 St & Westview Blvd
								"8955", // <>
								"8938", // <>
								"8989", // <> Lakeview Drive & Westview Boulevard
								"8975", // <> Westview Village & Lakeview Drive
								"8945", // !=
								"8065", // !=
								"8175", // <> 215 Street & Secord Boulevard
								"8146", // <>
								"8123", // <>
								"8066", // <> 217 Street & 94B Avenue
								"8080", // <> Secord Drive & Secord Boulevard
								"8061", // <> 218 Street & Secord Blvd
								"8068", // !=
								"8609", // Lewis Farms Transit Centre
						})) //
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
								"8941", "5105" //
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"5105", "8904", //
								"8694", "8927", "8163", "8846", "8975", "8927", "8163", "8955", "8938", "8989" //
						})) //
				.compileBothTripSort());
		map2.put(318l, new RouteTripSpec(318l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY, // WINDSOR_PARK
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BELGRAVIA) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2765", // 118 Street & 73 Avenue
								"2002", // University Transit Centre
								"2971", // 117 Street & University Avenue
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2971", // 117 Street & University Avenue
								"2001", // University Transit Centre
								"2765", // 118 Street & 73 Avenue
						})) //
				.compileBothTripSort());
		map2.put(319L, new RouteTripSpec(319L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BELGRAVIA, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2202", // Southgate Transit Centre
								"2852", // ++
								"2765", // 118 Street & 73 Avenue
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2765", // 118 Street & 73 Avenue
								"2895", // ++
								"2202", // Southgate Transit Centre
						})) //
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
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BELGRAVIA) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2765", "2680", "2821" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2821",
						/* + */"2648"/* + */, //
								"2765" })) //
				.compileBothTripSort());
		map2.put(330l, new RouteTripSpec(330l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH_CAMPUS, //
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
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BLACKBURNE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "9226", "4201", "4813", "4597", "4034", "2207", })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2207", "4034", "4042", "4805", "4204", "9226" })) //
				.compileBothTripSort());
		map2.put(339l, new RouteTripSpec(339l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, RUTHERFORD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "9251", "9685", "4213", })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4213", "9756", "9251" })) //
				.compileBothTripSort());
		map2.put(340l, new RouteTripSpec(340l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"3217", // Mill Woods Transit Centre
								"3122", // == Hewes Way & 27 Avenue
								"3244", // != Youville Drive W & 28 Avenue
								"3338", // != 65 Street & 28 Avenue
								"3462", // != Youville Drive W & 28 Avenue
								"3498", // != 66 Street & 31 Avenue
								"3264", // == 67 Street & 28 Avenue
								"3482", // ++
								"2102", // Millgate Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2102", // Millgate Transit Centre
								"3448", // ++
								"3217", // Mill Woods Transit Centre
						})) //
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
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3210", "3585", "2105", })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2105", "3529", "3210" })) //
				.compileBothTripSort());
		map2.put(362l, new RouteTripSpec(362l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHWOOD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3082",
						/* + */"3149"/* + */, //
								"3211", })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "3211", "3009", "3082" })) //
				.compileBothTripSort());
		map2.put(363l, new RouteTripSpec(363l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_, //
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
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
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
						/* + */"77430"/* + */, //
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
		map2.put(399l, new RouteTripSpec(399l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLISEUM, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CONCORDIA) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "1517",
						/* + */"1015"/* + */, //
								"1209" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1209",
						/* + */"1131"/* + */, //
								"1517" })) //
				.compileBothTripSort());
		map2.put(512l, new RouteTripSpec(512l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "1336", "1408", "1211", "7212", "7903" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7903", "7212", "1210", "1407", "1620" })) //
				.compileBothTripSort());
		map2.put(517l, new RouteTripSpec(517l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW_WEST_TC, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLISEUM) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"1211", "7903" //
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"7903", "1211" //
						})) //
				.compileBothTripSort());
		map2.put(540L, new RouteTripSpec(540L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EDM, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Beaumont") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"9892", // Ken Nichol RR Ctr Beaumont
								"4201", // Century Park Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"4201", // Century Park Transit Centre
								"9892", // Ken Nichol RR Ctr Beaumont
						})) //
				.compileBothTripSort());
		map2.put(560l, new RouteTripSpec(560l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SPRUCE_GRV) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5389", // 154 Street & 118 Avenue
								"8730", // == Century Road & Grove Drive
								"8743", // == Aspenglen Drive & Grove Drive
								"8737", // == King Street & McLeod Avenue
								"8785", // == Century Road & McLeod Avenue
								"8761", // == Century Road & Grove Drive
								"1890", // 109 Street & Princess Elizabeth Avenue
								"1983", // 105 Street & 104 Avenue
								"1479", // 97 Street & 103A Avenue
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1570", // 101 Street & 103A Avenue
								"1679", // 105 Street & 104 Avenue
								"1860", // 109 Street & Princess Elizabeth Avenue
								"8730", // == Century Road & Grove Drive
								"8743", // == Aspenglen Drive & Grove Drive
								"8737", // == King Street & McLeod Avenue
								"8785", // == Century Road & McLeod Avenue
								"8761", // == Century Road & Grove Drive
								"5415", // 154 Street & 119 Avenue
						})) //
				.compileBothTripSort());
		map2.put(561l, new RouteTripSpec(561l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NAIT, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Acheson") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"8169", "1890" //
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1860", "8169" //
						})) //
				.compileBothTripSort());
		map2.put(562l, new RouteTripSpec(562l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH_CAMPUS, // WEST_EDM_MALL
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SPRUCE_GRV) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"8207", // Jennifer Heil Way & Grove Drive
								"5219", // 175 Street & 87 Avenue
								"2708" // South Campus Transit Centre Fort Edmonton Park
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2708", // South Campus Transit Centre Fort
								"5014", // West Edmonton Mall Transit Centre
								"8207", // Jennifer Heil Way & Grove Drive
						})) //
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
		map2.put(580l, new RouteTripSpec(580l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, FT_SASKATCHEWAN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW_WEST_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"7908", // West Clareview Transit Centre
								"77162", // Southfort Drive & South Point Shopping Fort Sask
								"7405", // Dow Centennial Centre Fort Sask
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"7405", // Dow Centennial Centre Fort Sask
								"7926", // 95 Street & 96 Avenue Fort Sask
								"7908" // West Clareview Transit Centre
						})) //
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
		map2.put(591l, new RouteTripSpec(591l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHLANDS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2307", "2359", "1371" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1371", "2594", "2307" })) //
				.compileBothTripSort());
		map2.put(594l, new RouteTripSpec(594l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Valley Zoo", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5219", // 175 Street & 87 Avenue
								"5332", // 152 Street & 87 Avenue
								"5095", // 133 Street & Buena Vista Road
								"5015" // Valley Zoo Parking Lot
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"5015", // Valley Zoo Parking Lot
								"5095", // 133 Street & Buena Vista Road
								"5610", // 155 Street & 87 Avenue
								"5219" // 175 Street & 87 Avenue
						})) //
				.compileBothTripSort());
		map2.put(595l, new RouteTripSpec(595l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH_CAMPUS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, FT_EDM) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"4476", // Fort Edmonton
								"2978", // ++
								"2706" // South Campus Transit Centre Fort Edmonton Park
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2706",// South Campus Transit Centre Fort Edmonton Park
								"22160", // ++
								"4476" // Fort Edmonton
						})) //
				.compileBothTripSort());
		map2.put(596l, new RouteTripSpec(596l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH_CAMPUS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, VLY_ZOO_FT_EDM) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5015", "4476", "2706" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2706", "4476", "5015" })) //
				.compileBothTripSort());
		map2.put(599l, new RouteTripSpec(599l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EDM_GARRISON, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"6316", // Eaux Claires Transit Centre
								"7991", // 97 Street & 176 Avenue
								"7873", // C Ortona Road & Churchill Avenue Garrison
								"7681", // Ortona Road & Ubique Avenue Garrison
								"7412", // Korea Road & Ortona Road Garrison
								"7895" // B Hindenburg Line Road & Churchill Avenue Garrison
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"7895", // B Hindenburg Line Road & Churchill Avenue Garrison
								"7406", // Highway 28A & Mons Avenue Garrison
								"7873", // C Ortona Road & Churchill Avenue Garrison
								"7681", // Ortona Road & Ubique Avenue Garrison
								"6854", // 97 Street & 176 Avenue
								"6316" // Eaux Claires Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(601l, new RouteTripSpec(601l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ROSS_SHEPPARD, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEDGEWOOD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5627", "5908", "5983", "5548", "5392" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(602l, new RouteTripSpec(602l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ROSS_SHEPPARD, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ORMSBY_PL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5755", "5828", "5725", "5874", "5548", "5392" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(603L, new RouteTripSpec(603L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_CONACHIE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW_WEST_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"7018", // McConachie Boulevard & 176 Avenue
								"77436", // ++
								"7907", // West Clareview Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(606l, new RouteTripSpec(606l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CARLTON, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ARCH_MAC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "6603", "6853", "6293", "6369", "5211", "5548" })) //
				.compileBothTripSort());
		map2.put(607l, new RouteTripSpec(607l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ARCH_MAC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"6822", "6293", "6369", "5211", "5548" //
						})) //
				.compileBothTripSort());
		map2.put(608l, new RouteTripSpec(608l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BEAUMARIS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ARCH_MAC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"6593", "6027", //
								"6369", // ==
								"6372", "1664", // !=
								"5173", // !=
								"5090", // ==
								"5211", "5548" //
						})) //
				.compileBothTripSort());
		map2.put(609l, new RouteTripSpec(609l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ARCH_MAC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"6010", // Castle Downs Transit Centre
								"6369", // ==
								"6289", // !=
								"6372", // !=
								"1664", // !=
								"5356", // ==
								"5211", //
								"5548", // 142 Street & 109 Avenue
						})) //
				.compileBothTripSort());
		map2.put(610l, new RouteTripSpec(610l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, RAPPERSWILL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ARCH_MAC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"6405", // 125 Street & Rapperswill Drive
								"6177", // ++
								"5211", // ++
								"5548", // 142 Street & 109 Avenue
						})) //
				.compileBothTripSort());
		map2.put(612l, new RouteTripSpec(612l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, OXFORD, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ARCH_MAC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "6410", "6695", "5211", "5548" })) //
				.compileBothTripSort());
		map2.put(613l, new RouteTripSpec(613l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, RUTHERFORD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"9356", // 125 Street & 20 Avenue SW
								"4213" // Century Park Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"4213", // Century Park Transit Centre
								"9356" // 125 Street & 20 Avenue SW
						})) //
				.compileBothTripSort());
		map2.put(617l, new RouteTripSpec(617l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, KLARVATTEN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CARDINAL_LEGER) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7827", "7795", "7659" })) //
				.compileBothTripSort());
		map2.put(618l, new RouteTripSpec(618l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MATT_BERRY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, JJ_BOWLEN) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7992", "7058", "7449", "7545" })) //
				.compileBothTripSort());
		map2.put(620l, new RouteTripSpec(620l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BELVEDERE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, AOB) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7210", "1207", "2915" })) //
				.compileBothTripSort());
		map2.put(621l, new RouteTripSpec(621l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ABBOTTSFIELD, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, AOB) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1002", "2553" })) //
				.compileBothTripSort());
		map2.put(635l, new RouteTripSpec(635l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5210", "1481", "1242", "1083", "1393" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(689l, new RouteTripSpec(689l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WINDSOR_PARK, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2851", "2974" })) //
				.compileBothTripSort());
		map2.put(697l, new RouteTripSpec(697l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH_CAMPUS_TC, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"4810", // Leger Transit Centre
								"4455", // Falconer Road & Riverbend Square
								"4158", // Whitemud Drive SB & 53 Avenue
								"2703", // South Campus Transit Centre Fort Edmonton Park
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						/* no stops *///
						})) //
				.compileBothTripSort());
		map2.put(698l, new RouteTripSpec(698l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_PHERSON, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JACKSON_HTS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "3230", "3964" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(699l, new RouteTripSpec(699l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_PHERSON, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JACKSON_HTS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "3355", "3400", "3603" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(701l, new RouteTripSpec(701l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BELMEAD) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5914", "5001" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(702l, new RouteTripSpec(702l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, PARKVIEW, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ORMSBY_PL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5881", "5828", "5725", "5198" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(703l, new RouteTripSpec(703l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CRESTWOOD, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ST_FRANCIS_XAVIER) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5421", "5038", "5174", "5941" })) //
				.compileBothTripSort());
		map2.put(705l, new RouteTripSpec(705l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTLAWN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEWIS_FARMS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"8602", // Lewis Farms Transit Centre
								"5001", // West Edmonton Mall Transit Centre
								"5029", // == 163 Street & 88 Avenue
								"5577", // ?? 163 Street & 92 Avenue
								"5991",// ?? 163 Street & 92 Avenue
								"5522", // == 163 Street & 92 Avenue
								"5069" // 165 Street & 95 Avenue
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(706l, new RouteTripSpec(706l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Jasper Pl TC", // _157_ST_100A_AVE
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE) // High School
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "5106", "5177" })) //
				.compileBothTripSort());
		map2.put(707l, new RouteTripSpec(707l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, OSCAR_ROMERO, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, THE_HAMPTONS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "8670", "8135", "5986" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(708l, new RouteTripSpec(708l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, // not TC
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5012", "5874", "5221", "5109" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(709l, new RouteTripSpec(709l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MEADOWLARK) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5359", "5437", "1256" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(710l, new RouteTripSpec(710l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ROSS_SHEPPARD, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5001", "5174", "5588", "5392" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(711l, new RouteTripSpec(711l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ROSS_SHEPPARD, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEWIS_FARMS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "8603", "5013", "5929", "5433", "5180", "5896" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(712l, new RouteTripSpec(712l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, HILLCREST, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ORMSBY_PL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5822", // 178 Street & 76 Avenue
								"5828", // ++
								"5894", // 167 Street & 81 Avenue
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(717l, new RouteTripSpec(717l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, VICTORIA, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5001", "1426" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(723l, new RouteTripSpec(723l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HADDOW) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "4941", "4319", "4815", "4069", "2974" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(725l, new RouteTripSpec(725l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, KINGSWAY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "" })) // NO STOPS
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1111", "1857", "1939", "2002" })) //
				.compileBothTripSort());
		map2.put(726l, new RouteTripSpec(726l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ROSS_SHEPPARD, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "4808", "4249", "5511", "5180", "5896" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(728l, new RouteTripSpec(728l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BROOKSIDE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "4034", "4029", "2710", "2974" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(729l, new RouteTripSpec(729l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "4815", "4246", "2974" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(730l, new RouteTripSpec(730l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LAGO_LINDO, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ARCH_MAC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7377", "6317", "7016", "5548" })) //
				.compileBothTripSort());
		map2.put(731l, new RouteTripSpec(731l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, KINGSWAY, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5204", // Westmount Transit Centre
								"1105" // Kingsway RAH Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1105", // Kingsway RAH Transit Centre
								"5204" // Westmount Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(733l, new RouteTripSpec(733l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5001", "2714", "2002" })) //
				.compileBothTripSort());
		map2.put(734l, new RouteTripSpec(734l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LAGO_LINDO, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MARY_BUTTERWORTH) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7377", "7483", "6236" })) //
				.compileBothTripSort());
		map2.put(735l, new RouteTripSpec(735l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5006", "5156", "2714", "2002" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(738l, new RouteTripSpec(738l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH_CAMPUS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "4815", "4158", "2709" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(741l, new RouteTripSpec(741l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WP_WAGNER, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, KNOTTWOOD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3023", "3001", "2111", "2189" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(744l, new RouteTripSpec(744l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MAYLIEWAN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, QUEEN_ELIZABETH) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"7276", // 54 Street & 153 Avenue
								"7547", //
								"7441", //
								"7925", //
								"7060", // 95 Street & 132 Avenue
						})) //
				.compileBothTripSort());
		map2.put(739l, new RouteTripSpec(739l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY_TC, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LENDRUM) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2745", // 109 Street & 65 Avenue
								"2002", // University Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						/* no stops *///
						})) //
				.compileBothTripSort());
		map2.put(747l, new RouteTripSpec(747l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EDM_INT_AIRPORT) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "9747", "4216" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4216", "9747" })) //
				.compileBothTripSort() //
				.addBothFromTo(MDirectionType.SOUTH.intValue(), "4216", "4216") //
				.addBothFromTo(MDirectionType.NORTH.intValue(), "9747", "9747")); //
		map2.put(748l, new RouteTripSpec(748l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LAGO_LINDO, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CARDINAL_LEGER) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7377", "6309", "7353" })) //
				.compileBothTripSort());
		map2.put(750l, new RouteTripSpec(750l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, EASTGLEN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7015", "7165", "1203", "1033" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(753l, new RouteTripSpec(753l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LONDONDERRY, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7572", "7007" })) //
				.compileBothTripSort());
		map2.put(755l, new RouteTripSpec(755l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, OXFORD, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ARCH_O_LEARY) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "6452", "6695", "6628", "6442", "7358", "7165" })) //
				.compileBothTripSort());
		map2.put(756l, new RouteTripSpec(756l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LA_ZERTE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "6001", "6340", "6310", "7186" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(757l, new RouteTripSpec(757l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, _127_ST_129_AVE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						/* no stops *///
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"6369", // 127 Street & 129 Avenue
								"1965", // 127 Street & 122 Avenue
								"5206", // Westmount Transit Centre
								"2515", // ++
								"2002", // University Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(760l, new RouteTripSpec(760l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_NALLY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LARKSPUR) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3247", "3586", "2676" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(761l, new RouteTripSpec(761l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2285", "2974" })) //
				.compileBothTripSort());
		map2.put(762l, new RouteTripSpec(762l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_NALLY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, AVONMORE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2339", "2447", "2544", "2267", "2676" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(763l, new RouteTripSpec(763l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BONNIE_DOON, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2159", "2891", "2001" })) //
				.compileBothTripSort());
		map2.put(764l, new RouteTripSpec(764l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2301", "2267", "1620" })) //
				.compileBothTripSort());
		map2.put(765l, new RouteTripSpec(765l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, RHATIGAN_RIDGE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "4461", "4249", "2974" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(767l, new RouteTripSpec(767l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3217", "3011", "2111", "2974" })) //
				.compileBothTripSort());
		map2.put(768l, new RouteTripSpec(768l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WP_WAGNER) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3280", "3556", "3212", "3007", "2111", "2189" })) //
				.compileBothTripSort());
		map2.put(769l, new RouteTripSpec(769l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WP_WAGNER, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3211", "3585", "2111", "2189" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(771l, new RouteTripSpec(771l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WP_WAGNER, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CRAWFORD_PLAINS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3143", "3217", "3002", "2111", "2189" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(773l, new RouteTripSpec(773l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WP_WAGNER, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3217", "3585", "2111", "2189" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(774l, new RouteTripSpec(774l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HARDISTY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SILVERBERRY) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3255", "3708", "3740", "3491", "2915", "2177" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(776l, new RouteTripSpec(776l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_NALLY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3217", "3796", "3586", "2676" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(777l, new RouteTripSpec(777l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MEADOWS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3703", "3560", "3217" })) //
				.compileBothTripSort());
		map2.put(778l, new RouteTripSpec(778l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_NALLY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3217", "3255", "3491", "2676" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(779l, new RouteTripSpec(779l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HARDISTY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3217", "3255", "3491", "2915", "2177" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(780l, new RouteTripSpec(780l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HARDISTY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3217", "3796", "3586", "2915", "2177" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(781l, new RouteTripSpec(781l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_NALLY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2105", "2551", "2676" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(782l, new RouteTripSpec(782l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, KENILWORTH, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3211", "3585", "2111", "2255", "2487", "2160" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(783l, new RouteTripSpec(783l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, KENILWORTH, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, GREENVIEW) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3328", "3537", "2160" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(784l, new RouteTripSpec(784l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_NALLY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MEADOWS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3255", "3708", "3740", "3491", "2676" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(785l, new RouteTripSpec(785l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HARDISTY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WILDROSE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3247", "3491", "2915", "2177" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(786l, new RouteTripSpec(786l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, AVALON, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2202", "2518" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(787l, new RouteTripSpec(787l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2212", "2778", "2974" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(789l, new RouteTripSpec(789l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WP_WAGNER, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHWOOD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3143", "3217", "2189" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(790l, new RouteTripSpec(790l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BEARSPAW) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "4205", "4290", "4203", "4157", "4431", "2218" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(791l, new RouteTripSpec(791l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, RUTHERFORD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "9242", "9685", "4216", "2218" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(793l, new RouteTripSpec(793l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, HARRY_AINLAY) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3217", "3008", "4490" })) //
				.compileBothTripSort());
		map2.put(795l, new RouteTripSpec(795l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, TWIN_BROOKS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "4265", "4216", "2218" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(796l, new RouteTripSpec(796l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, EASTGLEN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, FRASER) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7470", "7620", "1185" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(799l, new RouteTripSpec(799l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, RIVERBEND, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "4808", "4489", "4069", "4246", "4029" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(800l, new RouteTripSpec(800l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MATT_BERRY, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, QUEEN_ELIZABETH) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"7383", // 97 Street & 132 Avenue
								"7288", //
								"7298", //
								"7140", //
								"7272", // 54 Street & 153 Avenue
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(802L, new RouteTripSpec(802L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT_TC, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE) // not TC
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"5061", // 163 Street & 93 Avenue
								"5101", // Jasper Place Transit Centre
								"5204", // Westmount Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"5209", // Westmount Transit Centre
								"5150", // ++
								"5101", // Jasper Place Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(803l, new RouteTripSpec(803l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ORMSBY_PL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BRUCE_SMITH) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5623", "5755", "5725" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(804l, new RouteTripSpec(804l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ARCH_MAC, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LA_PERLE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5548", "5012", "5024" })) //
				.compileBothTripSort());
		map2.put(805l, new RouteTripSpec(805l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL_TC) // WEDGEWOOD
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5061", "5069", "5002" })) //
				.compileBothTripSort());
		map2.put(806l, new RouteTripSpec(806l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE_TC, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"5181", "5107", "5207", //
								"5549", // ==
								"1759", // !=
								"1867", "1735", // !=
								"6122", // ==
								"6333", "7011" //
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(807l, new RouteTripSpec(807l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BERIAULT, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5894", "5001" })) //
				.compileBothTripSort());
		map2.put(808l, new RouteTripSpec(808l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, // not TC
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Jasper Place (not TC)") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5577", "5111" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(809l, new RouteTripSpec(809l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, HILLCREST, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5713", "5005" })) //
				.compileBothTripSort());
		map2.put(810l, new RouteTripSpec(810l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ST_ROSE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5611", "5001" })) //
				.compileBothTripSort());
		map2.put(811l, new RouteTripSpec(811l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, PARKVIEW, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5198", "5294", "5069", "5903", "5013" })) //
				.compileBothTripSort());
		map2.put(812l, new RouteTripSpec(812l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, PARKVIEW, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LA_PERLE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5656", "5011", "5024" })) //
				.compileBothTripSort());
		map2.put(814l, new RouteTripSpec(814l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ROSS_SHEPPARD, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5392", "5527", "5140", "5007" })) //
				.compileBothTripSort());
		map2.put(815l, new RouteTripSpec(815l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ARCH_MAC, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5548", "5005" })) //
				.compileBothTripSort());
		map2.put(817l, new RouteTripSpec(817l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BERIAULT, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5894", "5012" })) //
				.compileBothTripSort());
		map2.put(818l, new RouteTripSpec(818l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL_TC, // BERIAULT
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ORMSBY_PL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5718", "5725", "5004" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5894", "5004", "5755", "5828", "5718" })) //
				.compileBothTripSort());
		map2.put(819l, new RouteTripSpec(819l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT_TC, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "5203", "5102", "5007" })) //
				.compileBothTripSort());
		map2.put(820l, new RouteTripSpec(820l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LY_CAIRNS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] {/* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2812", "2218" })) //
				.compileBothTripSort());
		map2.put(821l, new RouteTripSpec(821l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CRESTWOOD, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] {/* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5096", "5225", "5005" })) //
				.compileBothTripSort());
		map2.put(822l, new RouteTripSpec(822l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE_TC, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "1591", "1108", "1476", "7001" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1532", "1104", "1426",
						/* + */"1050"/* + */, //
								"1142" })) //
				.compileBothTripSort());
		map2.put(824l, new RouteTripSpec(824l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, VICTORIA, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEWIS_FARMS_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "1911", "5001", "8605" })) //
				.compileBothTripSort());
		map2.put(825l, new RouteTripSpec(825l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE_TC, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, VICTORIA) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "1554", "1237", "7002" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(826l, new RouteTripSpec(826l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MAGRATH) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2974", "4815", "4306", "4506" })) //
				.compileBothTripSort());
		map2.put(828l, new RouteTripSpec(828l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BROOKSIDE) // Ramsey Heights
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2974", "2707", "4021", "4034" })) //
				.compileBothTripSort());
		map2.put(829l, new RouteTripSpec(829l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2974", "4815" })) //
				.compileBothTripSort());
		map2.put(830l, new RouteTripSpec(830l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_NALLY, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2547", "1142" })) //
				.compileBothTripSort());
		map2.put(832l, new RouteTripSpec(832l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS_TC, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ARCH_MAC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5180", "6725", "6011" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(835l, new RouteTripSpec(835l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE_TC, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, AMISKWACIY) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"1979", //
								"1669", // !=
								"1974", "1735", // ==
								"1799", "1759", // ==
								"6122", // !=
								"6333", "6579", "7003" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(836l, new RouteTripSpec(836l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE_TC, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, KINGSWAY) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"1109", //
								"1896", // ==
								"-11329", // !=
								"1821", "1669", "1974", // !=
								"6122", // ==
								"6328", "6252", "7003" //
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(837l, new RouteTripSpec(837l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLISEUM, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5204", // Westmount Transit Centre
								"1814", // 132 Street & 111 Avenue
								"1110", // Kingsway RAH Transit Centre
								"1401", // Stadium Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(839l, new RouteTripSpec(839l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ARCH_MAC, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5548", "5132", "5038", "5013" })) //
				.compileBothTripSort());
		map2.put(840l, new RouteTripSpec(840l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS_TC, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"5206", //
								"1725", // ==
								"1759", // !=
								"1867", "1735", // !=
								"6122", // ==
								"6333", "6002", "6047", "6001" //
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(841l, new RouteTripSpec(841l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "6285", "6317", "7003" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7011", "6314", "6009" })) //
				.compileBothTripSort());
		map2.put(842l, new RouteTripSpec(842l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ARCH_O_LEARY, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, OXFORD) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7120", "7496", "7060", "6348", "6243", "6337" })) //
				.compileBothTripSort());
		map2.put(843l, new RouteTripSpec(843l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ARCH_MAC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5442", "5445", "1881", "1322" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(844l, new RouteTripSpec(844l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, FRASER, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW_EAST_TC) // QUEEN_ELIZABETH
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"7358", // 95 Street & 132 Avenue #QueenElizabeth
								"7286", // 82 Street & 132 Avenue
								"7330", // ==
								"7206", // Belvedere Transit Centre
								"7210", // Belvedere Transit Centre
								"7335", // ==
								"7104", // East Clareview Transit Centre
								"7470", // 26 Street & 151 Avenue
								"7437" // 21 Street & 147 Avenue #Fraser
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"7437", // 21 Street & 147 Avenue #Fraser
								"7470", // 26 Street & 151 Avenue
								"7105" // East Clareview Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(845l, new RouteTripSpec(845l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7120", "7496", "7060", "7007", "7186", "7106" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7106", "7572", "7185", "7007" })) //
				.compileBothTripSort());
		map2.put(846l, new RouteTripSpec(846l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ARCH_MAC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"5180", // 142 Street & 107 Avenue
								"6091", // ++
								"6006", // Castle Downs Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(848l, new RouteTripSpec(848l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LAGO_LINDO, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CARDINAL_LEGER) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "7659", "6315", "7377", "7483" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(849l, new RouteTripSpec(849l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW_EAST_TC, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, QUEEN_ELIZABETH) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7358", "7209", "7823", "7943", "7269", "7101" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(851l, new RouteTripSpec(851l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, KLARVATTEN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CARDINAL_LEGER) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "7728", "7827", "7434" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(852l, new RouteTripSpec(852l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ARCH_O_LEARY) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "7496", "6130", "6522", "6011", "6127" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(853l, new RouteTripSpec(853l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BELVEDERE_TC, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7585", "7204" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7496", "7008" })) //
				.compileBothTripSort());
		map2.put(855l, new RouteTripSpec(855l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ARCH_O_LEARY) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "7496", "6301", "6039", "6447" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(856l, new RouteTripSpec(856l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, JH_PICARD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2779", "2824", "1729" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(858l, new RouteTripSpec(858l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, STADIUM_TC, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, AMISKWACIY) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "1979", "1110", "1401" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(859l, new RouteTripSpec(859l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE_TC, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ARCH_MAC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"5055", //
								"5548", "5207", //
								"5549", // ==
								"1759", // !=
								"1867", "1735", // !=
								"6122", // ==
								"6333", "7011" //
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(860l, new RouteTripSpec(860l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_NALLY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2676", "3230", "3217" })) //
				.compileBothTripSort());
		map2.put(861l, new RouteTripSpec(861l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_NALLY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2676", "3230", "3247", "3446" })) //
				.compileBothTripSort());
		map2.put(862l, new RouteTripSpec(862l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, OTTEWELL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) // BURNEWOOD
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						/* no stops *///
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2487", // 73 Street & 94B Avenue
								"2064", // !=
								"2360", // ==
								"2426", // ==
								"2915", // != 61 Street & 95 Avenue
								"2360", // ==
								"2426", // ==
								"2434", // !=
								"3230", // 49 Street & 44 Avenue
								"3704", // Meadows Transit Centre
								"3185", // 34 Street & 35A Avenue
						})) //
				.compileBothTripSort());
		map2.put(864l, new RouteTripSpec(864l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_NALLY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2676", "2196", "2393", "2188", "2385", "2103" })) //
				.compileBothTripSort());
		map2.put(865l, new RouteTripSpec(865l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, TD_BAKER, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3150", "3212" })) //
				.compileBothTripSort());
		map2.put(866l, new RouteTripSpec(866l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO_TC, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ST_KEVIN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2439", "2307" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(867l, new RouteTripSpec(867l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LAKEWOOD) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "3002", "3217" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(869l, new RouteTripSpec(869l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, OTTEWELL_AOB, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						/* no stops *///
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2487", // 73 Street & 94B Avenue
								"2064", // !=
								"2360", // ==
								"2426", // ==
								"2915", // != 61 Street & 95 Avenue
								"2360", // ==
								"2426", // ==
								"2434", // !=
								"3355", // 50 Street & Jamha Road
								"3411", // 23 Street & 37A Avenue
								"3217", // Mill Woods Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(870l, new RouteTripSpec(870l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHWOOD, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LAKEWOOD) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "3002", "3204", "3142" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(871l, new RouteTripSpec(871l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BELLE_RIVE, // LAGO_LINDO
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MARY_BUTTERWORTH) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "6285", "7377", "7780", "7430" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(872l, new RouteTripSpec(872l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WP_WAGNER, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2418", "2103", "3003", "3214" })) //
				.compileBothTripSort());
		map2.put(873l, new RouteTripSpec(873l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, KENILWORTH, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WOODVALE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2160", "3461" })) //
				.compileBothTripSort());
		map2.put(874l, new RouteTripSpec(874l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WP_WAGNER, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2189", "3204", "3142" })) //
				.compileBothTripSort());
		map2.put(875l, new RouteTripSpec(875l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WP_WAGNER, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2418", // 80 Street & Wagner Road
								"2105", // Millgate Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(876l, new RouteTripSpec(876l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WP_WAGNER, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2189", "3203", "3356" })) //
				.compileBothTripSort());
		map2.put(877l, new RouteTripSpec(877l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, JH_PICARD, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2080", "2640", "2245", "3004", "3201" })) //
				.compileBothTripSort());
		map2.put(878l, new RouteTripSpec(878l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HARDISTY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2177", "3355", "3217" })) //
				.compileBothTripSort());
		map2.put(879l, new RouteTripSpec(879l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, OTTEWELL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE) // Mill Woods?
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2487", "2188", "2526", "2103" })) //
				.compileBothTripSort());
		map2.put(880l, new RouteTripSpec(880l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, KENILWORTH, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2160", "2188", "2105", "3529", "3211" })) //
				.compileBothTripSort());
		map2.put(881l, new RouteTripSpec(881l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_NALLY) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2151", "2301" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(882l, new RouteTripSpec(882l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, KENILWORTH, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2160", "2188", "2526", "2103", "3003", "3214" })) //
				.compileBothTripSort());
		map2.put(883l, new RouteTripSpec(883l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, VERNON_BARFORD, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, TWIN_BROOKS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"4340", // 119 Street & Fairway Drive
								"4238", // 119 Street & Fairway Drive
								"4265", // Twin Brooks Drive & 12 Avenue
								"4248", // Running Creek Road & 12 Avenue
						})) //
				.compileBothTripSort());
		map2.put(884l, new RouteTripSpec(884l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WP_WAGNER, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LAKEWOOD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2418", "3008", "3023", "3008" })) //
				.compileBothTripSort());
		map2.put(885l, new RouteTripSpec(885l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, VERNON_BARFORD, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4270", "4238", "4214" })) //
				.compileBothTripSort());
		map2.put(886l, new RouteTripSpec(886l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, AVALON, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2009", "2207" })) //
				.compileBothTripSort());
		map2.put(887l, new RouteTripSpec(887l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HARRY_AINLAY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, TWIN_BROOKS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4143", "4204", "4265", "4248" })) //
				.compileBothTripSort());
		map2.put(888l, new RouteTripSpec(888l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, VERNON_BARFORD) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "4270", "4238", "4205" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(889l, new RouteTripSpec(889l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2206", "4490", "4143", "4198", "4205", //
								"4290", "4203" })) //
				.compileBothTripSort());
		map2.put(890l, new RouteTripSpec(890l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2974", "2201" })) //
				.compileBothTripSort());
		map2.put(892l, new RouteTripSpec(892l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HARRY_AINLAY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4490", "4486", "4208" })) //
				.compileBothTripSort());
		map2.put(893l, new RouteTripSpec(893l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, HARRY_AINLAY) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "4490", "3004", "3217" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(894l, new RouteTripSpec(894l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA) // Allendale
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2741", "2974", "2102" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(895l, new RouteTripSpec(895l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2821", "2664", "2212" })) //
				.compileBothTripSort());
		map2.put(896l, new RouteTripSpec(896l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, RIVERBEND, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4021", "4803" })) //
				.compileBothTripSort());
		map2.put(897l, new RouteTripSpec(897l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LAKEWOOD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3002", "3214", "3740", "2110" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(898l, new RouteTripSpec(898l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2974", "2102", "3001", "3217" })) //
				.compileBothTripSort());
		map2.put(899l, new RouteTripSpec(899l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "5991", "5061", "5069", "5903", "5012" })) //
				.compileBothTripSort());
		map2.put(901l, new RouteTripSpec(901l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LONDONDERRY, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, _142_ST_109_AVE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5055", "5548", "7011", "6304", "7456" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(902l, new RouteTripSpec(902l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, PARKVIEW, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ORMSBY_PL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5656", "5611", "5755", "5828", "5725" })) //
				.compileBothTripSort());
		map2.put(903l, new RouteTripSpec(903l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, QUEEN_ELIZABETH) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7383", "7260", "7909" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(907l, new RouteTripSpec(907l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HADDOW) // Rhatigan Rdg
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2974", "4021", "4016" })) //
				.compileBothTripSort());
		map2.put(908l, new RouteTripSpec(908l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, FRASER, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, EASTGLEN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "1033", "7237" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(909l, new RouteTripSpec(909l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EASTGLEN) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "1185", "7120", "7009", "6315" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(913l, new RouteTripSpec(913l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5211", "7011", "6313" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(914l, new RouteTripSpec(914l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, OXFORD, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5207", "6328", "6337" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(916l, new RouteTripSpec(916l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BATURYN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"5206", //
								"1725", // ==
								"1759", // !=
								"1867", "1735", // !=
								"6122", // ==
								"6002"//
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(917l, new RouteTripSpec(917l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, FR_TROY, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JACKSON_HTS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3411", "3267" })) //
				.compileBothTripSort());
		map2.put(918l, new RouteTripSpec(918l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, FR_TROY, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JACKSON_HTS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3964", "3420" })) //
				.compileBothTripSort());
		map2.put(919l, new RouteTripSpec(919l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, EASTGLEN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ABBOTTSFIELD) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "1033", "1521", "1001" })) //
				.compileBothTripSort());
		map2.put(920l, new RouteTripSpec(920l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MINCHAU, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HOLY_FAMILY) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3153", "3363" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(921l, new RouteTripSpec(921l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_NALLY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SILVERBERRY) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2676", "3230", "3419" })) //
				.compileBothTripSort());
		map2.put(922l, new RouteTripSpec(922l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "5548", "4579", "4806" })) //
				.compileBothTripSort());
		map2.put(923l, new RouteTripSpec(923l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, TWIN_BROOKS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2204", "4490", "4204", "4265", "4248" })) //
				.compileBothTripSort());
		map2.put(924l, new RouteTripSpec(924l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DAN_KNOTT) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "3572", "3006", "3208" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(925l, new RouteTripSpec(925l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WINDSOR_PARK) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2974", "2844" })) //
				.compileBothTripSort());
		map2.put(926l, new RouteTripSpec(926l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, OTTEWELL, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2050", // 61 Street & 94B Avenue
								"2287", // Ottewell Road & 94 Avenue
								"2752", // == 112 Street & 82 Avenue
								"2982", // != 114 Street & 83 Avenue
								"22354", // != 114 Street & 83 Avenue
								"2638", // == 114 Street & 85 Avenue
								"2001", // University Transit Centre
								"2702", // South Campus Transit Centre Fort Edmonton Park
								"5296", // ++
								"5006", // West Edmonton Mall Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(931l, new RouteTripSpec(931l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LA_ZERTE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LAGO_LINDO) // KLARVATTEN
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7384", "7483" })) //
				.compileBothTripSort());
		map2.put(932l, new RouteTripSpec(932l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW_WEST_TC, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LA_ZERTE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7384", "7241", "7604", "7901" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(934l, new RouteTripSpec(934l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LA_ZERTE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7572", "6311", "6008" })) //
				.compileBothTripSort());
		map2.put(935l, new RouteTripSpec(935l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HOLLICK_KENYON, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_LEOD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "7160", "7535", "7298", "7140" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(939l, new RouteTripSpec(939l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ELSINORE, // CHAMBERY
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MARY_BUTTERWORTH) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "6285", "6166", "6674" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(940l, new RouteTripSpec(940l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ABBOTTSFIELD, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, AMISKWACIY) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "1979", "1476", "1201", "1001" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(941l, new RouteTripSpec(941l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ABBOTTSFIELD, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, AOB) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2915", //
								"1086", // ==
								"1001", // !=
								"1003" // !=
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(943l, new RouteTripSpec(943l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BELVEDERE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, AOB) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2915", "1206", "7210" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(945l, new RouteTripSpec(945l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, _88_ST_132_AVE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "7496", "6315" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(948l, new RouteTripSpec(948l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, HILLCREST, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LA_PERLE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5713", "5011", "5024" })) //
				.compileBothTripSort());
		map2.put(949l, new RouteTripSpec(949l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, HILLCREST, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LA_PERLE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5713", "5012", "5024" })) //
				.compileBothTripSort());
		map2.put(950l, new RouteTripSpec(950l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BERIAULT, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LYMBURN_ORMSBY_PL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] {/* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5894", "5419", "5725" })) //
				.compileBothTripSort());
		map2.put(952l, new RouteTripSpec(952l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CRESTWOOD, // RIO_TERRACE
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ST_FRANCIS_XAVIER) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5071", "5174", "5433", //
								"5588", // ==
								"5198", // !=
								"5043", // !=
								"5120" // !=
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(953l, new RouteTripSpec(953l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LYMBURN) // ORMSBY_PL
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5355", "5004", "5755", "5828", "5725" })) //
				.compileBothTripSort());
		map2.put(954l, new RouteTripSpec(954l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEDGEWOOD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "5687", "5002", "5979", "5968" })) //
				.compileBothTripSort());
		map2.put(955l, new RouteTripSpec(955l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LA_PERLE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5355", "5011", "5024" })) //
				.compileBothTripSort());
		map2.put(956l, new RouteTripSpec(956l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, THE_GRANGE) // THE_HAMPTONS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5687", "8135", "8097", "8102" })) //
				.compileBothTripSort());
		map2.put(957l, new RouteTripSpec(957l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, OSCAR_ROMERO, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, THE_HAMPTONS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5980", "5695", "8583", "8033", "8670" })) //
				.compileBothTripSort());
		map2.put(959l, new RouteTripSpec(959l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL_TC, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, OSCAR_ROMERO) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5695", "5002" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(965l, new RouteTripSpec(965l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BRAEMAR, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2462", "1989" })) //
				.compileBothTripSort());
		map2.put(966l, new RouteTripSpec(966l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, OTTEWELL_AOB, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2487", //
								"2064", //
								"2915", //
								"2360", //
								"2426", //
								"2434", //
								"3355", "3157", "3217" })) //
				.compileBothTripSort());
		map2.put(967l, new RouteTripSpec(967l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WHITEMUD_DR_53_AVE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4021", "4353", "4809" })) //
				.compileBothTripSort());
		map2.put(968l, new RouteTripSpec(968l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ST_ROSE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "5611", "4579", "4806" })) //
				.compileBothTripSort());
		map2.put(969l, new RouteTripSpec(969l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WHITEMUD_DR_53_AVE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4021", "4129", "4804" })) //
				.compileBothTripSort());
		map2.put(970l, new RouteTripSpec(970l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WHITEMUD_DR_53_AVE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, JOSEPH_MC_NEIL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"4950", "4636", "4811", "4597", //
								"4158", //
								"4153" //
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] {/* no stops */})) //
				.compileBothTripSort());
		map2.put(971l, new RouteTripSpec(971l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, _84_ST_105_AVE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, _34_ST_35A_AVE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2676", "3355", "3708", "3185" })) //
				.compileBothTripSort());
		map2.put(972l, new RouteTripSpec(972l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MAC_EWAN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, RUTHERFORD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "9251", "9848", "9685" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4213", "9666", "9242", "9251" })) //
				.compileBothTripSort());
		map2.put(973l, new RouteTripSpec(973l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_PHERSON, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BURNEWOOD) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3603", "3267" })) //
				.compileBothTripSort());
		map2.put(974l, new RouteTripSpec(974l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_PHERSON, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BURNEWOOD) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3603", "3420" })) //
				.compileBothTripSort());
		map2.put(975l, new RouteTripSpec(975l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2106", "3355", "3748", "3185", "3206" })) //
				.compileBothTripSort());
		map2.put(976l, new RouteTripSpec(976l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, RIVERBEND, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4021", "4803", "4202" })) //
				.compileBothTripSort());
		map2.put(977l, new RouteTripSpec(977l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MEADOWS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "3217", "3470", "3703" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(979L, new RouteTripSpec(979L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"4207", // Century Park Transit Centre
								"3214", // Mill Woods Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()));
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	private static final Pattern N_A_I_T = Pattern.compile("((^|\\W){1}(n a i t)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String N_A_I_T_REPLACEMENT = "$2" + NAIT + "$4";

	private static final Pattern SUPER_EXPRESS = Pattern.compile("((^|\\W){1}(super express)(\\W|$){1})", Pattern.CASE_INSENSITIVE);

	private static final Pattern STARTS_WITH_RSN = Pattern.compile("(^[\\d]+\\s)", Pattern.CASE_INSENSITIVE);

	private static final String VIA = " via ";

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		int indexOfVIA = tripHeadsign.toLowerCase(Locale.ENGLISH).indexOf(VIA);
		if (indexOfVIA >= 0) {
			tripHeadsign = tripHeadsign.substring(indexOfVIA); // remove trip head sign from stop head sign
		}
		tripHeadsign = STARTS_WITH_RSN.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = TRANSIT_CENTER.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = TOWN_CENTER.matcher(tripHeadsign).replaceAll(TOWN_CENTER_REPLACEMENT);
		tripHeadsign = SUPER_EXPRESS.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = EDMONTON.matcher(tripHeadsign).replaceAll(EDMONTON_REPLACEMENT);
		tripHeadsign = N_A_I_T.matcher(tripHeadsign).replaceAll(N_A_I_T_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		tripHeadsign = CleanUtils.removePoints(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern TRANSIT_CENTER = Pattern.compile("((^|\\W){1}(transit center|transit centre)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String TRANSIT_CENTER_REPLACEMENT = "$2" + TRANSIT_CENTER_SHORT + "$4";

	private static final Pattern TOWN_CENTER = Pattern.compile("((^|\\W){1}(town center|town centre)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String TOWN_CENTER_REPLACEMENT = "$2TC$4";

	private static final Pattern INTERNATIONAL = Pattern.compile("((^|\\W){1}(international)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String INTERNATIONAL_REPLACEMENT = "$2Int$4";

	private static final Pattern EDMONTON = Pattern.compile("((^|\\W){1}(edmonton)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String EDMONTON_REPLACEMENT = "$2" + EDM + "$4";

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = TRANSIT_CENTER.matcher(gStopName).replaceAll(TRANSIT_CENTER_REPLACEMENT);
		gStopName = TOWN_CENTER.matcher(gStopName).replaceAll(TOWN_CENTER_REPLACEMENT);
		gStopName = INTERNATIONAL.matcher(gStopName).replaceAll(INTERNATIONAL_REPLACEMENT);
		gStopName = EDMONTON.matcher(gStopName).replaceAll(EDMONTON_REPLACEMENT);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@Override
	public int getStopId(GStop gStop) {
		return Math.abs(super.getStopId(gStop)); // remove negative stop IDs
	}

	private static final Pattern REMOVE_STARTING_DASH = Pattern.compile("(^\\-)", Pattern.CASE_INSENSITIVE);

	@Override
	public String getStopCode(GStop gStop) {
		String stopCode = super.getStopCode(gStop); // do not change, used by real-time API
		stopCode = REMOVE_STARTING_DASH.matcher(stopCode).replaceAll(StringUtils.EMPTY);
		return stopCode; // do not change, used by real-time API
	}
}
