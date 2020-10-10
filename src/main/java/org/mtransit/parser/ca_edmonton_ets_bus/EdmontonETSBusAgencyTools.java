package org.mtransit.parser.ca_edmonton_ets_bus;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GIDs;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

// https://data.edmonton.ca/
// http://www.edmonton.ca/ets/ets-data-for-developers.aspx
// https://data.edmonton.ca/Transit/ETS-Bus-Schedule-GTFS-Data-Schedules-zipped-files/urjq-fvmq
// https://gtfs.edmonton.ca/TMGTFSRealTimeWebService/GTFS/GTFS.zip
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
		MTLog.log("Generating ETS bus data...");
		long start = System.currentTimeMillis();
		boolean isNext = "next_".equalsIgnoreCase(args[2]);
		if (isNext) {
			setupNext();
		}
		this.serviceIds = extractUsefulServiceIds(args, this, true);
		super.start(args);
		MTLog.log("Generating ETS bus data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	private void setupNext() {
		// DO NOTHING
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

	private static final int AGENCY_ID_INT = GIDs.getInt("1"); // Edmonton Transit Service ONLY

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		if (gRoute.isDifferentAgency(AGENCY_ID_INT)) {
			return true; // exclude
		}
		return super.excludeRoute(gRoute);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if ("Not In Service".equalsIgnoreCase(gTrip.getTripHeadsign())) {
			return true; // exclude
		}
		if ("Sorry Not In Service".equalsIgnoreCase(gTrip.getTripHeadsign())) {
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

	private static final String _SLASH_ = " / ";
	private static final String _AND_ = " & ";
	private static final String _AVE = " Ave";
	private static final String _ST = " St";
	private static final String TOWN_CENTER_SHORT = "TC";
	private static final String TRANSIT_CENTER_SHORT = "TC";
	private static final String EDMONTON_SHORT = "Edm";
	private static final String INDUSTRIAL_SHORT = "Ind";
	private static final String WEST_EDMONTON_MALL = "West " + EDMONTON_SHORT + " Mall"; // TODO ? "WEM"
	private static final String LEWIS_FARMS = "Lewis Farms";
	private static final String CAPILANO = "Capilano"; //
	private static final String CLAREVIEW = "Clareview";
	private static final String CROMDALE = "Cromdale";
	private static final String JASPER_PLACE = "Jasper Pl";
	private static final String COLISEUM = "Coliseum";
	private static final String WESTMOUNT = "Westmount";
	private static final String UNIVERSITY = "University";
	private static final String MILL_WOODS = "Mill Woods";
	private static final String NAIT = "NAIT";
	private static final String SOUTHGATE = "Southgate";
	private static final String NORTHGATE = "Northgate";
	private static final String ABBOTTSFIELD = "Abbottsfield";
	private static final String EAUX_CLAIRES = "Eaux Claires";
	private static final String DOWNTOWN = "Downtown";
	private static final String MILLGATE = "Millgate";
	private static final String GOVERNMENT_SHORT = "Gov";
	private static final String GOV_CTR = GOVERNMENT_SHORT + " Ctr";
	private static final String CASTLE_DOWNS = "Castle Downs";
	private static final String CENTURY_PK = "Century Pk";
	private static final String YELLOWBIRD = "Yellowbird";
	private static final String SOUTH_CAMPUS = "South Campus";
	private static final String LEGER = "Leger";
	private static final String MEADOWS = "Mdws"; // "Meadows";
	private static final String HARRY_AINLAY = "Harry Ainlay";
	private static final String RUTHERFORD = "Rutherford";
	private static final String SOUTHWOOD = "Southwood";
	private static final String PARKALLEN = "Parkallen";
	private static final String KNOTTWOOD = "Knottwood";
	private static final String BELVEDERE = "Belvedere";
	private static final String BONNIE_DOON = "Bonnie Doon";
	private static final String TAMARACK = "Tamarack";
	private static final String LESSARD = "Lessard";
	private static final String LYMBURN = "Lymburn";
	private static final String CARLTON = "Carlton";
	private static final String WEDGEWOOD = "Wedgewood";
	private static final String MISTATIM_IND = "Mistatim " + INDUSTRIAL_SHORT;
	private static final String STADIUM = "Stadium";
	private static final String LAGO_LINDO = "Lago Lindo";
	private static final String MONTROSE = "Montrose";
	private static final String KINGSWAY = "Kingsway";
	private static final String KING_EDWARD_PK = "King Edward Pk";
	private static final String LONDONDERRY = "Londonderry";
	private static final String EVERGREEN = "Evergreen";
	private static final String FRASER = "Fraser";
	private static final String SPRUCE_GRV = "Spruce Grv";
	private static final String KLARVATTEN = "Klarvatten";
	private static final String RIVERDALE = "Riverdale";
	private static final String WINTERBURN = "Winterburn";
	private static final String WINTERBURN_IND = WINTERBURN + " " + INDUSTRIAL_SHORT;
	private static final String STRATHCONA = "Strathcona";
	private static final String WINDSOR_PARK = "Windsor Pk";
	private static final String BELGRAVIA = "Belgravia";
	private static final String SILVERBERRY = "Silverberry";
	private static final String LAKEWOOD = "Lakewood";
	private static final String LAUDERDALE = "Lauderdale";
	private static final String CALDER = "Calder";
	private static final String HENWOOD = "Henwood";
	private static final String ST_ALBERT_TRAIL = "St Albert Trl";
	private static final String JASPER_GATE = "Jasper Gt";
	private static final String MAPLE_RIDGE = "Maple Rdg";
	private static final String SECORD = "Secord";
	private static final String S_ = "S ";
	private static final String ROSSDALE = "Rossdale";
	private static final String NORTH = "North";
	private static final String HIGHLANDS = "Highlands";
	private static final String LAUREL = "Laurel";
	private static final String SCHOOL_SPECIAL = "School Special";

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
		throw new MTLog.Fatal("Unexpected routes to merge: %s & %s!", mRoute, mRouteToMerge);
	}

	@Override
	public String getRouteShortName(GRoute gRoute) {
		if (Utils.isDigitsOnly(gRoute.getRouteShortName())) {
			return gRoute.getRouteShortName();
		}
		if (Utils.isDigitsOnly(gRoute.getRouteId())) {
			return gRoute.getRouteId();
		}
		throw new MTLog.Fatal("Unexpected route ID for %s!", gRoute.toStringPlus());
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
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		mTrip.setHeadsignString(cleanTripHeadsign2(gTrip.getTripHeadsign()), gTrip.getDirectionId());
	}

	private static final String NB_ = ""; // TODO "NB ";
	private static final String SB_ = ""; // TODO "SB ";
	private static final String WB_ = ""; // TODO "WB ";
	private static final String EB_ = ""; // TODO "EB ";

	private static final String CW_ = ""; // TODO "CW ";
	private static final String CCW_ = ""; // TODO "CW ";

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		if (mTrip.getHeadsignValue().startsWith(S_)) {
			if (!mTripToMerge.getHeadsignValue().startsWith(S_)) {
				mTrip.setHeadsignString(mTripToMerge.getHeadsignValue(), mTrip.getHeadsignId());
				return true;
			}
		} else if (mTripToMerge.getHeadsignValue().startsWith(S_)) {
			mTrip.setHeadsignString(mTrip.getHeadsignValue(), mTrip.getHeadsignId());
			return true;
		}
		String tripHeadsignValue = mTrip.getHeadsignValue();
		if (tripHeadsignValue.startsWith(S_)) {
			tripHeadsignValue = tripHeadsignValue.substring(S_.length());
		}
		String tripToMergeHeadsignValue = mTripToMerge.getHeadsignValue();
		if (tripToMergeHeadsignValue.startsWith(S_)) {
			tripToMergeHeadsignValue = tripToMergeHeadsignValue.substring(S_.length());
		}
		List<String> headSignsValues = Arrays.asList(
				tripHeadsignValue,
				tripToMergeHeadsignValue
		);
		if (mTrip.getRouteId() == 1L) {
			if (Arrays.asList( //
					EB_ + DOWNTOWN, //
					EB_ + CAPILANO //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(EB_ + CAPILANO, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					WB_ + CAPILANO, // <>
					WB_ + WEST_EDMONTON_MALL //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(WB_ + WEST_EDMONTON_MALL, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 2L) {
			if (Arrays.asList( //
					WB_ + WEST_EDMONTON_MALL, // <>
					WB_ + LESSARD //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(WB_ + LESSARD, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					EB_ + WEST_EDMONTON_MALL, // <>
					EB_ + HIGHLANDS, //
					EB_ + DOWNTOWN, //
					EB_ + CLAREVIEW //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(EB_ + CLAREVIEW, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 3L) {
			if (Arrays.asList( //
					EB_ + DOWNTOWN, //
					EB_ + CROMDALE //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(EB_ + CROMDALE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 4L) {
			if (Arrays.asList( //
					EB_ + BONNIE_DOON, //
					EB_ + SOUTH_CAMPUS, //
					EB_ + UNIVERSITY, // <>
					EB_ + CAPILANO //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(EB_ + CAPILANO, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					WB_ + WEST_EDMONTON_MALL, //
					WB_ + UNIVERSITY, // <>
					WB_ + LEWIS_FARMS //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(WB_ + LEWIS_FARMS, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 5L) {
			if (Arrays.asList( //
					EB_ + DOWNTOWN, //
					EB_ + COLISEUM //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(EB_ + COLISEUM, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 6L) {
			if (Arrays.asList( //
					EB_ + MILLGATE, //
					EB_ + MILL_WOODS //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(EB_ + MILL_WOODS, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 7L) {
			if (Arrays.asList( //
					NB_ + "MacEwan University", //
					NB_ + DOWNTOWN, //
					NB_ + JASPER_PLACE //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(NB_ + JASPER_PLACE, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					SB_ + DOWNTOWN, //
					SB_ + UNIVERSITY //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(SB_ + UNIVERSITY, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 8L) {
			if (Arrays.asList( //
					NB_ + COLISEUM, // <>
					NB_ + DOWNTOWN, // <>
					NB_ + KINGSWAY, // <>
					NB_ + MILLGATE, // <>
					NB_ + NAIT, //
					NB_ + NORTH, //
					NB_ + ABBOTTSFIELD //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(NB_ + ABBOTTSFIELD, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					SB_ + DOWNTOWN, // <>
					SB_ + KINGSWAY, // <>
					SB_ + MILLGATE, // <>
					SB_ + COLISEUM, // <>
					SB_ + "Kingsway RAH BNR", //
					SB_ + LAKEWOOD, //
					SB_ + BONNIE_DOON, //
					SB_ + MILL_WOODS //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(SB_ + MILL_WOODS, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 9L) {
			if (Arrays.asList( //
					SB_ + DOWNTOWN, // <>
					SB_ + KINGSWAY, //
					SB_ + SOUTHGATE, //
					SB_ + CENTURY_PK //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(SB_ + CENTURY_PK, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					NB_ + DOWNTOWN, // <>
					NB_ + NORTHGATE, //
					NB_ + NAIT, //
					NB_ + EAUX_CLAIRES //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(NB_ + EAUX_CLAIRES, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 10L) {
			if (Arrays.asList( //
					NB_ + BELVEDERE, // <>
					NB_ + CLAREVIEW //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(NB_ + CLAREVIEW, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					SB_ + BELVEDERE, // <>
					SB_ + COLISEUM //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(SB_ + COLISEUM, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 11L) {
			if (Arrays.asList( //
					WB_ + "88" + _ST + _AND_ + "132" + _AVE, //
					WB_ + NORTHGATE //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(WB_ + NORTHGATE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 12L) {
			if (Arrays.asList( //
					SB_ + DOWNTOWN, //
					SB_ + KINGSWAY //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(SB_ + KINGSWAY, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 14L) {
			if (Arrays.asList( //
					EB_ + JASPER_PLACE, //
					EB_ + DOWNTOWN //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(EB_ + DOWNTOWN, mTrip.getHeadsignId()); // Jasper Pl
				return true;
			}
		} else if (mTrip.getRouteId() == 15L) {
			if (Arrays.asList( //
					NB_ + DOWNTOWN, // <>
					NB_ + NAIT, //
					NB_ + EAUX_CLAIRES //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(NB_ + EAUX_CLAIRES, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					SB_ + DOWNTOWN, // <>
					SB_ + MILLGATE, //
					SB_ + MILL_WOODS //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(SB_ + MILL_WOODS, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 16L) {
			if (Arrays.asList( //
					NB_ + EAUX_CLAIRES, // <>
					NB_ + NORTHGATE, // <>
					NB_ + CASTLE_DOWNS //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(NB_ + CASTLE_DOWNS, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					SB_ + EAUX_CLAIRES, // <>
					SB_ + NORTHGATE, // <>
					SB_ + GOV_CTR //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(SB_ + GOV_CTR, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 23L) {
			if (Arrays.asList( //
					WB_ + CENTURY_PK, // <>
					WB_ + SCHOOL_SPECIAL, //
					WB_ + LEGER, //
					WB_ + WEST_EDMONTON_MALL //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(WB_ + WEST_EDMONTON_MALL, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					EB_ + CENTURY_PK, // <>
					EB_ + MILL_WOODS //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(EB_ + MILL_WOODS, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 30L) {
			if (Arrays.asList( //
					SB_ + LEGER, //
					SB_ + CENTURY_PK //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(SB_ + CENTURY_PK, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					NB_ + SCHOOL_SPECIAL, //
					NB_ + SOUTH_CAMPUS //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(NB_ + SOUTH_CAMPUS, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 33L) {
			if (Arrays.asList( //
					EB_ + SOUTHGATE, //
					EB_ + MEADOWS //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(EB_ + MEADOWS, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 34L) {
			if (Arrays.asList( //
					EB_ + "Riverbend", //
					EB_ + SOUTHGATE //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(EB_ + SOUTHGATE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 36L) {
			if (Arrays.asList( //
					SB_ + LEGER, //
					SB_ + CENTURY_PK //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(SB_ + CENTURY_PK, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 39L) {
			if (Arrays.asList( //
					CW_ + RUTHERFORD, // <>
					CW_ + CENTURY_PK //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(CW_ + CENTURY_PK, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 40L) {
			if (Arrays.asList( //
					SB_ + YELLOWBIRD, //
					SB_ + CENTURY_PK //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(SB_ + CENTURY_PK, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 44L) {
			if (Arrays.asList( //
					NB_ + CENTURY_PK, //
					NB_ + SOUTHGATE //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(NB_ + SOUTHGATE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 46L) {
			if (Arrays.asList( //
					CW_ + CENTURY_PK, // <>
					CW_ + YELLOWBIRD, // <>
					CW_ + YELLOWBIRD + _SLASH_ + CENTURY_PK // ++
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(CW_ + YELLOWBIRD + _SLASH_ + CENTURY_PK, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					CW_ + CENTURY_PK, //  <>
					CW_ + YELLOWBIRD, // <>
					CW_ + HARRY_AINLAY, //
					CW_ + YELLOWBIRD + _SLASH_ + CENTURY_PK, // ++ <>
					CW_ + HARRY_AINLAY + _SLASH_ + YELLOWBIRD + _SLASH_ + CENTURY_PK // ++
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(CW_ + HARRY_AINLAY + _SLASH_ + YELLOWBIRD + _SLASH_ + CENTURY_PK, mTrip.getHeadsignId()); // Clockwise
				return true;
			}
		} else if (mTrip.getRouteId() == 51L) {
			if (Arrays.asList( //
					NB_ + PARKALLEN, // <>
					NB_ + UNIVERSITY //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(NB_ + UNIVERSITY, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 52L) {
			if (Arrays.asList( //
					NB_ + ROSSDALE, //
					NB_ + STRATHCONA, //
					NB_ + DOWNTOWN //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(NB_ + DOWNTOWN, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					SB_ + DOWNTOWN, // <>
					SB_ + GOV_CTR, //
					SB_ + SOUTHGATE //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(SB_ + SOUTHGATE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 54L) {
			if (Arrays.asList( //
					SOUTH_CAMPUS, // <>
					UNIVERSITY //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(UNIVERSITY, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 55L) {
			if (Arrays.asList( //
					SB_ + SOUTH_CAMPUS, // <>
					SB_ + SOUTHGATE //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(SB_ + SOUTHGATE, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					NB_ + BELGRAVIA, //
					NB_ + SOUTH_CAMPUS //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(NB_ + SOUTH_CAMPUS, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 60L) {
			if (Arrays.asList( //
					NB_ + DOWNTOWN, //
					NB_ + MILLGATE //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(NB_ + MILLGATE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 61L) {
			if (Arrays.asList( //
					NB_ + DOWNTOWN, //
					NB_ + MILLGATE //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(NB_ + MILLGATE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 62L) {
			if (Arrays.asList( //
					CCW_ + MILL_WOODS, //
					CCW_ + SOUTHWOOD //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(CCW_ + SOUTHWOOD, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 63L) {
			if (Arrays.asList( //
					CCW_ + MILL_WOODS, //
					CCW_ + SOUTHWOOD //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(CCW_ + SOUTHWOOD, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 65L) {
			if (Arrays.asList( //
					CCW_ + KNOTTWOOD, // <>
					CCW_ + MILL_WOODS //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(CCW_ + MILL_WOODS, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 66L) {
			if (Arrays.asList( //
					NB_ + DOWNTOWN, //
					NB_ + MILLGATE //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(NB_ + MILLGATE, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					SB_ + LAKEWOOD, //
					SB_ + MILL_WOODS //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(SB_ + MILL_WOODS, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 67L) {
			if (Arrays.asList( //
					CW_ + MEADOWS, //
					CW_ + SILVERBERRY, //
					CW_ + MILL_WOODS //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(CW_ + MILL_WOODS, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 68L) {
			if (Arrays.asList( //
					NB_ + DOWNTOWN, //
					NB_ + MILLGATE //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(NB_ + MILLGATE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 69L) {
			if (Arrays.asList( //
					NB_ + DOWNTOWN, //
					NB_ + MILLGATE //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(NB_ + MILLGATE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 70L) {
			if (Arrays.asList( //
					NB_ + STRATHCONA, //
					NB_ + DOWNTOWN // <>
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(NB_ + DOWNTOWN, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					SB_+ DOWNTOWN, // <>
					SB_+ MILL_WOODS //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(SB_+ MILL_WOODS, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 74L) {
			if (Arrays.asList( //
					WB_ + CENTURY_PK, //
					WB_ + SOUTHGATE //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(WB_ + SOUTHGATE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 81L) {
			if (Arrays.asList( //
					NB_ + STRATHCONA, //
					NB_ + DOWNTOWN //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(NB_ + DOWNTOWN, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					SB_ + MILLGATE, //
					SB_ + MILL_WOODS //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(SB_ + MILL_WOODS, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 87L) {
			if (Arrays.asList( //
					SB_ + STRATHCONA, //
					SB_ + MILLGATE //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(SB_ + MILLGATE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 89L) {
			if (Arrays.asList( //
					CCW_ + MEADOWS, //
					CCW_ + TAMARACK, //
					CCW_ + MEADOWS + _SLASH_ + TAMARACK // ++
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(CCW_ + MEADOWS + _SLASH_ + TAMARACK, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 95L) {
			if (Arrays.asList( //
					EB_ + LAUREL, //
					EB_ + MEADOWS //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(EB_ + MEADOWS, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 99L) {
			if (Arrays.asList( //
					SB_ + COLISEUM, // <>
					SB_ + CAPILANO //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(SB_ + CAPILANO, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					NB_ + COLISEUM, // <>
					NB_ + BELVEDERE //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(NB_ + BELVEDERE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 100L) {
			if (Arrays.asList( //
					WEST_EDMONTON_MALL, //
					LEWIS_FARMS //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(LEWIS_FARMS, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 102L) {
			if (Arrays.asList( //
					LYMBURN, //
					WEST_EDMONTON_MALL //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(WEST_EDMONTON_MALL, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 106L) {
			if (Arrays.asList( //
					WEST_EDMONTON_MALL, // <>
					LESSARD // <>
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(LESSARD, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					LESSARD, // <>
					WEST_EDMONTON_MALL, // <>
					UNIVERSITY //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(UNIVERSITY, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 109L) {
			if (Arrays.asList( //
					JASPER_PLACE, //
					DOWNTOWN //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(DOWNTOWN, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 111L) {
			if (Arrays.asList( //
					JASPER_PLACE, //
					DOWNTOWN //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(DOWNTOWN, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 112L) {
			if (Arrays.asList( //
					DOWNTOWN, // <>
					WEST_EDMONTON_MALL //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(WEST_EDMONTON_MALL, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					DOWNTOWN, // <>
					CAPILANO //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(CAPILANO, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 117L) {
			if (Arrays.asList( //
					LEWIS_FARMS, //
					WEST_EDMONTON_MALL //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(WEST_EDMONTON_MALL, mTrip.getHeadsignId()); // Lewis Farms
				return true;
			}
		} else if (mTrip.getRouteId() == 119L) {
			if (Arrays.asList( //
					"The Hamptons", // <>
					"Lewis Farms" //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString("Lewis Farms", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					"Oscar Romero", //
					"The Hamptons" // <>
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString("The Hamptons", mTrip.getHeadsignId());
				return true;
			}
			// headsignValue=The Hamptons) & MTrip(routeId=119, headsignId=0, headsignType=0, headsignValue=
			// Lewis Farms)!
		} else if (mTrip.getRouteId() == 125L) {
			if (Arrays.asList( //
					WESTMOUNT, // <>
					JASPER_PLACE //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(JASPER_PLACE, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					JASPER_PLACE, // <>
					WESTMOUNT, // <>
					STADIUM, //
					COLISEUM, //
					KINGSWAY, //
					BELVEDERE //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(BELVEDERE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 130L) {
			if (Arrays.asList( //
					NAIT, //
					NORTHGATE //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(NORTHGATE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 137L) {
			if (Arrays.asList( //
					NORTHGATE, //
					CLAREVIEW //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(CLAREVIEW, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 140L) {
			if (Arrays.asList( //
					NORTHGATE, // <>
					DOWNTOWN //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(DOWNTOWN, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					LAGO_LINDO, //
					NORTHGATE // <>
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(NORTHGATE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 143L) {
			if (Arrays.asList( //
					COLISEUM, // <>
					KINGSWAY //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(KINGSWAY, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					COLISEUM, // <>
					MONTROSE //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(MONTROSE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 145L) {
			if (Arrays.asList( //
					"88" + _ST + _AND_ + "132" + _AVE, //
					EAUX_CLAIRES //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(EAUX_CLAIRES, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					EAUX_CLAIRES, //
					LAGO_LINDO //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(LAGO_LINDO, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 150L) {
			if (Arrays.asList( //
					JASPER_PLACE, // <>
					WESTMOUNT, //
					NORTHGATE, //
					EAUX_CLAIRES //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(EAUX_CLAIRES, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					JASPER_PLACE, // <>
					WEST_EDMONTON_MALL //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(WEST_EDMONTON_MALL, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 151L) {
			if (Arrays.asList( //
					CALDER, // <>
					LAUDERDALE, // <>
					DOWNTOWN, // <>
					NAIT, //
					CASTLE_DOWNS //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(CASTLE_DOWNS, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					CALDER, // <>
					LAUDERDALE, // <>
					DOWNTOWN, // <>
					KING_EDWARD_PK //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(KING_EDWARD_PK, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 153L) {
			if (Arrays.asList( //
					CLAREVIEW, //
					BELVEDERE //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(BELVEDERE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 161L) {
			if (Arrays.asList( //
					DOWNTOWN, //
					GOV_CTR, //
					NORTHGATE //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(NORTHGATE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 162L) {
			if (Arrays.asList( //
					EAUX_CLAIRES, // <>
					CASTLE_DOWNS // <>
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(CASTLE_DOWNS, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					CASTLE_DOWNS, // <>
					DOWNTOWN, //
					GOV_CTR, //
					EAUX_CLAIRES // <>
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(EAUX_CLAIRES, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 168L) {
			if (Arrays.asList( //
					CARLTON, //
					EAUX_CLAIRES //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(EAUX_CLAIRES, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 180L) {
			if (Arrays.asList( //
					BELVEDERE, // <>
					ABBOTTSFIELD //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(ABBOTTSFIELD, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					BELVEDERE, // <>
					EAUX_CLAIRES //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(EAUX_CLAIRES, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 181L) {
			if (Arrays.asList( //
					LONDONDERRY, //
					BELVEDERE //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(BELVEDERE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 182L) {
			if (Arrays.asList( //
					CLAREVIEW, // <>
					NORTHGATE //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(NORTHGATE, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					CLAREVIEW, // <>
					FRASER //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(FRASER, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 186L) {
			if (Arrays.asList( //
					CLAREVIEW, //
					NORTHGATE //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(NORTHGATE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 187L) {
			if (Arrays.asList( //
					CLAREVIEW, //
					LONDONDERRY //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(LONDONDERRY, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 188L) {
			if (Arrays.asList( //
					LONDONDERRY, // <>
					EAUX_CLAIRES //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(EAUX_CLAIRES, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					LONDONDERRY, // <>
					CLAREVIEW //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(CLAREVIEW, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 191L) {
			if (Arrays.asList( //
					KLARVATTEN, // <>
					EAUX_CLAIRES //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(EAUX_CLAIRES, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 302L) {
			if (Arrays.asList( //
					HENWOOD, // <>
					CLAREVIEW //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(CLAREVIEW, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					HENWOOD, // <>
					EVERGREEN //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(EVERGREEN, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 303L) {
			if (Arrays.asList( //
					ST_ALBERT_TRAIL, //
					MISTATIM_IND //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(MISTATIM_IND, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 305L) {
			if (Arrays.asList( //
					WESTMOUNT, // <>
					JASPER_GATE //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(JASPER_GATE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 306L) {
			if (Arrays.asList( //
					BONNIE_DOON, //
					MAPLE_RIDGE //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(MAPLE_RIDGE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 309L) {
			if (Arrays.asList( //
					DOWNTOWN, //
					RIVERDALE //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(RIVERDALE, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 315L) {
			if (Arrays.asList( //
					SECORD, //
					WINTERBURN_IND //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(WINTERBURN_IND, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 318L) {
			if (Arrays.asList( //
					UNIVERSITY, // <>
					BELGRAVIA //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(BELGRAVIA, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					UNIVERSITY, // <>
					WINDSOR_PARK //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(WINDSOR_PARK, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 562L) {
			if (Arrays.asList( //
					WEST_EDMONTON_MALL, // <>
					"WEM" + _AND_ + SOUTH_CAMPUS //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString("WEM" + _AND_ + SOUTH_CAMPUS, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 594L) {
			if (Arrays.asList( //
					LEWIS_FARMS, //
					WEST_EDMONTON_MALL //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString(WEST_EDMONTON_MALL, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					WEST_EDMONTON_MALL,
					"Vly Zoo" //
			).containsAll(headSignsValues)) {
				mTrip.setHeadsignString("Vly Zoo", mTrip.getHeadsignId());
				return true;
			}
		}
		throw new MTLog.Fatal("Unexpected trips to merge: %s & %s!", mTrip, mTripToMerge);
	}

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;

	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<>();
		map2.put(64L, new RouteTripSpec(64L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Knottwood", //
				1, MTrip.HEADSIGN_TYPE_STRING, MILL_WOODS) //
				.addTripSort(0, //
						Arrays.asList( //
								"3208", // xx Mill Woods Transit Centre <=
								"3122", // != Hewes Way & 27 Avenue
								"3264", // != 67 Street & 28 Avenue
								"3009", // xx Lakewood Transit Centre <=
								"3365", // != Mill Woods Road & Lakewood Road S
								"3026" // Mill Woods Road & Knottwood Road N =>
						)) //
				.addTripSort(1, //
						Arrays.asList( //
								"3026", // Mill Woods Road & Knottwood Road N <=
								"3330", // == != Mill Woods Road & Lakewood Road S
								"3009", // != xx Lakewood Transit Centre =>
								"3006", // != Lakewood Transit Centre
								"3121", // !=
								"3208" // xx Mill Woods Transit Centre =>
						)) //
				.compileBothTripSort());
		map2.put(101L, new RouteTripSpec(101L, //
				0, MTrip.HEADSIGN_TYPE_STRING, WEST_EDMONTON_MALL, //
				1, MTrip.HEADSIGN_TYPE_STRING, WEDGEWOOD) //
				.addTripSort(0, //
						Arrays.asList(//
								"5660", // <> 187 Street & 52 Avenue #WedgeWood
								"5968", // <> Wedgewood Boulevard Loop
								"5888", // !=
								"5908", // 187 Street & 52 Avenue
								"5003" // West Edmonton Mall Transit Centre
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"5687", // 163 Street & 91 Avenue #WEM
								"5003", // == West Edmonton Mall Transit Centre
								"5863", // !=
								"5721", // ==
								"5834", // ++
								"5673", // ==
								"5412", // !=
								"5891", // !=
								"5948", // ==
								"5856", // !=
								"5660", // <> 187 Street & 52 Avenue #WedgeWood
								"5968" // <> Wedgewood Boulevard Loop
						)) //
				.compileBothTripSort());
		map2.put(560L, new RouteTripSpec(560L, //
				0, MTrip.HEADSIGN_TYPE_STRING, EDMONTON_SHORT, // DOWNTOWN
				1, MTrip.HEADSIGN_TYPE_STRING, SPRUCE_GRV) //
				.addTripSort(0, //
						Arrays.asList(//
								"8584", // Jennifer Heil Way & TransAlta Tri Leisure Centre #SpruceGrove
								"8761", // Century Road & Grove Drive
								"8910", // == Century Road & Vanderbilt Common
								"5415", // != 154 Street & 119 Avenue =>
								"1223", // ==
								"1226", // !=
								"1113", // <> Kingsway RAH Transit Centre =>
								"1989" // <> 108 Street & 104 Avenue #Edmonton =>
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"1113", // <> Kingsway RAH Transit Centre <=
								"1989", // <> 108 Street & 104 Avenue #Edmonton <=
								"1899", // !=
								"1227", // ==
								"5389", // != 154 Street & 118 Avenue <=
								"8371", // Century Road & Kings Link ==
								"8584" // Jennifer Heil Way & TransAlta Tri Leisure Centre #SpruceGrove
						)) //
				.compileBothTripSort());
		map2.put(804L, new RouteTripSpec(804L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Avalon", //
				1, MTrip.HEADSIGN_TYPE_STRING, "Southgate") //
				.addTripSort(0, //
						Arrays.asList(//
								// NO STOPS
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"2009", // 114 Street & 54A Avenue
								"2996", // ++
								"2949", // ++
								"2207" // Southgate TC
						)) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	private static final Pattern N_A_I_T = Pattern.compile("((^|\\W)(n a i t)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String N_A_I_T_REPLACEMENT = "$2" + NAIT + "$4";

	private static final Pattern SUPER_EXPRESS = Pattern.compile("((^|\\W)(super express)(\\W|$))", Pattern.CASE_INSENSITIVE);

	private static final Pattern STARTS_WITH_RSN = Pattern.compile("(^[\\d]+( )?)", Pattern.CASE_INSENSITIVE);

	private static final Pattern BOUNDS_ = Pattern.compile("((^|\\W)(eastbound|westbound|southbound|northbound)(\\W|$))", Pattern.CASE_INSENSITIVE);

	private static final Pattern CLOCKWISE_ = Pattern.compile("((^|\\W)(clockwise)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String CLOCKWISE_REPLACEMENT = "$2" + "CW" + "$4";

	private static final Pattern COUNTERCLOCKWISE_ = Pattern.compile("((^|\\W)(counterclockwise)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String COUNTERCLOCKWISE_REPLACEMENT = "$2" + "CCW" + "$4";

	private String cleanTripHeadsign2(String tripHeadsign) {
		tripHeadsign = CleanUtils.removeVia(tripHeadsign);
		tripHeadsign = BOUNDS_.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY); // TODO CleanUtils.cleanBounds(tripHeadsign);
		tripHeadsign = CLOCKWISE_.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY); // TODO CLOCKWISE_REPLACEMENT
		tripHeadsign = COUNTERCLOCKWISE_.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY); // TODO COUNTERCLOCKWISE_REPLACEMENT
		tripHeadsign = STARTS_WITH_RSN.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = TRANSIT_CENTER.matcher(tripHeadsign).replaceAll(TRANSIT_CENTER_REPLACEMENT);
		tripHeadsign = TOWN_CENTER.matcher(tripHeadsign).replaceAll(TOWN_CENTER_REPLACEMENT);
		tripHeadsign = SUPER_EXPRESS.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = INTERNATIONAL.matcher(tripHeadsign).replaceAll(INTERNATIONAL_REPLACEMENT);
		tripHeadsign = GOVERNMENT_.matcher(tripHeadsign).replaceAll(GOVERNMENT_REPLACEMENT);
		tripHeadsign = BELVEDERE_.matcher(tripHeadsign).replaceAll(BELVEDERE_REPLACEMENT);
		tripHeadsign = INDUSTRIAL_.matcher(tripHeadsign).replaceAll(INDUSTRIAL_REPLACEMENT);
		tripHeadsign = EDMONTON.matcher(tripHeadsign).replaceAll(EDMONTON_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		tripHeadsign = CleanUtils.removePoints(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		// !ONLY USED FOR stop trip head sign!
		tripHeadsign = CleanUtils.removeVia(tripHeadsign);
		tripHeadsign = STARTS_WITH_RSN.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = TRANSIT_CENTER.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = TOWN_CENTER.matcher(tripHeadsign).replaceAll(TOWN_CENTER_REPLACEMENT);
		tripHeadsign = SUPER_EXPRESS.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = EDMONTON.matcher(tripHeadsign).replaceAll(EDMONTON_REPLACEMENT);
		tripHeadsign = N_A_I_T.matcher(tripHeadsign).replaceAll(N_A_I_T_REPLACEMENT);
		tripHeadsign = CLOCKWISE_.matcher(tripHeadsign).replaceAll(CLOCKWISE_REPLACEMENT);
		tripHeadsign = COUNTERCLOCKWISE_.matcher(tripHeadsign).replaceAll(COUNTERCLOCKWISE_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanBounds(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		tripHeadsign = CleanUtils.removePoints(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern TRANSIT_CENTER = Pattern.compile("((^|\\W)(transit center|transit centre)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String TRANSIT_CENTER_REPLACEMENT = "$2" + TRANSIT_CENTER_SHORT + "$4";

	private static final Pattern TOWN_CENTER = Pattern.compile("((^|\\W)(town center|town centre)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String TOWN_CENTER_REPLACEMENT = "$2" + TOWN_CENTER_SHORT + "$4";

	private static final Pattern INTERNATIONAL = Pattern.compile("((^|\\W)(international)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String INTERNATIONAL_REPLACEMENT = "$2" + "Int" + "$4";

	private static final Pattern GOVERNMENT_ = Pattern.compile("((^|\\W)(government)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String GOVERNMENT_REPLACEMENT = "$2" + GOVERNMENT_SHORT + "$4";

	private static final Pattern BELVEDERE_ = Pattern.compile("((^|\\W)(belevedere)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String BELVEDERE_REPLACEMENT = "$2" + BELVEDERE + "$4";

	private static final Pattern INDUSTRIAL_ = Pattern.compile("((^|\\W)(industrial)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String INDUSTRIAL_REPLACEMENT = "$2" + INDUSTRIAL_SHORT + "$4";

	private static final Pattern EDMONTON = Pattern.compile("((^|\\W)(edmonton)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String EDMONTON_REPLACEMENT = "$2" + EDMONTON_SHORT + "$4";

	@Override
	public String cleanStopName(String gStopName) {
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
	public int getStopId(GStop gStop) {
		return Math.abs(super.getStopId(gStop)); // remove negative stop IDs
	}

	private static final Pattern REMOVE_STARTING_DASH = Pattern.compile("(^-)", Pattern.CASE_INSENSITIVE);

	@Override
	public String getStopCode(GStop gStop) {
		String stopCode = super.getStopCode(gStop); // do not change, used by real-time API
		stopCode = REMOVE_STARTING_DASH.matcher(stopCode).replaceAll(StringUtils.EMPTY);
		return stopCode; // do not change, used by real-time API
	}
}
