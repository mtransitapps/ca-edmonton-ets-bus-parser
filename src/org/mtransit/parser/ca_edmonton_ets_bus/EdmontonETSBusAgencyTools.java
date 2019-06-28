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
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
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
		if ("Not In Service".equalsIgnoreCase(gTrip.getTripHeadsign())) {
			return true; // exclude
		}
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
		return Long.parseLong(getRouteShortName(gRoute)); // using route short name as route ID
	}

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
	private static final String MILL_WOODS_ = MILL_WOODS + " TC";
	private static final String MILL_WOODS_TC = MILL_WOODS + " TC";
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
	private static final String _84_ST_111_AVE = "84" + _ST + SLASH + "111" + _AVE;
	private static final String _85_ST_132_AVE = "85" + _ST + SLASH + "132" + _AVE;
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
	private static final String LAKEWOOD = "Lakewood";
	private static final String WOODVALE = "Woodvale";
	private static final String VERNON_BARFORD = "Vernon Barford";
	private static final String BELLE_RIVE = "Belle Rive";
	private static final String LENDRUM = "Lendrum";

	@Override
	public String getRouteLongName(GRoute gRoute) {
		return cleanRouteLongName(gRoute.getRouteLongName());
	}

	private String cleanRouteLongName(String gRouteLongName) {
		gRouteLongName = CleanUtils.cleanStreetTypes(gRouteLongName);
		return CleanUtils.cleanLabel(gRouteLongName);
	}

	@Override
	public boolean mergeRouteLongName(MRoute mRoute, MRoute mRouteToMerge) {
		if (mRoute.simpleMergeLongName(mRouteToMerge)) {
			return super.mergeRouteLongName(mRoute, mRouteToMerge);
		}
		if (isGoodEnoughAccepted()) {
			return super.mergeRouteLongName(mRoute, mRouteToMerge);
		}
		System.out.printf("\nUnexpected routes to merge: %s & %s!\n", mRoute, mRouteToMerge);
		System.exit(-1);
		return false;
	}

	@Override
	public String getRouteShortName(GRoute gRoute) {
		if (Utils.isDigitsOnly(gRoute.getRouteShortName())) {
			return gRoute.getRouteShortName();
		}
		if (Utils.isDigitsOnly(gRoute.getRouteId())) {
			return gRoute.getRouteId();
		}
		System.out.printf("\nUnexpected route ID for %s!\n", gRoute);
		System.exit(-1);
		return null;
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
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
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
		map2.put(1L, new RouteTripSpec(1L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5009", // West Edmonton Mall Transit Centre
								"5302", // Meadowlark Transit Centre
								"5426", // ==
								"5110", // != Jasper Place Transit Centre
								"-55552", // !=
								"5112", // != 157 Street & Stony Plain Road #JasperPlace
								"5021", // ==
								"5169", // == 142 Street & Stony Plain Road
								"5440", // !=
								"1917", // !=
								"1242", // == 124 Street & 102 Avenue
								"1322", // == 103 Street & Jasper Avenue
								"1336", // != 101 Street & Jasper Avenue =>
								"1346", // != 101 Street & 101A Avenue
								"2591", // 79 Street & 106 Avenue
								"2301", // Capilano Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2301", // Capilano Transit Centre
								"2267", // 79 Street & 106 Avenue
								"1620", // 101 Street & Jasper Avenue
								"1746", // == 122 Street & 102 Avenue
								"1971", // !=
								"5087", // !=
								"5157", // == 140 Street & Stony Plain Road
								"5494", // ==
								"5101", // != Jasper Place Transit Centre
								"-55551", // !=
								"5106", // != 157 Street & 100A Avenue #JasperPlace
								"5580", // ==
								"5303", // !≃ Meadowlark Transit Centre
								"5241", // ==
								"5009", // West Edmonton Mall Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(2L, new RouteTripSpec(2L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LESSARD) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5723", // 172 Street & Callingwood Road
								"5008", // West Edmonton Mall Transit Centre
								"5437", // ++ 149 Street & 100 Avenue
								"1336", // 101 Street & Jasper Avenue
								"1256", // ++ Thornton Court & Jasper Avenue
								"1408", // ++ 84 Street & 111 Avenue
								"1584", // == !=
								"1561", // != <> 50 Street & 118 Avenue => LESSARD
								"1454", // != 50 Street & 118 Avenue
								"7902", // West Clareview Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"7902", // West Clareview Transit Centre
								"1786", // !=
								"1561", // <> 50 Street & 118 Avenue
								"1228", // !=
								"1407", // 83 Street & 111 Avenue
								"1266", // 97 Street & Jasper Avenue
								"1620", // 101 Street & Jasper Avenue
								"5185", // ++ 142 Street & Stony Plain Road
								"5002", // West Edmonton Mall Transit Centre
								"5003", // West Edmonton Mall Transit Centre
								"5841", // 178 Street & 76 Avenue
								"5723", // 172 Street & Callingwood Road
						})) //
				.compileBothTripSort());
		map2.put(3L, new RouteTripSpec(3L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CROMDALE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5112", // != 157 Street & Stony Plain Road #JasperPlace
								"5360", // ==
								"5928", // ++
								"5330", // ==
								"5331", // !=
								"5531", // !=
								"5224", // ==
								"1279", // ++
								"1360", // 101 Street & 107 Avenue
								"1243", // ==
								"1142", // != 101 Street & MacDonald Drive =>
								"1336", // !=
								"1256", // Thornton Court & Jasper Avenue
								"1147", // 82 Street & 115 Avenue Loop
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1147", // 82 Street & 115 Avenue Loop
								"1346", // 101 Street & 101A Avenue NS
								"1775", // ++
								"1846", // ++
								"1669", // ++
								"5389", // ++
								"5476", // ==
								"5112", // != 157 Street & Stony Plain Road #JasperPlace
						})) //
				.compileBothTripSort());
		map2.put(4L, new RouteTripSpec(4L, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEWIS_FARMS, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"8601", "8604", // Lewis Farms Transit Centre
								"5463", // !=
								"5006", // <> West Edmonton Mall Transit Centre
								"5054", // !=
								"22159", // !=
								"2707", // <> South Campus Fort Edmonton Transit Centre Bay A
								"2702", // <> South Campus Transit Centre Fort Edmonton Park // LAST
								"2712", "2713", "2714", // South Campus Fort Edmonton Transit Centre // CONTINUE
								"2748", // == !=
								"2982", // != <>
								"2638", // == <>
								"2625", // != <>
								"2890", // == <> 114 Street & 89 Avenue
								"2002", // <> University Transit Centre
								"2410", // <>
								"2834", // !=
								"2065", // == 87 Street & 82 Avenue
								"2593", // != 85 Street & 82 Avenue
								"2196", // != 83 Street & 90 Avenue
								"2952", // != 83 Street & 84 Avenue
								"2159", // <> 83 Street & 82 Avenue // LAST
								"2255", // 83 Street & 82 Avenue
								"2447", // 83 Street & 82 Avenue // CONTINUE
								"2222", // ++
								"2372", // ++
								"2306", // Capilano Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2306", // Capilano Transit Centre
								"2532", // ++
								"2476", "2568", "2050", "2462", "2161", "2287", "2288", "2494", "2376", "2231", "2015", "22666", "22659", "2167", // ++
								"2193", // ++
								"2037", // !=
								"2159", // <> 83 Street & 82 Avenue // CONTINUE
								"2590", // !=
								"2340", // ++
								"2087", "2131", "2294", "2236", "2033", "2659", "2853", "2723", "2891", "2845", "2683", "2893", "2788", "2689", //
								"2733", // ++
								"2752", // == !=
								"2982", // != <>
								"2638", // == <>
								"2625", // != <>
								"2890", // == <> 114 Street & 89 Avenue
								"2001", "2002", // <> != University Transit Centre
								"2410", // <>
								"2660", // !=
								"22156", // !=
								"2707", // <> South Campus Fort Edmonton Transit Centre Bay A
								"2702", // <> South Campus Transit Centre Fort Edmonton Park
								"22160", // !=
								"5449", // !=
								"5006", // <> West Edmonton Mall Transit Centre // LAST
								"5002", // West Edmonton Mall Transit Centre
								"5003", // West Edmonton Mall Transit Centre // CONTINUE
								"5042", // ++
								"5819", // ++
								"8601", "8604", // Lewis Farms Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(5L, new RouteTripSpec(5L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLISEUM, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5210", "1083", "1336", "1188",
						/* + */"1051"/* + */, //
								"1268", "1202" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "1202", "1328", "1620", "5210" })) //
				.compileBothTripSort());
		map2.put(6L, new RouteTripSpec(6L, //
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
		map2.put(7L, new RouteTripSpec(7L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5112", // 157 Street & Stony Plain Road #JasperPlace <=
								"1881", // 124 Street & 107 Avenue
								"1829", // 105 Street & 105 Avenue
								"1570", // !=
								"1358", // != <> 99 Street & 104 Avenue <=
								"1608", // ==
								"1542", // ++
								"2659", // ++
								"2891", // ++
								"2882", // !=
								"2890", // <> 114 Street & 89 Avenue
								"2002", // <> University Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2890", // <> 114 Street & 89 Avenue
								"2002", // <> University Transit Centre
								"22720", // !=
								"2860", // ++
								"2824", // ++
								"1457", // ==
								"1321", // !=
								"1246", // !=
								"1364", // ==
								"1358", // != <> 99 Street & 104 Avenue =>
								"11334", // !=
								"1989", // 108 Street & 104 Avenue
								"1808", // ++
								"5112", // 157 Street & Stony Plain Road #JasperPlace
						})) //
				.compileBothTripSort());
		map2.put(8L, new RouteTripSpec(8L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ABBOTTSFIELD, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"3207", // != Mill Woods Transit Centre <=
								"3208", // != Mill Woods Transit Centre <=
								"3122", // ==
								"3244", // !=
								"3338", // !=
								"3264", // ==
								"3347", // ==
								"2108", // != Millgate Transit Centre
								"2117", // != Millgate Transit Centre
								"2026", // ==
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
								"2024", // ==
								"2103", // != Millgate Transit Centre
								"2118", // != Millgate Transit Centre
								"3281", // ==
								"3599", // ==
								"3394", // !=
								"3121", // ==
								"3207", // != Mill Woods Transit Centre =>
								"3208", // != Mill Woods Transit Centre =>
						})) //
				.compileBothTripSort());
		map2.put(9L, new RouteTripSpec(9L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) // SOUTHGATE
				.addTripSort(MDirectionType.NORTH.intValue(), // CENTURY_PK / SOUTHGATE => EAUX_CLAIRES
						Arrays.asList(new String[] { //
						"4216", // Century Park Transit Centre
								"4054", // !=
								"2218", // == <> Southgate Transit Centre
								"2623", // == !=
								"22739", // !=
								"2830", // !=
								"22738", // !=
								"2852", // ==
								"1495", // ++
								"1591", // 101 Street & MacDonald Drive
								"1108", // Kingsway RAH Transit Centre
								"1476", // 106 Street & 118 Avenue
								"7011", "7016", // Northgate Transit Centre
								"6317", // Eaux Claires Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), // EAUX_CLAIRES => CENTURY_PK / SOUTHGATE
						Arrays.asList(new String[] { //
						"6317", // Eaux Claires Transit Centre
								"7001", "7003", // Northgate Transit Centre
								"1532", // 106 Street & 118 Avenue Loop
								"1104", // Kingsway RAH Transit Centre
								"1142", // 101 Street & MacDonald Drive nearside
								"2631", // ==
								"2895", // !=
								"2833", // !=
								"-22352", // !=
								"2773", // ==
								"2639", // == !=
								"-22223", //
								"2218", // != <> Southgate Transit Centre => END
								"2206", // Southgate Transit Centre
								"4216", // Century Park Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(10L, new RouteTripSpec(10L, //
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
		map2.put(11L, new RouteTripSpec(11L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"7007", // Northgate Transit Centre
								"7186", // 69 Street & 144 Avenue
								"7106", // East Clareview Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"7106", // East Clareview Transit Centre
								"7572", // 66 Street & 144 Avenue
								"7008", // Northgate Transit Centre
								"7496", // 88 Street & 132 Avenue
								"7060", // 95 Street & 132 Avenue
								"7007", // Northgate Transit Centre
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
								"-11562", //
								"1110", // != Kingsway RAH Transit Centre => NORTHGATE
								"1113", // != Kingsway RAH Transit Centre
								"1243", // !=
								"1251", // 102 Street & MacDonald Drive
						})) //
				.compileBothTripSort());
		map2.put(13L, new RouteTripSpec(13L, //
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
		map2.put(14L, new RouteTripSpec(14L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5011", // West Edmonton Mall Transit Centre
								"5024", // 180 Street & 98 Avenue
								"5153", // == 159 Street & Stony Plain Road
								"5112", // == 157 Street & Stony Plain Road #JasperPlace
								"5103", // != Jasper Place Transit Centre
								"-55551", //
								"5105", // != 156 Street & 100A Avenue =>
								"5293", // != 143 Street & Stony Plain Road
								"1999", // != 100 Street & 103A Avenue nearside =>
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1123", // 99 Street & 103A Avenue nearside
								"1812", // == 111 Street & Jasper Avenue Nearside
								"1828", // != 124 Street & 102 Avenue
								"1971", // != 124 Street & 102 Avenue
								"5185", // == 142 Street & Stony Plain Road
								"5103", // Jasper Place Transit Centre
								"5105", // 156 Street & 100A Avenue <=
								"5855", // 182 Street & 97A Avenue
								"5011", // West Edmonton Mall Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(15L, new RouteTripSpec(15L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"3207", // != Mill Woods Transit Centre <=
								"3208", // == Mill Woods Transit Centre <=
								"3244", // !=
								"3122", // !=
								"3008", // ==
								"3659", // ==
								"2117", // != Millgate Transit Centre
								"2108", // != Millgate Transit Centre
								"2338", // ==
								"1457", // 100 Street & Jasper Avenue
								"1989", // 108 Street & 104 Avenue
								"1227", // ==
								"1532", // != 106 Street & 118 Avenue Loop
								"1476", // != 106 Street & 118 Avenue
								"7016", // Northgate Transit Centre
								"6317", // Eaux Claires Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"6317", // Eaux Claires Transit Centre
								"7001", "7003", // Northgate Transit Centre
								"1532", // 106 Street & 118 Avenue Loop
								"1557", // 109 Street & 105 Avenue
								"1542", // 100 Street & Jasper Avenue
								"2385", // ==
								"2118", // != Millgate Transit Centre
								"2103", // != Millgate Transit Centre
								"3499", // ==
								"3121", // ==
								"3207", // != Mill Woods Transit Centre =>
								"3208", // != Mill Woods Transit Centre =>
						})) //
				.compileBothTripSort());
		map2.put(16L, new RouteTripSpec(16L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, GOV_CTR) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "1310", "7011", "6314", "6075", "6576", "6009" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "6009", "6124", "6317",
						/* + */"7011"/* + */, //
								"7003", "1310" })) //
				.compileBothTripSort());
		map2.put(17L, new RouteTripSpec(17L, //
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
		map2.put(23L, new RouteTripSpec(23L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5001", // West Edmonton Mall Transit Centre
								"4803", // Leger Transit Centre
								"4202", // Century Park Transit Centre
								"3217", // Mill Woods Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"3217", // Mill Woods Transit Centre
								"4211", // Century Park Transit Centre
								"4814", // Leger Transit Centre
								"5001", // West Edmonton Mall Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(24L, new RouteTripSpec(24L, //
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
		map2.put(25L, new RouteTripSpec(25L, //
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
		map2.put(26L, new RouteTripSpec(26L, //
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
		// 30
		boolean _30_WithMillWoodsTC = false;
		List<String> _30_North = Arrays.asList(new String[] { //
				"4211", // Century Park Transit Centre
						"4811", // Leger Transit Centre
						"4597", // ++
						"4153", // ++
						"2704", "2707", "2714" // South Campus Fort Edmonton Transit Centre
				});
		String _30_SouthHeadsignString = CENTURY_PK;
		List<String> _30_South = Arrays.asList(new String[] { //
				"2704", // South Campus Fort Edmonton Transit Centre
						"4021", // ++
						"4494", // ++
						"4262", // ==
						"4811", // != Leger Transit Centre =>
						"4803", // != Leger Transit Centre
						"4211", // Century Park Transit Centre
				});
		if (_30_WithMillWoodsTC) {
			_30_North.add(0, "3217"); // Mill Woods Transit Centre
			//
			_30_SouthHeadsignString = MILL_WOODS_TC;
			_30_South.add(_30_South.size(), "3217"); // Mill Woods Transit Centre
		}
		map2.put(30L, new RouteTripSpec(30L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH_CAMPUS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, _30_SouthHeadsignString) //
				.addTripSort(MDirectionType.NORTH.intValue(), _30_North) //
				.addTripSort(MDirectionType.SOUTH.intValue(), _30_South) //
				.compileBothTripSort());
		//
		map2.put(31L, new RouteTripSpec(31L, //
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
		map2.put(32L, new RouteTripSpec(32L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH_CAMPUS_TC, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BRANDER_GDNS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"4025", // 148 Street & Riverbend Road nearside
								"4153", // Whitemud Drive NB & 53 Avenue
								"2703", "2705", "2707", "2714", // South Campus Fort Edmonton Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2703", "2705", // South Campus Fort Edmonton Transit Centre
								"4021", // Whitemud Drive SB & 53 Avenue
								"4025", // 148 Street & Riverbend Road nearside
						})) //
				.compileBothTripSort());
		map2.put(33L, new RouteTripSpec(33L, //
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
		map2.put(34L, new RouteTripSpec(34L, //
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
		map2.put(35L, new RouteTripSpec(35L, //
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
		map2.put(36L, new RouteTripSpec(36L, //
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
								"2703", "2707", "2714", // South Campus Transit Centre Fort Edmonton Park
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
		map2.put(37L, new RouteTripSpec(37L, //
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
		map2.put(38L, new RouteTripSpec(38L, //
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
		map2.put(39L, new RouteTripSpec(39L, //
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
		map2.put(40L, new RouteTripSpec(40L, //
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
		map2.put(41L, new RouteTripSpec(41L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "4208", "4168", "2213" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2213", "4486", "4208" })) //
				.compileBothTripSort());
		map2.put(42L, new RouteTripSpec(42L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "4209", "4070", "2217" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2217", "4342", "4209" })) //
				.compileBothTripSort());
		map2.put(43L, new RouteTripSpec(43L, //
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
								"2711", "2714", // South Campus Transit Centre Fort Edmonton Park
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
		map2.put(44L, new RouteTripSpec(44L, //
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
		map2.put(45L, new RouteTripSpec(45L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "4207", "4588", "2214" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2214", "2888", "4198", "4207" })) //
				.compileBothTripSort());
		map2.put(46L, new RouteTripSpec(46L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HARRY_AINLAY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, YELLOWBIRD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "4290", "4209", "4307" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4490", "4208", "4290" })) //
				.compileBothTripSort());
		map2.put(47L, new RouteTripSpec(47L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ALLARD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"9301", // Allard Boulevard & Alexander Way SW
								"9552", // James Mowatt Trail & 30 Avenue SW
								"9163", // ++ Callaghan Drive & Callaghan Point
								"4548", // == 111 Street & 23 Avenue
								"4214", // != Century Park Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"4214", // != Century Park Transit Centre
								"4456", // == 111 Street & 23 Avenue
								"9164", // Callaghan Drive & Callaghan Close
								"9301" // Allard Boulevard & Alexander Way SW
						})) //
				.compileBothTripSort());
		map2.put(48L, new RouteTripSpec(48L, //
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
		map2.put(49L, new RouteTripSpec(49L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BLACKMUD_CRK) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "9756", "9542", "4210" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4210",
						/* + */"4105"/* + */, //
								"9756" })) //
				.compileBothTripSort());
		map2.put(50L, new RouteTripSpec(50L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH_CAMPUS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2210", // Southgate Transit Centre
								"4277", // ++
								"2517", // ++
								"2957", // ++
								"2710", "2714", // South Campus Fort Edmonton Transit Centre Bay H
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2710", // South Campus Fort Edmonton Transit Centre Bay L
								"2510", // ++
								"2924", // ++
								"4474", // ++
								"2210", // Southgate Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(51L, new RouteTripSpec(51L, //
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
								"2001", "2002", // University Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2001", "2002", // University Transit Centre
								"2889", // ++
								"2795", // 112 Street & 65 Avenue nearside
						})) //
				.compileBothTripSort());
		map2.put(52L, new RouteTripSpec(52L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2212", // Southgate Transit Centre
								"2849", // 104 Street & 81 Avenue
								"2290", // ==
								"1425", // != =>
								"-11553", // !=
								"1728", // !=
								"1307", // Government Transit Centre
								"1777", // == 103 Street & Jasper Avenue
								"1292", // 100 Street & 102A Avenue
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"1292", // 100 Street & 102A Avenue
								"1620", // == 101 Street & Jasper Avenue
								"1305", // Government Transit Centre
								"1993", // !=
								"1425", // != <=
								"1567", // ==
								"2821", // 104 Street & 82 Avenue
								"2212", // Southgate Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(53L, new RouteTripSpec(53L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH_CAMPUS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2216", // Southgate Transit Centre
								"2973", // ++
								"2709", "2712", "2714", // South Campus Fort Edmonton Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2709", "2712", // South Campus Fort Edmonton Transit Centre
								"2511", // ++
								"2216", // Southgate Transit Centre
						})) //
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
		map2.put(55L, new RouteTripSpec(55L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BELGRAVIA, // SOUTH_CAMPUS
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2202", // Southgate Transit Centre
								"2707", // South Campus Fort Edmonton Transit Centre Bay A
								"2706", // South Campus Transit Centre Fort Edmonton Park
								"2765", // 118 Street & 73 Avenue
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2765", // 118 Street & 73 Avenue
								"2714", // South Campus Fort Edmonton Transit Centre Bay H
								"2709", // South Campus Transit Centre Fort Edmonton Park
								"2202", // Southgate Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(57L, new RouteTripSpec(57L, //
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
		map2.put(59L, new RouteTripSpec(59L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH_EDM_COMMON) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "3440", "3003", "3209" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3209", "3007", "3440" })) //
				.compileBothTripSort());
		map2.put(60L, new RouteTripSpec(60L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3210", "3585", "2104", "2101", "1780", "1989" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1824", "1780", "2104", "3233", "3210" })) //
				.compileBothTripSort());
		map2.put(61L, new RouteTripSpec(61L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3211", "3585", "2105", "2104", "1780", "1989" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1824", "1780", "2105", "3529", "3211" })) //
				.compileBothTripSort());
		map2.put(62L, new RouteTripSpec(62L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHWOOD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"3154", // Mill Woods Road E & 20 Avenue
								"3128", // !=
								"3126", // ==
								"-33219", //
								"3212", // != Mill Woods Transit Centre
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
		map2.put(63L, new RouteTripSpec(63L, //
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
								"3126", // <>
								"-33219", // <>
								"3204", // <> Mill Woods Transit Centre
								"3212", // <> Mill Woods Transit Centre
								"3127", // <>
								"3087", // !=
								"1358", // 99 Street & 104 Avenue
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"1358", // 99 Street & 104 Avenue
								"3090", // !=
								"3126", // <>
								"-33219", // <>
								"3204", // <> Mill Woods Transit Centre
								"3127", // <>
								"3129", // !=
								"3141", // !=
								"3143", // 48 Street & Mill Woods Road S
						})) //
				.compileBothTripSort());
		map2.put(64L, new RouteTripSpec(64L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, KNOTTWOOD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"3026", // Mill Woods Rd & Knottwood Rd N
								"3330", // != ==
								"3006", // <> != Lakewood Transit Centre
								"3009", // <> != Lakewood Transit Centre
								"3599", // != ==
								"3208", // Mill Woods Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"3208", // Mill Woods Transit Centre
								"3264", // != ==
								"3006", // <> != Lakewood Transit Centre
								"3009", // <> != Lakewood Transit Centre
								"3365", // != ==
								"3026", // Mill Woods Rd & Knottwood Rd N
						})) //
				.compileBothTripSort());
		map2.put(65L, new RouteTripSpec(65L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, KNOTTWOOD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"3023", // Mill Woods Rd & Knottwood Rd N
								"3330", // != ==
								"3006", // <> != Lakewood Transit Centre
								"3009", // <> != Lakewood Transit Centre
								"3599", // != ==
								"3208", // Mill Woods Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"3208", // Mill Woods Transit Centre
								"3264", // != ==
								"3006", // <> != Lakewood Transit Centre
								"3009", // <> != Lakewood Transit Centre
								"3365", // != ==
								"3023", // Mill Woods Rd & Knottwood Rd N
						})) //
				.compileBothTripSort());
		map2.put(66L, new RouteTripSpec(66L, //
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
		map2.put(67L, new RouteTripSpec(67L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MEADOWS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "3206", "3952", "3957", "3708" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3708", "3957", "3950", "3311", "3116", "3206" })) //
				.compileBothTripSort());
		map2.put(68L, new RouteTripSpec(68L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), Arrays.asList(new String[] { "3202", "3399", "3586", "2107", "2110", "1989" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), Arrays.asList(new String[] { "1824", "2107", "3230", "3584", "3202" })) //
				.compileBothTripSort());
		map2.put(69L, new RouteTripSpec(69L, //
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
		map2.put(70L, new RouteTripSpec(70L, //
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
		map2.put(71L, new RouteTripSpec(71L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, GOV_CTR, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"3216", // Mill Woods Transit Centre
								"2111", // ++ 86 Street & Millgate
								"2329", // !=
								"1614", // <> 109 Street & 97 Avenue
								"1303", "1305", // <> Government Transit Centre
								"1993", // <> 106 Street & 97 Avenue
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"1614", // <> 109 Street & 97 Avenue
								"1303", "1305", // <> Government Transit Centre
								"1993", // <> 106 Street & 97 Avenue
								"2393", // !=
								"2103", // ++ Millgate Transit Centre
								"3216", // Mill Woods Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(72L, new RouteTripSpec(72L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, // MILLGATE
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3206", "3255", "3796", "3491", "2106", "2106", "2110", "1989" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1989", "2110", "2106", "3355", "3748", "3185", "3206" })) //
				.compileBothTripSort());
		map2.put(73L, new RouteTripSpec(73L, //
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
		map2.put(74L, new RouteTripSpec(74L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2204", "4202",
						/* + */"3671"/* + */, //
								"3107", "3559", "3209" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3209", "3007", "3430", "3110", "4202", "4212", "2204" })) //
				.compileBothTripSort());
		map2.put(77L, new RouteTripSpec(77L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "4210", "9850", "9111", "3205" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3205", "9761", "9361", "4210" })) //
				.compileBothTripSort());
		map2.put(78L, new RouteTripSpec(78L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "4205", "3675", "9384", "9725", "3215" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3215", "9147", "9387", "3865", "4205" })) //
				.compileBothTripSort());
		map2.put(79L, new RouteTripSpec(79L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "4207", "3319", "9260", "9139", "3214" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3214", "9287", "9671", "3513", "4207" })) //
				.compileBothTripSort());
		map2.put(80L, new RouteTripSpec(80L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2218", // Southgate Transit Centre
								"2769", // ++
								"2826", // 86 Street & Davies Road
								"2551", // ++
								"2599", // ++
								"2223", // ++
								"2305", // Capilano Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2305", // Capilano Transit Centre
								"2152", // ++
								"2264", // ++
								"2188", // 83 Street & 82 Avenue
								"2549", // 83 Street & 82 Avenue
								"2622", // 86 Street & 58 Avenue
								"2837", // ++
								"2888", // ++
								"2218", // Southgate Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(81L, new RouteTripSpec(81L, //
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
		map2.put(82L, new RouteTripSpec(82L, //
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
		map2.put(83L, new RouteTripSpec(83L, //
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
		map2.put(84L, new RouteTripSpec(84L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2111", "2303" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2303", "2112" })) //
				.compileBothTripSort());
		map2.put(85L, new RouteTripSpec(85L, //
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
		map2.put(86L, new RouteTripSpec(86L, //
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
		map2.put(87L, new RouteTripSpec(87L, //
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
		map2.put(88L, new RouteTripSpec(88L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "1680", "1336", "2274", "2449", "2307" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2307", "2298", "2267", "1718" })) //
				.compileBothTripSort());
		map2.put(89L, new RouteTripSpec(89L, //
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
		map2.put(90L, new RouteTripSpec(90L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MEADOWS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "1824", "2255", "3707" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3709", "2551", "1989" })) //
				.compileBothTripSort());
		map2.put(91L, new RouteTripSpec(91L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHLANDS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2307",
						/* + */"2425"/* + */, //
								"1371" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1371", "1131", "2307" })) //
				.compileBothTripSort());
		map2.put(92L, new RouteTripSpec(92L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, PLYPOW, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2101", "2118", "2876", /* + */"22330"/* + */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /*-"2876"-*//* + */"22330"/* + */, /* + */"22196"/* + */, "2118", "2101" })) //
				.compileBothTripSort());
		map2.put(94L, new RouteTripSpec(94L, //
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
		map2.put(95L, new RouteTripSpec(95L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MEADOWS, // LAUREL
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"3213", // Mill Woods Transit Centre
								"3775", // ==
								"3952", // != != 34 Street & 23 Avenue
								"3618", // != 34 Street & 23 Avenue CONTNUE
								"3762", // != !=
								"3753", // != <>
								"3746", // <>
								"3503", // <>
								"3304", // <>
								"3305", // != <> 32 Street & 22 Avenue
								"3759", // != != 32 Street & 23 Avenue
								"3852", // != 24 Street & 23 Avenue
								"3844", // != <> 24 Street & 23 Avenue => MILL_WOODS_TC
								"3303", // != <> 32 Street & 22 Avenue => MILL_WOODS_TC
								"3306", // != <>
								"3501", // <>
								"3736", // <>
								"3747", // != <> 32 Street & 17B Avenue
								"3758", // != 32 Street & 16A Avenue
								"3846", // !=
								"3858", // ==
								"3703", // Meadows Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"3703", // Meadows Transit Centre
								"3782", // == 17 Street & 23 Avenue
								"3844", // !== <> 24 Street & 23 Avenue
								"3963", // != !=
								"3788", // !=
								"3753", // <>
								"3746", // <>
								"3503", // <>
								"3304", // <>
								"3305", // <> 32 Street & 22 Avenue
								"3763", // !== 32 Street & 23 Avenue
								"3855", // !== 24 Street & 23 Avenue
								"3777", // != 24 Street & 23 Avenue
								"3303", // != <> 32 Street & 22 Avenue
								"3306", // != <>
								"3501", // <>
								"3736", // <>
								"3747", // != <> 32 Street & 17B Avenue
								"3761", // != 32 Street & 16A Avenue
								"3620", // !== 34 Street & 18 Avenue
								"3116", // == 34 Street & 23 Avenue
								"3213", // Mill Woods Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(96L, new RouteTripSpec(96L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BONNIE_DOON, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2110", /* + */"2433"/* + */, "2196" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2196", /* + */"2074"/* + */, "2110" })) //
				.compileBothTripSort());
		map2.put(97L, new RouteTripSpec(97L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NAIT, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3217", "3008", "2111", "1702", "1059" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1093", "1824", "2112", "3002", "3217" })) //
				.compileBothTripSort());
		map2.put(98L, new RouteTripSpec(98L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NAIT, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5219", "1059" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1093", "5003" })) //
				.compileBothTripSort());
		map2.put(99L, new RouteTripSpec(99L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BELVEDERE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2304", "1206", "7211" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7211", "1207", "2304" })) //
				.compileBothTripSort());
		map2.put(100L, new RouteTripSpec(100L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEWIS_FARMS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"1243", // 101 Street & 102 Avenue
								"5449", // != ==
								"5001", // != <> West Edmonton Mall Transit Centre =>
								"5010", // != West Edmonton Mall Transit Centre
								"8601", "8605", "8610", // Lewis Farms Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"8605", "8610", // Lewis Farms Transit Centre
								"5001", // <> West Edmonton Mall Transit Centre
								"5054", // !=
								"1256", // Thornton Court & Jasper Avenue
								"1243", // 101 Street & 102 Avenue
						})) //
				.compileBothTripSort());
		map2.put(101L, new RouteTripSpec(101L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEDGEWOOD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"5968", // ++ Wedgewood Boulevard Loop
								"5908", // 187 Street & 52 Avenue
								"5821", //
								"5002", // != West Edmonton Mall Transit Centre
								"5003", // != West Edmonton Mall Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"5002", // != West Edmonton Mall Transit Centre
								"5003", // != West Edmonton Mall Transit Centre
								"5979", // ++
								"5660", // 187 Street & 52 Avenue
								"5968", // ++ Wedgewood Boulevard Loop
						})) //
				.compileBothTripSort());
		map2.put(102L, new RouteTripSpec(102L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LYMBURN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5828", // ++ 188 Street & Ormsby Road West
								"5725", // ++
								"5004", // != West Edmonton Mall Transit Centre
								"5005", // != West Edmonton Mall Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"5004", // != West Edmonton Mall Transit Centre
								"5005", // != West Edmonton Mall Transit Centre
								"5755", // 183A Street & 76 Avenue
								"5828", // ++ 188 Street & Ormsby Road West
						})) //
				.compileBothTripSort());
		map2.put(103L, new RouteTripSpec(103L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAMERON_HTS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"5752", // Cameron Heights Drive & Cameron Heights Way
								"5695", //
								"5821", //
								"5002", // != West Edmonton Mall Transit Centre
								"5003", // != West Edmonton Mall Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"5002", // != West Edmonton Mall Transit Centre
								"5003", // != West Edmonton Mall Transit Centre
								"5979", //
								"5623", //
								"5752", // Cameron Heights Drive & Cameron Heights Way
						})) //
				.compileBothTripSort());
		map2.put(104L, new RouteTripSpec(104L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH_CAMPUS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LYMBURN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5828", // 188 Street & Ormsby Road West
								"5725", // 183A Street & 76 Avenue
								"5821", // ++
								"2706", "2707", "2714", // South Campus Fort Edmonton Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2706", // South Campus Fort Edmonton Transit Centre
								"5755", // 183A Street & 76 Avenue
								"5828", // 188 Street & Ormsby Road West
						})) //
				.compileBothTripSort());
		map2.put(105L, new RouteTripSpec(105L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH_CAMPUS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LESSARD) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5017", // 188 Street & Lessard Road
								"5932", // Lessard Road & 57 Avenue
								"5733", // ++
								"5821", // ++
								"2706", "2707", "2714", // South Campus Fort Edmonton Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2706", // South Campus Fort Edmonton Transit Centre
								"5634", // Lessard Road & 57 Avenue
								"5017", // 188 Street & Lessard Road
						})) //
				.compileBothTripSort());
		map2.put(106L, new RouteTripSpec(106L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LESSARD) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5733", // 172 Street & Callingwood Road
								"5650", //
								"5900", "5757", "5722", "5638", "5671", "5974", "5821", "5749", "5923", "5750", "55070", //
								"5463", // !=
								"5007", // West Edmonton Mall Transit Centre CONTINUE
								"5054", // ++
								"5186", "5486", "5566", "5578", "5359", "5281", "5197", "5332", "5451", "5499", "5298", "4425", "22162", "2978", // ,
								"22159", //
								"2714", // South Campus Fort Edmonton Transit Centre
								"2708", // South Campus Transit Centre Fort Edmonton Park
								"2885", //
								"22157", "2959", "2944", "2505", "2516", "2748", "2982", "2638", //
								"2625", // !=
								"2890", // <> 114 Street & 89 Avenue
								"2001", "2002", // <> University Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2890", // <> 114 Street & 89 Avenue
								"2001", "2002", // <> University Transit Centre
								"22720", // !=
								"5449", // !=
								"5005", // != West Edmonton Mall Transit Centre
								"5653", // !=
								"5797", // ++
								"5733", // 172 Street & Callingwood Road
						})) //
				.compileBothTripSort());
		map2.put(107L, new RouteTripSpec(107L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTRIDGE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"5747", //
								"5657", //
								"5004", // != West Edmonton Mall Transit Centre
								"5005", // != West Edmonton Mall Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"5004", // != West Edmonton Mall Transit Centre
								"5005", // != West Edmonton Mall Transit Centre
								"5877", //
								"5747", //
						})) //
				.compileBothTripSort());
		map2.put(108L, new RouteTripSpec(108L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEWIS_FARMS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BRECKENRIDGE_GRNS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"8670", // The Fairways & Potter Greens Drive nearside
								"8279", // ++
								"8601", "8608", // Lewis Farms Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"8608", // Lewis Farms Transit Centre
								"8999", // ++
								"8670", // The Fairways & Potter Greens Drive
						})) //
				.compileBothTripSort());
		map2.put(109L, new RouteTripSpec(109L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5012", // West Edmonton Mall Transit Centre
								"5874", // ++
								"5284", // ==
								"5366", // !=
								"5111", // != Jasper Place Transit Centre =>
								"5106", // != 157 Street & 100A Avenue #JasperPlace =>
								"5250", // !=
								"5344", // != ++
								"1496", // 97 Street & 102 Avenue #Downtown
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1571", // 97 Street & Jasper Avenue <= #Downtown
								"5132", // ++
								"5120", // !=
								"5111", // != Jasper Place Transit Centre <=
								"5106", // != 157 Street & 100A Avenue #JasperPlace <=
								"5379", // !=
								"5299", // ==
								"5903", // ++
								"5012", // West Edmonton Mall Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(110L, new RouteTripSpec(110L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTRIDGE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5004", // != West Edmonton Mall Transit Centre
								"5005", // != West Edmonton Mall Transit Centre
								"5877", //
								"5747", //
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"5747", //
								"5811", //
								"5811", //
								"5004", // != West Edmonton Mall Transit Centre
								"5005", // != West Edmonton Mall Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(111L, new RouteTripSpec(111L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5001", // West Edmonton Mall Transit Centre
								"5795", // 172 St & 99 Ave
								"5153", // ==
								"5109", // != Jasper Pl TC =>
								"5112", // != 157 Street & Stony Plain Road #JasperPlace =>
								"5021", // ==
								"1620", // 101 St & Jasper Ave #Downtown
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1620", // 101 St & Jasper Ave #Downtown
								"5494", // ==
								"5104", // != Jasper Pl TC
								"5106", // != 157 Street & 100A Avenue #JasperPlace
								"5353", // ==
								"5846", // 172 St & 99 Ave
								"5001", // West Edmonton Mall Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(112L, new RouteTripSpec(112L, //
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
		map2.put(113L, new RouteTripSpec(113L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"5001", // West Edmonton Mall Transit Centre
								"5069", //
								"5153", // ==
								"5104", // != Jasper Pl TC =>
								"5112", // != 157 Street & Stony Plain Road #JasperPlace
								"5105", // != 156 Street & 100A Avenue =>
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"5105", // != 156 Street & 100A Avenue <=
								"5104", // != Jasper Pl TC <=
								"5353", // ==
								"5151", //
								"5001", // West Edmonton Mall Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(114L, new RouteTripSpec(114L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTVIEW_VLG) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "8846", "8941", "5105" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5105", "8904", "8849", "8846" })) //
				.compileBothTripSort());
		map2.put(115L, new RouteTripSpec(115L, //
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
		map2.put(117L, new RouteTripSpec(117L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, THE_HAMPTONS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"5010", // West Edmonton Mall Transit Centre
								"5819", // != 189 Street & 87 Avenue
								"8601", "8607", // <> Lewis Farms Transit Centre
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
								"8601", "8607", // != <> Lewis Farms Transit Centre => THE_HAMPTONS
								"8603", "8605", // != Lewis Farms Transit Centre => WEST_EDM_MALL
								"5783", // == 187 Street & 87 Avenue
								"5010", // West Edmonton Mall Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(118L, new RouteTripSpec(118L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, RIO_TERRACE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5174", "5302", "5103" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "5103", "5301", "5174" })) //
				.compileBothTripSort());
		map2.put(119L, new RouteTripSpec(119L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEWIS_FARMS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, THE_HAMPTONS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"8583", // 215 Street & Hope Road
								"8097", // ++
								"8033", // ++
								"8601", "8607", // Lewis Farms Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"8607", // Lewis Farms Transit Centre
								"8135", // ++
								"8097", // ++
								"8046", // 199 Street & 62 Avenue
								"8583", // 215 Street & Hope Road
						})) //
				.compileBothTripSort());
		map2.put(120L, new RouteTripSpec(120L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, STADIUM, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5110", "1242", "1083", "1336", "1407" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "1407", "1328", "1620", "1746", "5110" })) //
				.compileBothTripSort());
		map2.put(121L, new RouteTripSpec(121L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5205", "5215", "6345", "6646", "7011" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7011", "6183", "6371", "5404", "5205" })) //
				.compileBothTripSort());
		map2.put(122L, new RouteTripSpec(122L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5012", // West Edmonton Mall Transit Centre
								"8389", // ++
								"5928", // ++
								"5330", // ++
								"5208", // Westmount Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"5208", // Westmount Transit Centre
								"5143", // ++
								"5389", // 154 Street & 118 Avenue
								"8952", // ++
								"5012", // West Edmonton Mall Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(123L, new RouteTripSpec(123L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5105", "8691", "5648", "5374", "5205" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5205", "5692", "5635", "8684", "5105" })) //
				.compileBothTripSort());
		map2.put(124L, new RouteTripSpec(124L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5106", // != 157 Street & 100A Avenue #JasperPlace <=
								"5105", // != 156 Street & 100A Avenue <=
								"6231", //
								"5206", // Westmount Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"5206", // Westmount Transit Centre
								"6781", //
								"5105", // != 156 Street & 100A Avenue =>
								"5106", // != 157 Street & 100A Avenue #JasperPlace =>
						})) //
				.compileBothTripSort());
		map2.put(125L, new RouteTripSpec(125L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BELVEDERE, // DOWNTOWN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5101", // Jasper Place Transit Centre
								"5106", // 157 Street & 100A Avenue #JasperPlace
								"-55552", // !=
								"5469", // !=
								"5448",// 161 Street & 109 Avenue
								"5127", // !=
								"5204", // Westmount Transit Centre
								"5098", // !=
								"11326", // <>
								"-11100", // <>
								"1105", // <> Kingsway RAH Transit Centre LAST
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
								"1105", // <> Kingsway RAH Transit Centre
								"1053", // !=
								"5077", // ==
								"5204", // == Westmount Transit Centre LAST
								"5209", // Westmount Transit Centre
								"5112", // != 157 Street & Stony Plain Road #JasperPlace
								"-55551", // !=
								"5106", // 157 Street & 100A Avenue #JasperPlace
								"5101", // Jasper Place Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(126L, new RouteTripSpec(126L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5013", "8882", "8590", "5928", "5208" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "5208", "5389", "8500", "8952", "5013" })) //
				.compileBothTripSort());
		map2.put(127L, new RouteTripSpec(127L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BELVEDERE, // 7205
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT) // 5204
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5204", "1110", "1401", "1209", "1205", "7205" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7205", "1209", "1402", "1110", "1105", "5204" })) //
				.compileBothTripSort());
		map2.put(128L, new RouteTripSpec(128L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY) // GOV_CTR
				.addTripSort(MDirectionType.NORTH.intValue(), // CASTLE_DOWNS
						Arrays.asList(new String[] { //
						"2002", // University Transit Centre
								"2638", // 114 Street & 85 Avenue
								"2749", // ++
								"1159", // !=
								"1308", // <> Government Transit Centre <=
								"1711", //
								"5481", // ==
								"5127", // !=
								"5206", // <> Westmount Transit Centre => END
								"5207", // Westmount Transit Centre
								"5466", // !=
								"6191", // !=
								"6333", // 127 Street & 129 Avenue
								"6553", // !=
								"6458", // !=
								"6006", // Castle Downs Transit Centre END >> UNIVERSITY
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), // UNIVERSITY
						Arrays.asList(new String[] { //
						"6006", // != Castle Downs Transit Centre <=
								"6137", // !=
								"6366", // != 127 Street & 131 Avenue
								"6369", // == 127 Street & 129 Avenue
								"5051", // !=
								"5206", // <> Westmount Transit Centre
								"5329", // !=
								"5445", // ==
								"1083", // !==
								"1964", // !=
								"1964", // !=
								"1308", // <> !== Government Transit Centre =>
								"2501", // !==
								"2890", // 114 Street & 89 Avenue
								"2002", // University Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(129L, new RouteTripSpec(129L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"5012", // West Edmonton Mall Transit Centre
								"8740", // ++
								"8740", // ++
								"5960", // ++
								"5207", // Westmount Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"5207", // Westmount Transit Centre
								"5936", // ++
								"8740", // ++
								"5012", // West Edmonton Mall Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(130L, new RouteTripSpec(130L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2002", // University Transit Centre
								"1306", // Government Transit Centre <=
								"1700", // ++
								"1532", // 106 Street & 118 Avenue Loop
								"1476", // ++
								"7002", // Northgate Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"7002", // Northgate Transit Centre
								"1532", // 106 Street & 118 Avenue Loop
								"1855", // ==
								"1083", // !=
								"1306", // != Government Transit Centre =>
								"1939", // !=
								"2890", // 114 Street & 89 Avenue
								"2002", // University Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(133L, new RouteTripSpec(133L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY, // S_CAMPUS_FT_EDM
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEWIS_FARMS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"8602", "8603", // Lewis Farms Transit Centre
								"5001", // West Edmonton Mall Transit Centre
								"2625", // !=
								"2890", // <> 114 Street & 89 Avenue
								"2002", // <> University Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2890", // <> 114 Street & 89 Avenue
								"2002", // <> University Transit Centre
								"22720", // !=
								"5010", // West Edmonton Mall Transit Centre
								"8601", "8602", "8603", // Lewis Farms Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(134L, new RouteTripSpec(134L, //
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
		map2.put(137L, new RouteTripSpec(137L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5010", "8882", "6850", /* + */"7011" /* + */, "7002", "7908" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7908", "7011", "6118", "8861", "5010" })) //
				.compileBothTripSort());
		map2.put(138L, new RouteTripSpec(138L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH_CAMPUS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEDGEWOOD) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5627", // <> Wedgewood Boulevard & Weber Gate
								"5858", "5968", "5888", // <>
								"5789", // <> Wedgewood Boulevard & Weber Gate
								"5605", // !=
								"5785", // !=
								"5938", // <>
								"5982", "5696", "5642", "5921", "5747", "5776", "5626", // <>
								"5981", // <>
								"5773", // !=
								"2705", "2707", "2714", // South Campus Fort Edmonton Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2705", "2707", // South Campus Fort Edmonton Transit Centre
								"5678", // !=
								"5938", // <>
								"5982", "5696", "5642", "5921", "5747", "5776", "5626", // <>
								"5981", // <>
								"5857", // !=
								"5310", // !=
								"5627", // <> Wedgewood Boulevard & Weber Gate
								"5858", "5968", "5888", // <>
								"5789", // <> Wedgewood Boulevard & Weber Gate
						})) //
				.compileBothTripSort());
		map2.put(139L, new RouteTripSpec(139L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH_CAMPUS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, THE_HAMPTONS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"8457", // <> Glastonbury Boulevard & 62 Avenue
								"8106", // ++
								"8033", // ++
								"2705", "2707", "2714", // South Campus Fort Edmonton Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2705", "2707", // South Campus Fort Edmonton Transit Centre
								"8135", // ++
								"8457", // <> Glastonbury Boulevard & 62 Avenue
								"8460", // 199 Street & 62 Avenue
						})) //
				.compileBothTripSort());
		map2.put(140L, new RouteTripSpec(140L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LAGO_LINDO, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"1251", // 102 Street & MacDonald Drive #Downtown
								"1040", // ++
								"7579", // ==
								"7003", // != Northgate Transit Centre =>
								"7010", // != Northgate Transit Centre
								"7448", // ==
								"7827", // 91 Street & 167 Avenue #LagoLindo
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"7827", // 91 Street & 167 Avenue #LagoLindo
								"7377", // ++
								"7042", // ++
								"7003", // != Northgate Transit Centre <=
								"1380", //
								"1251", // 102 Street & MacDonald Drive #Downtown
						})) //
				.compileBothTripSort());
		map2.put(141L, new RouteTripSpec(141L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ABBOTTSFIELD, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLISEUM) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "1204", "1561", "1002", "1003" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "1002", "1003", "1031", "1204" })) //
				.compileBothTripSort());
		map2.put(142L, new RouteTripSpec(142L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ABBOTTSFIELD, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLISEUM) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "1207", "1521", "1001" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "1001", "1367", "1207" })) //
				.compileBothTripSort());
		map2.put(143L, new RouteTripSpec(143L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MONTROSE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, KINGSWAY) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "1111", "1476", "1441", "1205", "1260" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "1260", "1213", "1278", "1075", "1111" })) //
				.compileBothTripSort());
		map2.put(145L, new RouteTripSpec(145L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LAGO_LINDO, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, _82_ST_132_AVE) // EAUX_CLAIRES
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"6315", // Eaux Claires Transit Centre
								"7377", // ++ 91 Street & 168 Avenue
								"7388", // ++ 92 Street & 179 Avenue
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"7388", // ++ 92 Street & 179 Avenue
								"7483", // ++ 91 Street & 168 Avenue
								"6362", // == 97 Street & 160 Avenue
								"-77857", // ==
								"-77855", // ==
								"6315", // != Eaux Claires Transit Centre
								"-6314", // !=
								"-77852", // !=
								"6317", // != Eaux Claires Transit Centre
								"7358", // ++ 95 Street & 132 Avenue
								"7165", // 88 Street & 132 Avenue
						})) //
				.compileBothTripSort());
		map2.put(149L, new RouteTripSpec(149L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "6308", "7736", "7113", "7904" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7904", "7153", "7959", "6308" })) //
				.compileBothTripSort());
		map2.put(150L, new RouteTripSpec(150L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"5007", // West Edmonton Mall Transit Centre
								"-55030", //
								"-55552", //
								"5112", // 157 Street & Stony Plain Road #JasperPlace
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
								"-77862", //
								"-7017", //
								"6369", // ++
								"6289", // ==
								"6372", // !=
								"1932", // !=
								"5090", // ==
								"5203", // Westmount Transit Centre
								"5494", // ==
								"5102", // != Jasper Place Transit Centre
								"-55551", // !=
								"5106", // != 157 Street & 100A Avenue #JasperPlace
								"5007", // West Edmonton Mall Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(151L, new RouteTripSpec(151L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, KING_EDWARD_PK) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { // CASTLE_DOWNS
						"2253", // 71 Street & 77 Avenue
								"2432", // 91 Street & 82 Avenue
								"1251", // == 102 Street & MacDonald Drive
								"-11027", // ++
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
								"-6011", // ++
								"6004", // Castle Downs Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { // KING_EDWARD_PK
						"6004", // Castle Downs Transit Centre
								"6366", // !=
								"6292", // 127 Street & 129 Avenue
								"6123", // !=
								"6116", // == 103 Street & 127 Avenue
								"-77825", // ++
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
		map2.put(152L, new RouteTripSpec(152L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BELVEDERE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7003", "7074", "7208" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7208", "7221", "7003" })) //
				.compileBothTripSort());
		map2.put(153L, new RouteTripSpec(153L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BELVEDERE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7008", /* + */"7143"/* + */, "7204" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7204", /* + */"7043"/* + */, "7008" })) //
				.compileBothTripSort());
		map2.put(154L, new RouteTripSpec(154L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BELVEDERE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7009", "7592", "7202" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7202", "7123", "7009" })) //
				.compileBothTripSort());
		map2.put(155L, new RouteTripSpec(155L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, RAPPERSWILL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "6823", /* + */"6416"/* + */, "6313" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "6313", /* + */"6078"/* + */, "6823" })) //
				.compileBothTripSort());
		map2.put(157L, new RouteTripSpec(157L, //
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
		map2.put(160L, new RouteTripSpec(160L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, OXFORD, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, GOV_CTR) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "1304", "1820", "6348", "6243", "6835", "6676" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "6835", "6676", "6442", "6594", "1304" })) //
				.compileBothTripSort());
		map2.put(161L, new RouteTripSpec(161L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MAC_EWAN_GOV_CTR) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"1301", "1309", // != Government Transit Centre START
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
								"1301", "1309", // !≃ Government Transit Centre END
						})) //
				.compileBothTripSort());
		map2.put(162L, new RouteTripSpec(162L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MAC_EWAN_GOV_CTR) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"1301", "1309", // != Government Transit Centre START
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
								"-77857", //
								"-77853", //
								"6311", // != <> Eaux Claires Transit Centre => CASTLE_DOWNS
								"-6302", //
								"-6306", //
								"6310", // != Eaux Claires Transit Centre
								"1622", // ==
								"1740", // !=
								"1989", // != 108 Street & 104 Avenue END
								"1964", // !=
								"1301", "1309", // != Government Transit Centre END
						})) //
				.compileBothTripSort());
		map2.put(163L, new RouteTripSpec(163L, //
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
		map2.put(164L, new RouteTripSpec(164L, //
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
		map2.put(165L, new RouteTripSpec(165L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, _85_ST_132_AVE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "7496", "6130", "6522", "6011", "6127" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "6074", "6010", "6396", "6579", "7299" })) //
				.compileBothTripSort());
		map2.put(166L, new RouteTripSpec(166L, //
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
		map2.put(167L, new RouteTripSpec(167L, //
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
		map2.put(168L, new RouteTripSpec(168L, //
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
		map2.put(180L, new RouteTripSpec(180L, //
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
		map2.put(181L, new RouteTripSpec(181L, //
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
		map2.put(182L, new RouteTripSpec(182L, //
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
		map2.put(183L, new RouteTripSpec(183L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ABBOTTSFIELD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "1002", "7668", "7885", "7102" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7102", "7983", "7729", "1002" })) //
				.compileBothTripSort());
		map2.put(184L, new RouteTripSpec(184L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EVERGREEN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "7903", "7262", "7128" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7128", "7262", "7903" })) //
				.compileBothTripSort());
		map2.put(185L, new RouteTripSpec(185L, //
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
		map2.put(186L, new RouteTripSpec(186L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, FRASER, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7358", "7286", "7206", "7104", "7470" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7470", "7105", "7205", "7120", "7011" })) //
				.compileBothTripSort());
		map2.put(187L, new RouteTripSpec(187L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, KERNOHAN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LONDONDERRY) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7230", "7103", "7756", "7943" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7943", "7103", "7102", "7185" })) //
				.compileBothTripSort());
		map2.put(188L, new RouteTripSpec(188L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LONDONDERRY, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "6309", "7230", "7186", "7907", "7729" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7668", "7904", "7549", "7185", "7188", "6309" })) //
				.compileBothTripSort());
		map2.put(190L, new RouteTripSpec(190L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "6306", "7763", "7803", "7054", "7906" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7906", "7384", "7815", "7674", "6306" })) //
				.compileBothTripSort());
		map2.put(191L, new RouteTripSpec(191L, //
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
		map2.put(192L, new RouteTripSpec(192L, //
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
		map2.put(193L, new RouteTripSpec(193L, //
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
		map2.put(194L, new RouteTripSpec(194L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SCHONSEE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "6308", "7677", "7919" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7919", "7674", "6308" })) //
				.compileBothTripSort());
		map2.put(195L, new RouteTripSpec(195L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_CONACHIE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"7907", // West Clareview Transit Centre
								"7879", // ++
								"77126", // == 55 Street & 167 Avenue
								"7308", // != McConachie Way & 167 Avenue =>
								"77130", // !=
								"7719", // 65 Street & McConachie Boulevard =>
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"7719", // != 65 Street & McConachie Boulevard <=
								"77607", // !=
								"7308", // != 59A Street & McConachie Way <=
								"77335", // !=
								"77424", // ==
								"77436", // ++
								"7907", // West Clareview Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(197L, new RouteTripSpec(197L, //
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
		map2.put(198L, new RouteTripSpec(198L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, FT_SASKATCHEWAN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7908",
						/* + */"77175"/* + */, //
								"7405" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7405", "7926", "7908" })) //
				.compileBothTripSort());
		map2.put(199L, new RouteTripSpec(199L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EDM_GARRISON, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "6316",
						/* + */"7873"/* + */, //
								"7895" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7895", "7873", "6316" })) //
				.compileBothTripSort());
		map2.put(211L, new RouteTripSpec(211L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW_WEST_TC, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "1643", "1321", "7903" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7903", "1620", "1643" })) //
				.compileBothTripSort());
		map2.put(301L, new RouteTripSpec(301L, //
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
		map2.put(302L, new RouteTripSpec(302L, //
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
		map2.put(303L, new RouteTripSpec(303L, //
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
		map2.put(304L, new RouteTripSpec(304L, //
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
		map2.put(305L, new RouteTripSpec(305L, //
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
		map2.put(306L, new RouteTripSpec(306L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MEADOWS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BONNIE_DOON) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2808", // Bonnie Doon Safeway
								"2593", // ==
								"2447", // !=
								"2255", // !=
								"2222", // ==
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
		map2.put(307L, new RouteTripSpec(307L, //
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
		map2.put(308L, new RouteTripSpec(308L, //
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
		map2.put(309L, new RouteTripSpec(309L, //
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
		map2.put(310L, new RouteTripSpec(310L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, RIO_TERRACE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5174", "5302", "5383", "5105" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "5105", "5491", "5301", "5174" })) //
				.compileBothTripSort());
		map2.put(311L, new RouteTripSpec(311L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5011", "5222", "5836", "5105" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "5105", "5851", "5325", "5011" })) //
				.compileBothTripSort());
		map2.put(312L, new RouteTripSpec(312L, //
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
								"8601", "8609", // Lewis Farms Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(316L, new RouteTripSpec(316L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HAWKS_RDG, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEWIS_FARMS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "8603", "6824", "6408", "6709" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "6709", "6617", "6825", "8603" })) //
				.compileBothTripSort());
		map2.put(317L, new RouteTripSpec(317L, // TODO better (same stops in both trips in different orders)
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
		map2.put(318L, new RouteTripSpec(318L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY, // WINDSOR_PARK
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BELGRAVIA) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2765", // 118 Street & 73 Avenue
								"2882", // !=
								"2890", // <>
								"2002", // <> University Transit Centre
								"2410", // <>
								"2660", // !=
								"2971", // 117 Street & University Avenue
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2971", // 117 Street & University Avenue
								"2844", // !=
								"2890", // <>
								"2001", "2002", // <> University Transit Centre
								"2410", // <>
								"2834", // !=
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
		map2.put(321L, new RouteTripSpec(321L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA_IND) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "3733", "3744", "2106" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2106",
						/* + */"3481"/* + */, //
								"3733" })) //
				.compileBothTripSort());
		map2.put(322L, new RouteTripSpec(322L, //
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
		map2.put(323L, new RouteTripSpec(323L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BONNIE_DOON, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, RITCHIE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2419", "2313", "2808" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2808",
						/* + */"2294"/* + */, //
								"2419" })) //
				.compileBothTripSort());
		map2.put(324L, new RouteTripSpec(324L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, AMBLESIDE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "9092", "9630", "4201" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "4201", "9635", "9092" })) //
				.compileBothTripSort());
		map2.put(325L, new RouteTripSpec(325L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WINDERMERE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "9632", "9526", "4801" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "4801",
						/* + */"4938"/* + */, //
								"9632" })) //
				.compileBothTripSort());
		map2.put(327L, new RouteTripSpec(327L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BELGRAVIA) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2765", "2680", "2821" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2821",
						/* + */"2648"/* + */, //
								"2765" })) //
				.compileBothTripSort());
		map2.put(330L, new RouteTripSpec(330L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH_CAMPUS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "4811", "4597", "4153", "2704", })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2704", "4021", "4494", "4811" })) //
				.compileBothTripSort());
		map2.put(331L, new RouteTripSpec(331L, //
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
		map2.put(333L, new RouteTripSpec(333L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEWIS_FARMS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ROSENTHAL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"8167", // 224 Street & Rosenthal Boulevard
								"8852", // ++
								"8601", "8604", "8610", // Lewis Farms Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"8604", "8610", // Lewis Farms Transit Centre
								"8168", // ++
								"8167", // 224 Street & Rosenthal Boulevard
						})) //
				.compileBothTripSort());
		map2.put(334L, new RouteTripSpec(334L, //
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
		map2.put(336L, new RouteTripSpec(336L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "4810", "4455", "4069", "2208", })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2208", "4167", "4129", "4810" })) //
				.compileBothTripSort());
		map2.put(337L, new RouteTripSpec(337L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "4802", "4117", "4110", "4215", })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "4215", "4941", "4856", "4802" })) //
				.compileBothTripSort());
		map2.put(338L, new RouteTripSpec(338L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BLACKBURNE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "9226", "4201", "4813", "4597", "4034", "2207", })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2207", "4034", "4042", "4805", "4204", "9226" })) //
				.compileBothTripSort());
		map2.put(339L, new RouteTripSpec(339L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, RUTHERFORD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "9251", "9685", "4213", })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4213", "9756", "9251" })) //
				.compileBothTripSort());
		map2.put(340L, new RouteTripSpec(340L, //
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
		map2.put(347L, new RouteTripSpec(347L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ALLARD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"9718", // Allard Gate & 30 Avenue SW
								"9717", // Allard Link & 30 Avenue SW
								"9773", // Allard Gate & Allard Boulevard SW
								"9685", // ++
								"4213", // Century Park Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"4213", // Century Park Transit Centre
								"9666", // ++
								"9718", // Allard Gate & 30 Avenue SW
						})) //
				.compileBothTripSort());
		map2.put(360L, new RouteTripSpec(360L, //
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
		map2.put(361L, new RouteTripSpec(361L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3210", "3585", "2105", })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2105", "3529", "3210" })) //
				.compileBothTripSort());
		map2.put(362L, new RouteTripSpec(362L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHWOOD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3082",
						/* + */"3149"/* + */, //
								"3211", })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "3211", "3009", "3082" })) //
				.compileBothTripSort());
		map2.put(363L, new RouteTripSpec(363L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHWOOD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3066", "3003", "3215", })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "3215",
						/* + */"3174"/* + */, //
								"3066" })) //
				.compileBothTripSort());
		map2.put(370L, new RouteTripSpec(370L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3206", "3957", "3796", "2106", })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2106", "3748", "3950", "3206" })) //
				.compileBothTripSort());
		map2.put(380L, new RouteTripSpec(380L, //
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
		map2.put(381L, new RouteTripSpec(381L, //
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
		map2.put(399L, new RouteTripSpec(399L, //
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
		map2.put(505L, new RouteTripSpec(505L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"4216", // Century Park Transit Centre
								"1322", // ++
								"7903", // West Clareview Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"7903", // West Clareview Transit Centre
								"1619", // ++
								"4216", // Century Park Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(510L, new RouteTripSpec(510L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"1301", // Government Transit Centre
								"1457", // ++
								"7903", // West Clareview Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"7903", // West Clareview Transit Centre
								"1292", // ++
								"1301", // Government Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(512L, new RouteTripSpec(512L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "1336", "1408", "1211", "7212", "7903" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7903", "7212", "1210", "1407", "1620" })) //
				.compileBothTripSort());
		map2.put(517L, new RouteTripSpec(517L, //
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
								"9467", // ++
								"4201", // Century Park Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"4201", // Century Park Transit Centre
								"9468", // ++
								"9892", // Ken Nichol RR Ctr Beaumont
						})) //
				.compileBothTripSort());
		map2.put(545L, new RouteTripSpec(545L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NAIT, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"1246", // 100 Street & 102 Avenue
								"1109", // Kingsway RAH Transit Centre
								"1860", // 109 Street & Princess Elizabeth Avenue

						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"1860", // 109 Street & Princess Elizabeth Avenue
								"1104", // Kingsway RAH Transit Centre
								"1246", // 100 Street & 102 Avenue
						})) //
				.compileBothTripSort());
		map2.put(560L, new RouteTripSpec(560L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SPRUCE_GRV) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"8584", // Jennifer Heil Way & TransAlta Tri Leisure Centre #SPRUCE_GRV-MIDDLE <=
								"8737", // King Street & McLeod Avenue #SPRUCE_GRV
								"8785", // Century Road & McLeod Avenue #SPRUCE _GRV
								"8761", // == Century Road & Grove Drive #SPRUCE_GRV
								"5415", // != 154 Street & 119 Avenue #MIDDLE =>
								"1890", // != == 109 Street & Princess Elizabeth Avenue #DOWNTOWN
								"1570", // != != 101 Street & 103A Avenue #DOWNTOWN =>
								"1366", // != != 101 Street & 111 Avenue #DOWNTOWN
								"11334", // != 102 Street & 104 Avenue #DOWNTOWN
								"1983", // != 105 Street & 104 Avenue #DOWNTOWN
								"1479", // != 97 Street & 103A Avenue #DOWNTOWN =>
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1570", // !== 101 Street & 103A Avenue #DOWNTOWN <=
								"1679", // != 105 Street & 104 Avenue #DOWNTOWN
								"1860", // !== 109 Street & Princess Elizabeth Avenue #DOWNTOWN
								"5389", // !== 154 Street & 118 Avenue #MIDDLE <=
								"8371", // == Century Road & Kings Link #SPRUCE_GRV
								"8730", // == Century Road & Grove Drive
								"8040", // Hilldowns Dr. & Grove Dr.
								"8743", // == Aspenglen Drive & Grove Drive
								"8584", // Jennifer Heil Way & TransAlta Tri Leisure Centre #SPRUCE_GRV-MIDDLE
						})) //
				.compileBothTripSort());
		map2.put(561L, new RouteTripSpec(561L, //
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
		map2.put(562L, new RouteTripSpec(562L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH_CAMPUS, // WEST_EDM_MALL
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SPRUCE_GRV) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"8207", // Jennifer Heil Way & Grove Drive
								"8925", // ++ Belmont Crescent & McLeod Avenue
								"5219", // 175 Street & 87 Avenue
								"2708", "2714", // South Campus Transit Centre Fort Edmonton Park
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2708", // South Campus Transit Centre Fort
								"5014", // West Edmonton Mall Transit Centre
								"9818", // ++ Fulton Drive & Acheson Road
								"8233", // Century Road & Grove Meadow Drive
								"8743", // ++ Aspenglen Drive & Grove Drive
								"8207", // Jennifer Heil Way & Grove Drive
						})) //
				.compileBothTripSort());
		map2.put(577L, new RouteTripSpec(577L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHLANDS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, _84_ST_111_AVE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"1408", // 84 Street & 111 Avenue
								"1480", // ++
								"1371", // Northlands South Entrance
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1371", // Northlands South Entrance
								"1180", // ++
								"1408", // 84 Street & 111 Avenue
						})) //
				.compileBothTripSort());
		map2.put(580L, new RouteTripSpec(580L, //
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
		map2.put(589L, new RouteTripSpec(589L, //
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
		map2.put(591L, new RouteTripSpec(591L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHLANDS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2307", "2359", "1371" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1371", "2594", "2307" })) //
				.compileBothTripSort());
		map2.put(594L, new RouteTripSpec(594L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Valley Zoo", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"8603", // != Lewis Farms Transit Centre <=
								"5219", // == 175 Street & 87 Avenue #WestEdmontonMall <=
								"5332", // 152 Street & 87 Avenue
								"5095", // 133 Street & Buena Vista Road #ValleyZoo
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"5095", // 133 Street & Buena Vista Road #ValleyZoo
								"5610", // 155 Street & 87 Avenue
								"5449", // ==
								"5014", // != West Edmonton Mall Transit Centre
								"8603", // != Lewis Farms Transit Centre =>
								"5219", // != 175 Street & 87 Avenue #WestEdmontonMall =>
						})) //
				.compileBothTripSort());
		map2.put(595L, new RouteTripSpec(595L, //
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
		map2.put(596L, new RouteTripSpec(596L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Hawrelak Pk") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2515", // Groat Road & Hawrelak Park
								"2666", // ++
								"2001", // University Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2001", // University Transit Centre
								"2851", // ++
								"2515", // Groat Road & Hawrelak Park
						})) //
				.compileBothTripSort());
		map2.put(599L, new RouteTripSpec(599L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EDM_GARRISON, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"6313", // != Eaux Claires Transit Centre <=
								"7991", // == 97 Street & 176 Avenue
								"-77838", // !=
								"-77770", // !=
								"7873", // <> C Ortona Road & Churchill Avenue Garrison
								"7681", // <> Ortona Road & Ubique Avenue Garrison
								"7412", // == Korea Road & Ortona Road Garrison
								"7895" // B Hindenburg Line Road & Churchill Avenue Garrison
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"7895", // B Hindenburg Line Road & Churchill Avenue Garrison
								"7406", // == Highway 28A & Mons Avenue Garrison
								"-77771", // ==
								"-77837", // ==
								"7873", // <> C Ortona Road & Churchill Avenue Garrison
								"7681", // <> Ortona Road & Ubique Avenue Garrison
								"6854", // == 97 Street & 176 Avenue
								"6313", // != Eaux Claires Transit Centre =>
						})) //
				.compileBothTripSort());
		map2.put(601L, new RouteTripSpec(601L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ROSS_SHEPPARD, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEDGEWOOD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5627", "5908", "5983", "5548", "5392" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(602L, new RouteTripSpec(602L, //
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
						"7719", // 65 Street & McConachie Boulevard
								"7018", // McConachie Boulevard & 176 Avenue
								"77436", // ++
								"7907", // West Clareview Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(606L, new RouteTripSpec(606L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CARLTON, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ARCH_MAC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"6747", // 132 Street & 160 Avenue
								"6603", //
								"6853", //
								"6369", //
								"5211", //
								"5548", // 142 Street & 109 Avenue
						})) //
				.compileBothTripSort());
		map2.put(607L, new RouteTripSpec(607L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ARCH_MAC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"6822", "6293", "6369", "5211", "5548" //
						})) //
				.compileBothTripSort());
		map2.put(608L, new RouteTripSpec(608L, //
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
		map2.put(609L, new RouteTripSpec(609L, //
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
		map2.put(610L, new RouteTripSpec(610L, //
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
		map2.put(612L, new RouteTripSpec(612L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, OXFORD, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ARCH_MAC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "6410", "6695", "5211", "5548" })) //
				.compileBothTripSort());
		map2.put(613L, new RouteTripSpec(613L, //
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
		map2.put(617L, new RouteTripSpec(617L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, KLARVATTEN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CARDINAL_LEGER) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7827", "7795", "7659" })) //
				.compileBothTripSort());
		map2.put(618L, new RouteTripSpec(618L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MATT_BERRY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, JJ_BOWLEN) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7992", "7058", "7449", "7545" })) //
				.compileBothTripSort());
		map2.put(620L, new RouteTripSpec(620L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BELVEDERE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, AOB) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7210", "1207", "2915" })) //
				.compileBothTripSort());
		map2.put(621L, new RouteTripSpec(621L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ABBOTTSFIELD, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, AOB) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1002", "2553" })) //
				.compileBothTripSort());
		map2.put(635L, new RouteTripSpec(635L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5210", "1481", "1242", "1083", "1393" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(689L, new RouteTripSpec(689L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WINDSOR_PARK, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2851", "2974" })) //
				.compileBothTripSort());
		map2.put(697L, new RouteTripSpec(697L, //
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
		map2.put(698L, new RouteTripSpec(698L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_PHERSON, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JACKSON_HTS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "3230", "3964" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(699L, new RouteTripSpec(699L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_PHERSON, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JACKSON_HTS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "3355", "3400", "3603" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(701L, new RouteTripSpec(701L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BELMEAD) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5914", "5001" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(702L, new RouteTripSpec(702L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, PARKVIEW, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ORMSBY_PL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5881", "5828", "5725", "5198" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(703L, new RouteTripSpec(703L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CRESTWOOD, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ST_FRANCIS_XAVIER) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5421", "5038", "5174", "5941" })) //
				.compileBothTripSort());
		map2.put(705L, new RouteTripSpec(705L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTLAWN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEWIS_FARMS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"8602", "8610", // Lewis Farms Transit Centre
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
		map2.put(706L, new RouteTripSpec(706L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Jasper Pl TC", // _157_ST_100A_AVE
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE) // High School
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "5106", "5177" })) //
				.compileBothTripSort());
		map2.put(707L, new RouteTripSpec(707L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, OSCAR_ROMERO, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, THE_HAMPTONS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "8670", "8135", "5986" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(708L, new RouteTripSpec(708L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"5012", // West Edmonton Mall Transit Centre
								"5874", //
								"5221", //
								"5109", // != Jasper Pl TC
								"5106", // 157 Street & 100A Avenue #JasperPlace
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(709L, new RouteTripSpec(709L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MEADOWLARK) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5359", "5437", "1256" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(710L, new RouteTripSpec(710L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ROSS_SHEPPARD, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5001", "5174", "5588", "5392" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(711L, new RouteTripSpec(711L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ROSS_SHEPPARD, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEWIS_FARMS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"8603", "8610", // Lewis Farms Transit Centre
								"5013", // West Edmonton Mall Transit Centre
								"5929", // ++
								"5433", // ++
								"5180", // ++
								"5896", // 138 Street & 111 Avenue
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(712L, new RouteTripSpec(712L, //
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
		map2.put(717L, new RouteTripSpec(717L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, VICTORIA, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5001", "1426" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(723L, new RouteTripSpec(723L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HADDOW) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "4941", "4319", "4815", "4069", "2974" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(725L, new RouteTripSpec(725L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, KINGSWAY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY) // GOV_CTR
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"1857", // 109 Street & 111 Avenue
								"1855", // ==
								"1083", // !=
								"1306", // != Government Transit Centre =>
								"1939", // !=
								"2002", // University Transit Centre =>
						})) //
				.compileBothTripSort());
		map2.put(726L, new RouteTripSpec(726L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ROSS_SHEPPARD, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "4808", "4249", "5511", "5180", "5896" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(728L, new RouteTripSpec(728L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BROOKSIDE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "4034", "4029", "2710", "2974" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(729L, new RouteTripSpec(729L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "4815", "4246", "2974" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(730L, new RouteTripSpec(730L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LAGO_LINDO, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ARCH_MAC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7377", "6317", "7016", "5548" })) //
				.compileBothTripSort());
		map2.put(731L, new RouteTripSpec(731L, //
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
		map2.put(733L, new RouteTripSpec(733L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5001", "2714", "2002" })) //
				.compileBothTripSort());
		map2.put(734L, new RouteTripSpec(734L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LAGO_LINDO, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MARY_BUTTERWORTH) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7377", "7483", "6236" })) //
				.compileBothTripSort());
		map2.put(735L, new RouteTripSpec(735L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5006", "5156", "2714", "2002" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(738L, new RouteTripSpec(738L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTH_CAMPUS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "4815", "4158", "2709" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(741L, new RouteTripSpec(741L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WP_WAGNER, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, KNOTTWOOD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3023", "3001", "2111", "2189" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(744L, new RouteTripSpec(744L, //
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
		map2.put(739L, new RouteTripSpec(739L, //
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
		map2.put(747L, new RouteTripSpec(747L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EDM_INT_AIRPORT) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "9747", "4216" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4216", "9747" })) //
				.compileBothTripSort() //
				.addBothFromTo(MDirectionType.SOUTH.intValue(), "4216", "4216") //
				.addBothFromTo(MDirectionType.NORTH.intValue(), "9747", "9747")); //
		map2.put(748L, new RouteTripSpec(748L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LAGO_LINDO, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CARDINAL_LEGER) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "7377", "6309", "7353" })) //
				.compileBothTripSort());
		map2.put(750L, new RouteTripSpec(750L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, EASTGLEN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7015", "7165", "1203", "1033" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(753L, new RouteTripSpec(753L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LONDONDERRY, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7572", "7007" })) //
				.compileBothTripSort());
		map2.put(755L, new RouteTripSpec(755L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, OXFORD, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ARCH_O_LEARY) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "6452", "6695", "6628", "6442", "7358", "7165" })) //
				.compileBothTripSort());
		map2.put(756L, new RouteTripSpec(756L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LA_ZERTE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "6001", "6340", "6310", "7186" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(757L, new RouteTripSpec(757L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, _127_ST_129_AVE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY) // GOV_CTR
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						/* no stops *///
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"6369", // 127 Street & 129 Avenue
								"1965", // 127 Street & 122 Avenue
								"5206", // Westmount Transit Centre
								"5445", // ==
								"-11533", // !=
								"-11525", // !=
								"1083", // !=
								"1308", // != Government Transit Centre =>
								"2501", // !=
								"2002", // != University Transit Centre =>
						})) //
				.compileBothTripSort());
		map2.put(760L, new RouteTripSpec(760L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_NALLY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LARKSPUR) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3247", "3586", "2676" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(761L, new RouteTripSpec(761L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2285", "2974" })) //
				.compileBothTripSort());
		map2.put(762L, new RouteTripSpec(762L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_NALLY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, AVONMORE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2339", "2447", "2544", "2267", "2676" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(763L, new RouteTripSpec(763L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BONNIE_DOON, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2159", "2891", "2001" })) //
				.compileBothTripSort());
		map2.put(764L, new RouteTripSpec(764L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2301", "2267", "1620" })) //
				.compileBothTripSort());
		map2.put(765L, new RouteTripSpec(765L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, RHATIGAN_RIDGE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "4461", "4249", "2974" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(767L, new RouteTripSpec(767L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3217", "3011", "2111", "2974" })) //
				.compileBothTripSort());
		map2.put(768L, new RouteTripSpec(768L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WP_WAGNER) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"3280", // 50 Street & 38 Avenue
								"3556", // Mill Woods Road E & 23 Avenue
								"3212", // Mill Woods Transit Centre
								"3007", // Lakewood TC
								"2111", // 86 St & Millgate
								"2455", // ==
								"2163", // !=
								"2189", // != 80 Street & Wagner Road =>
								"2418", // != 80 Street & Wagner Road =>
						})) //
				.compileBothTripSort());
		map2.put(769L, new RouteTripSpec(769L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WP_WAGNER, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"3211", // Mill Woods TC
								"3585", // Mill Woods Rd E & 36 Ave
								"2111", // 86 St & Millgate
								"2455", // ==
								"2163", // !=
								"2189", // != 80 Street & Wagner Road
								"2418", // != 80 Street & Wagner Road =>
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(771L, new RouteTripSpec(771L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WP_WAGNER, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CRAWFORD_PLAINS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3143", "3217", "3002", "2111", "2189" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(773L, new RouteTripSpec(773L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WP_WAGNER, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"3217", // Mill Woods Transit Centre
								"3585", // Mill Woods Rd E & 36 Ave
								"2111", // 86 St & Millgate
								"2455", // ==
								"22341", // !=
								"2163", // !=
								"2189", // != 80 Street & Wagner Road =>
								"2418", // != 80 Street & Wagner Road =>
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(774L, new RouteTripSpec(774L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HARDISTY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SILVERBERRY) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3255", "3708", "3740", "3491", "2915", "2177" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(776L, new RouteTripSpec(776L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_NALLY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3217", "3796", "3586", "2676" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(777L, new RouteTripSpec(777L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MEADOWS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3703", "3560", "3217" })) //
				.compileBothTripSort());
		map2.put(778L, new RouteTripSpec(778L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_NALLY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3217", "3255", "3491", "2676" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(779L, new RouteTripSpec(779L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HARDISTY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3217", "3255", "3491", "2915", "2177" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(780L, new RouteTripSpec(780L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HARDISTY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3217", "3796", "3586", "2915", "2177" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(781L, new RouteTripSpec(781L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_NALLY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2105", "2551", "2676" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(782L, new RouteTripSpec(782L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, KENILWORTH, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3211", "3585", "2111", "2255", "2487", "2160" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(783L, new RouteTripSpec(783L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, KENILWORTH, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, GREENVIEW) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3328", "3537", "2160" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(784L, new RouteTripSpec(784L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_NALLY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MEADOWS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3255", "3708", "3740", "3491", "2676" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(785L, new RouteTripSpec(785L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HARDISTY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WILDROSE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3247", "3491", "2915", "2177" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(786L, new RouteTripSpec(786L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, AVALON, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2202", "2518" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(787L, new RouteTripSpec(787L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2212", "2778", "2974" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(788L, new RouteTripSpec(788L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2212", // Southgate Transit Centre
								"2838", // ++
								"2974", // 104 Street & 73 Avenue #STRATHCONA
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						/* no stops */
						})) //
				.compileBothTripSort());
		map2.put(789L, new RouteTripSpec(789L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WP_WAGNER, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHWOOD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3143", "3217", "2189" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(790L, new RouteTripSpec(790L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BEARSPAW) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "4205", "4290", "4203", "4157", "4431", "2218" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(791L, new RouteTripSpec(791L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, RUTHERFORD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "9242", "9685", "4216", "2218" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(793L, new RouteTripSpec(793L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, HARRY_AINLAY) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3217", "3008", "4490" })) //
				.compileBothTripSort());
		map2.put(795L, new RouteTripSpec(795L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, TWIN_BROOKS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"4348", // 111 Street & 12 Avenue
								"4265", //
								"4216", //
								"2218", // Southgate Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(796L, new RouteTripSpec(796L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, EASTGLEN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, FRASER) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7470", "7620", "1185" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(799L, new RouteTripSpec(799L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, RIVERBEND, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "4808", "4489", "4069", "4246", "4029" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(800L, new RouteTripSpec(800L, //
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
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"5061", // 163 Street & 93 Avenue
								"5101", // Jasper Place Transit Centre
								"5112", // 157 Street & Stony Plain Road #JasperPlace
								"5204", // Westmount Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"5209", // Westmount Transit Centre
								"5150", // ++
								"5101", // Jasper Place Transit Centre
								"5112", // 157 Street & Stony Plain Road #JasperPlace
						})) //
				.compileBothTripSort());
		map2.put(803L, new RouteTripSpec(803L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ORMSBY_PL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BRUCE_SMITH) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5623", "5755", "5725" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(804L, new RouteTripSpec(804L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ARCH_MAC, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LA_PERLE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5548", "5012", "5024" })) //
				.compileBothTripSort());
		map2.put(805L, new RouteTripSpec(805L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL_TC) // LEWIS_FARMS_TC
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"5061", // 163 Street & 93 Avenue
								"5069", // ++
								"5002", // West Edmonton Mall Transit Centre
								"8601", "8603", "8610", // Lewis Farms Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(806L, new RouteTripSpec(806L, //
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
		map2.put(807L, new RouteTripSpec(807L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BERIAULT, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5894", "5001" })) //
				.compileBothTripSort());
		map2.put(808L, new RouteTripSpec(808L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, StringUtils.EMPTY) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"5577", // 163 Street & 92 Avenue
								"5457", // ==
								"-55552", // !=
								"5112", // != 157 Street & Stony Plain Road #JasperPlace =>
								"5111", // != Jasper Place Transit Centre =>
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(809L, new RouteTripSpec(809L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, HILLCREST, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5713", "5005" })) //
				.compileBothTripSort());
		map2.put(810L, new RouteTripSpec(810L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ST_ROSE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5611", "5001" })) //
				.compileBothTripSort());
		map2.put(811L, new RouteTripSpec(811L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, PARKVIEW, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5198", "5294", "5069", "5903", "5013" })) //
				.compileBothTripSort());
		map2.put(812L, new RouteTripSpec(812L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, PARKVIEW, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LA_PERLE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5656", "5011", "5024" })) //
				.compileBothTripSort());
		map2.put(814L, new RouteTripSpec(814L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ROSS_SHEPPARD, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5392", "5527", "5140", "5007" })) //
				.compileBothTripSort());
		map2.put(815L, new RouteTripSpec(815L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ARCH_MAC, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5548", "5005" })) //
				.compileBothTripSort());
		map2.put(817L, new RouteTripSpec(817L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BERIAULT, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5894", "5012" })) //
				.compileBothTripSort());
		map2.put(818L, new RouteTripSpec(818L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL_TC, // BERIAULT
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ORMSBY_PL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5718", "5725", "5004" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5894", "5004", "5755", "5828", "5718" })) //
				.compileBothTripSort());
		map2.put(819L, new RouteTripSpec(819L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT_TC, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"5203", // Westmount Transit Centre
								"5494", // ==
								"-55551", // !=
								"5106", // != 157 Street & 100A Avenue #JasperPlace
								"5102", // != Jasper Place Transit Centre
								"5246", // ==
								"5007", // West Edmonton Mall Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(820L, new RouteTripSpec(820L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LY_CAIRNS, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] {/* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2812", "2218" })) //
				.compileBothTripSort());
		map2.put(821L, new RouteTripSpec(821L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CRESTWOOD, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] {/* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5096", "5225", "5005" })) //
				.compileBothTripSort());
		map2.put(822L, new RouteTripSpec(822L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE_TC, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "1591", "1108", "1476", "7001" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "1532", "1104", "1426",
						/* + */"1050"/* + */, //
								"1142" })) //
				.compileBothTripSort());
		map2.put(824L, new RouteTripSpec(824L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, VICTORIA, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEWIS_FARMS_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1911", // 101 Street & Kingsway
								"5001", //
								"8601", "8605", // Lewis Farms Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(825L, new RouteTripSpec(825L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE_TC, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, VICTORIA) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "1554", "1237", "7002" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(826L, new RouteTripSpec(826L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MAGRATH) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2974", "4815", "4306", "4506" })) //
				.compileBothTripSort());
		map2.put(828L, new RouteTripSpec(828L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BROOKSIDE) // Ramsey Heights
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2974", "2707", "4021", "4034" })) //
				.compileBothTripSort());
		map2.put(829L, new RouteTripSpec(829L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2974", "4815" })) //
				.compileBothTripSort());
		map2.put(830L, new RouteTripSpec(830L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_NALLY, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2547", "1142" })) //
				.compileBothTripSort());
		map2.put(832L, new RouteTripSpec(832L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS_TC, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ARCH_MAC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5180", "6725", "6011" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(835L, new RouteTripSpec(835L, //
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
		map2.put(836L, new RouteTripSpec(836L, //
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
		map2.put(837L, new RouteTripSpec(837L, //
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
		map2.put(839L, new RouteTripSpec(839L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ARCH_MAC, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5548", "5132", "5038", "5013" })) //
				.compileBothTripSort());
		map2.put(840L, new RouteTripSpec(840L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS_TC, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"5207", // Westmount Transit Centre
								"1725", // ==
								"1867", // !=
								"1735", // !=
								"6122", // ==
								"6333", // ++
								"6002", // ++
								"6047", // ++
								"6001", // Castle Downs Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(841L, new RouteTripSpec(841L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "6285", "6317", "7003" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7011", "6314", "6009" })) //
				.compileBothTripSort());
		map2.put(842L, new RouteTripSpec(842L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ARCH_O_LEARY, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, OXFORD) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7120", "7496", "7060", "6348", "6243", "6337" })) //
				.compileBothTripSort());
		map2.put(843L, new RouteTripSpec(843L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ARCH_MAC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5442", "5445", "1881", "1322" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(844L, new RouteTripSpec(844L, //
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
		map2.put(845L, new RouteTripSpec(845L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7120", "7496", "7060", "7007", "7186", "7106" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7106", "7572", "7185", "7007" })) //
				.compileBothTripSort());
		map2.put(846L, new RouteTripSpec(846L, //
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
		map2.put(848L, new RouteTripSpec(848L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LAGO_LINDO, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CARDINAL_LEGER) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "7659", "6315", "7377", "7483" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(849L, new RouteTripSpec(849L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW_EAST_TC, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, QUEEN_ELIZABETH) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7358", "7209", "7823", "7943", "7269", "7101" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(851L, new RouteTripSpec(851L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, KLARVATTEN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CARDINAL_LEGER) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "7728", "7827", "7434" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(852L, new RouteTripSpec(852L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ARCH_O_LEARY) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "7496", "6130", "6522", "6011", "6127" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(853L, new RouteTripSpec(853L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BELVEDERE_TC, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NORTHGATE_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7585", "7204" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7496", "7008" })) //
				.compileBothTripSort());
		map2.put(855L, new RouteTripSpec(855L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ARCH_O_LEARY) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "7496", "6301", "6039", "6447" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(856L, new RouteTripSpec(856L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, JH_PICARD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2779", "2824", "1729" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(858L, new RouteTripSpec(858L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, STADIUM_TC, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, AMISKWACIY) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "1979", "1110", "1401" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(859L, new RouteTripSpec(859L, //
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
		map2.put(860L, new RouteTripSpec(860L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_NALLY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2676", "3230", "3217" })) //
				.compileBothTripSort());
		map2.put(861L, new RouteTripSpec(861L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_NALLY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2676", "3230", "3247", "3446" })) //
				.compileBothTripSort());
		map2.put(862L, new RouteTripSpec(862L, //
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
		map2.put(864L, new RouteTripSpec(864L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_NALLY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2676", "2196", "2393", "2188", "2385", "2103" })) //
				.compileBothTripSort());
		map2.put(865L, new RouteTripSpec(865L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, TD_BAKER, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3150", "3212" })) //
				.compileBothTripSort());
		map2.put(866L, new RouteTripSpec(866L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WP_WAGNER) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2418", // 80 Street & Wagner Road
								"2189", // 80 Street & Wagner Road
								"3204", // Mill Woods Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(867L, new RouteTripSpec(867L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WP_WAGNER) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2418", // 80 Street & Wagner Road
								"2189", // 80 Street & Wagner Road
								"3204", // Mill Woods Transit Centre
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						/* no stops */
						})) //
				.compileBothTripSort());
		map2.put(869L, new RouteTripSpec(869L, //
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
		map2.put(870L, new RouteTripSpec(870L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHWOOD, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LAKEWOOD) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "3002", "3204", "3142" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(871L, new RouteTripSpec(871L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BELLE_RIVE, // LAGO_LINDO
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MARY_BUTTERWORTH) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "6285", "7377", "7780", "7430" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(872L, new RouteTripSpec(872L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WP_WAGNER, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2418", "2103", "3003", "3214" })) //
				.compileBothTripSort());
		map2.put(873L, new RouteTripSpec(873L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, KENILWORTH, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WOODVALE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2160", "3461" })) //
				.compileBothTripSort());
		map2.put(874L, new RouteTripSpec(874L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WP_WAGNER, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2418", // 80 Street & Wagner Road
								"2189", // 80 Street & Wagner Road
								"3204", // Mill Woods Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(875L, new RouteTripSpec(875L, //
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
		map2.put(876L, new RouteTripSpec(876L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WP_WAGNER, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2189", "3203", "3356" })) //
				.compileBothTripSort());
		map2.put(877L, new RouteTripSpec(877L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, JH_PICARD, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2080", "2640", "2245", "3004", "3201" })) //
				.compileBothTripSort());
		map2.put(878L, new RouteTripSpec(878L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HARDISTY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2177", "3355", "3217" })) //
				.compileBothTripSort());
		map2.put(879L, new RouteTripSpec(879L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, OTTEWELL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE) // Mill Woods?
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2487", "2188", "2526", "2103" })) //
				.compileBothTripSort());
		map2.put(880L, new RouteTripSpec(880L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, KENILWORTH, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2160", "2188", "2105", "3529", "3211" })) //
				.compileBothTripSort());
		map2.put(881L, new RouteTripSpec(881L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CAPILANO, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_NALLY) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2151", "2301" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(882L, new RouteTripSpec(882L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, KENILWORTH, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2160", "2188", "2526", "2103", "3003", "3214" })) //
				.compileBothTripSort());
		map2.put(883L, new RouteTripSpec(883L, //
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
		map2.put(884L, new RouteTripSpec(884L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WP_WAGNER, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LAKEWOOD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2418", "3008", "3023", "3008" })) //
				.compileBothTripSort());
		map2.put(885L, new RouteTripSpec(885L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, VERNON_BARFORD, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4270", "4238", "4214" })) //
				.compileBothTripSort());
		map2.put(886L, new RouteTripSpec(886L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, AVALON, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2009", "2207" })) //
				.compileBothTripSort());
		map2.put(887L, new RouteTripSpec(887L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HARRY_AINLAY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, TWIN_BROOKS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4143", "4204", "4265", "4248" })) //
				.compileBothTripSort());
		map2.put(888L, new RouteTripSpec(888L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, VERNON_BARFORD) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "4270", "4238", "4205" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(889L, new RouteTripSpec(889L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2206", "4490", "4143", "4198", "4205", //
								"4290", "4203" })) //
				.compileBothTripSort());
		map2.put(890L, new RouteTripSpec(890L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2974", "2201" })) //
				.compileBothTripSort());
		map2.put(892L, new RouteTripSpec(892L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HARRY_AINLAY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4490", "4486", "4208" })) //
				.compileBothTripSort());
		map2.put(893L, new RouteTripSpec(893L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, HARRY_AINLAY) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "4490", "3004", "3217" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(894L, new RouteTripSpec(894L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA) // Allendale
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2741", "2974", "2102" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(895L, new RouteTripSpec(895L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2821", "2664", "2212" })) //
				.compileBothTripSort());
		map2.put(896L, new RouteTripSpec(896L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, RIVERBEND, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4021", "4803" })) //
				.compileBothTripSort());
		map2.put(897L, new RouteTripSpec(897L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LAKEWOOD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3002", "3214", "3740", "2110" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(898L, new RouteTripSpec(898L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_TC) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2974", "2102", "3001", "3217" })) //
				.compileBothTripSort());
		map2.put(899L, new RouteTripSpec(899L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "5991", "5061", "5069", "5903", "5012" })) //
				.compileBothTripSort());
		map2.put(901L, new RouteTripSpec(901L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LONDONDERRY, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, _142_ST_109_AVE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "5055", "5548", "7011", "6304", "7456" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(902L, new RouteTripSpec(902L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, PARKVIEW, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ORMSBY_PL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5656", "5611", "5755", "5828", "5725" })) //
				.compileBothTripSort());
		map2.put(903L, new RouteTripSpec(903L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, QUEEN_ELIZABETH) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7383", "7260", "7909" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(907L, new RouteTripSpec(907L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HADDOW) // Rhatigan Rdg
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2974", "4021", "4016" })) //
				.compileBothTripSort());
		map2.put(908L, new RouteTripSpec(908L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, FRASER, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, EASTGLEN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "1033", "7237" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(909L, new RouteTripSpec(909L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EASTGLEN) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "1185", "7120", "7009", "6315" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(913L, new RouteTripSpec(913L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5211", "7011", "6313" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(914L, new RouteTripSpec(914L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, OXFORD, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5207", "6328", "6337" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(916L, new RouteTripSpec(916L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BATURYN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"5206", "5207", // Westmount Transit Centre
								"1725", // ==
								"1759", // !=
								"1867", "1735", // !=
								"6122", // ==
								"6002", // Castle Downs Transit Centre
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(917L, new RouteTripSpec(917L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, FR_TROY, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JACKSON_HTS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3411", "3267" })) //
				.compileBothTripSort());
		map2.put(918L, new RouteTripSpec(918L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, FR_TROY, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JACKSON_HTS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3964", "3420" })) //
				.compileBothTripSort());
		map2.put(919L, new RouteTripSpec(919L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, EASTGLEN, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ABBOTTSFIELD) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1033", // 68 Street & 118 Avenue
								"1521", // ++
								"1002", // Abbottsfield Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(920L, new RouteTripSpec(920L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MINCHAU, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HOLY_FAMILY) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "3153", "3363" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(921L, new RouteTripSpec(921L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_NALLY, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SILVERBERRY) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2676", "3230", "3419" })) //
				.compileBothTripSort());
		map2.put(922L, new RouteTripSpec(922L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WESTMOUNT, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "5548", "4579", "4806" })) //
				.compileBothTripSort());
		map2.put(923L, new RouteTripSpec(923L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, TWIN_BROOKS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2204", "4490", "4204", "4265", "4248" })) //
				.compileBothTripSort());
		map2.put(924L, new RouteTripSpec(924L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DAN_KNOTT) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "3572", "3006", "3208" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(925L, new RouteTripSpec(925L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, STRATHCONA, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WINDSOR_PARK) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2974", "2844" })) //
				.compileBothTripSort());
		map2.put(926L, new RouteTripSpec(926L, //
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
								"2890", // 114 Street & 89 Avenue
								"2001", // University Transit Centre
								"2702", // South Campus Transit Centre Fort Edmonton Park
								"5296", // ++ 159 St & Whitemud Dr
								"5006", // West Edmonton Mall Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(931L, new RouteTripSpec(931L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LA_ZERTE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LAGO_LINDO) // KLARVATTEN
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7384", "7483" })) //
				.compileBothTripSort());
		map2.put(932L, new RouteTripSpec(932L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW_WEST_TC, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LA_ZERTE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "7384", "7241", "7604", "7901" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(934L, new RouteTripSpec(934L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LA_ZERTE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, CASTLE_DOWNS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "7572", "6311", "6008" })) //
				.compileBothTripSort());
		map2.put(935L, new RouteTripSpec(935L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HOLLICK_KENYON, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_LEOD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "7160", "7535", "7298", "7140" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(939L, new RouteTripSpec(939L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ELSINORE, // CHAMBERY
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MARY_BUTTERWORTH) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "6285", "6166", "6674" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(940L, new RouteTripSpec(940L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ABBOTTSFIELD, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, AMISKWACIY) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "1979", "1476", "1201", "1001" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(941L, new RouteTripSpec(941L, //
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
		map2.put(943L, new RouteTripSpec(943L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, BELVEDERE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, AOB) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2915", "1206", "7210" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(945L, new RouteTripSpec(945L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, EAUX_CLAIRES, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, _88_ST_132_AVE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "7496", "6315" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(948L, new RouteTripSpec(948L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, HILLCREST, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LA_PERLE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5713", "5011", "5024" })) //
				.compileBothTripSort());
		map2.put(949L, new RouteTripSpec(949L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, HILLCREST, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LA_PERLE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5713", "5012", "5024" })) //
				.compileBothTripSort());
		map2.put(950L, new RouteTripSpec(950L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, HILLCREST, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LYMBURN_ORMSBY_PL) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						/* no stops */
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"5713", // 164 Street & 80 Avenue
								"5894", //
								"5419", //
								"5725", // 183A Street & 76 Avenue
						})) //
				.compileBothTripSort());
		map2.put(952L, new RouteTripSpec(952L, //
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
		map2.put(953L, new RouteTripSpec(953L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LYMBURN) // ORMSBY_PL
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5355", "5004", "5755", "5828", "5725" })) //
				.compileBothTripSort());
		map2.put(954L, new RouteTripSpec(954L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEDGEWOOD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "5687", "5002", "5979", "5968" })) //
				.compileBothTripSort());
		map2.put(955L, new RouteTripSpec(955L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LA_PERLE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5355", "5011", "5024" })) //
				.compileBothTripSort());
		map2.put(956L, new RouteTripSpec(956L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, THE_GRANGE) // THE_HAMPTONS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5687", "8135", "8097", "8102" })) //
				.compileBothTripSort());
		map2.put(957L, new RouteTripSpec(957L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, OSCAR_ROMERO, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, THE_HAMPTONS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "5980", "5695", "8583", "8033", "8670" })) //
				.compileBothTripSort());
		map2.put(958L, new RouteTripSpec(958L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						/* no stops */
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"5355", // 163 Street & 92 Avenue
								"5583", // ++ 163 Street & 87 Avenue
								"5011", // West Edmonton Mall Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(959L, new RouteTripSpec(959L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL_TC, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, OSCAR_ROMERO) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "5695", "5002" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.compileBothTripSort());
		map2.put(960L, new RouteTripSpec(960L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WEST_EDM_MALL_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						/* no stops */
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"5355", // 163 Street & 92 Avenue
								"5583", // ++ 163 Street & 87 Avenue
								"5011", // West Edmonton Mall Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(961L, new RouteTripSpec(961L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, JASPER_PLACE, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEWIS_FARMS_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						/* no stops */
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"5355", // 163 Street & 92 Avenue #JasperPlaceSchool
								"5014", // West Edmonton Mall Transit Centre
								"8601", // Lewis Farms Transit Centre
						})) //
				.compileBothTripSort());
		map2.put(965L, new RouteTripSpec(965L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BRAEMAR, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2462", "1989" })) //
				.compileBothTripSort());
		map2.put(966L, new RouteTripSpec(966L, //
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
		map2.put(967L, new RouteTripSpec(967L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WHITEMUD_DR_53_AVE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4021", "4353", "4809" })) //
				.compileBothTripSort());
		map2.put(968L, new RouteTripSpec(968L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, ST_ROSE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "5611", "4579", "4806" })) //
				.compileBothTripSort());
		map2.put(969L, new RouteTripSpec(969L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WHITEMUD_DR_53_AVE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4021", "4129", "4804" })) //
				.compileBothTripSort());
		map2.put(970L, new RouteTripSpec(970L, //
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
		map2.put(971L, new RouteTripSpec(971L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, _84_ST_105_AVE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, _34_ST_35A_AVE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2676", "3355", "3708", "3185" })) //
				.compileBothTripSort());
		map2.put(972L, new RouteTripSpec(972L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MAC_EWAN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, RUTHERFORD) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "9251", "9848", "9685" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4213", "9666", "9242", "9251" })) //
				.compileBothTripSort());
		map2.put(973L, new RouteTripSpec(973L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MAGRATH, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LEGER_TC) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"4802", // Leger Transit Centre
								"4647", // ++
								"4110", // Magrath Road & 23 Avenue
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						/* no stops */
						})) //
				.compileBothTripSort());
		map2.put(974L, new RouteTripSpec(974L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, MC_PHERSON, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, BURNEWOOD) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "3603", "3420" })) //
				.compileBothTripSort());
		map2.put(975L, new RouteTripSpec(975L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILLGATE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS_) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2106", "3355", "3748", "3185", "3206" })) //
				.compileBothTripSort());
		map2.put(976L, new RouteTripSpec(976L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, RIVERBEND, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { /* no stops */})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "4021", "4803", "4202" })) //
				.compileBothTripSort());
		map2.put(977L, new RouteTripSpec(977L, //
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
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
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
